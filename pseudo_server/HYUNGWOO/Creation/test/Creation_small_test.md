# 공동구매 딜 생성 Small Test

Small Test는 Redis, RDBMS 같은 외부 저장소에 직접 연결하지 않고 순수한 판단 로직(검증, 성공 조건 빌드, 멱등 키 판단)을 검증하는 테스트입니다.

## 테스트 1. 마감 시간이 과거이면 딜을 생성할 수 없다

```text
// Given - 마감 시간이 현재 시각보다 이르다.
// When - 사용자가 딜 생성을 요청한다.
// Then - DEADLINE_IN_PAST 결과를 반환해야 한다.
```

## 테스트 2. 가격이 0 이하이면 딜을 생성할 수 없다

```text
// Given - 가격이 0 또는 음수이다.
// When - 사용자가 딜 생성을 요청한다.
// Then - INVALID_PRICE 결과를 반환해야 한다.
```

## 테스트 3. 성공 최소값이 1 미만이면 딜을 생성할 수 없다

```text
// Given - 성공 조건의 최소값(min)이 0이다.
// When - 사용자가 딜 생성을 요청한다.
// Then - INVALID_MIN 결과를 반환해야 한다.
```

## 테스트 4. 정원이 최소값보다 작으면 딜을 생성할 수 없다

```text
// Given - 최소값(min)이 10이고 정원(capacity)이 5이다.
// When - 사용자가 딜 생성을 요청한다.
// Then - CAPACITY_LESS_THAN_MIN 결과를 반환해야 한다.
```

## 테스트 5. 배달 공구 타입은 금액 기준으로만 성공 조건을 만든다

```text
// Given - dealType이 FOOD_DELIVERY이고 metric이 AMOUNT이며 min이 30000이다.
// When - 성공 조건 빌더가 실행된다.
// Then - SuccessCriterion(metric = AMOUNT, min = 30000)이 생성되어야 한다.
```

## 테스트 6. 배달 공구 타입에 금액이 아닌 기준이 오면 생성할 수 없다

```text
// Given - dealType이 FOOD_DELIVERY인데 metric이 HEADCOUNT이다.
// When - 배달 공구 전략의 검증이 실행된다.
// Then - INVALID_METRIC_FOR_TYPE 결과를 반환해야 한다.
```

## 테스트 7. 생필품 타입은 인원 또는 수량 기준으로 성공 조건을 만든다

```text
// Given - dealType이 GROCERY이고 metric이 QUANTITY이며 min이 20이다.
// When - 성공 조건 빌더가 실행된다.
// Then - SuccessCriterion(metric = QUANTITY, min = 20)이 생성되어야 한다.
```

## 테스트 8. 생성되는 딜의 초기 상태는 항상 OPEN이다

```text
// Given - 모든 입력이 유효하다.
// When - 딜 객체가 생성된다.
// Then - 딜의 상태는 OPEN이어야 한다.
// Then - SUCCESS, FAILED 같은 다른 상태로는 생성될 수 없어야 한다.
```

## 테스트 9. 같은 멱등 키는 중복 생성으로 판단한다

```text
// Given - 이미 사용된 Idempotency-Key가 주어진다.
// When - 멱등성 판단 로직이 실행된다.
// Then - 신규 생성이 아니라 중복으로 판단되어야 한다.
```
