# 통합 Medium Test

Medium Test는 다른 인스턴스에 무언가를 **요청하는 경우**(DB/Redis I/O 등 저장소 접근)를 검증합니다. 서비스 경계에서 한 서비스가 쓴 데이터를 다른 서비스가 올바르게 읽는지를 확인합니다.

> AMOUNT(배달 공구) 관련 케이스(테스트 4)는 주문에 amount 필드 추가가 선행되어야 한다(현재 order_service.md 미지원).

## 테스트 1. 생성된 딜을 다른 서비스가 dealId로 동일하게 읽는다

```text
// Given - 딜 생성 서비스가 Deal을 저장하고 DealCreatedEvent를 발행한다.
// When  - 참여/상세/주문 서비스의 DealReadModel을 dealId로 조회한다.
// Then  - 세 서비스의 deadline / capacity / metric / min이 모두 동일해야 한다.
```

## 테스트 2. 참여가 쓴 Redis 키를 상세가 같은 규약으로 읽는다

```text
// Given - 참여 서비스가 timedeal:{dealId}:participants에 userId 15개를 SADD 한다.
// When  - 상세 서비스가 같은 키를 SCARD로 조회한다.
// Then  - 진행도 인원=15가 반환되어야 한다.
```

## 테스트 3. 마감 확정이 주문 서비스의 수량 합을 읽어 판정한다

```text
// Given - QUANTITY 딜(min=50), 주문 서비스에 CONFIRMED 주문 수량 합=55가 저장되어 있다.
// When  - closeDeal(dealId)이 getCurrentValue로 주문 수량 합을 조회한다.
// Then  - 딜 상태가 SUCCESS로 전이되어야 한다.
// Given - 주문 수량 합=40
// Then  - 딜 상태가 FAILED로 전이되어야 한다.
```

## 테스트 4. 마감 확정이 주문 서비스의 금액 합을 읽어 판정한다

```text
// Given - AMOUNT 딜(min=30000), 주문 서비스에 CONFIRMED 금액 합=32000이 저장되어 있다.
// When  - closeDeal(dealId)이 주문 금액 합을 조회한다.
// Then  - 딜 상태가 SUCCESS로 전이되어야 한다.
```

## 테스트 5. 진행도 저장소(Redis)와 기록 저장소(RDBMS)가 정합한다

```text
// Given - 참여 성공이 N건 처리되었다.
// When  - Redis 참여자 수와 RDBMS JOINED 이력 수를 조회한다.
// Then  - 두 값이 같아야 한다.
// Given - 주문 성공이 M건 처리되고 PENDING 주문이 모두 정착되었다.
// When  - (초기재고 - 현재재고)와 CONFIRMED 주문 수량 합을 비교한다.
// Then  - 두 값이 같아야 한다.
```

## 테스트 6. 생성 이벤트가 주문 재고를 초기화한다

```text
// Given - 딜 생성이 QUANTITY 딜(capacity=100)을 만들고 이벤트를 발행한다.
// When  - 주문 서비스가 onDealCreated로 이벤트를 처리한다.
// Then  - deal:{dealId}:stock = 100으로 설정되어야 한다.
```
