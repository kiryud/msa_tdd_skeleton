# 통합 Large Test

Large Test는 실제 API(POST, GET 등) 호출 이상의 크기로, 생성부터 마감까지 여러 서비스를 거치는 **전체 흐름**을 검증합니다.

> 테스트 3(AMOUNT, 배달 공구)은 주문에 amount 필드 추가가 선행되어야 실행 가능하다(현재 order_service.md는 quantity만 저장).

## 테스트 1. 인원(HEADCOUNT) 딜: 생성 → 상세 → 참여 → 마감 성공

```text
// Given - POST /deals로 HEADCOUNT 딜(min=3, capacity=5)을 생성한다.
// When  - GET /timedeals/{dealId}로 상세를 조회한다.
// Then  - status=OPEN, 진행도 단위=인원, current=0이어야 한다.
// When  - 서로 다른 사용자 3명이 POST /timedeals/{dealId}/join 한다.
// Then  - 상세 진행도 인원=3, status=READY_TO_CONFIRM이어야 한다.
// When  - 마감 시간이 지나 closeDeal이 실행된다.
// Then  - status=SUCCESS로 확정되고 성공 알림이 요청되어야 한다.
```

## 테스트 2. 수량(QUANTITY) 딜: 생성 → 상세 → 주문 → 마감 (metric 기준)

```text
// Given - POST /deals로 QUANTITY 딜(min=50, capacity=100=재고)을 생성한다.
// When  - GET /timedeals/{dealId}로 상세를 조회한다.
// Then  - 진행도 단위=수량, current=0, capacity=100이어야 한다(인원이 아님).
// When  - 사용자들이 POST /orders로 합계 수량 55를 주문한다.
// Then  - 재고=45, 상세 진행도 current=55가 되어야 한다.
// When  - 마감 시 closeDeal이 metric=QUANTITY로 주문 수량 합을 집계한다.
// Then  - 55 >= 50이므로 status=SUCCESS로 전이되어야 한다.
```

## 테스트 3. 금액(AMOUNT, 배달) 딜: 생성 → 주문 → 마감

```text
// Given - POST /deals로 FOOD_DELIVERY 딜(metric=AMOUNT, min=30000)을 생성한다.
// When  - 사용자들이 합계 금액 32000원을 POST /orders로 주문한다.
// When  - 마감 시 closeDeal이 metric=AMOUNT로 주문 금액 합을 집계한다.
// Then  - 32000 >= 30000이므로 status=SUCCESS로 전이되어야 한다.
// Given - 같은 딜에서 합계 금액이 29000원뿐일 때
// Then  - status=FAILED로 전이되어야 한다.
```

## 테스트 4. 동시성 통합: 재고 정합 + metric 일관 마감

```text
// Given - QUANTITY 딜(재고=10, min=10), 서로 다른 사용자 100명이 quantity=1로 동시 주문한다.
// When  - 100개 요청이 동시에 처리된다.
// Then  - CONFIRMED 주문 정확히 10건, 나머지 90건은 재고 부족, 재고 음수가 없어야 한다.
// When  - 마감 시 closeDeal이 실행된다.
// Then  - 주문 수량 합 10 >= min 10이므로 status=SUCCESS로 전이되어야 한다.
```

## 테스트 5. 취소 통합: 진행도 복구와 상태 되돌림

```text
// Given - HEADCOUNT 딜이 인원 3=min 3으로 READY_TO_CONFIRM 상태이다.
// When  - 한 명이 참여를 취소한다.
// Then  - 상세 진행도 인원이 2로 줄고, 최소 미달로 status가 OPEN으로 되돌아가야 한다.
// Given - QUANTITY 딜에서 한 주문이 취소된다.
// Then  - 재고가 취소 수량만큼 복구되고, 상세 진행도 current도 그만큼 감소해야 한다.
```

## 테스트 6. 신원 통합: 로그인 → 토큰 → 딜 참여까지 userId·동네가 일관된다

```text
// Given - 사용자가 사용자 서비스에 로그인해 세션 토큰을 발급받았다.
// When  - 그 토큰으로 동네 딜에 참여(또는 주문)를 요청한다.
// Then  - 토큰의 userId로 참여가 기록되고, 동네(normalizeRegion 후 regionCode)가 딜 지역과 일치해 통과해야 한다.
// Given - 토큰 없이(또는 만료 토큰으로) 같은 요청을 보낸다.
// Then  - 인증 실패로 거부되고 참여/주문이 기록되지 않아야 한다.
```

## 테스트 7. 마감 결과 통합: 마감 성사 → 참여자 전원 알림 + unread 증가

```text
// Given - HEADCOUNT 딜이 마감 시점에 최소 인원을 충족한다(참여자 N명, 각 unread 초기값 보유).
// When  - closeDeal이 SUCCESS를 확정하고 sendDealResultNotification(dealId, DEAL_SUCCESS, 참여자 N명)을 호출한다.
// Then  - notifications 테이블에 N건이 저장되고, 참여자 N명의 notification:unread가 각각 1씩 증가해야 한다.
// Then  - SSE 연결 중인 참여자는 DEAL_SUCCESS 알림을 실시간 수신해야 한다.
// Given - 같은 딜이 최소 인원 미달로 마감되면
// Then  - 참여자 전원에게 DEAL_FAILED 알림이 발송되어야 한다.
```
