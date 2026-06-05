# 동네 기반 타임딜 공동구매 플랫폼

## 구상한 서비스

동네 기반 타임딜 공동구매 플랫폼은 같은 지역에 있는 사용자들이 제한 시간 안에 공동구매에 참여해서 생활용품, 식재료, 배달 음식 등을 함께 구매할 수 있도록 돕는 서비스입니다.

사용자는 현재 위치나 등록된 동네를 기준으로 주변 공동구매 방을 확인하고, 원하는 타임딜에 참여할 수 있습니다. 공동구매는 정해진 마감 시간과 최대 참여 인원 안에서 진행되며, 최소 참여 인원을 넘기면 공동구매가 성사됩니다.

## 내가 담당할 기능

제가 담당할 기능은 `타임딜 상세 페이지 조회 기능`입니다.

이 기능은 사용자가 공동구매 목록에서 특정 타임딜을 선택했을 때, 서버에서 상품 정보, 가격, 판매자 정보, 판매처 위치, 공동구매 현황을 조합하여 반환하는 역할을 합니다.

상세 페이지 조회에서 아래 조건을 정확하게 확인해야 합니다.

- 요청한 타임딜이 존재하는지 확인
- 연결된 상품이 삭제되거나 비공개 상태가 아닌지 확인
- Redis에서 현재 참여 인원 수를 실시간으로 조회
- 사용자의 동네가 공동구매 가능 지역에 해당하는지 확인
- 해당 사용자가 이미 참여했는지 여부를 응답에 포함

## 이 기능을 선택한 이유

상세 페이지 조회는 단순 SELECT처럼 보이지만, 현재 참여 인원을 RDBMS가 아닌 Redis에서 실시간으로 가져와야 하고, 사용자의 지역 조건과 참여 여부도 함께 판단해야 합니다.

여러 데이터 소스를 조합해서 하나의 응답을 만드는 구조이기 때문에, N+1 문제 방지를 위한 fetch join, 읽기 전용 트랜잭션, Redis 조회 타이밍 같은 실무 설계 이유를 자연스럽게 설명할 수 있습니다. 또한 참여 처리 기능과 연결되어 있어 전체 플로우를 함께 설명하기에도 적합하다고 생각했습니다.

## 사용 기술과 이유

### Kotlin / Spring Boot

상세 조회 API를 작성하기 위해 사용합니다. Kotlin의 data class로 응답 DTO를 간결하게 표현할 수 있고, `@Transactional(readOnly = true)`를 사용하면 읽기 전용 작업임을 명시하고 불필요한 dirty checking을 방지할 수 있습니다.

### Redis

현재 참여 인원 수와 사용자의 참여 여부를 빠르게 조회하기 위해 사용합니다. RDBMS에 저장된 참여 이력을 집계하는 대신 Redis의 `SCARD`와 `SISMEMBER` 명령으로 빠르게 조회합니다.

### RDBMS

타임딜, 상품, 판매자 정보처럼 구조화된 데이터는 RDBMS에서 조회합니다. JPA의 fetch join을 활용해 타임딜과 연관된 상품, 판매자 정보를 한 번의 쿼리로 가져와 N+1 문제를 방지합니다.

### TDD

타임딜이 없는 경우, 삭제된 상품인 경우, Redis 참여 인원 조회, 지역 조건 판단, 이미 참여한 사용자 표시 등을 테스트 우선으로 검증하기 위해 사용합니다.

## 데이터 구조 예시

```kotlin
data class TimeDeal(
    val dealId: Long,
    val title: String,
    val regionCode: String,
    val minParticipants: Int,
    val maxParticipants: Int,
    val deadline: LocalDateTime,
    val status: DealStatus
)

data class Product(
    val productId: Long,
    val name: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val originalPrice: Int,
    val dealPrice: Int,
    val isDeleted: Boolean
)

data class Seller(
    val sellerId: Long,
    val name: String,
    val phone: String,
    val email: String,
    val businessNumber: String,
    val address: String,
    val availableRegions: List<String>
)

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

data class ProductInfo(
    val name: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val originalPrice: Int,
    val dealPrice: Int,
    val discountRate: Int
)

data class SellerInfo(
    val name: String,
    val phone: String,
    val email: String,
    val businessNumber: String
)

data class ParticipantsInfo(
    val currentParticipants: Int,
    val minParticipants: Int,
    val maxParticipants: Int
)

data class RegionInfo(
    val address: String,
    val regionCode: String,
    val availableRegions: List<String>,
    val isAvailableForUser: Boolean
)

enum class DealStatus {
    OPEN,
    READY_TO_CONFIRM,
    SUCCESS,
    FAILED,
    CLOSED
}
```

## 핵심 의사코드

```text
// 타임딜 상세 페이지 조회
// 사용자가 공동구매 목록에서 특정 타임딜을 선택했을 때 실행된다.

/*
    인자    : dealId, userId, userRegionCode
    반환값  : 타임딜 상세 정보 (상품, 가격, 판매자, 공동구매 현황, 지역 정보)
    역할    : RDBMS와 Redis에서 정보를 조합하여 상세 페이지 응답을 구성한다.
*/

function getTimeDealDetail(dealId, userId, userRegionCode):
    deal = TimeDealRepository.findWithProductAndSellerById(dealId)

    if deal does not exist:
        return DEAL_NOT_FOUND

    if deal.product.isDeleted:
        return PRODUCT_NOT_AVAILABLE

    redisKey = "timedeal:" + dealId + ":participants"

    currentParticipants = Redis.SCARD(redisKey)

    isJoined = Redis.SISMEMBER(redisKey, userId)

    isAvailableForUser = (userRegionCode == deal.regionCode)

    discountRate = ((deal.product.originalPrice - deal.product.dealPrice)
                    / deal.product.originalPrice * 100).toInt()

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
            isAvailableForUser = isAvailableForUser
        ),
        isJoined     = isJoined
    )
```

## API 예시

```text
GET /timedeals/{dealId}
- 타임딜 상세 페이지 조회
- Header: Authorization: Bearer {token}
- 응답: TimeDealDetailResponse
```

## Jira 이슈 예시

```text
[타임딜/설계] 상세 페이지 조회 기능 구조 설계
[타임딜/기능] 타임딜 상세 조회 API 작성
[타임딜/기능] 상품·판매자 정보 fetch join 쿼리 작성
[타임딜/기능] Redis 현재 참여 인원 및 참여 여부 조회 연동
[타임딜/TDD] 타임딜 없을 때 오류 반환 테스트 작성
[타임딜/TDD] 현재 참여 인원 Redis 조회 테스트 작성
[타임딜/TDD] 지역 조건 판단 및 isJoined 표시 테스트 작성
```
