# 공동구매 딜 생성 Medium Test

Medium Test는 서비스 로직과 RDBMS 저장, 커밋 후 이벤트 발행 흐름이 함께 동작하는지 확인하는 테스트입니다.

## 테스트 1. 생성 성공 시 RDBMS에 딜이 OPEN 상태로 저장된다

```text
// Given - 유효한 딜 생성 요청이 들어온다.
// When - createDeal 함수가 실행된다.
// Then - DealRepository에 딜이 저장되어야 한다.
// Then - 저장된 딜의 상태는 OPEN이어야 한다.
```

## 테스트 2. 생성 성공 시 커밋 이후 DealCreatedEvent가 발행된다

```text
// Given - 유효한 딜 생성 요청이 들어온다.
// When - createDeal 함수가 실행되고 트랜잭션이 커밋된다.
// Then - 커밋 이후 DealCreatedEvent가 발행되어야 한다.
// Then - 이벤트에는 dealId, capacity, successCriterion, deadline이 포함되어야 한다.
```

## 테스트 3. 같은 멱등 키로 두 번 요청하면 딜은 한 번만 저장된다

```text
// Given - 동일한 Idempotency-Key로 두 번의 생성 요청이 들어온다.
// When - createDeal이 연속으로 두 번 실행된다.
// Then - DealRepository에는 딜이 한 건만 저장되어야 한다.
// Then - 두 번째 응답은 첫 번째와 같은 dealId를 반환해야 한다.
```

## 테스트 4. 저장된 딜을 dealId로 조회할 수 있다

```text
// Given - 딜이 생성되어 저장되어 있다.
// When - getDeal(dealId)를 호출한다.
// Then - 저장된 딜의 deadline, capacity, successCriterion이 그대로 조회되어야 한다.
```

## 테스트 5. 딜 타입별 성공 조건이 그대로 저장되고 조회된다

```text
// Given - FOOD_DELIVERY 타입, metric = AMOUNT, min = 30000으로 생성한다.
// When - 딜을 저장한 뒤 다시 조회한다.
// Then - successCriterion은 metric = AMOUNT, min = 30000으로 유지되어야 한다.
```

## 테스트 6. 검증에 실패하면 딜이 저장되지 않고 이벤트도 발행되지 않는다

```text
// Given - 마감 시간이 과거인 잘못된 요청이 들어온다.
// When - createDeal 함수가 실행된다.
// Then - DealRepository에 저장된 딜이 없어야 한다.
// Then - 발행된 DealCreatedEvent도 없어야 한다.
```
