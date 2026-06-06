# 통합 — 딜 라이프사이클 연동

네 개의 마이크로서비스(딜 생성 / 상세 조회 / 참여 처리 / 주문 처리)를 하나의 서비스로 합칠 때, 서비스 사이를 잇는 **연동(글루) 로직**을 정의하는 문서입니다. 이 의사코드를 기준으로 통합 테스트(small / medium / large)가 작성됩니다.

## 통합 서비스 개요

| 서비스 | 책임 | 소유 엔티티 |
|---|---|---|
| 딜 생성 (Creation) | 딜 최초 등록(OPEN), 생성 이벤트 발행 | `Deal` |
| 상세 조회 (Detail) | 딜+상품+판매자+진행도 조합 응답 | `Product`, `Seller` |
| 참여 처리 (Participation) | 인원 기준 선착순 참여/취소/마감 | `Participation` |
| 주문 처리 (Order) | 수량/금액 기준 선착순 주문/취소 | `Order` |

모든 서비스를 잇는 단 하나의 공유 키는 **`dealId`** 입니다. 딜 생성이 `dealId`를 만들고, 나머지 서비스는 이를 참조합니다.

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

    if result == SUCCESS:
        DealRepository.updateStatus(dealId, SUCCESS)
        NotificationService.sendSuccess(dealId)
        return DEAL_SUCCESS

    DealRepository.updateStatus(dealId, FAILED)
    NotificationService.sendFailed(dealId)
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
    DealReadModelRepository.save(DealReadModel.from(event))

    switch event.metric:
        case QUANTITY:
            Redis.SET("deal:" + event.dealId + ":stock", event.capacity)
        case AMOUNT:
            return
        case HEADCOUNT:
            return
```

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

## 통합 라이프사이클

```text
[딜 생성 Creation]
   POST /deals -> Deal 저장(OPEN) -> DealCreatedEvent 발행
        | dealId, metric, min, capacity, deadline, regionCode
        v
   onDealCreated: 각 서비스 DealReadModel 저장 + 진행도 초기화
        v
[상세 조회 Detail] --buildProgress--> metric별 진행도 + 상품/판매자
        v  (resolveParticipationModel(metric)로 분기)
   +-------------------+--------------------+
   | JOIN (HEADCOUNT)  | ORDER (QUANTITY/AMOUNT)
   v                   v
[참여 처리]          [주문 처리]
 Redis SET           Redis 재고 / 주문 저장
        +---------+---------+
                  v  deadline 경과
            closeDeal: getCurrentValue(metric별) -> evaluateSuccess -> SUCCESS/FAILED
```

## 경계 계약

- **딜 단일 소유**: `Deal` 쓰기 소유자는 생성 서비스뿐. 나머지는 `DealReadModel`(이벤트 복제본)을 읽는다.
- **이벤트**: `DealCreatedEvent(dealId, metric, min, capacity, deadline, regionCode, hostId)` 커밋 후 발행.
- **Redis 키 규약**: 참여 `timedeal:{dealId}:participants`, 재고 `deal:{dealId}:stock`, 락 `lock:timedeal:{dealId}`.
- **상태 소유**: 생성은 OPEN만. `READY_TO_CONFIRM`/`SUCCESS`/`FAILED` 전이는 마감 확정(`closeDeal`)만 수행.

## 주문 서비스 실제 사양 반영

`order_service.md`를 확인해 주문 연동 부분을 실제 사양에 맞춘다.

- **Redis 재고 키**: `deal:{dealId}:stock` (DECRBY 감소, INCRBY 복구). 참여 진행도 키 `timedeal:{dealId}:participants`와 접두어가 달라(`deal:` vs `timedeal:`) 통합 시 키 규약 통일이 안전하다.
- **주문 상태**: `PENDING → CONFIRMED → (취소) CANCELLED` 3단계. 주문은 DECRBY 후 PENDING으로 저장됐다가 CONFIRMED로 갱신된다. 따라서 "초기재고 − 현재재고 = CONFIRMED 수량 합" 정합은 PENDING이 모두 정착된 뒤에만 성립한다.
- **동시성 방식**: 주문은 분산 락 없이 Redis `DECRBY` 원자성으로 선착순을 보장한다(참여 처리의 AOP 락과 다름).
- **금액(AMOUNT) 미지원**: 현재 주문 데이터는 `quantity`만 저장하고 금액/단가 필드가 없다. 수량 집계(`sumConfirmedQuantity`)는 가능하지만, **금액 집계(`sumConfirmedAmount`)와 배달 공구(AMOUNT) 마감은 주문에 `amount`(또는 `unitPrice`) 추가가 선행되어야 한다.** 추가 시 `금액 합 = Σ(quantity × unitPrice)`로 계산한다. 그 전까지 AMOUNT 경로는 미구현으로 표시한다.

## API 흐름

```text
POST /deals                       - 딜 생성 (생성 서비스)
GET  /timedeals/{dealId}          - 상세 조회 (상세 서비스, buildProgress 사용)
POST /timedeals/{dealId}/join     - 인원 딜 참여 (참여 서비스)
POST /orders                      - 수량/금액 딜 주문 (주문 서비스)
(스케줄러)                         - 마감 확정 closeDeal (통합/참여 서비스)
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
