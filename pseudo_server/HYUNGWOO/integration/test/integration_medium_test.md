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

## 테스트 7. 사용자 서비스가 발급한 토큰을 다른 서비스가 검증해 신원을 얻는다

```text
// Given - 사용자 서비스가 로그인 성공으로 세션 토큰을 발급해 Redis에 저장했다.
// When  - 참여(또는 주문) 서비스가 그 토큰으로 신원 검증을 수행한다.
// Then  - userId와 동네 정보가 정상 추출되어야 한다.
// Given - 만료되었거나 위조된 토큰이 주어진다.
// When  - 같은 검증을 수행한다.
// Then  - 인증 실패로 거부되어야 한다.
```

> 토큰 형식(JWT/세션)은 통합 시 단일화가 필요하다. 세션 방식이면 이 검증은 Redis 조회(다른 인스턴스 I/O)이므로 Medium에 해당한다.

## 테스트 8. 마감 확정이 알림 서비스로 참여자 전원 알림을 발송한다

```text
// Given - HEADCOUNT 딜이 성사(SUCCESS)로 마감되고 참여자 SET에 userId 3명이 있다.
// When  - closeDeal이 collectParticipants로 3명을 모아 sendDealResultNotification(dealId, DEAL_SUCCESS, [3명])을 호출한다.
// Then  - notifications 테이블에 3건이 저장되고, 각 userId의 notification:unread가 1씩 증가해야 한다.
// Then  - 각 알림 type=DEAL_SUCCESS, title="공구가 성사되었습니다!"여야 한다.
```

> 수량/금액 딜의 동일 시나리오는 주문 서비스의 딜 단위 주문자 조회가 선행되어야 한다.
