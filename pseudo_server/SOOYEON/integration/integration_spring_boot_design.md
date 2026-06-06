# 통합 연동 — Spring Boot 설계 보강

`integration.md`의 의사코드를 Spring Boot 계층·어노테이션으로 배치하는 문서입니다.

---

## 계층 구조 한눈에 보기

```
[딜 생성] ──DealCreatedEvent──► DealCreatedEventHandler  (@TransactionalEventListener AFTER_COMMIT)
                                  ├─ DealReadModelRepository.save()
                                  └─ StockInitializer.init()          ← QUANTITY만

DealClosingScheduler  (@Scheduled fixedDelay=60s)
  └─ DealLifecycleService.closeExpiredDeals()
       └─ DealQueryService.findExpiredAndOpen()
            └─ for each dealId → closeDeal(dealId)
                 ├─ CurrentValueResolver.resolve(deal)
                 │    ├─ HEADCOUNT → RedisParticipantManager.count()
                 │    ├─ QUANTITY  → OrderAggregateClient.sumConfirmedQuantity()
                 │    └─ AMOUNT    → OrderAggregateClient.sumConfirmedAmount()  [미구현]
                 ├─ SuccessEvaluator.evaluate()
                 ├─ DealStatusUpdater.update()
                 └─ NotificationComponent.send()
```

---

## 생성 이벤트 수신 — `DealCreatedEventHandler`

`@TransactionalEventListener(AFTER_COMMIT)` 로 딜 생성이 **커밋된 뒤에만** read-model과 진행도를 세팅합니다.
롤백 시 핸들러가 실행되지 않으므로, Redis에 잘못된 재고가 초기화되는 상황을 방지합니다.

```kotlin
@Component
class DealCreatedEventHandler(
    private val dealReadModelRepository: DealReadModelRepository,
    private val stockInitializer: StockInitializer
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDealCreated(event: DealCreatedEvent) {
        requireNotNull(event) { "이벤트는 null일 수 없습니다" }

        dealReadModelRepository.save(DealReadModel.from(event))

        // QUANTITY 딜만 Redis 재고를 초기화한다.
        // HEADCOUNT는 참여 시점에 participants SET이 생성되고,
        // AMOUNT는 재고 개념이 없다.
        if (event.metric == SuccessMetric.QUANTITY) {
            stockInitializer.init(event.dealId, requireNotNull(event.capacity))
        }
    }
}
```

> 대응 테스트: `spring_boot_test` § 1-E1 · 1-E2 · 2-F1 ~ 2-F4

---

## metric 기준 마감 스케줄러 — `DealClosingScheduler` · `DealLifecycleService`

단일 딜 처리 실패가 전체 스케줄러를 멈추지 않도록 `runCatching`으로 예외를 격리합니다.
딜 조회(`DealQueryService`)와 마감 처리(`DealLifecycleService`)를 분리해 단일 책임을 유지합니다.

```kotlin
@Component
class DealClosingScheduler(private val dealLifecycleService: DealLifecycleService) {
    @Scheduled(fixedDelay = 60_000)
    fun closeExpiredDeals() = dealLifecycleService.closeExpiredDeals()
}
```

```kotlin
@Service
class DealLifecycleService(
    private val dealQueryService: DealQueryService,
    private val dealReadModelRepository: DealReadModelRepository,
    private val dealStatusUpdater: DealStatusUpdater,
    private val currentValueResolver: CurrentValueResolver,
    private val successEvaluator: SuccessEvaluator,
    private val notification: NotificationComponent
) {
    fun closeExpiredDeals() {
        dealQueryService.findExpiredAndOpen().forEach { dealId ->
            runCatching { closeDeal(dealId) }
                .onFailure { log.error("딜 마감 처리 실패: dealId=$dealId", it) }
            // 단일 딜 실패 → 로그 후 다음 딜 계속 처리
        }
    }

    fun closeDeal(dealId: Long): DealStatus {
        val deal = dealReadModelRepository.findById(dealId)
            ?: throw DealNotFoundException("딜을 찾을 수 없습니다: dealId=$dealId")

        require(deal.deadline <= LocalDateTime.now()) { "아직 마감 전: dealId=$dealId" }

        val currentValue = currentValueResolver.resolve(deal)
        val result = successEvaluator.evaluate(SuccessCriterion(deal.metric, deal.min), currentValue)

        return if (result == SuccessResult.SUCCESS) {
            dealStatusUpdater.update(dealId, DealStatus.SUCCESS)
            notification.sendSuccess(dealId)
            DealStatus.SUCCESS
        } else {
            dealStatusUpdater.update(dealId, DealStatus.FAILED)
            notification.sendFailed(dealId)
            DealStatus.FAILED
        }
    }
}
```

> 대응 테스트: `spring_boot_test` § 3-E1 · 4-F1 · 4-F2 · 5-R1

---

## metric 전략 분리 — `SuccessEvaluator` · `CurrentValueResolver`

성공 판정과 누적값 소스 선택을 전략으로 분리합니다.
**외부 의존이 없는 순수 로직**이므로 Small Test로 고정합니다.

```kotlin
@Component
class SuccessEvaluator {
    fun evaluate(criterion: SuccessCriterion, currentValue: Long): SuccessResult {
        require(currentValue >= 0)  { "누적값(currentValue)은 음수일 수 없습니다" }
        require(criterion.min > 0)  { "최솟값(min)은 양수여야 합니다" }
        return if (currentValue >= criterion.min) SuccessResult.SUCCESS else SuccessResult.FAILED
    }
}
```

```kotlin
@Component
class CurrentValueResolver(
    private val participantManager: RedisParticipantManager,
    private val orderAggregateClient: OrderAggregateClient
) {
    fun resolve(deal: DealReadModel): Long {
        requireNotNull(deal) { "deal은 null일 수 없습니다" }
        return when (deal.metric) {
            SuccessMetric.HEADCOUNT -> participantManager.count(deal.dealId)
            SuccessMetric.QUANTITY  -> orderAggregateClient.sumConfirmedQuantity(deal.dealId)
            SuccessMetric.AMOUNT    -> orderAggregateClient.sumConfirmedAmount(deal.dealId)  // [미구현]
        }
    }
}
```

> 대응 테스트: `small_test` § 2 · § 7 / `spring_boot_test` § 6 · § 7 · § 8-R1

---

## 주문 서비스 집계 조회 — `OrderAggregateClient`

인터페이스로 정의해 테스트에서 `@MockBean` / WireMock으로 교체 가능하게 합니다.

```kotlin
interface OrderAggregateClient {
    fun sumConfirmedQuantity(dealId: Long): Long
    fun sumConfirmedAmount(dealId: Long): Long   // [미구현 — amount 필드 추가 후 활성화]
}

@Component
class HttpOrderAggregateClient(private val restTemplate: RestTemplate) : OrderAggregateClient {

    override fun sumConfirmedQuantity(dealId: Long): Long =
        restTemplate.getForObject("/internal/orders/$dealId/quantity-sum", Long::class.java)
            ?: throw ServiceUnavailableException("주문 수량 집계 조회 실패: dealId=$dealId")

    override fun sumConfirmedAmount(dealId: Long): Long =
        throw NotImplementedError("AMOUNT 집계는 주문 서비스에 amount 필드 추가 후 구현합니다")
}
```

> 대응 테스트: `medium_test` § 5-E3 · 6-F1 ~ 6-F3 / `spring_boot_test` § 6-E2

---

## 읽기 전용 모델 — `DealReadModel`

각 서비스는 딜 원본을 소유하지 않고, `DealCreatedEvent`로 받은 **복제본**만 읽습니다.

```kotlin
@Entity
data class DealReadModel(
    @Id val dealId: Long,
    val metric: SuccessMetric,
    val min: Long,
    val capacity: Long?,           // AMOUNT 딜은 null
    val deadline: LocalDateTime,
    val regionCode: String
) {
    companion object {
        fun from(event: DealCreatedEvent): DealReadModel {
            requireNotNull(event.dealId) { "이벤트 dealId는 null일 수 없습니다" }
            return DealReadModel(
                dealId     = event.dealId,
                metric     = event.metric,
                min        = event.min,
                capacity   = event.capacity,
                deadline   = event.deadline,
                regionCode = event.regionCode
            )
        }
    }
}

@Repository
interface DealReadModelRepository : JpaRepository<DealReadModel, Long>
```

> 대응 테스트: `small_test` § 8 · § 9 / `spring_boot_test` § 9-E1 · 10-F1

---

## 테스트 격리 전략

| 계층 | 테스트 방법 | 이유 |
|---|---|---|
| `SuccessEvaluator` | 순수 함수 호출 (Small) | 외부 의존 없음 |
| `CurrentValueResolver` | Mock 주입 (Small) | 저장소 호출 방향만 검증 |
| `DealCreatedEventHandler` | `@SpringBootTest` + Testcontainers | 커밋 후 이벤트 타이밍 검증 필요 |
| `DealClosingScheduler` | `@MockBean` 으로 자동 실행 차단 | 의도치 않은 스케줄러 트리거 방지 |
| `OrderAggregateClient` | WireMock 또는 `@MockBean` | 실제 주문 서비스 연결 불필요 |

---

## 점수 반영 포인트

| 항목 | 내용 |
|---|---|
| 기술 적용도 | 커밋 후 이벤트 수신, metric 전략, 스케줄러, 서비스 간 집계 조회 연결 |
| TDD | `SuccessEvaluator` · `CurrentValueResolver` → Small / 이벤트·집계 → Medium / 전체 흐름 → Large |
| 트러블슈팅 | 선착순 모델 이중화, metric 무시 마감, 진행도 단위 불일치, 스케줄러 단일 실패 전파 해결 |
| 협업 | `RedisKeyConstants` 공유 상수로 서비스 간 키 규약 불일치 방지 |
