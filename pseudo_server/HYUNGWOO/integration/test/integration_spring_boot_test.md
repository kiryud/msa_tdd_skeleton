# 통합 Spring Boot 보강 테스트

`integration_spring_boot_design.md`의 계층/어노테이션 구현(이벤트 핸들러, 마감 스케줄러, 집계 클라이언트, 전략)을 검증하는 테스트입니다.

## 이벤트 수신 테스트

### 테스트 1. 생성 커밋 후 DealReadModel이 저장된다

```text
// Given - 딜 생성 트랜잭션이 커밋된다.
// When  - DealCreatedEventHandler가 AFTER_COMMIT으로 동작한다.
// Then  - DealReadModelRepository에 해당 dealId의 복제본이 저장되어야 한다.
```

### 테스트 2. QUANTITY 딜 생성 시 재고가 초기화된다

```text
// Given - metric=QUANTITY, capacity=100인 딜 생성 이벤트가 발행된다.
// When  - DealCreatedEventHandler가 처리한다.
// Then  - deal:{dealId}:stock = 100으로 초기화되어야 한다.
// Given - metric=HEADCOUNT 또는 AMOUNT인 경우
// Then  - 재고 키는 생성되지 않아야 한다.
```

## 마감 스케줄러 테스트

### 테스트 3. 스케줄러는 마감된 딜만 확정 대상으로 넘긴다

```text
// Given - deadline이 지난 딜과 지나지 않은 딜이 섞여 있다.
// When  - DealClosingScheduler.closeExpiredDeals가 실행된다.
// Then  - deadline이 지난 딜만 closeDeal로 넘어가야 한다.
```

### 테스트 4. 마감은 metric에 맞는 누적값 소스를 사용한다

```text
// Given - QUANTITY 딜이 마감 대상이다.
// When  - DealLifecycleService.closeDeal이 실행된다.
// Then  - CurrentValueResolver가 주문 수량 합을 조회해야 한다(참여 SCARD는 호출되지 않는다).
```

## 전략 / 집계 계층 테스트

### 테스트 5. CurrentValueResolver는 metric별로 다른 소스를 호출한다

```text
// Given - participantManager와 orderAggregateClient가 목으로 주입되어 있다.
// When  - resolve(deal)이 metric별로 호출된다.
// Then  - HEADCOUNT는 participantManager.count, QUANTITY/AMOUNT는 orderAggregateClient가 호출되어야 한다.
```

### 테스트 6. DealReadModel은 응답/판정에 그대로 쓰이고 원본 Deal을 수정하지 않는다

```text
// Given - 각 서비스가 DealReadModel을 보유한다.
// When  - 마감/상세 로직이 실행된다.
// Then  - 딜 생성 서비스의 원본 Deal 테이블에는 쓰기가 발생하지 않아야 한다(상태 전이 제외).
```

## 인증 / 보안 테스트

### 테스트 7. 인증되지 않은 사용자는 참여/주문 API를 호출할 수 없다

```text
// Given - Authorization 헤더가 없는 요청이다.
// When  - POST /timedeals/{dealId}/join 또는 POST /orders를 호출한다.
// Then  - 로직이 실행되지 않고 UNAUTHORIZED를 반환해야 한다.
```
