# 통합 Medium Test

다른 인스턴스에 무언가를 **요청하는 경우** (DB · Redis I/O) 를 검증합니다.
한 서비스가 쓴 데이터를 다른 서비스가 올바르게 읽는지, 그리고 **저장소 간 정합**이 유지되는지 확인합니다.

| 검증 대상 | 섹션 |
|---|:---:|
| `DealReadModel` 저장 / 생성 이벤트 수신 | § 1 · 2 |
| 참여 Redis 키 규약 정합 | § 3 · 4 |
| 주문 집계 ↔ 마감 판정 | § 5 · 6 |
| Redis · RDBMS 정합 | § 7 · 8 · 9 |
| 재고 초기화 | § 10 |

> [!NOTE]
> 케이스 순서: **⚠️ 예외 처리 → ✅ 기능 구현 → 🔄 리팩터링**

> [!WARNING]
> `[AMOUNT 미구현]` 케이스는 주문 서비스에 `amount` 또는 `unitPrice` 필드 추가 후 활성화합니다.

---

## § 1 · `DealReadModel` 저장 — ⚠️ 예외 처리

### ⚠️ E1 · 존재하지 않는 dealId로 DealReadModel을 조회하면 빈 결과를 반환한다

```text
Given - DealReadModel 저장소(DB)에 dealId=999L에 해당하는 레코드가 없다.
When  - DealReadModelRepository.findById(999L)을 호출한다.
Then  - Optional.empty() 또는 null을 반환해야 한다.
        예외가 발생하지 않아야 한다.
```

### ⚠️ E2 · 같은 dealId로 DealReadModel을 중복 저장하면 레코드가 1건만 유지된다 (멱등성)

```text
Given - DealCreatedEvent(dealId=1L, metric=HEADCOUNT)로 DealReadModel이 이미 저장되어 있다.
        동일한 dealId=1L 이벤트가 재전달된다 (재시도 시나리오).
When  - DealReadModelRepository.save(DealReadModel.from(event))를 다시 호출한다.
Then  - 저장소에 dealId=1L 레코드가 정확히 1건만 존재해야 한다.
        저장 결과는 두 번째 이벤트의 값으로 덮어쓰여야 한다.
        // 이벤트 재전달 시 중복 레코드가 쌓이지 않아야 한다.
```

### ⚠️ E3 · DealReadModel 저장 중 DB 오류가 발생하면 예외가 전파되고 Redis 초기화가 중단된다

```text
Given - DealReadModelRepository.save가 DataAccessException을 던지도록 설정되어 있다.
When  - onDealCreated(event)가 실행된다.
Then  - DataAccessException이 전파되어야 한다.
        Redis 재고 초기화는 실행되지 않아야 한다.
        // DB 저장 실패 시 후속 초기화가 진행되면 안 된다.
```

---

## § 2 · `DealReadModel` 저장 — ✅ 기능 구현

### ✅ F1 · 생성된 딜을 다른 서비스가 dealId로 동일하게 읽는다

```text
Given - 딜 생성 서비스가 DealCreatedEvent(dealId=10L, metric=QUANTITY, min=50, capacity=100,
                                          deadline=내일 14:00, regionCode="SEOUL_GANGNAM")를 발행한다.
        각 서비스가 onDealCreated 핸들러를 통해 DealReadModel을 저장한다.
When  - 참여 · 상세 · 주문 서비스가 각자의 저장소에서 dealId=10L을 조회한다.
Then  - 세 서비스의 DealReadModel이 모두 동일해야 한다.
        metric=QUANTITY, min=50, capacity=100, deadline=내일 14:00, regionCode="SEOUL_GANGNAM"
```

### ✅ F2 · QUANTITY 딜 생성 이벤트가 수신되면 Redis에 재고가 초기화된다

```text
Given - DealCreatedEvent(dealId=20L, metric=QUANTITY, capacity=100)이 발행된다.
When  - onDealCreated 핸들러가 이벤트를 처리한다.
Then  - Redis "deal:20:stock" 값이 100으로 설정되어야 한다.
```

### ✅ F3 · HEADCOUNT 딜 생성 이벤트가 수신되면 Redis 재고 키는 생성되지 않는다

```text
Given - DealCreatedEvent(dealId=21L, metric=HEADCOUNT, capacity=5)이 발행된다.
When  - onDealCreated 핸들러가 이벤트를 처리한다.
Then  - DealReadModel은 저장되어야 한다.
        Redis "deal:21:stock" 키는 존재하지 않아야 한다.
        // HEADCOUNT 딜은 Redis SET 재고를 사용하지 않는다.
```

---

## § 3 · 참여 키 규약 정합 — ⚠️ 예외 처리

### ⚠️ E1 · 참여 키 형식이 잘못된 경우 상세 서비스의 SCARD는 0을 반환한다

```text
Given - 참여 서비스가 "timedeal:abc:participants" (비정상 키)에 SADD를 수행했다.
        상세 서비스는 dealId=1L 기준 ("timedeal:1:participants")으로 SCARD를 조회한다.
When  - 상세 서비스가 buildProgress(deal)을 호출한다.
Then  - SCARD는 0을 반환해야 한다.
        // 키 불일치 → 데이터 없음으로 처리되어야 한다.
```

---

## § 4 · 참여 키 규약 정합 — ✅ 기능 구현

### ✅ F1 · 참여 서비스가 쓴 Redis 키를 상세 서비스가 같은 규약으로 읽는다

```text
Given - 참여 서비스가 "timedeal:30:participants"에 userId 15개를 SADD 했다.
When  - 상세 서비스가 buildProgress(deal.dealId=30)에서 SCARD로 동일 키를 조회한다.
Then  - 진행도 current=15가 반환되어야 한다.
        // 두 서비스가 동일한 Redis 키 규약을 공유함을 확인.
```

### ✅ F2 · 동일 사용자가 중복 참여해도 SCARD는 중복 없이 집계된다

```text
Given - userId=100이 "timedeal:30:participants"에 이미 SADD 되어 있다.
When  - 동일 userId=100으로 SADD를 한 번 더 시도한다.
Then  - SCARD 결과가 1이어야 한다.
        // Redis SET의 중복 방지 특성으로 인원이 과집계되지 않아야 한다.
```

---

## § 5 · 주문 집계 ↔ 마감 판정 — ⚠️ 예외 처리

### ⚠️ E1 · closeDeal 호출 시 DealReadModel이 없으면 예외가 발생하고 상태가 변경되지 않는다

```text
Given - DealReadModel 저장소에 dealId=999L 레코드가 없다.
When  - closeDeal(999L)을 호출한다.
Then  - DealNotFoundException이 발생해야 한다.
        딜 상태 변경 · 알림 요청은 발생하지 않아야 한다.
```

### ⚠️ E2 · closeDeal 호출 시 마감 시간이 지나지 않았으면 예외가 발생한다

```text
Given - deal.deadline이 현재 시각 + 1시간인 딜이 저장되어 있다.
When  - closeDeal(dealId)을 호출한다.
Then  - IllegalStateException이 발생해야 한다.
        딜 상태가 변경되지 않아야 한다.
```

### ⚠️ E3 · 주문 집계 서비스 호출이 실패하면 closeDeal이 예외를 전파하고 상태를 변경하지 않는다

```text
Given - QUANTITY 딜이 마감 대상이다.
        OrderAggregateClient.sumConfirmedQuantity가 ServiceUnavailableException을 던지도록 설정.
When  - closeDeal(dealId)이 실행된다.
Then  - 예외가 호출부로 전파되어야 한다.
        딜 상태가 변경되지 않아야 한다.
        // 집계 실패 시 잘못된 마감 확정이 되지 않아야 한다.
```

---

## § 6 · 주문 집계 ↔ 마감 판정 — ✅ 기능 구현

### ✅ F1 · QUANTITY 딜에서 주문 수량 합이 min 이상이면 딜이 SUCCESS로 전이된다

```text
Given - QUANTITY 딜(min=50)이 저장되어 있고, deadline이 이미 경과했다.
        주문 서비스에 CONFIRMED 상태의 주문 수량 합=55가 저장되어 있다.
When  - closeDeal(dealId)이 getCurrentValue로 주문 수량 합을 조회한다.
Then  - DealRepository의 status가 SUCCESS로 변경되어야 한다.
        NotificationService.sendSuccess(dealId)가 호출되어야 한다.
```

### ✅ F2 · QUANTITY 딜에서 주문 수량 합이 min 미만이면 딜이 FAILED로 전이된다

```text
Given - QUANTITY 딜(min=50)이 저장되어 있고, deadline이 이미 경과했다.
        주문 서비스에 CONFIRMED 상태의 주문 수량 합=40이 저장되어 있다.
When  - closeDeal(dealId)이 주문 수량 합을 조회한다.
Then  - DealRepository의 status가 FAILED로 변경되어야 한다.
        NotificationService.sendFailed(dealId)가 호출되어야 한다.
```

### ✅ F3 · AMOUNT 딜에서 주문 금액 합이 min 이상이면 딜이 SUCCESS로 전이된다 `[AMOUNT 미구현]`

```text
Given - AMOUNT 딜(min=30000)이 저장되어 있고, deadline이 이미 경과했다.
        주문 서비스에 CONFIRMED 금액 합=32000이 저장되어 있다.
When  - closeDeal(dealId)이 주문 금액 합을 조회한다.
Then  - DealRepository의 status가 SUCCESS로 변경되어야 한다.
```

---

## § 7 · Redis–RDBMS 정합 — ⚠️ 예외 처리

### ⚠️ E1 · Redis 참여자 수와 RDBMS 참여 이력 수가 다르면 정합 오류로 감지된다

```text
Given - Redis "timedeal:40:participants" SCARD=10이다.
        RDBMS Participation 테이블에 dealId=40, status=JOINED 이력이 8건뿐이다.
When  - 정합 검증 로직이 실행된다.
Then  - 두 값이 다름을 감지하고 경고 로그 또는 정합 오류 이벤트를 발행해야 한다.
        // 정합 불일치가 무시되지 않아야 한다.
```

---

## § 8 · Redis–RDBMS 정합 — ✅ 기능 구현

### ✅ F1 · 참여 성공 N건 후 Redis 참여자 수와 RDBMS 이력 수가 일치한다

```text
Given - HEADCOUNT 딜에 참여 성공이 5건 처리되었다.
When  - Redis "timedeal:{dealId}:participants" SCARD와
        RDBMS Participation 테이블의 JOINED 이력 수를 각각 조회한다.
Then  - 두 값이 모두 5로 같아야 한다.
        // Redis와 RDBMS가 동기화되어야 한다.
```

### ✅ F2 · 주문 성공 M건 후 재고 감소분과 CONFIRMED 수량 합이 일치한다

```text
Given - QUANTITY 딜(초기재고=100)에 quantity=2 주문 5건이 CONFIRMED 처리되었다.
        모든 PENDING 주문이 정착(CONFIRMED 또는 CANCELLED)되었다.
When  - (초기재고 - 현재 Redis 재고)와 RDBMS의 CONFIRMED 수량 합을 조회한다.
Then  - (100 - 90) = 10 과 CONFIRMED 수량 합(2×5=10)이 같아야 한다.
        // Redis 원자성 감소와 RDBMS 기록이 정합해야 한다.
```

---

## § 9 · Redis–RDBMS 정합 — 🔄 리팩터링

### 🔄 R1 · 정합 검증 로직은 비즈니스 로직과 분리된 전용 컴포넌트에서 실행된다

```text
Given - ConsistencyChecker 컴포넌트가 별도 Bean으로 분리되어 있다.
When  - ConsistencyChecker.verify(dealId)를 직접 호출한다.
Then  - DealLifecycleService · OrderAggregateClient에 의존하지 않고
        독립적으로 Redis와 RDBMS를 조회해 비교 결과를 반환해야 한다.
        // 관심사 분리 확인.
```

---

## § 10 · 재고 초기화 — ✅ 기능 구현

### ✅ F1 · QUANTITY 딜 생성 이벤트 수신 후 Redis 재고가 정확히 초기화된다

```text
Given - 딜 생성 서비스가 DealCreatedEvent(dealId=50L, metric=QUANTITY, capacity=100)를 발행한다.
When  - 주문 서비스의 onDealCreated 핸들러가 이벤트를 처리한다.
Then  - Redis "deal:50:stock" 값이 정확히 100이어야 한다.
```

### ✅ F2 · 이미 재고가 있는 딜에 이벤트가 재전달되면 재고가 capacity로 덮어쓰여진다

```text
Given - "deal:50:stock"에 이미 70이 저장되어 있다 (주문 30건 처리 상태).
        동일 이벤트(dealId=50L, capacity=100)가 재전달된다.
When  - onDealCreated 핸들러가 이벤트를 재처리한다.
Then  - "deal:50:stock"이 100으로 초기화되어야 한다.
        재처리 사실을 경고 로그로 남겨야 한다.
        // 이벤트 재처리 시 멱등성이 보장되어야 한다.
```
