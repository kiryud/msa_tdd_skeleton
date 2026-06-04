# 타임딜 공동구매 Spring Boot 설계 보강

## 보강 목적

기존 `timedeal.md`에서는 타임딜 공동구매 참여 처리의 흐름을 중심으로 정리했습니다.

이번 보강 문서는 교수님 노션에 정리된 Spring Boot 핵심 개념을 과제에 더 잘 반영하기 위해 작성했습니다. 단순히 "참여 로직을 작성했다"에서 끝내지 않고, Spring Boot의 계층형 아키텍처, 어노테이션, JPA, 트랜잭션, 인증, 동시성 제어까지 어떤 위치에 적용할 수 있는지 설명합니다.

## 담당 기능 재정의

제가 담당하는 기능은 `타임딜 공동구매 참여 처리 기능`입니다.

조금 더 정확히 말하면, 사용자가 공동구매 방에서 `참여하기` 버튼을 눌렀을 때 서버에서 아래 일을 처리하는 기능입니다.

- 사용자가 참여 가능한 동네에 있는지 확인
- 타임딜이 아직 마감되지 않았는지 확인
- 같은 사용자가 이미 참여하지 않았는지 확인
- 최대 참여 인원을 초과하지 않는지 확인
- 참여 가능하면 Redis에 선착순 참여자로 등록
- RDBMS에 참여 이력 저장
- 최소 참여 인원 달성 여부 확인
- 마감 시 성공 또는 실패 상태 확정

## Spring Boot 계층형 구조 적용

타임딜 참여 기능은 Spring Boot의 Layered Architecture를 기준으로 나누어 설계합니다.

```text
Client
  -> TimeDealController
  -> TimeDealService
  -> TimeDealRepository / ParticipationRepository
  -> RDBMS

TimeDealService
  -> RedisParticipantManager
  -> Redis

TimeDealService
  -> NotificationComponent
  -> Notification Service
```

### Presentation Layer

`@RestController`를 사용합니다.

이 계층은 HTTP 요청을 받고 응답을 반환하는 역할만 담당합니다. 공동구매 참여 가능 여부를 직접 판단하지 않고, 요청 데이터를 DTO로 받은 뒤 Service 계층에 전달합니다.

```kotlin
@RestController
class TimeDealController(
    private val timeDealService: TimeDealService
) {
    @PostMapping("/timedeals/{dealId}/join")
    fun joinTimeDeal(
        @PathVariable dealId: Long,
        @RequestBody request: JoinDealRequest
    ): JoinDealResponse {
        return timeDealService.joinTimeDeal(dealId, request)
    }

    @DeleteMapping("/timedeals/{dealId}/participants/me")
    fun cancelTimeDeal(
        @PathVariable dealId: Long,
        @RequestBody request: CancelDealRequest
    ): CancelDealResponse {
        return timeDealService.cancelTimeDeal(dealId, request)
    }
}
```

### DTO

요청과 응답에는 Entity를 직접 사용하지 않고 DTO를 사용합니다.

Entity를 그대로 외부에 노출하면 DB 구조가 API 응답에 드러나고, 나중에 DB 컬럼이 바뀔 때 API까지 같이 흔들릴 수 있습니다. 그래서 `JoinDealRequest`, `JoinDealResponse` 같은 DTO로 필요한 값만 전달합니다.

```kotlin
data class JoinDealRequest(
    val userId: Long,
    val userRegionCode: String
)

data class JoinDealResponse(
    val dealId: Long,
    val userId: Long,
    val currentParticipants: Int,
    val status: DealStatus,
    val message: String
)
```

### Business Logic Layer

`@Service`를 사용합니다.

이 계층은 타임딜 참여 처리의 핵심 규칙을 담당합니다. 컨트롤러는 요청을 받는 역할만 하고, 실제 판단은 Service가 수행합니다.

```kotlin
@Service
class TimeDealService(
    private val timeDealRepository: TimeDealRepository,
    private val participationRepository: ParticipationRepository,
    private val redisParticipantManager: RedisParticipantManager
) {
    @Transactional
    @DealJoinLock
    fun joinTimeDeal(dealId: Long, request: JoinDealRequest): JoinDealResponse {
        val deal = timeDealRepository.findById(dealId)
            ?: throw DealNotFoundException()

        validateJoinable(deal, request)

        val currentParticipants =
            redisParticipantManager.addParticipant(dealId, request.userId, deal.maxParticipants)

        participationRepository.save(
            Participation(
                dealId = dealId,
                userId = request.userId,
                status = ParticipationStatus.JOINED
            )
        )

        if (currentParticipants >= deal.minParticipants) {
            deal.readyToConfirm()
        }

        return JoinDealResponse(
            dealId = dealId,
            userId = request.userId,
            currentParticipants = currentParticipants,
            status = deal.status,
            message = "공동구매 참여가 완료되었습니다."
        )
    }
}
```

### Data Access Layer

`@Repository`와 JPA를 사용합니다.

공동구매 방 정보, 참여 이력, 성공 또는 실패 상태처럼 기록으로 남아야 하는 데이터는 RDBMS에 저장합니다.

```kotlin
@Repository
interface TimeDealRepository : JpaRepository<TimeDeal, Long>

@Repository
interface ParticipationRepository : JpaRepository<Participation, Long> {
    fun existsByDealIdAndUserId(dealId: Long, userId: Long): Boolean
}
```

### Entity

`@Entity`는 DB 테이블과 직접 연결되는 객체입니다. DTO와 Entity를 분리해서 API 응답과 DB 구조가 서로 강하게 묶이지 않도록 합니다.

```kotlin
@Entity
class TimeDeal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val regionCode: String,

    @Column(nullable = false)
    val minParticipants: Int,

    @Column(nullable = false)
    val maxParticipants: Int,

    @Column(nullable = false)
    val deadline: LocalDateTime,

    @Enumerated(EnumType.STRING)
    var status: DealStatus
) {
    fun readyToConfirm() {
        if (status == DealStatus.OPEN) {
            status = DealStatus.READY_TO_CONFIRM
        }
    }

    fun success() {
        status = DealStatus.SUCCESS
    }

    fun fail() {
        status = DealStatus.FAILED
    }
}
```

## 동시성 제어 보강

이 기능에서 가장 중요한 문제는 동시에 많은 사용자가 `참여하기` 버튼을 누를 때입니다.

단순히 현재 인원을 조회한 뒤 저장하면, 거의 같은 순간에 여러 요청이 들어왔을 때 모두 "아직 정원이 남았다"고 판단할 수 있습니다. 그러면 최대 참여 인원을 초과하거나 중복 참여가 발생할 수 있습니다.

이를 막기 위해 Redis와 Lock 개념을 함께 사용합니다.

### Redis 사용 이유

- 현재 참여자 수를 빠르게 조회할 수 있음
- 같은 userId의 중복 참여를 빠르게 확인할 수 있음
- 선착순 참여자 목록을 메모리에서 빠르게 관리할 수 있음

### Lock 사용 이유

동시에 같은 dealId에 참여 요청이 몰릴 때, 한 순간에 하나의 요청만 참여자 수를 확정하도록 만들기 위해 사용합니다.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DealJoinLock
```

```kotlin
@Aspect
@Component
class DealJoinLockAspect(
    private val redisLockManager: RedisLockManager
) {
    @Around("@annotation(DealJoinLock)")
    fun around(joinPoint: ProceedingJoinPoint): Any {
        val dealId = extractDealId(joinPoint)
        val lockKey = "lock:timedeal:$dealId"

        redisLockManager.lock(lockKey)

        try {
            return joinPoint.proceed()
        } finally {
            redisLockManager.unlock(lockKey)
        }
    }
}
```

## 트랜잭션 보강

`@Transactional`은 참여 처리 중간에 오류가 발생했을 때 RDBMS 작업을 되돌리기 위해 사용합니다.

예를 들어 Redis에는 참여자가 추가되었는데 RDBMS 저장에 실패하면 데이터가 서로 맞지 않을 수 있습니다. 그래서 Service 계층에서 참여 이력 저장과 상태 변경을 하나의 트랜잭션으로 묶습니다.

다만 Redis는 RDBMS 트랜잭션과 완전히 같은 방식으로 롤백되지 않기 때문에, 실패 시 Redis 참여자 제거 보상 로직도 함께 고려합니다.

```text
try:
    Redis에 참여자 등록
    RDBMS에 참여 이력 저장
    최소 인원 달성 시 상태 변경
catch error:
    Redis에서 참여자 제거
    RDBMS 트랜잭션 롤백
    JOIN_FAILED 반환
```

## 보안 보강

공동구매 참여 기능은 로그인한 사용자만 사용할 수 있어야 합니다.

그래서 실제 구현에서는 JWT 인증을 통해 userId를 요청 Body에서 직접 믿지 않고, 토큰에서 꺼낸 사용자 정보를 기준으로 처리하는 것이 더 안전합니다.

```kotlin
@Auth
@PostMapping("/timedeals/{dealId}/join")
fun joinTimeDeal(
    @PathVariable dealId: Long,
    @RequestBody request: JoinDealRequest
): JoinDealResponse {
    return timeDealService.joinTimeDeal(dealId, request)
}
```

보안상 주의할 점은 아래와 같습니다.

- 요청 Body의 userId만 믿지 않기
- JWT 토큰에서 인증된 사용자 정보를 기준으로 참여 처리
- Entity를 API 응답으로 직접 반환하지 않기
- 내부 DB id, 결제 상태, 관리자용 상태값은 필요한 경우에만 응답
- 실패 사유는 너무 자세한 내부 정보까지 노출하지 않기

## 자동 마감 처리

마감 시간이 지난 공동구매는 사용자가 직접 호출하지 않아도 서버에서 자동으로 성공 또는 실패 상태로 확정할 수 있습니다.

이때 `@Scheduled`를 사용할 수 있습니다.

```kotlin
@Component
class TimeDealClosingScheduler(
    private val timeDealService: TimeDealService
) {
    @Scheduled(fixedDelay = 60000)
    fun closeExpiredDeals() {
        timeDealService.closeExpiredDeals()
    }
}
```

자동 마감 처리는 아래 기준으로 동작합니다.

```text
1. deadline이 지난 OPEN 또는 READY_TO_CONFIRM 상태의 타임딜 조회
2. Redis 참여자 수 확인
3. 최소 인원 이상이면 SUCCESS
4. 최소 인원 미만이면 FAILED
5. 참여자에게 성공/실패 알림 요청
```

## 점수 반영 포인트

이 보강 설계를 통해 과제 평가 기준 중 아래 항목을 더 명확히 설명할 수 있습니다.

- 기술 적용도: Spring Boot, JPA, Redis, Coroutine, TDD 활용 이유 명시
- TDD: 계층별 테스트 기준 분리
- 트러블슈팅: 동시 참여, 정원 초과, Redis/RDBMS 불일치 문제 정의
- 문서 구성: Controller, Service, Repository, Entity 흐름 분리
- 협업 과정: 담당 기능이 다른 서비스와 어떤 경계로 연결되는지 명확화
