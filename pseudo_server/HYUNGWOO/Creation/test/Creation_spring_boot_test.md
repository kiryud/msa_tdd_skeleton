# 공동구매 딜 생성 Spring Boot 보강 테스트

이 문서는 기존 small / medium / large 테스트에 Spring Boot 계층 구조와 어노테이션 활용 검증을 추가한 테스트 설계입니다.

## Controller 계층 테스트

### 테스트 1. 생성 요청은 Controller에서 Service로 위임된다

```text
// Given - 사용자가 POST /deals 요청을 보낸다.
// When - DealController의 createDeal 메서드가 호출된다.
// Then - Controller는 직접 유효성을 판단하지 않는다.
// Then - DealService.createDeal 메서드로 요청을 위임해야 한다.
```

### 테스트 2. 요청 Body는 DTO로 변환된다

```text
// Given - 클라이언트가 JSON 형태의 생성 요청을 보낸다.
// When - @RequestBody가 요청 데이터를 받는다.
// Then - CreateDealRequest DTO로 변환되어야 한다.
// Then - Deal Entity가 Controller 요청 객체로 직접 사용되면 안 된다.
```

## Service / Strategy 계층 테스트

### 테스트 3. Service는 타입에 맞는 전략을 선택해 검증한다

```text
// Given - dealType이 FOOD_DELIVERY인 요청이 들어온다.
// When - DealService.createDeal이 실행된다.
// Then - FoodDeliveryDealStrategy가 선택되어야 한다.
// Then - 금액(AMOUNT) 기준이 아니면 검증에서 실패해야 한다.
```

### 테스트 4. 새 타입이 추가되어도 기존 생성 로직은 수정되지 않는다

```text
// Given - 새로운 DealType 전략 클래스가 추가된다.
// When - 해당 타입으로 생성 요청이 들어온다.
// Then - DealService 코드 변경 없이 새 전략이 동작해야 한다.
```

## Repository / Entity 계층 테스트

### 테스트 5. 딜은 OPEN 상태로만 저장된다

```text
// Given - 딜 생성 요청이 들어온다.
// When - Deal Entity가 저장된다.
// Then - 저장된 상태는 OPEN이어야 한다.
// Then - 생성 단계에서 SUCCESS나 FAILED 상태로 저장될 수 없어야 한다.
```

### 테스트 6. 멱등 키에는 unique 제약이 적용된다

```text
// Given - 같은 idempotencyKey를 가진 두 딜을 저장하려 한다.
// When - 두 번째 저장이 시도된다.
// Then - unique 제약으로 두 번째 저장은 거부되어야 한다.
```

## 멱등성 테스트

### 테스트 7. 같은 키의 따닥 요청은 한 건만 생성한다

```text
// Given - 같은 사용자가 같은 Idempotency-Key로 생성 버튼을 빠르게 두 번 누른다.
// When - 동일 키로 여러 생성 요청이 들어온다.
// Then - 딜은 한 건만 생성되어야 한다.
// Then - 나머지 요청은 같은 dealId를 반환해야 한다.
```

## Transaction / 이벤트 발행 테스트

### 테스트 8. 저장 트랜잭션이 롤백되면 이벤트가 발행되지 않는다

```text
// Given - 딜 저장 중 오류가 발생해 트랜잭션이 롤백된다.
// When - createDeal 작업이 실패한다.
// Then - 딜은 저장되지 않아야 한다.
// Then - DealCreatedEvent도 발행되지 않아야 한다.
```

### 테스트 9. 저장이 커밋되면 커밋 후 리스너가 이벤트를 발행한다

```text
// Given - 유효한 생성 요청으로 딜 저장이 커밋된다.
// When - @TransactionalEventListener(AFTER_COMMIT)가 동작한다.
// Then - 커밋 이후 DealCreatedEvent가 참여 서비스로 전달되어야 한다.
```

## 인증 / 보안 테스트

### 테스트 10. 인증되지 않은 사용자는 딜을 생성할 수 없다

```text
// Given - Authorization 헤더가 없는 요청이다.
// When - 사용자가 딜 생성 API를 호출한다.
// Then - 생성 로직은 실행되지 않아야 한다.
// Then - UNAUTHORIZED 결과를 반환해야 한다.
```

### 테스트 11. hostId는 토큰의 사용자 정보를 기준으로 한다

```text
// Given - JWT 토큰의 userId는 1이고 요청 Body에 userId 2가 들어 있다.
// When - 사용자가 딜 생성을 요청한다.
// Then - 서버는 Body의 userId를 그대로 믿으면 안 된다.
// Then - 토큰에서 검증된 userId 1을 hostId로 저장해야 한다.
```

### 테스트 12. 판매자 등록 타입은 사업자 인증된 계정만 생성할 수 있다

```text
// Given - dealType이 SELLER인데 인증 사용자가 사업자 인증되지 않았다.
// When - 사용자가 딜 생성을 요청한다.
// Then - SELLER_NOT_VERIFIED 결과를 반환해야 한다.
// Then - 딜은 생성되지 않아야 한다.
```
