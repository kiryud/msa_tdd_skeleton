# 통합 — 타임딜 공동구매 마이크로서비스 연동

여섯 개의 마이크로서비스(사용자 / 딜 생성 / 상세 조회 / 참여 처리 / 주문 처리 / 알림)를 하나의 서비스로 합칠 때, 서비스 사이를 잇는 **연동(글루) 로직**을 정의하는 문서입니다. 이 의사코드를 기준으로 통합 테스트(small / medium / large)가 작성됩니다.

## 통합 서비스 개요

| 서비스 | 책임 | 소유 엔티티 |
|---|---|---|
| 사용자 (User) | 로그인·세션 토큰 발급, GraphQL 프로필 조회, 동네(baseLocation) 출처 | `User`(예정) |
| 딜 생성 (Creation) | 딜 최초 등록(OPEN), 생성 이벤트 발행 | `Deal` |
| 상세 조회 (Detail) | 딜+상품+판매자+진행도 조합 응답 | `Product`, `Seller` |
| 참여 처리 (Participation) | 인원 기준 선착순 참여/취소/마감 | `Participation` |
| 주문 처리 (Order) | 수량/금액 기준 선착순 주문/취소 | `Order` |
| 알림 (Notification) | 이벤트 기반 실시간 알림 발송/읽음/unread/SSE | `Notification` |

서비스를 잇는 두 축은 **`dealId`**(딜 식별)와 **`userId`/동네**(신원·지역)입니다. 딜 생성이 `dealId`를 만들고, 사용자 서비스가 `userId`와 동네 정보를 제공하며, 나머지 서비스는 이 둘을 참조합니다. 알림 서비스는 마감 결과·주문 확인 같은 **이벤트의 소비자**로, 다른 서비스가 만든 결과를 받아 사용자에게 전달합니다.

## 통합 계층이 책임지는 것

개별 서비스는 각자 정상이지만, 합치면 두 가지가 비어 있습니다. 통합 계층은 이 둘을 메웁니다.

1. **선착순 모델 분기**: 딜은 `metric`(인원/수량/금액)에 따라 **하나의 모델만** 쓴다. 인원(HEADCOUNT)이면 참여 처리, 수량/금액(QUANTITY/AMOUNT)이면 주문 처리로 라우팅한다.
2. **metric 기준 마감 확정**: 기존 마감은 인원(SCARD)만 봤다. 통합 마감은 `metric`에 따라 참여 인원 / 주문 수량 합 / 주문 금액 합을 골라 집계해 성사 여부를 판정한다.

## 사용 기술과 이유

### 생성 이벤트 + read-model
딜 생성이 발행한 `DealCreatedEvent`를 각 서비스가 받아 읽기 전용 모델(`DealReadModel`)과 진행도 초기값(재고 등)을 세팅합니다. 딜 원본은 생성 서비스만 소유하고, 나머지는 복제본을 읽습니다.

### metric 전략
성공 판정과 진행도 계산을 `metric`별로 분기합니다. 분기 자체는 순수 함수로 떼어내 Small 테스트로 고정합니다.

### Redis / RDBMS
진행도(참여자 SET, 주문 재고)는 Redis, 기록(참여 이력, 주문, 딜 상태)은 RDBMS에서 다루며, 둘의 정합을 통합 테스트로 검증합니다.

## 데이터 구조 예시

```kotlin
enum class SuccessMetric { HEADCOUNT, QUANTITY, AMOUNT }

enum class ParticipationModel { JOIN, ORDER }

data class SuccessCriterion(
    val metric: SuccessMetric,
    val min: Long
)

// 각 서비스가 DealCreatedEvent로 받아 보관하는 읽기 전용 모델
data class DealReadModel(
    val dealId: Long,
    val metric: SuccessMetric,
    val min: Long,
    val capacity: Long?,
    val deadline: LocalDateTime,
    val regionCode: String
)

// 상세 페이지 진행도 (metric에 따라 단위가 달라짐)
data class Progress(
    val unit: SuccessMetric,
    val current: Long,
    val min: Long,
    val capacity: Long?
)

enum class SuccessResult { SUCCESS, FAILED }
```

## 핵심 의사코드

```text
// 성공 판정기
// 마감 시점에 metric별 현재 누적값이 최소 기준을 넘었는지 판정한다.

/*
    인자    : successCriterion(metric, min), currentValue(metric에 맞는 누적값)
    반환값  : SUCCESS 또는 FAILED
    역할    : 누적값이 최소 기준 이상이면 성사, 미만이면 실패로 판정한다.
*/

function evaluateSuccess(successCriterion, currentValue):
    if currentValue >= successCriterion.min:
        return SUCCESS
    return FAILED
```

```text
// 선착순 모델 분기
// 딜의 metric에 따라 참여(인원) 모델인지 주문(수량/금액) 모델인지 결정한다.

/*
    인자    : metric
    반환값  : ParticipationModel (JOIN 또는 ORDER)
    역할    : 한 딜이 어느 선착순 메커니즘을 쓰는지 단일하게 라우팅한다.
*/

function resolveParticipationModel(metric):
    if metric == HEADCOUNT:
        return JOIN
    return ORDER
```

```text
// 현재 누적값 집계
// metric에 맞는 저장소에서 현재 누적값을 읽어온다.

/*
    인자    : deal(DealReadModel)
    반환값  : 현재 누적값 (인원 수 / 주문 수량 합 / 주문 금액 합)
    역할    : 마감 판정과 진행도 표시에 쓸 현재값을 metric별 소스에서 가져온다.
*/

function getCurrentValue(deal):
    switch deal.metric:
        case HEADCOUNT:
            return Redis.SCARD("timedeal:" + deal.dealId + ":participants")
        case QUANTITY:
            return OrderService.sumConfirmedQuantity(deal.dealId)
        case AMOUNT:
            return OrderService.sumConfirmedAmount(deal.dealId)
```

```text
// 마감 확정 (통합)
// 마감 시간이 지난 딜을 metric 기준으로 성사/실패 확정한다.

/*
    인자    : dealId
    반환값  : 딜 최종 상태
    역할    : metric별 누적값을 집계해 성공 판정 후 딜 상태를 전이하고 알림을 요청한다.
*/

function closeDeal(dealId):
    deal = DealReadModel.findById(dealId)

    if deal does not exist:
        return DEAL_NOT_FOUND

    if currentTime() < deal.deadline:
        return DEAL_NOT_EXPIRED

    currentValue = getCurrentValue(deal)
    result = evaluateSuccess(SuccessCriterion(deal.metric, deal.min), currentValue)

    participantIds = collectParticipants(deal)

    if result == SUCCESS:
        DealRepository.updateStatus(dealId, SUCCESS)
        NotificationService.sendDealResultNotification(dealId, DEAL_SUCCESS, participantIds)
        return DEAL_SUCCESS

    DealRepository.updateStatus(dealId, FAILED)
    NotificationService.sendDealResultNotification(dealId, DEAL_FAILED, participantIds)
    return DEAL_FAILED
```

```text
// 생성 이벤트 수신
// 딜이 생성되면 각 서비스가 읽기 전용 모델과 진행도 초기값을 세팅한다.

/*
    인자    : event(DealCreatedEvent)
    반환값  : 없음
    역할    : 딜 복제본을 저장하고 metric별 진행도 저장소를 초기화한다.
*/

function onDealCreated(event):
    readModel = DealReadModel.from(event)
    DealReadModelRepository.save(readModel)

    switch readModel.metric:
        case QUANTITY:
            Redis.SET("deal:" + readModel.dealId + ":stock", readModel.capacity)
        case AMOUNT:
            return
        case HEADCOUNT:
            return
```

`DealReadModel.from(event)`는 이벤트의 `successCriterion`을 `metric`/`min`으로 펼치고 `dealType`/`hostId`처럼 라이프사이클에 불필요한 필드는 제외한다.

```text
// 상세 진행도 구성
// 상세 페이지에 보여줄 진행도를 metric에 맞는 단위로 만든다.

/*
    인자    : deal(DealReadModel)
    반환값  : Progress(unit, current, min, capacity)
    역할    : 인원/수량/금액 딜에 맞는 진행도 단위와 현재값을 계산한다.
*/

function buildProgress(deal):
    switch deal.metric:
        case HEADCOUNT:
            current = Redis.SCARD("timedeal:" + deal.dealId + ":participants")
            return Progress(HEADCOUNT, current, deal.min, deal.capacity)
        case QUANTITY:
            remaining = Redis.GET("deal:" + deal.dealId + ":stock")
            return Progress(QUANTITY, deal.capacity - remaining, deal.min, deal.capacity)
        case AMOUNT:
            current = OrderService.sumConfirmedAmount(deal.dealId)
            return Progress(AMOUNT, current, deal.min, null)
```

```text
// 지역 일치 판정 (공용)
// 참여 처리와 상세 조회가 같은 규칙으로 지역을 판정하도록 단일화한 함수.

/*
    인자    : userRegionCode, dealRegionCode
    반환값  : 일치 여부(true/false)
    역할    : 사용자 동네와 딜 지역이 같은지 판정한다(참여/상세 공용).
*/

function isRegionMatched(userRegionCode, dealRegionCode):
    return userRegionCode == dealRegionCode
```

```text
// 동네 정규화 (공용)
// 사용자 서비스의 baseLocation(자유 문자열)을 지역 일치 판정에 쓰는 regionCode로 변환한다.

/*
    인자    : baseLocation (사용자 서비스의 동네 문자열)
    반환값  : regionCode (예: MAPO_YEONNAM)
    역할    : 참여/상세의 지역 비교에 쓸 수 있도록 동네 표현을 표준 코드로 변환한다.
*/

function normalizeRegion(baseLocation):
    regionCode = RegionMapper.toRegionCode(baseLocation)
    if regionCode is null:
        return UNSUPPORTED_REGION
    return regionCode
```

```text
// 참여자 수집 (공용, metric 기준)
// 마감 알림을 보낼 참여자 userId 목록을 metric에 맞는 소스에서 모은다.

/*
    인자    : deal (DealReadModel)
    반환값  : 참여자 userId 목록
    역할    : 인원 딜은 참여 SET에서, 수량/금액 딜은 주문 기록에서 참여자를 모은다.
*/

function collectParticipants(deal):
    switch deal.metric:
        case HEADCOUNT:
            return Redis.SMEMBERS("timedeal:" + deal.dealId + ":participants")
        case QUANTITY:
        case AMOUNT:
            return OrderService.findUserIdsByDeal(deal.dealId)
```

## 통합 라이프사이클

```text
[사용자 User]
   로그인 -> 세션 토큰 발급 -> 토큰에서 userId + 동네(baseLocation->regionCode) 확보
        v  (이후 모든 요청은 이 신원으로 인증)
[딜 생성 Creation]
   POST /deals -> Deal 저장(OPEN) -> DealCreatedEvent 발행
        | dealId, metric, min, capacity, deadline, regionCode
        v
   onDealCreated: 각 서비스 DealReadModel 저장 + 진행도 초기화
        v
[상세 조회 Detail] --buildProgress--> metric별 진행도 + 상품/판매자 (isRegionMatched로 지역 표시)
        v  (resolveParticipationModel(metric)로 분기)
   +-------------------+--------------------+
   | JOIN (HEADCOUNT)  | ORDER (QUANTITY/AMOUNT)
   v                   v
[참여 처리]          [주문 처리]
 Redis SET           Redis 재고 / 주문 저장
        +---------+---------+
                  v  deadline 경과
            closeDeal: getCurrentValue(metric별) -> evaluateSuccess -> SUCCESS/FAILED
                  v  collectParticipants(metric별)
[알림 Notification] sendDealResultNotification(dealId, 결과, 참여자목록)
   -> DB 이력 저장 -> Redis unread INCR -> SSE Publish (실시간 수신)

(별도 흐름) 주문 CONFIRMED -> 주문 서비스가 sendNotification(ORDER_CONFIRMED) 호출
(별도 흐름) deadline 30분 전 -> 참여자에게 DEADLINE_ALERT
```

## 경계 계약

- **딜 단일 소유**: `Deal` 쓰기 소유자는 생성 서비스뿐. 나머지는 `DealReadModel`(이벤트 복제본)을 읽는다.
- **이벤트**: `DealCreatedEvent(dealId, dealType, regionCode, capacity, successCriterion(metric, min), deadline, hostId)` 커밋 후 발행. 읽기 모델은 `successCriterion`을 `metric`/`min`으로 펼쳐 보관한다.
- **Redis 키 규약**: 참여 `timedeal:{dealId}:participants`, 재고 `deal:{dealId}:stock`, 락 `lock:timedeal:{dealId}`, 알림 미읽음 `notification:unread:{userId}`, 알림 채널 `notification:channel:{userId}`.
- **상태 소유**: 생성은 OPEN만. `READY_TO_CONFIRM`/`SUCCESS`/`FAILED` 전이는 마감 확정(`closeDeal`)만 수행.

## 주문 서비스 실제 사양 반영

`order_service.md`를 확인해 주문 연동 부분을 실제 사양에 맞춘다.

- **Redis 재고 키**: `deal:{dealId}:stock` (DECRBY 감소, INCRBY 복구). 참여 진행도 키 `timedeal:{dealId}:participants`와 접두어가 달라(`deal:` vs `timedeal:`) 통합 시 키 규약 통일이 안전하다.
- **주문 상태**: `PENDING → CONFIRMED → (취소) CANCELLED` 3단계. 주문은 DECRBY 후 PENDING으로 저장됐다가 CONFIRMED로 갱신된다. 따라서 "초기재고 − 현재재고 = CONFIRMED 수량 합" 정합은 PENDING이 모두 정착된 뒤에만 성립한다.
- **동시성 방식**: 주문은 분산 락 없이 Redis `DECRBY` 원자성으로 선착순을 보장한다(참여 처리의 AOP 락과 다름).
- **금액(AMOUNT) 미지원**: 현재 주문 데이터는 `quantity`만 저장하고 금액/단가 필드가 없다. 수량 집계(`sumConfirmedQuantity`)는 가능하지만, **금액 집계(`sumConfirmedAmount`)와 배달 공구(AMOUNT) 마감은 주문에 `amount`(또는 `unitPrice`) 추가가 선행되어야 한다.** 추가 시 `금액 합 = Σ(quantity × unitPrice)`로 계산한다. 그 전까지 AMOUNT 경로는 미구현으로 표시한다.

## 사용자 서비스 연동 (User Service)

`user_service.md`(친구 구현)를 확인해 신원/인증 연동을 정리한다. 사용자 서비스는 로그인 시 **세션 토큰**을 발급(Redis 저장 예정)하고, GraphQL로 프로필을 조회하며, 사용자 동네 정보(`baseLocation`)의 출처다.

- **신원 단일 소유**: `userId`와 동네 정보의 출처는 사용자 서비스뿐이다. 참여·주문·상세·생성은 요청 토큰에서 `userId`와 동네를 얻어 사용한다.
- **인증 흐름**: 모든 딜 관련 요청(참여/주문/생성)은 사용자 서비스가 발급한 토큰을 검증한 뒤 처리한다. 비인증·만료 토큰은 거부한다.
- **동네 변환**: 사용자 서비스의 `baseLocation`을 `normalizeRegion`으로 `regionCode`로 변환한 뒤 `isRegionMatched`에 넘긴다.

### 통합 시 결정 필요(열린 항목)

- **토큰 형식 불일치**: 다른 서비스 문서들은 "JWT에서 userId/regionCode를 꺼낸다"고 가정하지만, 사용자 서비스는 **Redis 세션 토큰**을 발급한다. 둘 중 하나로 통일해야 한다.
  - 안(A): 사용자 서비스가 JWT를 발급하고 각 서비스가 공유 시크릿으로 로컬 검증(요청마다 호출 불필요).
  - 안(B): 세션 토큰을 유지하고 각 서비스가 Redis(또는 사용자 서비스)로 세션을 조회해 검증.
  - 구현된 코드가 세션 방식이므로 단기적으로는 안(B), 무상태 확장을 노린다면 안(A)가 유리하다.
- **동네 표현 불일치**: `baseLocation`은 자유 문자열("우리동네 기반 위치정보")이라 그대로는 `regionCode` 비교에 못 쓴다. 사용자 서비스가 구조화된 `regionCode`를 직접 제공하거나, 통합 계층에서 `normalizeRegion`으로 매핑해야 한다.

## 알림 서비스 연동 (Notification)

`notification_service.md`를 확인해 알림 연동을 정리한다. 알림 서비스는 이벤트 소비자로, 결과를 받아 사용자에게 발송하고(`sendNotification`/`sendDealResultNotification`), 미읽음 수(`notification:unread:{userId}` INCR/DECR)와 SSE(`/notifications/subscribe/{userId}`)를 관리한다.

- **마감 결과 알림**: 마감 확정(`closeDeal`)이 `SUCCESS`/`FAILED`를 정한 뒤, 참여자 목록과 함께 `sendDealResultNotification(dealId, DEAL_SUCCESS|DEAL_FAILED, participantIds)`를 호출한다. 알림 타입 `DEAL_SUCCESS`/`DEAL_FAILED`는 알림 서비스의 enum과 일치한다.
- **주문 확인 알림**: 주문이 `CONFIRMED`되면 주문 서비스가 `sendNotification(ORDER_CONFIRMED, userId, dealId, orderId)`를 호출한다(주문 → 알림 직접 호출).
- **마감 임박 알림**: 마감 30분 전 참여자에게 `DEADLINE_ALERT`를 보낸다. 이를 누가 트리거할지(통합 스케줄러 vs 알림 서비스 자체 스케줄러)는 조율이 필요하다.
- **발송 독립성**: 알림은 Coroutine 비동기로 발송되어 호출한 서비스의 응답을 막지 않는다.

### 통합 시 결정 필요(열린 항목)

- **참여자 목록 확보 경로**: `sendDealResultNotification`은 `participantUserIds`를 인자로 받는다. 인원(HEADCOUNT) 딜은 `Redis.SMEMBERS("timedeal:{dealId}:participants")`로 모을 수 있다. 그러나 **수량/금액(QUANTITY/AMOUNT) 딜은 "딜 단위 주문자 목록"이 필요한데, 주문 서비스 API는 `orderId`/`userId` 기준 조회만 있고 `dealId` 기준 조회(`findUserIdsByDeal`)가 없다.** 주문 서비스에 해당 조회를 추가해야 수량/금액 딜의 마감 알림을 보낼 수 있다.
- **알림 채널 키 표기**: 본문 도입부는 `notification:{userId}` 채널로 느슨히 적혀 있으나 키 설계·의사코드 기준 정식 키는 `notification:channel:{userId}`다. 통합에서는 후자를 사용한다.

## API 흐름

```text
POST /auth/login (또는 GraphQL Mutation) - 로그인/세션 토큰 발급 (사용자 서비스)
GraphQL userProfile(userId)              - 프로필 조회 (사용자 서비스)
POST /deals                       - 딜 생성 (생성 서비스, 토큰 검증)
GET  /timedeals/{dealId}          - 상세 조회 (상세 서비스, buildProgress 사용)
POST /timedeals/{dealId}/join     - 인원 딜 참여 (참여 서비스, 토큰 검증)
POST /orders                      - 수량/금액 딜 주문 (주문 서비스, 토큰 검증)
(스케줄러)                         - 마감 확정 closeDeal (통합/참여 서비스)
POST /notifications/deal-result   - 마감 결과 참여자 전원 알림 (알림 서비스, 내부 호출)
POST /notifications/send          - 주문 확인 등 단건 알림 (알림 서비스, 내부 호출)
GET  /notifications/subscribe/{userId} - SSE 실시간 알림 수신 (알림 서비스)
```

## Jira 이슈 예시

```text
[통합/설계] 딜 라이프사이클 연동 구조 설계
[통합/설계] metric 기준 성공 판정기 / 모델 라우팅 의사코드 작성
[통합/기능] DealCreatedEvent 수신 및 read-model / 진행도 초기화 작성
[통합/기능] metric 기준 마감 확정(closeDeal) 작성
[통합/기능] 상세 진행도 metric 분기(buildProgress) 작성
[통합/TDD] 성공 판정기 / 모델 라우팅 Small 테스트 작성
[통합/TDD] 서비스 간 저장소 정합 Medium 테스트 작성
[통합/TDD] 생성-상세-참여/주문-마감 전체 흐름 Large 테스트 작성
```
