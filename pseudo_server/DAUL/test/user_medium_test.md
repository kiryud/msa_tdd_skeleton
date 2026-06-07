# User Service Medium Test

Medium Test는 다른 인스턴스에 무언가를 **요청하는 경우**(MySQL/Redis I/O, GraphQL Resolver↔Service 연동 등)를 검증한다. 단일 서비스 내부 통합 관점이다.

## 테스트 1. 로그인 시 MySQL에서 사용자 정보를 조회한다

```text
// Given - MySQL에 사용자 정보가 저장되어 있고 userId가 존재한다.
// When  - 로그인 요청을 수행한다.
// Then  - 사용자 정보가 정상 조회되고 인증 절차가 진행되어야 한다.
```

## 테스트 2. 로그인 성공 후 Redis에 세션이 저장된다

```text
// Given - 사용자 인증에 성공한다.
// When  - 세션 토큰이 발급된다.
// Then  - Redis에 세션이 저장되어 로그인 상태가 유지될 수 있어야 한다.
```

## 테스트 3. 잘못된 로그인 요청은 인증 실패하고 세션이 생성되지 않는다

```text
// Given - 존재하지 않는 사용자이거나 비밀번호가 틀리다.
// When  - 로그인 요청을 수행한다.
// Then  - 인증에 실패하고 Redis 세션이 생성되지 않아야 한다.
```

## 테스트 4. GraphQL Resolver가 User Service를 통해 프로필을 조회한다

```text
// Given - 사용자 정보가 존재한다.
// When  - GraphQL Query로 프로필을 요청한다.
// Then  - Resolver가 User Service를 호출하고 nickname/baseLocation이 반환되어야 한다.
```

## 테스트 5. Redis 세션이 TTL 만료되면 제거된다

```text
// Given - Redis에 로그인 세션이 저장되어 있다.
// When  - 세션 TTL이 만료된다.
// Then  - 세션이 자동 제거되고 재로그인이 필요해야 한다.
```
