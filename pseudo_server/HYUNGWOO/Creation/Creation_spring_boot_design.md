# 공동구매 딜 생성 Spring Boot 설계 보강

## 보강 목적

기존 `Creation.md`에서는 딜 생성의 흐름을 중심으로 정리했습니다.

이 문서는 교수님이 정리한 Spring Boot 핵심 개념(계층형 아키텍처, 어노테이션, JPA, 트랜잭션, 인증)을 딜 생성 기능에 어떻게 적용하는지 설명합니다. 참여 처리 서비스와 달리 생성 서비스의 핵심 난이도는 동시성 락이 아니라 **타입 다형성 / 멱등성 / 저장-이벤트 정합**에 있습니다.

## 담당 기능 재정의

제가 담당하는 기능은 `공동구매 딜 최초 생성 기능`입니다.

사용자(또는 판매자)가 딜 등록 버튼을 눌렀을 때 서버에서 아래 일을 처리합니다.

- 딜 타입(생필품 / 판매자 / 배달 공구) 판별
- 타입별 입력 유효성 검증
- 성공 조건(인원/수량/금액 + 최소값) 설정
- 정원(capacity) 설정
- 초기 상태 OPEN으로 저장
- 중복 생성 방지(멱등성)
- 딜 생성 이벤트(DealCreatedEvent) 발행

## Spring Boot 계층형 구조 적용

```text
Client
  -> DealController
  -> DealService
  -> DealCreationStrategy (타입별)
  -> DealRepository
  -> RDBMS

DealService
  -> ApplicationEventPublisher
  -> @TransactionalEventListener (AFTER_COMMIT)
  -> 참여 서비스
```

### Presentation Layer

`@RestController`를 사용합니다. HTTP 요청을 받아 DTO로 변환하고 Service에 위임합니다. 검증/판단은 직접 하지 않습니다.

```kotlin
@RestController
class DealController(
    private val dealService: DealService
) {
    @PostMapping("/deals")
    fun createDeal(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CreateDealRequest
    ): CreateDealResponse {
        return dealService.createDeal(idempotencyKey, request)
    }

    @GetMapping("/deals/{dealId}")
    fun getDeal(@PathVariable dealId: Long): DealResponse {
        return dealService.getDeal(dealId)
    }
}
```

### DTO

요청/응답에 Entity를 직접 쓰지 않고 DTO를 사용합니다. Entity를 노출하면 DB 구조가 API에 드러나고, 컬럼 변경 시 API까지 흔들립니다.

```kotlin
data class CreateDealRequest(
    val dealType: DealType,
    val title: String,
    val regionCode: String,
    val price: Long,
    val capacity: Long?,
    val metric: SuccessMetric,
    val min: Long,
    val deadline: LocalDateTime
)

data class CreateDealResponse(
    val dealId: Long,
    val dealType: DealType,
    val status: DealStatus,
    val message: String
)
```

### Business Logic Layer

`@Service`를 사용합니다. 생성의 핵심 규칙을 담당하며, 타입별 차이는 전략에 위임합니다.

```kotlin
@Service
class DealService(
    private val dealRepository: DealRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val strategyFactory: DealCreationStrategyFactory
) {
    @Transactional
    fun createDeal(idempotencyKey: String, request: CreateDealRequest): CreateDealResponse {
        dealRepository.findByIdempotencyKey(idempotencyKey)?.let {
            return it.toResponse()
        }

        val strategy = strategyFactory.get(request.dealType)
        strategy.validate(request)
        val criterion = strategy.buildSuccessCriterion(request)

        val deal = dealRepository.save(
            Deal(
                dealType = request.dealType,
                title = request.title,
                regionCode = request.regionCode,
                hostId = AuthContext.currentUserId(),
                price = request.price,
                capacity = request.capacity,
                successCriterion = criterion,
                deadline = request.deadline,
                status = DealStatus.OPEN,
                idempotencyKey = idempotencyKey
            )
        )

        eventPublisher.publishEvent(DealCreatedEvent.from(deal))

        return deal.toResponse()
    }
}
```

### 전략 패턴 (타입 다형성)

타입마다 검증 규칙과 성공 조건이 다르므로 전략으로 분리합니다.

```kotlin
interface DealCreationStrategy {
    fun supports(): DealType
    fun validate(request: CreateDealRequest)
    fun buildSuccessCriterion(request: CreateDealRequest): SuccessCriterion
}

class GroceryDealStrategy : DealCreationStrategy { ... }

class SellerDealStrategy : DealCreationStrategy { ... }

class FoodDeliveryDealStrategy : DealCreationStrategy {
    override fun validate(request: CreateDealRequest) {
        require(request.metric == SuccessMetric.AMOUNT) { "배달 공구는 금액 기준이어야 함" }
    }
    override fun buildSuccessCriterion(request: CreateDealRequest) =
        SuccessCriterion(SuccessMetric.AMOUNT, request.min)
}
```

타입을 추가해도 새 전략 클래스만 더하면 되고 기존 코드는 건드리지 않습니다(개방-폐쇄 원칙).

### Data Access Layer

`@Repository`와 JPA를 사용합니다. 멱등성 키로 중복 조회가 가능해야 합니다.

```kotlin
@Repository
interface DealRepository : JpaRepository<Deal, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): Deal?
}
```

### Entity

`@Entity`는 DB와 직접 연결됩니다. 상태는 생성 시 OPEN으로만 설정하고, 이후 전이 메서드는 두지 않습니다(전이는 참여 서비스 책임).

```kotlin
@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["idempotencyKey"])]
)
class Deal(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    val dealType: DealType,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val regionCode: String,

    @Column(nullable = false)
    val hostId: Long,

    @Column(nullable = false)
    val price: Long,

    @Column(nullable = true)
    val capacity: Long?,

    @Embedded
    val successCriterion: SuccessCriterion,

    @Column(nullable = false)
    val deadline: LocalDateTime,

    @Enumerated(EnumType.STRING)
    val status: DealStatus = DealStatus.OPEN,

    @Column(nullable = false, unique = true)
    val idempotencyKey: String
)
```

## 멱등성 보강

생성 버튼을 빠르게 두 번 누르면 같은 딜이 두 개 만들어질 수 있습니다. 이를 막기 위해 클라이언트가 보낸 `Idempotency-Key`를 딜에 저장하고, **DB unique 제약**으로 중복을 차단합니다.

```text
1. 요청에서 Idempotency-Key 추출
2. 해당 키로 이미 생성된 딜이 있으면 그 결과를 그대로 반환
3. 없으면 새로 생성. 동시 요청이 unique 제약에 걸리면 1건만 저장되고 나머지는 기존 건 반환
```

(분산 환경에서는 Redis SETNX로 키를 선점하는 방식도 대안)

## 트랜잭션 / 이벤트 정합 보강

"딜은 저장됐는데 생성 이벤트는 먼저 나가는" 불일치를 막기 위해 스프링의 **`@TransactionalEventListener`**(커밋 후 처리)를 씁니다.

```text
@Transactional 안에서:
    1. Deal 저장
    2. eventPublisher.publishEvent(DealCreatedEvent) 호출 (이벤트는 등록만 됨)
커밋 이후:
    3. @TransactionalEventListener(AFTER_COMMIT)가 실행되어 실제로 이벤트 전송
```

```kotlin
@Component
class DealEventHandler(
    private val dealEventSender: DealEventSender
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDealCreated(event: DealCreatedEvent) {
        dealEventSender.send(event)
    }
}
```

저장 트랜잭션이 롤백되면 AFTER_COMMIT 리스너는 실행되지 않으므로, 저장과 이벤트가 어긋나지 않습니다. (추가 인프라 없이 스프링 기본 기능으로 처리)

## 보안 보강

딜 생성은 인증된 사용자만 할 수 있어야 합니다.

- 요청 Body의 hostId를 믿지 않고, **JWT 토큰에서 꺼낸 userId를 hostId로 사용**
- 판매자 등록(SELLER) 타입은 **사업자 인증된 계정만** 허용
- Entity를 API 응답으로 직접 반환하지 않기
- 실패 사유는 내부 정보까지 노출하지 않기

```kotlin
@Auth
@PostMapping("/deals")
fun createDeal(
    @RequestHeader("Idempotency-Key") idempotencyKey: String,
    @RequestBody request: CreateDealRequest
): CreateDealResponse {
    return dealService.createDeal(idempotencyKey, request)
}
```

## 점수 반영 포인트

- 기술 적용도: Spring Boot, JPA, 전략 패턴, 트랜잭션 커밋 후 이벤트 발행, TDD 활용 이유 명시
- TDD: 타입별 검증을 순수 로직(Small)으로 분리, 저장/이벤트는 Medium, 연동은 Large
- 트러블슈팅: 중복 생성, 저장-이벤트 불일치, 타입별 성공 기준 차이 문제 정의
- 문서 구성: Controller, Service, Strategy, Repository, Entity 흐름 분리
- 협업 과정: 생성 서비스가 참여 서비스에 어떤 계약(이벤트/조회/상태경계)으로 연결되는지 명확화
