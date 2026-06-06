# 02주차 정리

## 학습 주제

### Kotlin 기초 문법 및 함수형 프로그래밍

Kotlin 언어의 기본 문법과 Collection API를 활용한 함수형 프로그래밍 기법을 학습하였다.

---

### Kotlin Collection API

- filter()
- firstOrNull()
- map()
- mapNotNull()
- flatMap()

컬렉션(List)의 데이터를 반복문 없이 선언적으로 처리하는 방법을 학습하였다.

---

### Null Safety

- Nullable Type (`String?`, `Int?`)
- Elvis Operator (`?:`)
- Safe Call (`?.`)
- takeUnless()

Kotlin의 Null Safety 시스템을 활용하여 NullPointerException을 예방하는 방법을 학습하였다.

---

### Data Class

```kotlin
data class User(
    val name: String?,
    val age: Int?
)
```

DTO(Data Transfer Object) 및 데이터 모델 객체를 간결하게 작성하는 방법을 학습하였다.

---

### 함수형 프로그래밍 스타일

기존 Java의 반복문 중심 개발 방식에서 벗어나 Collection API와 Lambda Expression을 활용한 선언형 프로그래밍 방식을 학습하였다.

---

# 핵심 개념

## filter()

조건에 맞는 데이터만 추출한다.

### 예시

```kotlin
val evenNumbers = list.filter {
    it % 2 == 0
}
```

---

## firstOrNull()

조건에 맞는 첫 번째 데이터를 반환한다.

조건에 맞는 데이터가 없으면 null을 반환한다.

### 예시

```kotlin
val user = users.firstOrNull {
    it.email == email
}
```

---

## map()

데이터를 다른 형태로 변환한다.

### 예시

```kotlin
val names = users.map {
    it.name
}
```

---

## mapNotNull()

Null 값을 제거하면서 데이터를 변환한다.

### 예시

```kotlin
val names = users.mapNotNull {
    it.name
}
```

---

## flatMap()

중첩된 컬렉션(List<List<T>>)을 하나의 컬렉션으로 평탄화한다.

### 예시

```kotlin
orders.flatMap {
    it.products
}
```

---

## Null Safety

Kotlin의 대표적인 특징으로 NullPointerException을 컴파일 단계에서 예방한다.

### 예시

```kotlin
val nickname: String? = null
```

---

### Elvis Operator

```kotlin
val name = user.name ?: "Unknown"
```

null인 경우 기본값을 반환한다.

---

## Data Class

DTO 및 Entity 설계 시 사용된다.

### 장점

- Getter 자동 생성
- Setter 자동 생성
- equals()
- hashCode()
- toString()

등을 자동으로 생성하여 보일러플레이트 코드를 줄일 수 있다.

---

# 프로젝트 적용

## 프로젝트 주제

### 우리동네 기반 타임딜 공동구매 플랫폼

사용자 인증 및 공동구매 참여 기능을 제공하는 하이퍼로컬 플랫폼이다.

---

## Kotlin 선택 이유

본 프로젝트는 Kotlin 기반으로 개발한다.

### 선택 이유

- Null Safety 지원
- Data Class 지원
- Coroutine 지원
- Spring Boot와 높은 호환성
- Java 생태계 활용 가능

---

## Collection API 활용

기존 반복문보다 가독성이 높은 함수형 프로그래밍 방식을 적용한다.

예시

```kotlin
users.filter {
    it.isVerified
}
```

---

## Data Class 활용

회원가입 및 로그인 요청 객체를 DTO 형태로 설계한다.

예시

```kotlin
data class UserRegisterDto(
    val email: String,
    val password: String,
    val nickname: String
)
```

---

# User Service 적용

## 회원가입 DTO 설계

User Service에서는 회원가입 요청 정보를 Data Class로 정의한다.

```kotlin
data class UserRegisterDto(
    val email: String,
    val password: String,
    val nickname: String
)
```

---

## 로그인 사용자 조회

로그인 시 이메일로 사용자를 조회할 때 Collection API 패턴을 활용할 수 있다.

```kotlin
users.firstOrNull {
    it.email == request.email
}
```

---

## 사용자 응답 객체 생성

DB에서 조회한 Entity를 API 응답 DTO로 변환한다.

```kotlin
userEntity.toResponse()
```

또는

```kotlin
users.map {
    UserResponseDto(
        nickname = it.nickname
    )
}
```

---

## Null Safety 적용

회원가입 및 로그인 과정에서 발생 가능한 NullPointerException을 방지한다.

예시

```kotlin
val nickname = user.nickname ?: "Guest"
```

---

## GraphQL 적용 기반

GraphQL Resolver에서 조회된 사용자 데이터를 Response DTO로 변환할 때 Collection API를 활용할 수 있다.

---

# README 반영 여부

## 반영 완료

- Kotlin Data Class
- Kotlin 사용 이유
- DTO 설계
- GraphQL 연계 설명

---

## 추가 반영 가능

### Null Safety

```markdown
Kotlin의 Null Safety 기능을 활용하여 회원가입 DTO와 사용자 응답 객체의 안정성을 확보하였다.
```

---

### Collection API

```markdown
filter(), map(), firstOrNull() 등의 Collection API를 활용하여 사용자 데이터 조회 및 변환 로직을 간결하게 구현하였다.
```

---

# 발표 예상 질문

## Q1. Kotlin을 선택한 이유는 무엇인가요?

### 답변

Kotlin은 Null Safety, Data Class, Coroutine을 기본적으로 지원하며 Spring Boot와 높은 호환성을 제공하기 때문에 선택하였습니다.

---

## Q2. Data Class를 사용한 이유는 무엇인가요?

### 답변

회원가입 및 로그인 DTO를 설계할 때 Getter, Setter 등의 보일러플레이트 코드를 줄이고 가독성을 향상시키기 위해 사용하였습니다.

---

## Q3. Null Safety가 무엇인가요?

### 답변

Kotlin이 제공하는 기능으로 NullPointerException을 컴파일 단계에서 예방할 수 있도록 지원하는 시스템입니다.

---

## Q4. Collection API를 사용한 이유는 무엇인가요?

### 답변

반복문 중심 코드보다 가독성이 높고 유지보수가 쉬운 함수형 프로그래밍 스타일을 구현할 수 있기 때문입니다.

---

## Q5. User Service에서 2주차 내용을 어떻게 적용했나요?

### 답변

회원가입 DTO를 Data Class로 설계하였고, 사용자 조회 및 데이터 변환 과정에서 Collection API와 Null Safety 개념을 적용하였습니다.

---

# 실무 관점

실제 Spring Boot 기반 백엔드 개발에서는 DTO 설계, Entity 변환, GraphQL 응답 처리, 비즈니스 로직 구현 과정에서 Data Class와 Collection API를 매우 빈번하게 사용한다.

특히 Kotlin의 Null Safety는 런타임 오류를 크게 줄여 서비스 안정성을 향상시키는 핵심 기능으로 활용된다.

---

# 한 줄 요약

2주차에서는 Kotlin Collection API, Null Safety, Data Class를 학습하였으며, 이를 User Service의 DTO 설계와 사용자 데이터 처리 로직에 적용할 수 있는 기반을 마련하였다.