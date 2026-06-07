# User Service - 사용자 서비스

## 서비스 개요
우리동네 기반 타임딜 공동구매 플랫폼에서 로그인, 세션 발급, 사용자 프로필 조회를 담당하는 마이크로서비스다. (Jira: [KAN-5])

현재는 의사코드 수준이며, MySQL 계정 검증과 Redis 세션 저장은 추후 추가 예정이다.

## 내가 담당할 기능
- 로그인 요청 처리 및 세션 토큰 발급
- GraphQL 기반 사용자 프로필 조회
- 회원가입 요청 DTO(`UserRegisterDto`) 정의

## 이 기능을 선택한 이유
로그인과 세션 발급은 출입 게이트처럼 입주민(사용자)을 확인하고 출입증(세션 토큰)을 내주는 핵심 길목이다. 또한 화면마다 필요한 사용자 정보가 다르기 때문에 GraphQL로 필요한 필드만 조회하는 구조가 자연스럽다.

## 사용 기술과 이유

### Kotlin Data Class
회원가입 정보를 하나의 객체(`UserRegisterDto`)로 묶기 위해 사용한다. 보일러플레이트를 줄이고 객체의 불변성을 보장한다.

### Coroutine
타임딜 마감 직전 대량의 로그인 요청이 몰려도 스레드 점유를 최소화하기 위해 `loginAndIssueToken`을 `suspend`로 작성한다.

### GraphQL
화면마다 필요한 데이터가 다르기 때문에 사용한다. 메인 화면은 닉네임만, 프로필 화면은 추가 정보가 필요할 수 있다. REST의 오버패칭 문제를 줄이기 위한 설계다.

### MySQL (예정)
계정 데이터를 영속 저장하기 위해 사용한다. 현재는 검증 로직이 예정 상태다.

### Redis (예정)
세션 토큰 저장과 만료(TTL) 관리를 위해 사용한다. 현재는 저장 로직이 예정 상태다.

### TDD
DTO 생성, 로그인 검증, 입력값 검증, 예외 처리, GraphQL Resolver 반환을 테스트 우선으로 검증한다.

## 데이터 구조 예시

```kotlin
// 회원가입 요청 DTO (회원가입 신청서 역할)
data class UserRegisterDto(
    val userId: String,
    val passwordEncrypted: String,
    val nickname: String,
    val baseLocation: String
)
```

`baseLocation`은 사용자의 GPS 기반 동네 인증 정보다.

## 핵심 의사코드

```text
// 로그인 및 세션 토큰 발급
// 사용자가 로그인할 때 실행된다. (Coroutine 기반 suspend 함수)

/*
    인자    : userId, userPw
    반환값  : 세션 토큰 문자열
    역할    : 입력값을 검증한 뒤 사용자를 인증하고 세션 토큰을 발급한다.
*/

function loginAndIssueToken(userId, userPw):
    if userId is blank:
        throw IllegalArgumentException("User ID is required")
    if userPw is blank:
        throw IllegalArgumentException("Password is required")

    sessionToken = issue session token for userId
    return sessionToken
```

MySQL 사용자 검증과 Redis 세션 저장은 위 흐름 사이에 추가될 예정이다.

```text
// 사용자 프로필 조회 (GraphQL Resolver)
// GraphQL Query로 프로필을 요청할 때 실행된다.

/*
    인자    : userId
    반환값  : UserRegisterDto (프로필)
    역할    : 입력값을 검증한 뒤 필요한 사용자 프로필 정보를 반환한다.
*/

function getUserProfile(userId):
    if userId is blank:
        throw IllegalArgumentException("User ID is required")

    return UserRegisterDto(userId, passwordEncrypted, nickname, baseLocation)
```

프로필 응답의 비밀번호 필드는 보호 처리(`passwordEncrypted = "PROTECTED"`)되어 원문이 노출되지 않는다.

## API 예시

```text
GraphQL Query: userProfile(userId)
- 사용자 프로필 조회 -> UserRegisterDto (비밀번호 미반환)

로그인: loginAndIssueToken(userId, userPw)
- 세션 토큰 발급 (노출 엔드포인트는 GraphQL Mutation 또는 REST로 추후 확정)
```

## 다른 서비스와의 연동 계약

- **인증**: User Service가 세션 토큰을 발급한다(Redis 저장 예정). 다른 서비스는 이 세션으로 사용자를 인증한다.
- **동네(baseLocation)**: 사용자의 GPS 기반 동네 정보의 출처는 User Service다. 참여·상세가 지역 일치 판정에 쓰는 값과 연결된다.
- **비밀번호 비노출**: 프로필 응답에 비밀번호 원문은 포함하지 않는다.

### 통합 시 확인 필요(주의)

- **인증 방식 불일치**: 다른 서비스 문서들은 "JWT 토큰에서 userId/regionCode를 꺼낸다"고 가정하지만, 이 User Service는 **Redis 세션 토큰** 방식이다. 통합 시 JWT 또는 세션 중 하나로 통일해야 한다.
- **동네 표현 불일치**: 현재 `baseLocation`은 자유 문자열("우리동네 기반 위치정보")이다. 참여·상세가 쓰는 구조화된 `regionCode`(예: `MAPO_YEONNAM`)와는 정규화/매핑이 필요하다.

## Jira 이슈 예시

```text
[KAN-5][User/설계] User Service 구조 및 GraphQL Resolver 설계
[KAN-5][User/기능] 로그인 및 세션 토큰 발급(Coroutine) 작성
[KAN-5][User/기능] GraphQL 프로필 조회 Resolver 작성
[KAN-5][User/기능] 회원가입 요청 DTO(UserRegisterDto) 정의
[KAN-5][User/TDD] DTO 생성/로그인/입력값 검증 Small 테스트 작성
[KAN-5][User/TDD] MySQL/Redis/GraphQL 연동 Medium 테스트 작성
[KAN-5][User/TDD] 로그인->토큰->세션->프로필 Large 테스트 작성
```
