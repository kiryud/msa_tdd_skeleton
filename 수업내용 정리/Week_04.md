# 04주차 정리

## 학습 주제

### Spring Boot 프로젝트 환경 구축

Spring Initializr를 활용하여 Spring Boot 프로젝트를 생성하고 실행 환경을 구성하는 방법을 학습하였다.

---

### 데이터베이스 연동

- MySQL
- JPA (Hibernate)

Spring Boot 애플리케이션과 관계형 데이터베이스를 연결하는 방법을 학습하였다.

---

### Redis 연동

- Redis Server
- Redis Insight

인메모리 데이터베이스(Redis)를 설치하고 Spring Boot와 연결하는 방법을 학습하였다.

---

### 데이터베이스 관리 도구

- HeidiSQL

MySQL 데이터베이스를 GUI 환경에서 관리하는 방법을 학습하였다.

---

### API 문서화

- Swagger UI

REST API를 시각적으로 문서화하고 테스트하는 방법을 학습하였다.

---

# 핵심 개념

## Spring Initializr

Spring Boot 프로젝트를 자동으로 생성해주는 공식 프로젝트 생성 도구이다.

### 주요 역할

- Spring Boot 프로젝트 생성
- Dependency 관리
- Gradle/Maven 설정 자동화

---

## MySQL

관계형 데이터베이스(RDBMS)이다.

### 특징

- ACID 보장
- 트랜잭션 지원
- 데이터 정합성 보장
- SQL 기반 데이터 관리

---

### Spring Boot 연결 예시

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user
    username: root
    password: 1234
```

---

## JPA (Java Persistence API)

객체(Entity)를 데이터베이스 테이블과 매핑해주는 기술이다.

### 장점

- SQL 작성 감소
- 객체 중심 개발
- 생산성 향상

---

### 예시

```kotlin
@Entity
class User(
    @Id
    @GeneratedValue
    val id: Long? = null,

    val email: String,
    val password: String
)
```

---

## Redis

메모리 기반 NoSQL 데이터베이스이다.

### 특징

- 매우 빠른 조회 속도
- Key-Value 구조
- 캐시(Cache)
- 세션(Session) 저장

---

### Redis 사용 목적

- 로그인 세션 관리
- JWT 토큰 저장
- 인기 데이터 캐싱
- 대기열 관리

---

## Swagger UI

REST API를 웹 브라우저에서 테스트할 수 있도록 제공하는 API 문서화 도구이다.

---

### 장점

- API 테스트 가능
- API 문서 자동 생성
- 프론트엔드 협업 지원

---

# 프로젝트 적용

## 프로젝트 주제

### 우리동네 기반 타임딜 공동구매 플랫폼

사용자의 위치 기반 공동구매 및 타임딜 서비스를 제공하는 플랫폼이다.

---

## MySQL 적용

회원 정보를 저장하기 위해 MySQL을 사용한다.

---

### 저장 데이터

```text
사용자 정보

- 이메일
- 비밀번호
- 닉네임
- 동네 인증 여부
- 생성일
```

---

### 사용 이유

사용자 데이터는 서비스의 핵심 데이터이며 높은 정합성이 요구되므로 ACID를 보장하는 MySQL을 선택하였다.

---

## Redis 적용

초고속 조회가 필요한 데이터를 저장한다.

---

### 저장 데이터

```text
Access Token

Refresh Token

로그인 세션

인기 공동구매 목록

실시간 대기열
```

---

### 사용 이유

타임딜 마감 직전 트래픽 폭주 상황에서도 빠른 응답을 제공하기 위해 사용한다.

---

## Swagger 적용

프론트엔드 개발자와 API 규격을 공유하기 위해 사용한다.

---

### 예시

```text
POST /users/register

POST /users/login

GET /users/profile
```

---

# User Service 적용

## User Entity 설계

회원 정보를 데이터베이스에 저장하기 위한 Entity를 설계한다.

### 예시

```kotlin
@Entity
class User(
    @Id
    @GeneratedValue
    val id: Long? = null,

    val email: String,

    val password: String,

    val nickname: String
)
```

---

## User Repository

JPA를 이용하여 사용자 데이터를 조회한다.

### 예시

```kotlin
interface UserRepository
    : JpaRepository<User, Long>
```

---

## 회원가입 기능

회원가입 요청 시

```text
입력

↓

비밀번호 암호화

↓

MySQL 저장

↓

응답 반환
```

과정을 수행한다.

---

## 로그인 기능

로그인 성공 시

```text
사용자 조회

↓

비밀번호 검증

↓

JWT 생성

↓

Redis 저장

↓

응답 반환
```

과정을 수행한다.

---

## Redis 활용

로그인 세션 및 인증 토큰을 Redis에 저장한다.

---

### 이유

MySQL보다 훨씬 빠른 조회 성능을 제공하기 때문이다.

---

# README 반영 여부

## 반영 완료

- MySQL 사용 이유
- Redis 사용 이유
- User Service DB 설계
- API 설계

---

## 추가 반영 추천

### Redis 활용 목적

```markdown
Redis는 로그인 세션 및 JWT 토큰 저장소로 활용하여 인증 요청 처리 속도를 향상시켰다.
```

---

### JPA 사용 이유

```markdown
JPA를 활용하여 데이터베이스 접근 로직을 객체 중심으로 구현하고 생산성을 향상시켰다.
```

---

### Swagger 사용 이유

```markdown
Swagger UI를 활용하여 API 문서를 자동 생성하고 프론트엔드와 협업 효율을 향상시켰다.
```

---

# 발표 예상 질문

## Q1. MySQL을 사용한 이유는 무엇인가요?

### 답변

사용자 정보는 데이터 정합성이 매우 중요하므로 ACID를 보장하는 MySQL을 선택하였습니다.

---

## Q2. Redis를 사용한 이유는 무엇인가요?

### 답변

로그인 세션과 JWT 토큰을 빠르게 조회하기 위해 인메모리 데이터베이스인 Redis를 사용하였습니다.

---

## Q3. JPA를 사용한 이유는 무엇인가요?

### 답변

객체 중심 개발을 지원하며 반복적인 SQL 작성량을 줄일 수 있기 때문입니다.

---

## Q4. User Service에서 Redis는 어떤 역할을 하나요?

### 답변

로그인 후 생성된 Access Token 및 Refresh Token을 저장하고 인증 요청을 빠르게 처리하는 역할을 수행합니다.

---

## Q5. Swagger를 사용한 이유는 무엇인가요?

### 답변

API 문서를 자동 생성하고 프론트엔드와 API 규격을 공유하기 위해 사용하였습니다.

---

# 실무 관점

실제 Spring Boot 서비스에서는 MySQL만 사용하는 경우보다 Redis를 함께 사용하는 경우가 많다.

MySQL은 영구 저장소(Persistent Storage) 역할을 수행하고,

Redis는 초고속 캐시(Cache) 및 세션 저장소 역할을 수행한다.

이를 통해 성능과 데이터 정합성을 동시에 확보할 수 있다.

---

# 한 줄 요약

4주차에서는 Spring Boot와 MySQL, Redis, JPA, Swagger를 연동하는 방법을 학습하였으며, 이를 User Service의 회원가입, 로그인, 인증 기능 구현에 적용할 수 있는 기반을 마련하였다.