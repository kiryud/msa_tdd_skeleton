# 통합 연동 Spring Boot 설계 보강

`integration.md`의 의사코드를 Spring Boot 계층/어노테이션으로 어디에 배치하는지 정리한 문서입니다. 통합 계층의 핵심은 **생성 이벤트 수신**, **metric 기준 마감 스케줄러**, **주문 서비스 집계 조회**입니다.

## 인증 연동 (사용자 서비스)

딜 관련 엔드포인트(참여/주문/생성)는 사용자 서비스가 발급한 토큰을 검증한 뒤 처리합니다. 인증 필터/인터셉터에서 토큰을 검증하고 `userId`와 동네 정보를 컨텍스트에 주입합니다. 토큰 형식(JWT vs Redis 세션)은 통합 시 단일화 결정이 필요하며, 세션 방식이면 검증 단계에서 Redis 세션을 조회합니다. 동네 비교가 필요한 경우 `baseLocation`을 `normalizeRegion`으로 `regionCode`로 변환해 사용합니다.

## 통합 계층 구조

```text
[딜 생성] --DealCreatedEvent--> DealCreatedEventHandler -> DealReadModelRepository
                                                        -> 진행도 초기화(Redis)

DealClosingScheduler(@Scheduled)
  -> DealLifecycleService.closeExpiredDeals()
       -> getCurrentValue(metric별)
            HEADCOUNT -> RedisParticipantManager.count()
            QUANTITY/AMOUNT -> OrderAggregateClient.sum()
       -> evaluateSuccess() -> DealStatusUpdater
       -> collectParticipants(metric별) -> NotificationClient.sendDealResultNotification()
```

## 생성 이벤트 수신

`@TransactionalEventListener(AFTER_COMMIT)`로 딜 생성이 커밋된 뒤에만 read-model과 진행도를 세팅합니다.

```kotlin
@Component
class DealCreatedEventHandler(
    private val dealReadModelRepository: DealReadModelRepository,
    private val stockInitializer: StockInitializer
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDealCreated(event: DealCreatedEvent) {
        dealReadModelRepository.save(DealReadModel.from(event))
        if (event.successCriterion.metric == SuccessMetric.QUANTITY) {
            stockInitializer.init(event.dealId, event.capacity)
        }
    }
}
```

## metric 기준 마감 스케줄러

기존 인원 전용 마감을 `metric` 분기로 일반화합니다. 마감 판정에 필요한 누적값을 metric별 소스에서 가져옵니다.

```kotlin
@Component
class DealClosingScheduler(
    private val dealLifecycleService: DealLifecycleService
) {
    @Scheduled(fixedDelay = 60000)
    fun closeExpiredDeals() {
        dealLifecycleService.closeExpiredDeals()
    }
}
```

```kotlin
@Service
class DealLifecycleService(
    private val dealReadModelRepository: DealReadModelRepository,
    private val dealStatusUpdater: DealStatusUpdater,
    private val currentValueResolver: CurrentValueResolver,
    private val successEvaluator: SuccessEvaluator,
    private val participantCollector: ParticipantCollector,
    private val notificationClient: NotificationClient
) {
    fun closeDeal(dealId: Long): DealStatus {
        val deal = dealReadModelRepository.findById(dealId)
            ?: throw DealNotFoundException()

        require(deal.deadline <= now()) { "아직 마감 전" }

        val currentValue = currentValueResolver.resolve(deal)
        val result = successEvaluator.evaluate(SuccessCriterion(deal.metric, deal.min), currentValue)
        val participantIds = participantCollector.collect(deal)

        return if (result == SuccessResult.SUCCESS) {
            dealStatusUpdater.update(dealId, DealStatus.SUCCESS)
            notificationClient.sendDealResultNotification(dealId, NotificationType.DEAL_SUCCESS, participantIds)
            DealStatus.SUCCESS
        } else {
            dealStatusUpdater.update(dealId, DealStatus.FAILED)
            notificationClient.sendDealResultNotification(dealId, NotificationType.DEAL_FAILED, participantIds)
            DealStatus.FAILED
        }
    }
}
```

`ParticipantCollector`는 `metric`에 따라 참여자(HEADCOUNT는 참여 SET의 SMEMBERS, QUANTITY/AMOUNT는 주문 서비스의 딜 단위 주문자)를 모읍니다. 단, 주문 서비스의 딜 단위 주문자 조회는 현재 API에 없어 추가가 필요합니다.

## metric 전략 (순수 로직 분리)

성공 판정과 누적값 소스 선택을 전략으로 분리해 Small 테스트로 고정합니다.

```kotlin
@Component
class SuccessEvaluator {
    fun evaluate(criterion: SuccessCriterion, currentValue: Long): SuccessResult =
        if (currentValue >= criterion.min) SuccessResult.SUCCESS else SuccessResult.FAILED
}

@Component
class CurrentValueResolver(
    private val participantManager: RedisParticipantManager,
    private val orderAggregateClient: OrderAggregateClient
) {
    fun resolve(deal: DealReadModel): Long = when (deal.metric) {
        SuccessMetric.HEADCOUNT -> participantManager.count(deal.dealId)
        SuccessMetric.QUANTITY  -> orderAggregateClient.sumConfirmedQuantity(deal.dealId)
        SuccessMetric.AMOUNT    -> orderAggregateClient.sumConfirmedAmount(deal.dealId)
    }
}
```

## 주문 서비스 집계 조회

수량/금액 딜은 마감 판정에 주문 서비스의 집계가 필요합니다. 마감 계층이 주문 서비스를 읽는 경계입니다. 단, `order_service.md`의 주문 데이터는 현재 `quantity`만 저장하므로 `sumConfirmedQuantity`는 바로 가능하지만 `sumConfirmedAmount`는 주문에 `amount`(또는 `unitPrice`) 필드 추가가 선행되어야 합니다.

```kotlin
interface OrderAggregateClient {
    fun sumConfirmedQuantity(dealId: Long): Long
    fun sumConfirmedAmount(dealId: Long): Long
}
```

## 읽기 전용 모델

각 서비스는 딜 원본을 소유하지 않고, 이벤트로 받은 복제본만 읽습니다.

```kotlin
@Repository
interface DealReadModelRepository : JpaRepository<DealReadModel, Long>
```

## 점수 반영 포인트

- 기술 적용도: 커밋 후 이벤트 수신, metric 전략, 스케줄러, 서비스 간 집계 조회를 통합 흐름에 연결
- TDD: 전략(SuccessEvaluator/CurrentValueResolver)은 Small, 이벤트·집계·정합은 Medium, 전체 흐름은 Large
- 트러블슈팅: 선착순 모델 이중화, metric 무시 마감, 진행도 단위 불일치 문제 해결
- 협업 과정: 네 서비스가 dealId / 이벤트 / Redis 키 규약으로 어떻게 맞물리는지 명확화
