# 공동구매 딜 생성 Large Test

Large Test는 딜 생성부터 저장, 이벤트 발행, 참여 서비스 수신까지 연결된 전체 흐름을 검증하는 테스트입니다.

## 테스트 1. 생성 요청부터 이벤트 발행까지 전체 흐름이 연결된다

```text
// Given - 유효한 딜 생성 요청이 준비되어 있다.
// When - POST /deals 요청을 보낸다.
// Then - 딜이 OPEN 상태로 저장되어야 한다.
// Then - DealCreatedEvent가 발행되어야 한다.
// Then - 응답으로 dealId와 OPEN 상태가 반환되어야 한다.
```

## 테스트 2. 발행된 생성 이벤트를 참여 서비스(mock)가 수신한다

```text
// Given - 딜 생성이 완료되어 DealCreatedEvent가 발행된다.
// When - 참여 서비스(mock)가 이벤트를 받는다.
// Then - 참여 서비스는 dealId, capacity, successCriterion, deadline을 수신해야 한다.
```

## 테스트 3. 생성된 딜의 정보로 참여 검증에 필요한 값이 모두 제공된다

```text
// Given - 딜이 생성되어 저장되어 있다.
// When - 참여 서비스(mock)가 GET /deals/{dealId}로 딜을 조회한다.
// Then - deadline(마감 시간), capacity(정원), successCriterion(최소값)이 모두 조회되어야 한다.
// Then - 참여 서비스는 이 값들로 마감/정원/성공 판정을 수행할 수 있어야 한다.
```

## 테스트 4. 같은 멱등 키의 동시 생성 요청에도 딜은 한 번만 만들어진다

```text
// Given - 동일한 Idempotency-Key를 가진 생성 요청 여러 개가 거의 동시에 들어온다.
// When - 요청들이 동시에 처리된다.
// Then - 딜은 한 건만 저장되어야 한다.
// Then - 모든 응답은 같은 dealId를 반환해야 한다.
```

## 테스트 5. 타입이 다른 여러 딜을 생성해도 각각 올바른 성공 기준으로 저장된다

```text
// Given - GROCERY(인원), SELLER(수량), FOOD_DELIVERY(금액) 딜을 차례로 생성한다.
// When - 세 딜을 모두 저장한다.
// Then - GROCERY는 metric = HEADCOUNT로 저장되어야 한다.
// Then - SELLER는 metric = QUANTITY로 저장되어야 한다.
// Then - FOOD_DELIVERY는 metric = AMOUNT로 저장되어야 한다.
// Then - 세 딜 모두 OPEN 상태로 저장되고 각각 생성 이벤트가 발행되어야 한다.
```
