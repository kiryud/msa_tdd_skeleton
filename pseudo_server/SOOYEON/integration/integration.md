# 통합 — 딜 라이프사이클 연동 의사코드

네 개의 마이크로서비스를 연결하는 **글루 로직**을 정의합니다.
이 의사코드가 `small / medium / large` 통합 테스트의 기준이 됩니다.

---

## 서비스 개요

| 서비스 | 책임 | 소유 엔티티 |
|---|---|:---:|
| 딜 생성 (Creation) | 딜 최초 등록 `OPEN`, 생성 이벤트 발행 | `Deal` |
| 상세 조회 (Detail) | 딜 + 상품 + 판매자 + 진행도 조합 응답 | `Product` `Seller` |
| 참여 처리 (Participation) | 인원 기준 선착순 참여 / 취소 / 마감 | `Participation` |
| 주문 처리 (Order) | 수량 기준 선착순 주문 / 취소 | `Order` |

> [!NOTE]
> 모든 서비스를 잇는 단 하나의 공유 키는 **`dealId`** 입니다.

---

## 통합 계층의 역할

| # | 역할 | 기존 방식 | 통합 방식 |
|:-:|---|---|---|
| 1 | **선착순 모델 분기** | 서비스마다 별도 처리 | `metric` 으로 JOIN / ORDER 단일 라우팅 |
| 2 | **metric 기준 마감** | 인원(`SCARD`)만 판정 | metric별 누적값 집계 후 판정 |

---

## 사용 기술 요약

| 기술 | 이유 |
|---|---|
| 생성 이벤트 + read-model | 딜 원본은 생성 서비스만 소유, 나머지는 `DealCreatedEvent` 복제본 사용 |
| metric 전략 패턴 | 성공 판정 · 진행도 분기를 순수 함수로 분리 → Small Test 고정 가능 |
| Redis | 진행도(참여자 SET, 주문 재고)는 Redis — 원자성 보장 |
| RDBMS | 기록(참여 이력, 주문, 딜 상태)은 RDBMS — 정합 검증 대상 |

---

## 데이터 구조

```kotlin
enum class SuccessMetric { HEADCOUNT, QUANTITY, AMOUNT }
enum class ParticipationModel { JOIN, ORDER }

data class SuccessCriterion(val metric: SuccessMetric, val min: Long)

/** 각 서비스가 DealCreatedEvent로 받아 보관하는 읽기 전용 모델 */
data class DealReadModel(
    val dealId: Long,
    val metric: SuccessMetric,
    val min: Long,
    val capacity: Long?,         // AMOUNT 딜은 null
    val deadline: LocalDateTime,
    val regionCode: String
)

/** 상세 페이지 진행도 — metric에 따라 단위가 달라짐 */
data class Progress(
    val unit: SuccessMetric,
    val current: Long,
    val min: Long,
    val capacity: Long?
)

enum class SuccessResult { SUCCESS, FAILED }
```

---

## 핵심 의사코드

### 1. 성공 판정기 — `evaluateSuccess`

```text
인자   : successCriterion(metric, min), currentValue
반환값 : SUCCESS | FAILED
예외   : criterion이 null          → IllegalArgumentException
         currentValue < 0          → IllegalArgumentException
         criterion.min <= 0        → IllegalArgumentException

note   : metric 값은 분기에 사용하지 않는다 — 순수 비교만 수행
         (Small Test 3-R1 검증 대상)

evaluateSuccess(successCriterion, currentValue):
  guard successCriterion != null
  guard currentValue >= 0
  guard successCriterion.min > 0

  if currentValue >= successCriterion.min → return SUCCESS
  return FAILED
```

> 대응 테스트: `small_test` § 1(예외) · § 2(기능) · § 3(리팩터링)

---

### 2. 선착순 모델 분기 — `resolveParticipationModel`

```text
인자   : metric
반환값 : ParticipationModel (JOIN | ORDER)
예외   : metric이 null → IllegalArgumentException

resolveParticipationModel(metric):
  guard metric != null

  if metric == HEADCOUNT → return JOIN
  return ORDER            // QUANTITY · AMOUNT 모두 ORDER
```

> 대응 테스트: `small_test` § 4(예외) · § 5(기능)

---

### 3. 현재 누적값 집계 — `getCurrentValue`

```text
인자   : deal(DealReadModel)
반환값 : 현재 누적값 (인원 수 | 주문 수량 합 | 주문 금액 합)
예외   : deal이 null → IllegalArgumentException

note   : 각 metric은 정확히 하나의 소스만 조회 (Small Test 12-R1 검증 대상)

getCurrentValue(deal):
  guard deal != null

  switch deal.metric:
    HEADCOUNT → return Redis.SCARD("timedeal:{dealId}:participants")
    QUANTITY  → return OrderService.sumConfirmedQuantity(deal.dealId)
    AMOUNT    → return OrderService.sumConfirmedAmount(deal.dealId)   // [미구현]
```

> 대응 테스트: `small_test` § 6(예외) · § 7(기능)

---

### 4. 마감 확정 — `closeDeal`

```text
인자   : dealId
반환값 : DEAL_SUCCESS | DEAL_FAILED | DEAL_NOT_FOUND | DEAL_NOT_EXPIRED
예외   : deal 없음          → DealNotFoundException
         deadline 미경과    → IllegalStateException
         집계 서비스 실패   → 예외 전파 (상태 변경 없음)

closeDeal(dealId):
  deal = DealReadModel.findById(dealId)
  guard deal != null              // DealNotFoundException

  guard currentTime() >= deal.deadline   // IllegalStateException

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

> 대응 테스트: `medium_test` § 5(예외) · § 6(기능)

---

### 5. 생성 이벤트 수신 — `onDealCreated`

```text
인자   : event(DealCreatedEvent)
반환값 : 없음
예외   : event가 null         → IllegalArgumentException
         DB 저장 실패         → 예외 전파 (Redis 초기화 진행 안 함)

note   : @TransactionalEventListener(AFTER_COMMIT) — 롤백 시 실행되지 않음

onDealCreated(event):
  guard event != null

  DealReadModelRepository.save(DealReadModel.from(event))

  switch event.metric:
    QUANTITY  → Redis.SET("deal:{dealId}:stock", event.capacity)
    HEADCOUNT → return   // participants SET은 참여 시점에 생성
    AMOUNT    → return   // 재고 개념 없음
```

> 대응 테스트: `medium_test` § 1(예외) · § 2(기능) / `spring_boot_test` § 1 · § 2

---

### 6. 상세 진행도 구성 — `buildProgress`

```text
인자   : deal(DealReadModel)
반환값 : Progress(unit, current, min, capacity)
예외   : deal이 null                        → IllegalArgumentException
         QUANTITY 딜에서 재고 > capacity   → IllegalStateException

buildProgress(deal):
  guard deal != null

  switch deal.metric:
    HEADCOUNT:
      current = Redis.SCARD("timedeal:{dealId}:participants")
      return Progress(HEADCOUNT, current, deal.min, deal.capacity)

    QUANTITY:
      remaining = Redis.GET("deal:{dealId}:stock")
      guard remaining <= deal.capacity     // IllegalStateException
      return Progress(QUANTITY, deal.capacity - remaining, deal.min, deal.capacity)

    AMOUNT:                                // [미구현]
      current = OrderService.sumConfirmedAmount(deal.dealId)
      return Progress(AMOUNT, current, deal.min, null)
```

> 대응 테스트: `small_test` § 10(예외) · § 11(기능) · § 12(리팩터링)

---

### 7. 지역 일치 판정 — `isRegionMatched`

```text
인자   : userRegionCode, dealRegionCode
반환값 : Boolean
예외   : 둘 중 하나라도 null 또는 빈 문자열 → IllegalArgumentException

note   : 대소문자 구분 비교. 정규화가 필요하면 호출부에서 처리.
         참여 처리 · 상세 조회 공용 함수.

isRegionMatched(userRegionCode, dealRegionCode):
  guard userRegionCode != null && !blank
  guard dealRegionCode != null && !blank
  return userRegionCode == dealRegionCode
```

> 대응 테스트: `small_test` § 13(예외) · § 14(기능)

---

## 통합 라이프사이클

```
[딜 생성]
  POST /deals
    └─ Deal 저장(OPEN) ──► DealCreatedEvent 발행
                                  │ AFTER_COMMIT
                                  ▼
                       onDealCreated
                         ├─ DealReadModel 저장
                         └─ Redis 재고 초기화 (QUANTITY만)

[상세 조회]
  GET /timedeals/{dealId}
    └─ buildProgress(metric별) + 상품·판매자 조합 응답

[선착순 참여 / 주문]  ◄── resolveParticipationModel(metric)
  HEADCOUNT ──► POST /timedeals/{dealId}/join   → Redis SADD(participants)
  QUANTITY  ──► POST /orders                    → Redis DECRBY(stock) + RDBMS 저장
  AMOUNT    ──► POST /orders  [미구현]

[마감 확정]  @Scheduled
  deadline 경과
    └─ closeDeal
         ├─ getCurrentValue(metric별)
         ├─ evaluateSuccess
         └─ DealStatusUpdater + NotificationComponent
```

---

## 경계 계약

| 항목 | 규칙 |
|---|---|
| 딜 단일 소유 | `Deal` 쓰기는 생성 서비스만. 나머지는 `DealReadModel` 읽기 전용 |
| 이벤트 | `DealCreatedEvent(dealId, metric, min, capacity, deadline, regionCode, hostId)` — 커밋 후 발행 |
| Redis 키 | `RedisKeyConstants` 공유 상수 사용 (서비스 간 키 규약 불일치 방지) |
| 상태 전이 | `OPEN` 설정은 생성 서비스만. `SUCCESS` / `FAILED` 전이는 `closeDeal`만 수행 |
| 동시성 | 참여: AOP 분산 락(`lock:timedeal:{dealId}`) / 주문: Redis `DECRBY` 원자성 |

---

## Redis 키 규약

```kotlin
object RedisKeyConstants {
    fun participantsKey(dealId: Long) = "timedeal:$dealId:participants"  // 참여 처리 · 상세 조회 공용
    fun stockKey(dealId: Long)        = "deal:$dealId:stock"             // 주문 처리 · 통합 계층 공용
    fun lockKey(dealId: Long)         = "lock:timedeal:$dealId"          // 참여 처리 분산 락
}
```

---

## 주문 서비스 실제 사양

| 항목 | 현황 |
|---|---|
| Redis 재고 키 | `deal:{dealId}:stock` — `DECRBY` 감소 / `INCRBY` 복구 |
| 주문 상태 | `PENDING → CONFIRMED → CANCELLED` |
| RDBMS 정합 조건 | `초기재고 − 현재재고 = CONFIRMED 수량 합` — PENDING 정착 후에만 성립 |
| 동시성 방식 | Redis `DECRBY` 원자성 (AOP 분산 락 사용 안 함) |
| AMOUNT 지원 | ❌ 미지원 — `quantity`만 저장. `sumConfirmedAmount`는 `amount` 필드 추가 후 구현 |

---

## API 흐름 요약

```
POST /deals                      딜 생성 (생성 서비스)
GET  /timedeals/{dealId}         상세 조회 (상세 서비스, buildProgress)
POST /timedeals/{dealId}/join    인원 딜 참여 (참여 서비스)
DELETE /timedeals/{dealId}/join  인원 딜 참여 취소 (참여 서비스)
POST /orders                     수량·금액 딜 주문 (주문 서비스)
DELETE /orders/{orderId}         주문 취소 (주문 서비스)
(스케줄러)                        마감 확정 closeDeal (통합 계층)
```

---

## Jira 이슈 예시

```
[통합/설계] 딜 라이프사이클 연동 구조 설계
[통합/설계] metric 기준 성공 판정기 / 모델 라우팅 의사코드 작성
[통합/기능] DealCreatedEvent 수신 및 read-model / 진행도 초기화
[통합/기능] metric 기준 마감 확정(closeDeal)
[통합/기능] 상세 진행도 metric 분기(buildProgress)
[통합/기능] 스케줄러 단일 실패 전파 방지 (runCatching 적용)
[통합/TDD]  성공 판정기 / 모델 라우팅 Small 테스트
[통합/TDD]  서비스 간 저장소 정합 Medium 테스트
[통합/TDD]  생성-상세-참여/주문-마감 전체 흐름 Large 테스트
[통합/TDD]  이벤트 핸들러 / 스케줄러 Spring Boot 보강 테스트
[통합/트러블슈팅] Redis-RDBMS 정합 불일치 감지 · 경고 로깅
[통합/트러블슈팅] RedisKeyConstants 도입 — 키 규약 불일치 방지
```
