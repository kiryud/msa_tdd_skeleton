# 07주차 정리

## 학습 주제

### Thread와 Virtual Thread

운영체제가 관리하는 전통적인 Thread와 Java의 Virtual Thread 개념을 학습하였다.

---

### Kotlin Coroutine

Kotlin에서 제공하는 경량 비동기 처리 기술인 Coroutine을 학습하였다.

---

### Coroutine Builder

- launch
- async
- await
- runBlocking

Coroutine 실행 및 결과 처리 방식을 학습하였다.

---

### Coroutine Dispatcher

- Dispatchers.IO
- Dispatchers.Default

작업 성격에 따라 적절한 스레드 풀을 사용하는 방법을 학습하였다.

---

### Coroutine 실습

DataFaker를 활용하여 대량의 가상 데이터를 생성하고 Coroutine을 통해 비동기 처리하는 실습을 진행하였다.

---

# 핵심 개념

## Thread

Thread는 운영체제가 관리하는 실행 단위이다.

---

### 특징

- 독립적인 실행 흐름
- 자체 Stack 보유
- Heap 공유
- 생성 비용 높음

---

### 문제점

- 메모리 사용량 증가
- Context Switching 비용
- 동시성 문제 발생 가능

---

## Virtual Thread

Java Loom 프로젝트에서 등장한 경량 Thread이다.

---

### 특징

- 매우 적은 메모리 사용
- 대량 생성 가능
- Thread 생성 비용 감소

---

## Coroutine

Kotlin에서 제공하는 경량 비동기 처리 기술이다.

---

### 특징

- 적은 메모리 사용
- 높은 동시성 처리
- 비동기 프로그래밍 단순화
- Thread보다 생성 비용이 낮음

---

### 목적

대기 시간이 많은 작업(I/O 작업)을 효율적으로 처리하기 위함이다.

---

# runBlocking

Coroutine 실행 환경을 생성한다.

---

## 예시

```kotlin
runBlocking {
    println("Coroutine Start")
}
```

---

### 역할

테스트 환경 또는 메인 함수에서 Coroutine 실행을 시작할 때 사용한다.

---

# launch

결과 반환 없이 비동기 작업을 실행한다.

---

## 예시

```kotlin
launch {
    delay(1000)
    println("Task Complete")
}
```

---

### 특징

- 반환값 없음
- Fire-And-Forget 방식
- 로그 기록
- 이벤트 처리

에 적합

---

# async / await

비동기 작업을 수행하고 결과를 반환받는다.

---

## 예시

```kotlin
val result = async {
    "Hello"
}

println(result.await())
```

---

### 특징

- 결과 반환 가능
- 병렬 처리 가능
- 여러 작업 동시 수행 가능

---

# suspend Function

Coroutine 내부에서 실행 가능한 함수이다.

---

## 예시

```kotlin
suspend fun fetchUser() {
    delay(1000)
}
```

---

### 특징

대기 중인 작업을 효율적으로 관리할 수 있다.

---

# Dispatchers

Coroutine이 어떤 스레드 풀에서 실행될지를 결정한다.

---

## Dispatchers.IO

I/O 작업 처리

---

### 사용 예시

```text
MySQL 조회

Redis 조회

파일 처리

네트워크 통신
```

---

## Dispatchers.Default

CPU 계산 작업 처리

---

### 사용 예시

```text
통계 계산

데이터 분석

암호화 연산
```

---

# 실습 내용

## DataFaker 활용

가상의 사용자 데이터를 생성하였다.

---

### 생성 데이터

```text
이름

이메일

주소
```

---

## 문제 1

100명의 가상 사용자 이름 생성

---

### 사용 기술

- Coroutine
- launch
- List

---

## 문제 2

50명의 가상 사용자 정보 생성

---

### 사용 기술

- Coroutine
- launch
- DataFaker

---

## 문제 3

30명의 사용자 데이터 생성 후 나이순 정렬

---

### 사용 기술

- Data Class
- async
- await
- sortedBy

---

### 예시

```kotlin
data class User(
    val name: String,
    val age: Int
)
```

---

# 프로젝트 적용

## 프로젝트 주제

### 우리동네 기반 타임딜 공동구매 플랫폼

본 프로젝트는 타임딜 마감 직전 다수 사용자가 동시에 접속하는 환경을 가정한다.

따라서 효율적인 비동기 처리 구조가 필요하다.

---

# User Service 적용

## 담당자

최다울

---

## 담당 영역

```text
User Service

- 회원가입
- 로그인
- 사용자 인증
- GraphQL 조회
```

---

## 회원가입

```text
회원가입 요청

↓

이메일 중복 조회

↓

MySQL 조회
```

---

### 특징

DB 조회가 포함된 I/O 작업

---

### 적용 기술

```kotlin
Dispatchers.IO
```

---

## 로그인

```text
사용자 조회

↓

비밀번호 검증

↓

JWT 생성
```

---

### 적용 기술

```kotlin
Coroutine

async / await
```

---

## GraphQL 조회

```text
User 조회

↓

DB 조회

↓

응답 반환
```

---

### 적용 기술

```kotlin
Coroutine
```

---

# Coroutine을 사용하는 이유

현재 프로젝트는

```text
MySQL

Redis

외부 API
```

와 통신한다.

---

이들은 모두

```text
I/O 작업
```

이다.

---

일반 Thread를 대량 생성하면

```text
메모리 증가

Context Switching 증가
```

가 발생한다.

---

Coroutine은

```text
적은 자원

높은 동시성

빠른 응답
```

을 제공한다.

---

따라서 User Service의 회원가입, 로그인, 인증 기능에 적합하다.

---

# README 반영 여부

## 반영 완료

- Coroutine 사용 이유
- async / await
- 비동기 처리 설명

---

## 추가 반영 추천

```markdown
회원가입 및 로그인 과정은 MySQL, Redis 등 외부 자원과 통신하는 I/O 작업이므로 Kotlin Coroutine과 Dispatchers.IO를 활용하여 비동기 처리할 수 있도록 설계하였다.
```

---

# 발표 예상 질문

## Q1. Thread와 Coroutine의 차이는 무엇인가요?

### 답변

Thread는 운영체제가 관리하는 실행 단위이고 생성 비용이 높습니다.

Coroutine은 Kotlin이 제공하는 경량 비동기 처리 기술로 적은 자원으로 많은 작업을 동시에 처리할 수 있습니다.

---

## Q2. 왜 Coroutine을 사용했나요?

### 답변

회원가입, 로그인, 사용자 조회 과정은 대부분 MySQL, Redis와 통신하는 I/O 작업이므로 적은 자원으로 높은 동시성을 제공하는 Coroutine을 사용하였습니다.

---

## Q3. launch와 async의 차이는 무엇인가요?

### 답변

launch는 결과를 반환하지 않는 비동기 작업이고, async는 결과를 반환하는 비동기 작업입니다.

---

## Q4. Dispatchers.IO는 언제 사용하나요?

### 답변

MySQL 조회, Redis 조회, 파일 처리, 네트워크 통신 등 I/O 작업을 처리할 때 사용합니다.

---

## Q5. User Service에서는 Coroutine을 어디에 적용할 수 있나요?

### 답변

회원가입 시 이메일 중복 확인, 로그인 시 사용자 조회, GraphQL 조회 등 데이터베이스 접근이 필요한 영역에 적용할 수 있습니다.

---

# 실무 관점

실제 대규모 서비스에서는 Thread를 무분별하게 생성하는 대신 Coroutine을 활용하여 높은 동시성과 효율적인 자원 관리를 수행한다.

특히 Spring Boot + Kotlin 환경에서는 Coroutine이 대표적인 비동기 처리 방식으로 사용된다.

---

# 한 줄 요약

7주차에서는 Kotlin Coroutine을 활용한 비동기 처리 방법을 학습하였으며, 이를 User Service의 회원가입, 로그인, 인증 기능에서 발생하는 I/O 작업 최적화에 적용할 수 있음을 이해하였다.