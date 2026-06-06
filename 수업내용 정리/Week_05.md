# 05주차 정리

## 학습 주제

### REST API 테스트

Swagger UI를 활용하여 REST API를 테스트하는 방법을 학습하였다.

---

### GraphQL

GraphQL 서버를 구축하고 Query 및 Mutation을 작성하는 방법을 학습하였다.

---

### GraphiQL

GraphQL API를 테스트하기 위한 웹 기반 도구(GraphiQL)의 사용 방법을 학습하였다.

---

### API 테스트 환경 구축

- Swagger UI
- GraphiQL

을 활용하여 REST API와 GraphQL API를 직접 테스트하는 방법을 학습하였다.

---

# 핵심 개념

## REST API

REST(Representational State Transfer)는 자원(Resource)을 URI로 표현하고 HTTP Method를 이용하여 데이터를 처리하는 API 설계 방식이다.

---

### 주요 HTTP Method

#### GET

데이터 조회

```http
GET /users
```

---

#### POST

데이터 생성

```http
POST /users
```

---

#### PUT

데이터 수정

```http
PUT /users/1
```

---

#### DELETE

데이터 삭제

```http
DELETE /users/1
```

---

# Swagger UI

Swagger UI는 REST API를 문서화하고 테스트할 수 있는 도구이다.

---

## 접속 주소

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 장점

- API 문서 자동 생성
- API 테스트 가능
- 프론트엔드 협업 지원
- 요청/응답 확인 가능

---

# GraphQL

GraphQL은 Facebook(Meta)에서 개발한 API Query Language이다.

클라이언트가 필요한 데이터만 선택적으로 요청할 수 있다.

---

## 특징

### 단일 엔드포인트

```text
/graphql
```

하나의 엔드포인트만 사용한다.

---

### 필요한 데이터만 조회

클라이언트가 원하는 필드만 요청한다.

---

## 예시

```graphql
query {
  user(id: "1") {
    nickname
  }
}
```

---

응답

```json
{
  "nickname": "daoul"
}
```

---

# GraphiQL

GraphQL API를 테스트하기 위한 웹 기반 IDE이다.

---

## 접속 주소

```text
http://localhost:8080/graphiql.html
```

---

## 역할

- Query 테스트
- Mutation 테스트
- 응답 확인
- Schema 탐색

---

# Query

데이터를 조회하는 GraphQL 명령이다.

---

## 예시

```graphql
query {
  findAllTests {
    id
    address
    email
    tel
    age
  }
}
```

---

## 역할

```text
SELECT
```

와 유사하다.

---

### 데이터 조회

```text
DB

↓

조회

↓

응답 반환
```

---

# Mutation

데이터를 생성, 수정, 삭제하는 GraphQL 명령이다.

---

## 예시

```graphql
mutation {
  createTest(userId:"1") {
    id
    address
    email
    tel
    age
  }
}
```

---

## 역할

```text
INSERT
UPDATE
DELETE
```

와 유사하다.

---

### 데이터 생성

```text
요청

↓

Service

↓

DB 저장

↓

응답 반환
```

---

# 프로젝트 적용

## 프로젝트 주제

### 우리동네 기반 타임딜 공동구매 플랫폼

사용자 정보, 공동구매 정보, 참여자 정보 등을 효율적으로 조회하기 위해 GraphQL을 활용할 수 있다.

---

# REST API 적용

회원가입

```http
POST /users/register
```

---

로그인

```http
POST /users/login
```

---

사용자 조회

```http
GET /users/{id}
```

---

# GraphQL 적용

## User 조회

```graphql
query {
  findUserById(id:"1") {
    nickname
    email
  }
}
```

---

## 회원가입

```graphql
mutation {
  registerUser(
    email:"test@test.com",
    password:"1234",
    nickname:"daoul"
  ) {
    id
    nickname
  }
}
```

---

# User Service 적용

## UserQueryResolver

현재 프로젝트의 UserQueryResolver는 GraphQL Query를 처리하는 역할을 수행한다.

예시

```text
findUserById()

findAllUsers()
```

---

## UserService

비즈니스 로직을 처리한다.

```text
회원가입

로그인

사용자 조회
```

---

## UserRegisterDto

GraphQL Mutation 요청 데이터를 전달한다.

예시

```kotlin
data class UserRegisterDto(
    val email: String,
    val password: String,
    val nickname: String
)
```

---

# GraphQL을 사용하는 이유

현재 프로젝트에서는 화면마다 필요한 데이터가 다르다.

---

예시

### 지도 화면

필요 데이터

```text
공구방 위치

마감 시간
```

---

### 상세 화면

필요 데이터

```text
참여자

댓글

상품 정보

작성자 정보
```

---

REST API

```text
불필요한 데이터 포함 가능
```

---

GraphQL

```text
필요한 데이터만 조회 가능
```

---

## 장점

- 네트워크 트래픽 감소
- Overfetching 방지
- 모바일 환경 최적화
- 프론트엔드 유연성 향상

---

# README 반영 여부

## 반영 완료

- GraphQL 사용 이유
- GraphQL 개념
- User Service 설계
- REST API 설명

---

## 추가 반영 추천

### GraphQL Query 예시

```graphql
query {
  findUserById(id:"1") {
    nickname
    email
  }
}
```

---

### GraphQL Mutation 예시

```graphql
mutation {
  registerUser(...) {
    id
    nickname
  }
}
```

---

# 발표 예상 질문

## Q1. GraphQL을 사용하는 이유는 무엇인가요?

### 답변

클라이언트가 필요한 데이터만 선택적으로 요청할 수 있어 네트워크 비용을 줄이고 Overfetching 문제를 해결할 수 있기 때문입니다.

---

## Q2. Query와 Mutation의 차이는 무엇인가요?

### 답변

Query는 데이터 조회를 담당하고 Mutation은 데이터 생성, 수정, 삭제를 담당합니다.

---

## Q3. REST API와 GraphQL의 차이는 무엇인가요?

### 답변

REST는 여러 엔드포인트를 사용하지만 GraphQL은 단일 엔드포인트를 사용하며 필요한 데이터만 요청할 수 있습니다.

---

## Q4. UserQueryResolver의 역할은 무엇인가요?

### 답변

GraphQL Query 요청을 받아 UserService를 호출하고 응답 데이터를 반환하는 역할을 수행합니다.

---

## Q5. 현재 프로젝트에서 GraphQL이 필요한 이유는 무엇인가요?

### 답변

지도 화면과 상세 화면이 요구하는 데이터가 다르기 때문에 필요한 데이터만 조회할 수 있는 GraphQL이 효율적입니다.

---

# 실무 관점

실제 MSA 환경에서는 REST API와 GraphQL을 함께 사용하는 경우가 많다.

특히 모바일 서비스에서는 GraphQL을 사용하여 네트워크 비용을 줄이고 사용자 경험을 향상시키는 사례가 증가하고 있다.

---

# 한 줄 요약

5주차에서는 Swagger UI와 GraphiQL을 활용한 API 테스트 방법을 학습하였으며, GraphQL의 Query와 Mutation을 활용하여 효율적인 데이터 조회 및 조작 방법을 이해하였다.