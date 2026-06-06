# 통합 Small Test

외부 저장소에 접근하지 않고, **함수 인자나 Mock 반환값을 받아 반환값을 검사**하는 수준의 테스트입니다.

| 대상 함수 | 섹션 |
|---|:---:|
| `evaluateSuccess` | § 1 · 2 · 3 |
| `resolveParticipationModel` | § 4 · 5 |
| `getCurrentValue` | § 6 · 7 |
| `DealReadModel.from` | § 8 · 9 |
| `buildProgress` | § 10 · 11 · 12 |
| `isRegionMatched` | § 13 · 14 |

> [!NOTE]
> 케이스 순서: **⚠️ 예외 처리 → ✅ 기능 구현 → 🔄 리팩터링**
> 레이블: `E` = Exception / `F` = Feature / `R` = Refactoring

> [!WARNING]
> `[AMOUNT 미구현]` 케이스는 주문 서비스에 `amount` 또는 `unitPrice` 필드 추가 후 활성화합니다.

---

## § 1 · `evaluateSuccess` — ⚠️ 예외 처리

### ⚠️ E1 · 성공 판정기에 null 기준이 주입되면 예외를 던진다

```text
Given - SuccessCriterion이 null이다.
When  - evaluateSuccess(null, 10)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "성공 기준(criterion)은 null일 수 없습니다" 메시지가 포함되어야 한다.
```

### ⚠️ E2 · 성공 판정기에 음수 currentValue가 전달되면 예외를 던진다

```text
Given - SuccessCriterion(HEADCOUNT, min=10), currentValue=-1이다.
When  - evaluateSuccess(criterion, -1)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "누적값(currentValue)은 음수일 수 없습니다" 메시지가 포함되어야 한다.
```

### ⚠️ E3 · 성공 판정기에 min이 0 이하인 기준이 주입되면 예외를 던진다

```text
Given - SuccessCriterion(HEADCOUNT, min=0)이다.
When  - evaluateSuccess(criterion, 0)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "최솟값(min)은 양수여야 합니다" 메시지가 포함되어야 한다.
```

---

## § 2 · `evaluateSuccess` — ✅ 기능 구현

### ✅ F1 · 인원(HEADCOUNT) 기준, 경계값 이상이면 SUCCESS를 반환한다

```text
Given - SuccessCriterion(metric=HEADCOUNT, min=10), currentValue=10 (경계값 동일)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.SUCCESS를 반환해야 한다.  // currentValue >= min → 성사
```

### ✅ F2 · 인원(HEADCOUNT) 기준, 경계값 미만이면 FAILED를 반환한다

```text
Given - SuccessCriterion(metric=HEADCOUNT, min=10), currentValue=9 (경계값 - 1)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.FAILED를 반환해야 한다.  // currentValue < min → 미성사
```

### ✅ F3 · 수량(QUANTITY) 기준, 경계값 이상이면 SUCCESS를 반환한다

```text
Given - SuccessCriterion(metric=QUANTITY, min=50), currentValue=50 (경계값 동일)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.SUCCESS를 반환해야 한다.
```

### ✅ F4 · 수량(QUANTITY) 기준, 경계값 미만이면 FAILED를 반환한다

```text
Given - SuccessCriterion(metric=QUANTITY, min=50), currentValue=49 (경계값 - 1)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.FAILED를 반환해야 한다.
```

### ✅ F5 · 금액(AMOUNT) 기준, 경계값 이상이면 SUCCESS를 반환한다`[AMOUNT 미구현]`

```text
Given - SuccessCriterion(metric=AMOUNT, min=30000), currentValue=30000 (경계값 동일)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.SUCCESS를 반환해야 한다.
```

### ✅ F6 · 금액(AMOUNT) 기준, 경계값 미만이면 FAILED를 반환한다 `[AMOUNT 미구현]`

```text
Given - SuccessCriterion(metric=AMOUNT, min=30000), currentValue=29999 (경계값 - 1)이다.
When  - evaluateSuccess(criterion, currentValue)를 호출한다.
Then  - SuccessResult.FAILED를 반환해야 한다.
```

---

## § 3 · `evaluateSuccess` — 🔄 리팩터링

### 🔄 R1 · 성공 판정기는 metric 값을 분기하지 않고 순수 비교만 수행한다

```text
Given - metric만 다른 두 SuccessCriterion:
          (HEADCOUNT, min=10) / (QUANTITY, min=10)
        두 케이스 모두 currentValue=10이다.
When  - evaluateSuccess를 각각 호출한다.
Then  - 두 케이스 모두 SUCCESS를 반환해야 한다.
        → 판정기가 currentValue >= min 비교만 수행함을 확인한다.
           리팩터링 후 metric별 분기가 제거됐는지 검증.
```

---

## § 4 · `resolveParticipationModel` — ⚠️ 예외 처리

### ⚠️ E1 · metric이 null이면 예외를 던진다

```text
Given - metric이 null이다.
When  - resolveParticipationModel(null)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "지원하지 않는 metric입니다" 메시지가 포함되어야 한다.
```

---

## § 5 · `resolveParticipationModel` — ✅ 기능 구현

### ✅ F1 · HEADCOUNT metric은 JOIN 모델로 라우팅된다

```text
Given - metric=HEADCOUNT이다.
When  - resolveParticipationModel(HEADCOUNT)을 호출한다.
Then  - ParticipationModel.JOIN을 반환해야 한다.
        // 인원 딜 → 참여 처리 서비스로 라우팅
```

### ✅ F2 · QUANTITY metric은 ORDER 모델로 라우팅된다

```text
Given - metric=QUANTITY이다.
When  - resolveParticipationModel(QUANTITY)을 호출한다.
Then  - ParticipationModel.ORDER를 반환해야 한다.
        // 수량 딜 → 주문 처리 서비스로 라우팅
```

### ✅ F3 · AMOUNT metric은 ORDER 모델로 라우팅된다 `[AMOUNT 미구현]`

```text
Given - metric=AMOUNT이다.
When  - resolveParticipationModel(AMOUNT)을 호출한다.
Then  - ParticipationModel.ORDER를 반환해야 한다.
        // 금액 딜 → 주문 처리 서비스로 라우팅
```

---

## § 6 · `getCurrentValue` — ⚠️ 예외 처리

### ⚠️ E1 · deal이 null이면 예외를 던진다

```text
Given - deal이 null이다.
        SCARD · 주문 집계 함수는 Mock으로 대체되어 있다.
When  - getCurrentValue(null)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        어떤 저장소 호출도 발생하지 않아야 한다.
```

---

## § 7 · `getCurrentValue` — ✅ 기능 구현

### ✅ F1 · HEADCOUNT 딜은 참여자 SCARD를 반환하고 주문 집계를 호출하지 않는다

```text
Given - deal.metric=HEADCOUNT, deal.dealId=1L이다.
        Redis.SCARD("timedeal:1:participants") Mock → 15L 반환.
        OrderService.sumConfirmedQuantity / sumConfirmedAmount Mock → 호출 감시용.
When  - getCurrentValue(deal)을 호출한다.
Then  - 15L를 반환해야 한다.
        OrderService의 어떤 메서드도 호출되지 않아야 한다.
```

### ✅ F2 · QUANTITY 딜은 주문 수량 합을 반환하고 SCARD를 호출하지 않는다

```text
Given - deal.metric=QUANTITY, deal.dealId=2L이다.
        OrderService.sumConfirmedQuantity(2L) Mock → 55L 반환.
        Redis.SCARD Mock → 호출 감시용.
When  - getCurrentValue(deal)을 호출한다.
Then  - 55L를 반환해야 한다.
        Redis.SCARD는 호출되지 않아야 한다.
```

### ✅ F3 · AMOUNT 딜은 주문 금액 합을 반환하고 SCARD를 호출하지 않는다 `[AMOUNT 미구현]`

```text
Given - deal.metric=AMOUNT, deal.dealId=3L이다.
        OrderService.sumConfirmedAmount(3L) Mock → 32000L 반환.
        Redis.SCARD Mock → 호출 감시용.
When  - getCurrentValue(deal)을 호출한다.
Then  - 32000L를 반환해야 한다.
        Redis.SCARD는 호출되지 않아야 한다.
```

---

## § 8 · `DealReadModel.from` — ⚠️ 예외 처리

### ⚠️ E1 · event가 null이면 예외를 던진다

```text
Given - DealCreatedEvent가 null이다.
When  - DealReadModel.from(null)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "이벤트는 null일 수 없습니다" 메시지가 포함되어야 한다.
```

### ⚠️ E2 · event의 dealId가 null이면 예외를 던진다

```text
Given - DealCreatedEvent에서 dealId 필드만 null이다. 나머지 필드는 유효하다.
When  - DealReadModel.from(event)를 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
```

---

## § 9 · `DealReadModel.from` — ✅ 기능 구현

### ✅ F1 · QUANTITY 이벤트가 capacity를 포함해 DealReadModel로 매핑된다

```text
Given - DealCreatedEvent(dealId=10L, metric=QUANTITY, min=50, capacity=100,
                         deadline=내일, regionCode="SEOUL_GANGNAM")이다.
When  - DealReadModel.from(event)를 호출한다.
Then  - dealId=10L, metric=QUANTITY, min=50, capacity=100,
        deadline=내일, regionCode="SEOUL_GANGNAM" 으로 매핑되어야 한다.
```

### ✅ F2 · HEADCOUNT 이벤트가 capacity를 포함해 DealReadModel로 매핑된다

```text
Given - DealCreatedEvent(dealId=11L, metric=HEADCOUNT, min=3, capacity=5,
                         deadline=내일, regionCode="BUSAN_HAEUNDAE")이다.
When  - DealReadModel.from(event)를 호출한다.
Then  - metric=HEADCOUNT, min=3, capacity=5로 매핑되어야 한다.
```

### ✅ F3 · AMOUNT 이벤트는 capacity=null로 매핑된다 `[AMOUNT 미구현]`

```text
Given - DealCreatedEvent(dealId=12L, metric=AMOUNT, min=30000, capacity=null,
                         deadline=내일, regionCode="DAEGU_JUNG")이다.
When  - DealReadModel.from(event)를 호출한다.
Then  - capacity=null로 매핑되어야 한다.
        // 금액 딜은 정원 개념이 없으므로 capacity가 없어야 한다.
```

---

## § 10 · `buildProgress` — ⚠️ 예외 처리

### ⚠️ E1 · deal이 null이면 예외를 던진다

```text
Given - deal이 null이다. 모든 저장소 Mock은 준비되어 있다.
When  - buildProgress(null)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        어떤 저장소 호출도 발생하지 않아야 한다.
```

### ⚠️ E2 · QUANTITY 딜에서 Redis 재고가 capacity를 초과하면 예외를 던진다

```text
Given - deal.metric=QUANTITY, deal.capacity=100이다.
        Redis.GET("deal:{dealId}:stock") Mock → 110 반환 (비정상 상태).
When  - buildProgress(deal)을 호출한다.
Then  - IllegalStateException을 던져야 한다.
        "현재 재고가 최대 용량을 초과했습니다" 메시지가 포함되어야 한다.
```

---

## § 11 · `buildProgress` — ✅ 기능 구현

### ✅ F1 · HEADCOUNT 딜의 진행도는 참여자 수와 단위를 그대로 반영한다

```text
Given - deal.metric=HEADCOUNT, deal.min=3, deal.capacity=5이다.
        Redis.SCARD("timedeal:{dealId}:participants") Mock → 3L 반환.
When  - buildProgress(deal)을 호출한다.
Then  - Progress(unit=HEADCOUNT, current=3, min=3, capacity=5)를 반환해야 한다.
```

### ✅ F2 · QUANTITY 딜의 진행도 current는 capacity - 남은재고이다

```text
Given - deal.metric=QUANTITY, deal.capacity=100, deal.min=50이다.
        Redis.GET("deal:{dealId}:stock") Mock → 40 반환.
        // 초기재고 100, 현재재고 40 → 처리 완료 수량 60
When  - buildProgress(deal)을 호출한다.
Then  - Progress(unit=QUANTITY, current=60, min=50, capacity=100)을 반환해야 한다.
```

### ✅ F3 · QUANTITY 딜에서 재고가 0이면 current는 capacity와 동일하다

```text
Given - deal.metric=QUANTITY, deal.capacity=100이다.
        Redis.GET Mock → 0 반환 (재고 소진).
When  - buildProgress(deal)을 호출한다.
Then  - Progress(unit=QUANTITY, current=100, capacity=100)을 반환해야 한다.
        // 재고 소진 시 진행도가 capacity와 일치해야 한다.
```

### ✅ F4 · AMOUNT 딜의 진행도는 capacity=null이며 주문 금액 합을 current로 반환한다 `[AMOUNT 미구현]`

```text
Given - deal.metric=AMOUNT, deal.min=30000, deal.capacity=null이다.
        OrderService.sumConfirmedAmount Mock → 32000L 반환.
When  - buildProgress(deal)을 호출한다.
Then  - Progress(unit=AMOUNT, current=32000, min=30000, capacity=null)을 반환해야 한다.
```

---

## § 12 · `buildProgress` — 🔄 리팩터링

### 🔄 R1 · 각 metric은 다른 저장소를 정확히 한 번만 조회한다

```text
Given - HEADCOUNT / QUANTITY / AMOUNT 딜이 각각 준비되어 있다.
        Redis.SCARD / Redis.GET / OrderService.sumConfirmedAmount Mock이 준비되어 있다.
When  - buildProgress를 각각의 deal로 호출한다.
Then  - HEADCOUNT → SCARD 1회, GET · sumConfirmedAmount 미호출
        QUANTITY  → GET 1회,   SCARD · sumConfirmedAmount 미호출
        AMOUNT    → sumConfirmedAmount 1회, SCARD · GET 미호출
        // 리팩터링 후 불필요한 저장소 호출이 없음을 검증.
```

---

## § 13 · `isRegionMatched` — ⚠️ 예외 처리

### ⚠️ E1 · userRegionCode가 null이면 예외를 던진다

```text
Given - userRegionCode=null, dealRegionCode="SEOUL_GANGNAM"이다.
When  - isRegionMatched(null, "SEOUL_GANGNAM")을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "지역 코드는 null일 수 없습니다" 메시지가 포함되어야 한다.
```

### ⚠️ E2 · dealRegionCode가 null이면 예외를 던진다

```text
Given - userRegionCode="SEOUL_GANGNAM", dealRegionCode=null이다.
When  - isRegionMatched("SEOUL_GANGNAM", null)을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
```

### ⚠️ E3 · 지역 코드가 빈 문자열이면 예외를 던진다

```text
Given - userRegionCode="", dealRegionCode="SEOUL_GANGNAM"이다.
When  - isRegionMatched("", "SEOUL_GANGNAM")을 호출한다.
Then  - IllegalArgumentException을 던져야 한다.
        "지역 코드는 빈 값일 수 없습니다" 메시지가 포함되어야 한다.
```

---

## § 14 · `isRegionMatched` — ✅ 기능 구현

### ✅ F1 · 지역 코드가 다르면 false를 반환한다

```text
Given - userRegionCode="SEOUL_MAPO", dealRegionCode="SEOUL_GANGNAM"이다.
When  - isRegionMatched(userRegionCode, dealRegionCode)를 호출한다.
Then  - false를 반환해야 한다.
        // 사용자 동네와 딜 지역이 다름 → 참여 불가
```

### ✅ F2 · 지역 코드가 동일하면 true를 반환한다

```text
Given - userRegionCode="SEOUL_GANGNAM", dealRegionCode="SEOUL_GANGNAM"이다.
When  - isRegionMatched(userRegionCode, dealRegionCode)를 호출한다.
Then  - true를 반환해야 한다.
        // 사용자 동네와 딜 지역이 같음 → 참여 가능
```

### ✅ F3 · 대소문자가 다른 지역 코드는 불일치로 판정한다

```text
Given - userRegionCode="seoul_gangnam", dealRegionCode="SEOUL_GANGNAM"이다.
When  - isRegionMatched(userRegionCode, dealRegionCode)를 호출한다.
Then  - false를 반환해야 한다.
        // 정규화 없이 대소문자 구분 비교가 적용됨을 확인.
```

---

> [!TIP]
> **리팩터링 노트**
> - `isRegionMatched` 가 단순 동등 비교만 하므로, 정규화가 필요하면 호출부에서 처리하거나 별도 `RegionCodeNormalizer` 를 도입한다.
> - `buildProgress` 의 Redis 키 상수는 `RedisKeyConstants` 로 추출해 서비스 간 키 규약 불일치를 방지한다.
> - `DealReadModel.from` 매핑 테스트는 이벤트 필드가 추가될 때마다 반드시 동반해 회귀를 막는다.
