# 타임딜 상세 페이지 Spring Boot 설계 보강

## 보강 목적

기존 `timedeal_detail.md`에서는 타임딜 상세 페이지 조회 흐름을 중심으로 정리했습니다.

이번 보강 문서는 교수님 노션에 정리된 Spring Boot 핵심 개념을 과제에 더 잘 반영하기 위해 작성했습니다. 단순히 "상세 정보를 반환했다"에서 끝내지 않고, Spring Boot의 계층형 아키텍처, 어노테이션, JPA fetch join, 읽기 전용 트랜잭션, 인증 처리까지 어떤 위치에 적용할 수 있는지 설명합니다.

## 담당 기능 재정의

제가 담당하는 기능은 `타임딜 상세 페이지 조회 기능`입니다.

조금 더 정확히 말하면, 사용자가 공동구매 목록에서 특정 항목을 선택했을 때 서버에서 아래 일을 처리하는 기능입니다.

- 타임딜과 연결된 상품·판매자 정보를 fetch join으로 한 번에 조회
- Redis에서 현재 참여 인원 수(`SCARD`)와 사용자 참여 여부(`SISMEMBER`) 조회
- 사용자의 동네가 공동구매 가능 지역에 해당하는지 판단
- 할인율 계산 후 DTO로 조합하여 응답 반환

## Spring Boot 계층형 구조 적용

타임딜 상세 조회 기능은 Spring Boot의 Layered Architecture를 기준으로 나누어 설계합니다.

```text
Client
  -> TimeDealController
  -> TimeDealService
  -> TimeDealRepository (fetch join)
  -> RDBMS

TimeDealService
  -> RedisParticipantManager
  -> Redis
```

### Presentation Layer

`@RestController`를 사용합니다.

이 계층은 HTTP 요청을 받고 응답을 반환하는 역할만 담당합니다. JWT 토큰에서 userId와 regionCode를 꺼낸 뒤 Service 계층에 전달합니다.

```kotlin
@RestController
class TimeDealController(
    private val timeDealService: TimeDealService
) {
    @GetMapping("/timedeals/{dealId}")
    fun getTimeDealDetail(
        @PathVariable dealId: Long,
        @Auth authUser: AuthUser
    ): TimeDealDetailResponse {
        return timeDealService.getTimeDealDetail(dealId, authUser.userId, authUser.regionCode)
    }
}
```

### DTO

요청과 응답에는 Entity를 직접 사용하지 않고 DTO를 사용합니다.

Entity를 그대로 외부에 노출하면 DB 구조가 API 응답에 드러나고, 나중에 DB 컬럼이 바뀔 때 API까지 같이 흔들릴 수 있습니다. 그래서 `TimeDealDetailResponse`, `ProductInfo`, `SellerInfo`, `ParticipantsInfo`, `RegionInfo` 같은 DTO로 필요한 값만 전달합니다.

```kotlin
data class TimeDealDetailResponse(
    val dealId: Long,
    val title: String,
    val status: DealStatus,
    val deadline: LocalDateTime,
    val product: ProductInfo,
    val seller: SellerInfo,
    val participants: ParticipantsInfo,
    val region: RegionInfo,
    val isJoined: Boolean
)
```

### Business Logic Layer

`@Service`를 사용합니다.

이 계층은 RDBMS와 Redis에서 가져온 정보를 조합하고, 할인율 계산과 지역 조건 판단을 수행합니다. 컨트롤러는 요청을 받는 역할만 하고, 실제 조합 로직은 Service가 담당합니다.

```kotlin
@Service
class TimeDealService(
    private val timeDealRepository: TimeDealRepository,
    private val redisParticipantManager: RedisParticipantManager
) {
    @Transactional(readOnly = true)
    fun getTimeDealDetail(
        dealId: Long,
        userId: Long,
        userRegionCode: String
    ): TimeDealDetailResponse {
        val deal = timeDealRepository.findWithProductAndSellerById(dealId)
            ?: throw DealNotFoundException()

        if (deal.product.isDeleted) throw ProductNotAvailableException()

        val currentParticipants = redisParticipantManager.getParticipantCount(dealId)
        val isJoined = redisParticipantManager.isParticipant(dealId, userId)

        val discountRate = ((deal.product.originalPrice - deal.product.dealPrice)
                .toDouble() / deal.product.originalPrice * 100).toInt()

        return TimeDealDetailResponse(
            dealId       = deal.dealId,
            title        = deal.title,
            status       = deal.status,
            deadline     = deal.deadline,
            product      = ProductInfo(
                name          = deal.product.name,
                description   = deal.product.description,
                category      = deal.product.category,
                imageUrl      = deal.product.imageUrl,
                originalPrice = deal.product.originalPrice,
                dealPrice     = deal.product.dealPrice,
                discountRate  = discountRate
            ),
            seller       = SellerInfo(
                name           = deal.seller.name,
                phone          = deal.seller.phone,
                email          = deal.seller.email,
                businessNumber = deal.seller.businessNumber
            ),
            participants = ParticipantsInfo(
                currentParticipants = currentParticipants,
                minParticipants     = deal.minParticipants,
                maxParticipants     = deal.maxParticipants
            ),
            region       = RegionInfo(
                address            = deal.seller.address,
                regionCode         = deal.regionCode,
                availableRegions   = deal.seller.availableRegions,
                isAvailableForUser = userRegionCode == deal.regionCode
            ),
            isJoined     = isJoined
        )
    }
}
```

### Data Access Layer

`@Repository`와 JPA를 사용합니다.

타임딜과 연관된 상품, 판매자 정보를 한 번의 쿼리로 가져오기 위해 JPQL fetch join을 사용합니다. 이를 통해 N+1 문제를 방지합니다.

```kotlin
@Repository
interface TimeDealRepository : JpaRepository<TimeDeal, Long> {

    @Query("""
        SELECT t FROM TimeDeal t
        JOIN FETCH t.product p
        JOIN FETCH t.seller s
        WHERE t.id = :dealId
    """)
    fun findWithProductAndSellerById(@Param("dealId") dealId: Long): TimeDeal?
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
    var status: DealStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: Seller
)
```

## 읽기 전용 트랜잭션 보강

상세 페이지 조회는 데이터를 변경하지 않습니다. 그래서 `@Transactional(readOnly = true)`를 사용합니다.

읽기 전용 트랜잭션을 사용하면 아래 효과를 얻을 수 있습니다.

- JPA dirty checking 비활성화로 성능 향상
- 데이터베이스 read replica로 라우팅 가능
- 코드만 봐도 데이터를 변경하지 않는 메서드임을 명확히 표현

```kotlin
@Transactional(readOnly = true)
fun getTimeDealDetail(dealId: Long, userId: Long, userRegionCode: String): TimeDealDetailResponse {
    // 조회만 수행, 데이터 변경 없음
}
```

## 보안 보강

상세 페이지 조회는 로그인한 사용자만 사용할 수 있어야 합니다.

userId와 regionCode를 쿼리 파라미터나 요청 Body에서 직접 받지 않고, JWT 토큰에서 꺼낸 인증된 사용자 정보를 기준으로 처리합니다. 이를 통해 다른 사용자의 regionCode를 위조하거나, 로그인하지 않은 채로 isJoined 정보를 가져오는 것을 방지합니다.

보안상 주의할 점은 아래와 같습니다.

- 쿼리 파라미터의 userId나 regionCode를 그대로 믿지 않기
- JWT 토큰에서 인증된 사용자 정보를 기준으로 조회
- Entity를 API 응답으로 직접 반환하지 않기
- 판매자의 내부 정보(계좌번호, 관리자용 메모 등)는 DTO에 포함하지 않기

## N+1 문제 보강

타임딜 Entity에는 상품과 판매자가 연관되어 있습니다. `FetchType.LAZY`를 기본으로 설정하면 타임딜 조회 후 상품, 판매자 정보를 각각 추가 쿼리로 가져와 N+1 문제가 발생합니다.

상세 페이지 조회는 항상 상품과 판매자 정보를 함께 사용하기 때문에, 이 경우에는 fetch join으로 한 번에 가져옵니다.

```text
// fetch join 미사용 시
SELECT * FROM time_deal WHERE id = 1;       -- 1번 쿼리
SELECT * FROM product WHERE id = 10;        -- 추가 쿼리
SELECT * FROM seller WHERE id = 5;          -- 추가 쿼리

// fetch join 사용 시
SELECT t.*, p.*, s.*
FROM time_deal t
JOIN product p ON t.product_id = p.id
JOIN seller s ON t.seller_id = s.id
WHERE t.id = 1;                             -- 1번 쿼리로 해결
```

## 점수 반영 포인트

이 보강 설계를 통해 과제 평가 기준 중 아래 항목을 더 명확히 설명할 수 있습니다.

- 기술 적용도: Spring Boot, JPA, Redis 활용 이유 명시
- TDD: 계층별 테스트 기준 분리
- 트러블슈팅: N+1 문제, 읽기 전용 트랜잭션, Redis/RDBMS 데이터 불일치 문제 정의
- 문서 구성: Controller, Service, Repository, Entity 흐름 분리
- 협업 과정: 담당 기능이 참여 처리 기능과 어떤 경계로 연결되는지 명확화
