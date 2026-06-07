# User Service Spring Boot 보강 테스트

`user_spring_boot_design.md`의 계층/어노테이션 구현(GraphQL Resolver, Service, Repository/Entity, Redis 세션)을 검증하는 테스트다. MySQL/Redis 연동 부분은 예정 사항을 기준으로 한다.

## Presentation(GraphQL) 계층 테스트

### 테스트 1. 프로필 조회 요청은 Resolver에서 Service로 위임된다

```text
// Given - GraphQL Query userProfile(userId) 요청이 들어온다.
// When  - UserQueryResolver의 메서드가 호출된다.
// Then  - Resolver는 직접 조회하지 않고 UserService.getUserProfile로 위임해야 한다.
```

### 테스트 2. 빈 userId 요청은 예외로 차단된다

```text
// Given - userId가 비어 있는 프로필 요청이다.
// When  - getUserProfile("")가 호출된다.
// Then  - IllegalArgumentException이 발생해야 한다.
```

## Service 계층 테스트

### 테스트 3. 로그인은 Coroutine(suspend)으로 동작한다

```text
// Given - 정상 userId/비밀번호가 입력된다.
// When  - suspend 함수 loginAndIssueToken을 호출한다.
// Then  - 코루틴 컨텍스트에서 세션 토큰이 정상 반환되어야 한다.
```

## Data Access / Redis 계층 테스트 (예정 기준)

### 테스트 4. 로그인 시 MySQL에서 사용자를 조회한다

```text
// Given - MySQL에 사용자 계정이 저장되어 있다.
// When  - 로그인 요청을 수행한다.
// Then  - UserRepository로 사용자 계정이 조회되어야 한다.
```

### 테스트 5. 세션 토큰은 Redis에 저장되고 TTL로 만료된다

```text
// Given - 로그인에 성공한다.
// When  - 세션 토큰이 발급된다.
// Then  - Redis에 세션이 저장되고 TTL 만료 시 제거되어야 한다.
```

## 보안 테스트

### 테스트 6. 프로필 응답에 비밀번호 원문이 포함되지 않는다

```text
// Given - 프로필 조회가 수행된다.
// When  - 응답 DTO를 확인한다.
// Then  - passwordEncrypted는 PROTECTED로 마스킹되고 원문이 노출되면 안 된다.
```
