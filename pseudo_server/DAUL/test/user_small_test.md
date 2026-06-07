# User Service Small Test

Small Test는 외부 시스템(MySQL, Redis, Network)에 의존하지 않고, 함수에 들어온 값이나 (목으로 대체한) 결과를 받아 **반환값을 검사**하는 수준의 단위 테스트다.

## 테스트 1. UserRegisterDto가 정상 생성되고 각 필드가 저장된다

```text
// Given - userId, passwordEncrypted, nickname, baseLocation 값이 있다.
// When  - UserRegisterDto 객체를 생성한다.
// Then  - 객체가 정상 생성되고 각 필드 값이 그대로 저장되어야 한다.
```

## 테스트 2. 정상 로그인 시 세션 토큰이 발급된다

```text
// Given - 등록된 사용자 정보와 올바른 비밀번호가 입력된다.
// When  - loginAndIssueToken(userId, userPw)를 호출한다.
// Then  - 세션 토큰 문자열이 반환되어야 한다.
```

## 테스트 3. 빈 아이디로 로그인하면 예외가 발생한다

```text
// Given - userId가 비어 있고 비밀번호는 정상이다.
// When  - loginAndIssueToken("", userPw)를 호출한다.
// Then  - IllegalArgumentException이 발생하고 로그인에 실패해야 한다.
```

## 테스트 4. 빈 비밀번호로 로그인하면 예외가 발생한다

```text
// Given - userId는 정상이고 비밀번호가 비어 있다.
// When  - loginAndIssueToken(userId, "")를 호출한다.
// Then  - IllegalArgumentException이 발생하고 로그인에 실패해야 한다.
```

## 테스트 5. 프로필 조회 시 닉네임과 동네 정보가 담긴 DTO가 반환된다

```text
// Given - 존재하는 userId가 주어진다.
// When  - getUserProfile(userId)를 호출한다.
// Then  - nickname과 baseLocation이 담긴 UserRegisterDto가 반환되어야 한다.
// Then  - 비밀번호는 PROTECTED로 마스킹되어 원문이 노출되지 않아야 한다.
```
