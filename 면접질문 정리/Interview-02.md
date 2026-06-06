# Interview Question 02

## 주제

### Kotlin JVM Target의 숨은 비용(Hidden Cost)

### 데이터 검색 및 필터링 알고리즘

---

# 질문 1

## Kotlin으로 작성한 JVM Target 코드는 숨은 비용이 있습니다.

### 어떤 숨은 비용을 말하는 걸까요?

### 그럼에도 불구하고 Kotlin을 써야 할까요?

---

# 답변

Kotlin은 Java보다 생산성과 안정성이 뛰어난 언어이지만, 다양한 편의 기능을 제공하는 과정에서 몇 가지 숨은 비용(Hidden Cost)이 발생할 수 있다.

이러한 비용은 대부분 개발 생산성 향상과 코드 안정성을 위한 대가이며, 일반적인 백엔드 서비스에서는 충분히 감수할 만한 수준이다.

---

# 1. Collection API 비용

Kotlin은 Collection API를 통해 데이터를 매우 직관적으로 처리할 수 있다.

예시

```kotlin
val result = users
    .filter { it.age >= 20 }
    .map { it.name }
```

---

겉보기에는 한 줄의 코드처럼 보이지만 내부적으로는 다음과 같은 작업이 수행된다.

```text
users 순회

↓

filter 수행

↓

임시 리스트 생성

↓

map 수행

↓

새로운 리스트 생성
```

---

즉,

- 반복문 수행
- 임시 객체 생성
- 메모리 사용 증가

와 같은 추가 비용이 발생할 수 있다.

특히 대규모 데이터셋에서는 성능에 영향을 줄 수 있다.

---

# 2. Lambda Expression 비용

Kotlin의 람다는 내부적으로 객체 또는 익명 클래스로 변환된다.

예시

```kotlin
users.filter {
    it.age > 20
}
```

---

실제로는

```text
Lambda 객체 생성

↓

함수 호출

↓

실행
```

과정을 거친다.

---

### 영향

- 클래스 파일 증가
- 메모리 사용 증가
- 함수 호출 오버헤드

---

# 3. Null Safety 비용

Kotlin은 NullPointerException 방지를 위해 다양한 Null Safety 기능을 제공한다.

예시

```kotlin
val nickname = user?.nickname ?: "Guest"
```

---

내부적으로는

```text
Null 체크

↓

값 존재 여부 확인

↓

기본값 반환
```

과정이 수행된다.

---

### 장점

- 런타임 오류 감소
- 서비스 안정성 향상

---

### 비용

- 추가적인 CPU 연산 발생

---

# 4. Property Getter / Setter 비용

Kotlin의 Property는 실제로 Getter와 Setter 메서드로 변환된다.

예시

```kotlin
data class User(
    val nickname: String
)
```

---

실제로는

```java
public String getNickname()
```

형태로 생성된다.

---

### 영향

직접 필드 접근보다 약간의 오버헤드가 존재한다.

다만 대부분의 경우 무시 가능한 수준이다.

---

# Kotlin을 사용해야 하는 이유

숨은 비용이 존재하지만 Kotlin을 사용하는 장점이 훨씬 크다.

---

## 장점

### Null Safety

```kotlin
String?
```

을 통해 NullPointerException 예방

---

### Data Class

```kotlin
data class UserDto(...)
```

를 통한 DTO 작성 단순화

---

### Coroutine

비동기 프로그래밍 간소화

---

### Spring Boot 호환성

기존 Java 생태계를 그대로 활용 가능

---

### 생산성 향상

보일러플레이트 코드 감소

---

# 결론

일반적인 Spring Boot 백엔드 서비스에서는 Kotlin의 숨은 비용보다 생산성과 안정성 향상 효과가 훨씬 크므로 Kotlin 사용이 매우 합리적이다.

---

# 프로젝트 연계

## User Service와의 관계

현재 프로젝트는 Kotlin 기반 User Service를 개발한다.

예시

```kotlin
data class UserRegisterDto(
    val email: String,
    val password: String,
    val nickname: String
)
```

---

회원가입 및 로그인 과정에서

- Data Class
- Collection API
- Null Safety

를 적극적으로 활용한다.

---

### 실제 사용 예시

```kotlin
users.firstOrNull {
    it.email == email
}
```

---

Collection API의 편의성을 활용하여 사용자 조회 로직을 구현할 수 있다.

---

# 질문 2

## 데이터를 필터링하거나 검색할 때 사용할 수 있는 다양한 알고리즘과 접근 방식이 있습니다.

### 어떤 방식이 있는지와 사용 조건을 설명해주세요.

---

# 답변

데이터 검색 및 필터링에는 다양한 알고리즘이 존재하며, 데이터의 크기와 구조에 따라 적절한 알고리즘을 선택해야 한다.

---

# 1. 선형 검색 (Linear Search)

## 방식

데이터를 처음부터 끝까지 하나씩 확인한다.

---

## 예시

```kotlin
users.find {
    it.email == email
}
```

---

## 시간 복잡도

```text
O(N)
```

---

## 사용 조건

- 데이터가 적은 경우
- 데이터가 정렬되지 않은 경우

---

## Kotlin 적용 예시

```kotlin
filter()
find()
firstOrNull()
```

대부분 선형 탐색 기반이다.

---

# 2. 이진 검색 (Binary Search)

## 방식

정렬된 데이터의 중간값을 기준으로 탐색 범위를 절반씩 줄여간다.

---

## 시간 복잡도

```text
O(log N)
```

---

## 사용 조건

- 데이터가 정렬되어 있어야 함

---

## 장점

대규모 데이터 검색에 매우 빠름

---

# 3. 해시 테이블 (Hash Table)

## 방식

Key를 Hash 함수에 적용하여 즉시 데이터 위치를 찾는다.

---

## 예시

```kotlin
HashMap<String, User>
```

---

## 시간 복잡도

```text
평균 O(1)
```

---

## 사용 조건

- 빠른 검색
- 빠른 삽입
- 빠른 삭제

---

## 단점

데이터 순서가 유지되지 않는다.

---

# 4. 이진 탐색 트리 (Binary Search Tree)

## 방식

노드를 트리 구조로 저장하고 정렬 상태를 유지한다.

---

## 시간 복잡도

```text
평균 O(log N)
```

---

## 사용 조건

- 검색
- 삽입
- 삭제

를 모두 효율적으로 수행해야 할 때

---

## 실무 예시

MySQL Index

```text
B-Tree
```

구조 사용

---

# 5. 그래프 탐색 알고리즘

## BFS (Breadth First Search)

### 방식

가까운 노드부터 탐색

---

### 활용

- 친구 추천
- 최단 경로 탐색

---

## DFS (Depth First Search)

### 방식

끝까지 탐색 후 되돌아옴

---

### 활용

- 미로 찾기
- 트리 탐색

---

# 프로젝트 연계

## User Service

현재 User Service의 로그인 기능은

```kotlin
users.find {
    it.email == email
}
```

형태라면

실제로는

```text
Linear Search

O(N)
```

이다.

---

하지만 실무 환경에서는

```text
MySQL Index

↓

B-Tree

↓

O(logN)
```

방식으로 검색 성능을 향상시킨다.

---

## Redis

Redis는 내부적으로 Hash 구조를 활용하여 매우 빠른 조회 성능을 제공한다.

---

# 발표 예상 질문

## Q1. Kotlin의 숨은 비용은 무엇인가요?

### 답변

Collection API, Lambda, Null Safety, Property Getter/Setter 등으로 인해 약간의 메모리 및 CPU 오버헤드가 발생할 수 있습니다.

---

## Q2. 그럼에도 Kotlin을 사용하는 이유는 무엇인가요?

### 답변

Null Safety, Data Class, Coroutine 지원을 통해 생산성과 안정성을 크게 향상시킬 수 있기 때문입니다.

---

## Q3. filter()는 어떤 알고리즘을 사용하나요?

### 답변

Collection을 처음부터 끝까지 순회하는 선형 탐색(Linear Search) 기반으로 동작합니다.

---

## Q4. MySQL은 어떤 검색 방식을 사용하나요?

### 답변

인덱스가 적용된 컬럼은 일반적으로 B-Tree 기반 탐색을 사용하여 O(logN) 수준의 성능을 제공합니다.

---

## Q5. User Service에서는 어떤 검색 방식이 사용되나요?

### 답변

예제 수준에서는 Collection 기반 선형 탐색을 사용할 수 있지만, 실제 서비스에서는 MySQL Index와 Redis를 활용하여 검색 성능을 최적화합니다.

---

# 실무 관점

실제 Spring Boot 백엔드에서는 Kotlin의 생산성과 유지보수성이 약간의 성능 오버헤드보다 훨씬 큰 장점을 제공한다.

또한 사용자 수가 증가하는 서비스에서는 단순 선형 탐색이 아닌 Database Index, Redis Cache, Hash 기반 조회 전략을 함께 사용하여 성능 문제를 해결한다.

---

# 한 줄 요약

Kotlin은 Collection API, Lambda, Null Safety 등으로 인해 약간의 숨은 비용이 존재하지만 생산성과 안정성 측면의 이점이 훨씬 크며, 데이터 검색 시에는 상황에 따라 Linear Search, Binary Search, Hash Table, B-Tree 등의 적절한 알고리즘을 선택해야 한다.