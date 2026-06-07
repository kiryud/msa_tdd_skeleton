# User Service Spring Boot 설계 보강

`user_service.md`의 의사코드(친구 구현: `UserRegisterDto`, `UserService`, `UserQueryResolver`)를 Spring Boot 계층/어노테이션으로 배치하는 설계다. 현재 코드는 의사코드 수준이며, MySQL/Redis 연동 부분은 예정으로 표시한다.

## Spring Boot 계층형 구조

```text
Client (GraphQL)
  -> UserQueryResolver (GraphQL Controller)
  -> UserService
  -> UserRepository (MySQL, 예정)
  -> MySQL

UserService
  -> RedisSessionManager (세션 저장/TTL, 예정)
  -> Redis
```

### Presentation Layer (GraphQL)

GraphQL Resolver가 프로필 조회 요청을 받는다. Spring for GraphQL에서는 `@Controller` + `@QueryMapping`으로 매핑한다. 현재 구현은 Resolver가 입력값을 검증한 뒤 프로필을 직접 반환한다(MySQL 연동 후에는 조회 로직을 Service/Repository로 분리하는 것이 권장).

```kotlin
@Controller
class UserQueryResolver {

    @QueryMapping
    fun getUserProfile(@Argument userId: String): UserRegisterDto {
        require(userId.isNotBlank()) { "User ID is required" }
        return UserRegisterDto(userId, "PROTECTED", "타임딜메이트", "우리동네 기반 위치정보")
    }
}
```

### Business Logic Layer

`@Service`를 사용한다. 로그인 검증과 세션 토큰 발급을 담당하며, Coroutine(`suspend`)으로 작성한다.

```kotlin
@Service
class UserService {

    suspend fun loginAndIssueToken(userId: String, userPw: String): String {
        require(userId.isNotBlank()) { "User ID is required" }
        require(userPw.isNotBlank()) { "Password is required" }

        val sessionToken = "SESSION_TOKEN_FOR_$userId"
        return sessionToken
    }
}
```

### Data Access Layer (예정)

`@Repository`와 JPA로 MySQL 계정을 조회한다. 현재는 예정 상태다.

```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUserId(userId: String): User?
}
```

### Entity (예정)

`@Entity`로 사용자 계정을 영속화한다. 비밀번호는 암호화된 형태로만 저장한다.

```kotlin
@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true)
    val userId: String,

    @Column(nullable = false)
    val passwordEncrypted: String,

    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = false)
    val baseLocation: String
)
```

## 세션 관리 보강 (Redis, 예정)

로그인 성공 시 세션 토큰을 Redis에 저장하고 TTL로 만료를 관리한다. 만료되면 세션이 제거되고 재로그인이 필요하다.

```text
로그인 성공 -> 세션 토큰 발급 -> Redis 저장(TTL 설정)
TTL 만료    -> 세션 자동 제거 -> 재로그인 필요
```

## 동시성 보강 (Coroutine)

`loginAndIssueToken`을 `suspend`로 작성해, 타임딜 마감 직전 대량 로그인 요청에도 스레드 점유를 최소화한다.

## 보안 보강

- 비밀번호는 암호화된 형태(`passwordEncrypted`)로만 다루고, 프로필 응답에는 원문 대신 `PROTECTED`로 마스킹한다.
- `userId`/비밀번호가 비어 있으면 `IllegalArgumentException`으로 차단한다.
- 세션 토큰은 Redis에 저장하고 TTL로 만료 관리한다(예정).

## 점수 반영 포인트

- 기술 적용도: GraphQL Resolver, Coroutine, MySQL/Redis(예정)를 사용자 인증 흐름에 연결
- TDD: DTO/검증/Resolver 반환은 Small, MySQL/Redis/GraphQL 연동은 Medium, 로그인→프로필 전체는 Large
- 트러블슈팅: 입력값 검증, 세션 만료(TTL), 비밀번호 비노출 정의
- 문서 구성: 의사코드 / Spring Boot 설계 보강 / 계층별 테스트 문서 분리
