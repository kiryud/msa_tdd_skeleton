# 통합 Large Test

실제 API(POST · GET 등) 호출 이상의 크기로, **생성부터 마감까지 여러 서비스를 거치는 전체 흐름**을 검증합니다.
실제 HTTP 요청·응답 기준으로 작성하며, 서비스 경계를 넘나드는 데이터 일관성까지 확인합니다.

| 시나리오 | 섹션 |
|---|:---:|
| 인증 실패 (공통) | § 1 |
| 딜 생성 실패 | § 2 |
| 상세 조회 실패 | § 3 |
| HEADCOUNT 딜 — 전체 흐름 | § 4 |
| QUANTITY 딜 — 전체 흐름 | § 5 |
| AMOUNT 딜 — 전체 흐름 | § 6 |
| 동시성 통합 | § 7 · 8 |
| 취소 통합 | § 9 · 10 · 11 |

> [!NOTE]
> 케이스 순서: **⚠️ 예외 처리 → ✅ 기능 구현 → 🔄 리팩터링**

> [!WARNING]
> **§ 6 (AMOUNT 딜)** 은 주문 서비스에 `amount` 또는 `unitPrice` 필드 추가 후 활성화합니다.

---

## § 1 · 인증 실패 — ⚠️ 예외 처리 (공통)

### ⚠️ E1 · 인증 토큰 없이 참여 API를 호출하면 UNAUTHORIZED를 반환한다

```text
Given - Authorization 헤더가 없는 요청이다.
When  - POST /timedeals/{dealId}/join 을 호출한다.
Then  - HTTP 401 UNAUTHORIZED를 반환해야 한다.
        "인증이 필요합니다" 메시지가 응답에 포함되어야 한다.
        참여 로직(Redis SADD, DB 저장)이 실행되지 않아야 한다.
```

### ⚠️ E2 · 인증 토큰 없이 주문 API를 호출하면 UNAUTHORIZED를 반환한다

```text
Given - Authorization 헤더가 없는 요청이다.
When  - POST /orders 를 호출한다.
Then  - HTTP 401 UNAUTHORIZED를 반환해야 한다.
        주문 로직(재고 감소, DB 저장)이 실행되지 않아야 한다.
```

### ⚠️ E3 · 만료된 토큰으로 참여 API를 호출하면 UNAUTHORIZED를 반환한다

```text
Given - 만료 시간이 지난 JWT 토큰이 Authorization 헤더에 포함되어 있다.
When  - POST /timedeals/{dealId}/join 을 호출한다.
Then  - HTTP 401 UNAUTHORIZED를 반환해야 한다.
        "토큰이 만료되었습니다" 메시지가 응답에 포함되어야 한다.
```

---

## § 2 · 딜 생성 실패 — ⚠️ 예외 처리

### ⚠️ E1 · 지역 코드 없이 딜 생성을 요청하면 BAD_REQUEST를 반환한다

```text
Given - regionCode 필드가 빠진 딜 생성 요청 바디이다.
When  - POST /deals 를 호출한다.
Then  - HTTP 400 BAD_REQUEST를 반환해야 한다.
        "regionCode는 필수입니다" 메시지가 포함되어야 한다.
        DB에 Deal 레코드가 저장되지 않아야 한다.
        DealCreatedEvent가 발행되지 않아야 한다.
```

### ⚠️ E2 · 마감 시간이 현재보다 과거인 딜 생성을 요청하면 BAD_REQUEST를 반환한다

```text
Given - deadline이 현재 시각보다 1시간 이전인 딜 생성 요청이다.
When  - POST /deals 를 호출한다.
Then  - HTTP 400 BAD_REQUEST를 반환해야 한다.
        "마감 시간은 현재 시각 이후여야 합니다" 메시지가 포함되어야 한다.
```

### ⚠️ E3 · 동일 Idempotency-Key로 딜을 두 번 생성하면 두 번째는 기존 결과를 반환한다

```text
Given - Idempotency-Key: "key-abc"로 첫 번째 POST /deals 가 성공했다.
When  - 동일한 Idempotency-Key: "key-abc"로 POST /deals 를 다시 호출한다.
Then  - HTTP 200 OK와 함께 첫 번째 응답과 동일한 dealId를 반환해야 한다.
        DB에 Deal 레코드가 1건만 존재해야 한다.
        DealCreatedEvent가 두 번 발행되지 않아야 한다.
```

---

## § 3 · 상세 조회 실패 — ⚠️ 예외 처리

### ⚠️ E1 · 존재하지 않는 dealId로 상세 조회를 요청하면 NOT_FOUND를 반환한다

```text
Given - dealId=999L은 DB에 존재하지 않는다.
When  - GET /timedeals/999 를 호출한다.
Then  - HTTP 404 NOT_FOUND를 반환해야 한다.
        "존재하지 않는 딜입니다" 메시지가 포함되어야 한다.
```

### ⚠️ E2 · 다른 지역 사용자가 상세 조회를 요청하면 FORBIDDEN을 반환한다

```text
Given - dealRegionCode="SEOUL_GANGNAM"인 딜이 존재한다.
        사용자의 userRegionCode="BUSAN_HAEUNDAE"이다.
When  - 해당 사용자가 GET /timedeals/{dealId} 를 호출한다.
Then  - HTTP 403 FORBIDDEN을 반환해야 한다.
        "해당 지역 딜에 접근할 수 없습니다" 메시지가 포함되어야 한다.
```

---

## § 4 · HEADCOUNT 딜: 생성 → 상세 → 참여 → 마감 — ✅ 기능 구현

### ✅ F1 · HEADCOUNT 딜 생성 후 상세 조회 시 진행도 단위가 인원이고 current=0이다

```text
Given - 딜 생성 요청: metric=HEADCOUNT, min=3, capacity=5,
                      deadline=1시간 후, regionCode="SEOUL_MAPO"
When  - POST /deals 로 딜을 생성한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - HTTP 201 CREATED와 함께 dealId가 반환되어야 한다.
        상세 응답: status=OPEN, progress.unit=HEADCOUNT, progress.current=0,
                   progress.min=3, progress.capacity=5
```

### ✅ F2 · HEADCOUNT 딜에 min만큼 참여하면 진행도가 채워지고 READY_TO_CONFIRM 상태가 된다

```text
Given - HEADCOUNT 딜(min=3, capacity=5)이 OPEN 상태로 존재한다.
When  - 서로 다른 사용자 3명이 각각 POST /timedeals/{dealId}/join 을 호출한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - 3번의 참여 요청 모두 HTTP 200 OK를 반환해야 한다.
        상세 응답: progress.current=3, status=READY_TO_CONFIRM
```

### ✅ F3 · HEADCOUNT 딜이 마감 확정되면 SUCCESS로 전이되고 알림이 발송된다

```text
Given - HEADCOUNT 딜에 3명이 참여해 READY_TO_CONFIRM 상태이다.
        deadline이 경과했다.
When  - closeDeal 스케줄러(또는 직접 호출)가 실행된다.
Then  - DealRepository의 status=SUCCESS로 변경되어야 한다.
        NotificationService.sendSuccess(dealId)가 호출되어야 한다.
        이후 GET /timedeals/{dealId} 응답의 status=SUCCESS여야 한다.
```

---

## § 5 · QUANTITY 딜: 생성 → 상세 → 주문 → 마감 — ✅ 기능 구현

### ✅ F1 · QUANTITY 딜 생성 후 상세 조회 시 진행도 단위가 수량이고 current=0이다

```text
Given - 딜 생성 요청: metric=QUANTITY, min=50, capacity=100,
                      deadline=2시간 후, regionCode="INCHEON_NAMDONG"
When  - POST /deals 로 딜을 생성한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - 상세 응답: progress.unit=QUANTITY, progress.current=0, progress.capacity=100
        Redis "deal:{dealId}:stock"이 100으로 초기화되어야 한다.
        // 단위가 인원이 아닌 수량이어야 한다.
```

### ✅ F2 · QUANTITY 딜에 주문이 누적되면 진행도 current가 증가하고 재고가 감소한다

```text
Given - QUANTITY 딜(capacity=100)이 OPEN 상태로 존재한다.
When  - 사용자들이 quantity=5 주문 11건(합계 55)을 POST /orders 로 제출한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - Redis "deal:{dealId}:stock"이 45로 감소해야 한다.
        상세 응답의 progress.current=55여야 한다.
```

### ✅ F3 · QUANTITY 딜 마감 시 주문 수량 합이 min 이상이면 SUCCESS로 전이된다

```text
Given - QUANTITY 딜(min=50)에 CONFIRMED 수량 합=55가 존재하고, deadline이 경과했다.
When  - closeDeal이 metric=QUANTITY로 주문 수량 합을 집계해 판정한다.
Then  - 55 >= 50이므로 status=SUCCESS로 전이되어야 한다.
        NotificationService.sendSuccess가 호출되어야 한다.
```

---

## § 6 · AMOUNT 딜: 생성 → 주문 → 마감 `[AMOUNT 미구현]`

### ✅ F1 · AMOUNT 딜 생성 후 진행도 단위가 금액이고 capacity=null이다 `[AMOUNT 미구현]`

```text
Given - 딜 생성 요청: metric=AMOUNT, min=30000, capacity=null,
                      deadline=3시간 후, regionCode="DAEGU_JUNG"
When  - POST /deals 로 딜을 생성한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - 상세 응답: progress.unit=AMOUNT, progress.capacity=null
        Redis 재고 키가 생성되지 않아야 한다.
```

### ✅ F2 · AMOUNT 딜, 합계 금액 >= min이면 마감 시 SUCCESS로 전이된다 `[AMOUNT 미구현]`

```text
Given - AMOUNT 딜(min=30000)에 CONFIRMED 금액 합=32000이 존재하고, deadline이 경과했다.
When  - closeDeal이 metric=AMOUNT로 주문 금액 합을 집계한다.
Then  - 32000 >= 30000이므로 status=SUCCESS로 전이되어야 한다.
```

### ✅ F3 · AMOUNT 딜, 합계 금액 < min이면 마감 시 FAILED로 전이된다 `[AMOUNT 미구현]`

```text
Given - AMOUNT 딜(min=30000)에 CONFIRMED 금액 합=29000이 존재하고, deadline이 경과했다.
When  - closeDeal이 주문 금액 합을 집계한다.
Then  - 29000 < 30000이므로 status=FAILED로 전이되어야 한다.
        NotificationService.sendFailed가 호출되어야 한다.
```

---

## § 7 · 동시성 통합 — ⚠️ 예외 처리

### ⚠️ E1 · 재고 소진 후 추가 주문은 재고 부족 응답을 반환한다

```text
Given - QUANTITY 딜(재고=10)이 존재한다.
        이미 10건의 주문이 처리되어 재고=0이다.
When  - 11번째 사용자가 quantity=1로 POST /orders 를 호출한다.
Then  - HTTP 409 CONFLICT (또는 400 BAD_REQUEST)를 반환해야 한다.
        "재고가 부족합니다" 메시지가 포함되어야 한다.
        Redis 재고가 음수가 되지 않아야 한다.
```

---

## § 8 · 동시성 통합 — ✅ 기능 구현

### ✅ F1 · 100명이 동시에 quantity=1 주문 시 재고(10개)만큼만 CONFIRMED 처리된다

```text
Given - QUANTITY 딜(재고=10, min=10)이 존재한다.
        서로 다른 사용자 100명이 각각 quantity=1로 동시에 주문을 요청한다.
When  - 100개 요청이 동시에 처리된다.
Then  - CONFIRMED 주문이 정확히 10건이어야 한다.
        나머지 90건은 재고 부족으로 실패해야 한다.
        최종 Redis 재고가 0이어야 한다. (음수 없음)
        // Redis DECRBY 원자성으로 동시성이 보장되어야 한다.
```

### ✅ F2 · 동시성 처리 후 마감 시 CONFIRMED 수량 합 기준으로 SUCCESS가 확정된다

```text
Given - § 8-F1 이후 상태: CONFIRMED 주문 10건, 재고=0, deadline 경과.
When  - closeDeal이 실행된다.
Then  - CONFIRMED 수량 합(10) >= min(10)이므로 status=SUCCESS로 전이되어야 한다.
```

---

## § 9 · 취소 통합 — ⚠️ 예외 처리

### ⚠️ E1 · 참여하지 않은 사용자가 참여 취소를 요청하면 NOT_FOUND를 반환한다

```text
Given - userId=999는 딜에 참여한 이력이 없다.
When  - DELETE /timedeals/{dealId}/join (userId=999)을 호출한다.
Then  - HTTP 404 NOT_FOUND를 반환해야 한다.
        Redis 참여자 SET과 RDBMS 이력에 변동이 없어야 한다.
```

### ⚠️ E2 · FAILED 상태 딜의 주문을 취소하면 BAD_REQUEST를 반환한다

```text
Given - 딜이 이미 FAILED 상태로 마감되었다.
        해당 딜의 주문이 DB에 존재한다.
When  - DELETE /orders/{orderId} 를 호출한다.
Then  - HTTP 400 BAD_REQUEST를 반환해야 한다.
        "마감된 딜의 주문은 취소할 수 없습니다" 메시지가 포함되어야 한다.
        Redis 재고와 주문 상태에 변동이 없어야 한다.
```

---

## § 10 · 취소 통합 — ✅ 기능 구현

### ✅ F1 · HEADCOUNT 딜 참여 취소 시 진행도가 줄고 min 미달이면 OPEN으로 되돌아간다

```text
Given - HEADCOUNT 딜(min=3)이 인원 3명으로 READY_TO_CONFIRM 상태이다.
When  - 참여자 1명이 DELETE /timedeals/{dealId}/join 으로 참여를 취소한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - 상세 응답의 progress.current=2로 줄어야 한다.
        status가 OPEN으로 되돌아가야 한다.
        Redis SCARD("timedeal:{dealId}:participants")=2여야 한다.
```

### ✅ F2 · QUANTITY 딜 주문 취소 시 재고가 복구되고 진행도 current가 감소한다

```text
Given - QUANTITY 딜(capacity=100)에서 quantity=5 주문이 CONFIRMED 처리된 후 재고=95이다.
When  - 해당 주문을 DELETE /orders/{orderId} 로 취소한다.
        GET /timedeals/{dealId} 로 상세를 조회한다.
Then  - Redis 재고가 100으로 복구되어야 한다.
        상세 응답의 progress.current=0으로 감소해야 한다.
        RDBMS의 Order 상태가 CANCELLED로 변경되어야 한다.
```

---

## § 11 · 취소 통합 — 🔄 리팩터링

### 🔄 R1 · 참여 취소와 주문 취소는 동일한 인터페이스를 통해 진행도를 갱신한다

```text
Given - ProgressRollbackService가 공통 인터페이스로 분리되어 있다.
        HEADCOUNT 딜 참여 취소와 QUANTITY 딜 주문 취소가 각각 실행된다.
When  - 두 취소 요청이 처리된다.
Then  - 두 케이스 모두 ProgressRollbackService.rollback(dealId, delta)를 통해 처리되어야 한다.
        취소 후 상세 진행도 current가 각각 올바르게 감소해야 한다.
        // 리팩터링 후 취소 로직이 단일 인터페이스로 통합됐음을 확인.
```
