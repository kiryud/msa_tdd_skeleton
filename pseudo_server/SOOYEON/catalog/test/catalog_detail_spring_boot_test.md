# 타임딜 상세 페이지 Spring Boot 보강 테스트

이 문서는 기존 small / medium 테스트에 Spring Boot 계층 구조와 어노테이션 활용 검증을 추가한 테스트 설계입니다.

## Controller 계층 테스트

### 테스트 1. 상세 조회 요청은 Controller에서 Service로 위임된다

```text
// Given - 사용자가 GET /timedeals/{dealId} 요청을 보낸다.
// When - TimeDealController의 getTimeDealDetail 메서드가 호출된다.
// Then - Controller는 직접 타임딜 정보를 조회하거나 조합하지 않는다.
// Then - TimeDealService.getTimeDealDetail 메서드로 요청을 위임해야 한다.
```

### 테스트 2. userId와 regionCode는 JWT 토큰에서 꺼낸 값을 사용한다

```text
// Given - 사용자가 Authorization 헤더에 JWT 토큰을 포함해 요청을 보낸다.
// When - @Auth 어노테이션이 토큰에서 AuthUser 객체를 추출한다.
// Then - Service 호출 시 토큰에서 꺼낸 userId와 regionCode가 전달되어야 한다.
// Then - 쿼리 파라미터나 Body의 userId, regionCode는 사용하지 않아야 한다.
```

## Service 계층 테스트

### 테스트 3. Service는 RDBMS와 Redis 결과를 조합해서 응답을 구성한다

```text
// Given - RDBMS에 타임딜, 상품, 판매자 정보가 저장되어 있다.
// Given - Redis에 현재 참여 인원이 저장되어 있다.
// When - getTimeDealDetail이 실행된다.
// Then - 응답에 RDBMS의 상품·판매자 정보와 Redis의 참여 인원이 모두 포함되어야 한다.
```

### 테스트 4. 할인율은 Service에서 계산되어 응답 DTO에 포함된다

```text
// Given - 원가가 26,000원이고 딜 가격이 18,900원인 상품이 있다.
// When - getTimeDealDetail이 실행된다.
// Then - 응답의 product.discountRate는 27이어야 한다.
// Then - 할인율 계산은 Service 내부에서 처리되어야 한다.
```

### 테스트 5. getTimeDealDetail은 readOnly 트랜잭션 안에서 실행된다

```text
// Given - getTimeDealDetail 메서드에 @Transactional(readOnly = true)가 선언되어 있다.
// When - 메서드가 실행된다.
// Then - 트랜잭션 안에서 데이터 변경이 발생해도 DB에 반영되지 않아야 한다.
// Then - dirty checking이 동작하지 않아야 한다.
```

## Repository / Entity 계층 테스트

### 테스트 6. findWithProductAndSellerById는 fetch join으로 한 번의 쿼리만 실행한다

```text
// Given - dealId에 연결된 상품과 판매자 정보가 RDBMS에 저장되어 있다.
// When - findWithProductAndSellerById 메서드가 실행된다.
// Then - RDBMS 쿼리는 한 번만 실행되어야 한다.
// Then - 상품과 판매자 정보를 가져오기 위한 추가 쿼리가 실행되면 안 된다.
```

### 테스트 7. Entity는 응답 DTO로 직접 반환되지 않는다

```text
// Given - TimeDeal Entity가 조회된다.
// When - getTimeDealDetail이 실행된다.
// Then - 반환 타입은 TimeDealDetailResponse DTO이어야 한다.
// Then - Entity 객체가 Controller 응답으로 직접 사용되면 안 된다.
```

## Redis 조회 테스트

### 테스트 8. 현재 참여 인원은 Redis SCARD로 조회된다

```text
// Given - Redis의 timedeal:{dealId}:participants 키에 23명의 userId가 저장되어 있다.
// When - getTimeDealDetail이 실행된다.
// Then - 응답의 participants.currentParticipants는 23이어야 한다.
// Then - RDBMS 참여 이력 집계 쿼리는 실행되지 않아야 한다.
```

### 테스트 9. 참여한 사용자의 isJoined는 Redis SISMEMBER로 확인된다

```text
// Given - Redis 참여자 목록에 해당 userId가 저장되어 있다.
// When - 해당 userId로 getTimeDealDetail이 실행된다.
// Then - 응답의 isJoined는 true여야 한다.
// Then - RDBMS 참여 이력 조회 쿼리는 실행되지 않아야 한다.
```

## 인증 / 보안 테스트

### 테스트 10. 인증되지 않은 사용자는 상세 페이지를 조회할 수 없다

```text
// Given - Authorization 헤더가 없는 요청이다.
// When - 사용자가 GET /timedeals/{dealId}를 호출한다.
// Then - 조회 로직은 실행되지 않아야 한다.
// Then - UNAUTHORIZED 결과를 반환해야 한다.
```

### 테스트 11. 응답 DTO에 판매자의 민감한 내부 정보는 포함되지 않는다

```text
// Given - Seller Entity에 계좌번호, 관리자 메모 등 내부 필드가 존재한다.
// When - getTimeDealDetail이 실행된다.
// Then - 응답의 seller 정보에는 name, phone, email, businessNumber만 포함되어야 한다.
// Then - Entity의 내부 전용 필드는 응답에 노출되면 안 된다.
```

## 예외 처리 테스트

### 테스트 12. 존재하지 않는 타임딜 조회 시 DealNotFoundException이 발생한다

```text
// Given - 요청한 dealId에 해당하는 타임딜이 RDBMS에 없다.
// When - getTimeDealDetail이 실행된다.
// Then - DealNotFoundException이 발생해야 한다.
// Then - 응답 코드는 404여야 한다.
```

### 테스트 13. 삭제된 상품의 타임딜 조회 시 ProductNotAvailableException이 발생한다

```text
// Given - 타임딜은 존재하지만 연결된 상품의 isDeleted가 true이다.
// When - getTimeDealDetail이 실행된다.
// Then - ProductNotAvailableException이 발생해야 한다.
// Then - 응답 코드는 410이어야 한다.
```
