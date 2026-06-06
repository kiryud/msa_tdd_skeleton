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
