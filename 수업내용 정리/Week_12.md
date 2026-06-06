# 12주차 정리

## 학습 주제

### Microservice Architecture (MSA)

### Monolithic Architecture

### Event Driven Microservice (EDM)

### Service Communication

### Distributed System

---

# 마이크로서비스 아키텍처(MSA)

## 정의

Microservice Architecture(MSA)는 하나의 거대한 애플리케이션을 여러 개의 독립적인 서비스로 분리하여 개발, 배포, 운영하는 아키텍처 방식이다.

각 서비스는 특정 비즈니스 기능을 담당하며 독립적으로 개발 및 배포가 가능하다.

---

## 특징

### 서비스 독립성

각 서비스는 독립적으로 개발된다.

---

예시

```text
User Service

TimeDeal Service

Payment Service

Notification Service
```

---

각 서비스는 서로 독립적으로 동작한다.

---

### 독립 배포

특정 서비스만 수정 후 재배포 가능하다.

---

예시

```text
User Service 수정

↓

User Service만 배포
```

---

전체 시스템 재배포 불필요

---

### 독립 확장

트래픽이 많은 서비스만 확장 가능하다.

---

예시

```text
타임딜 오픈

↓

User Service 요청 증가

↓

User Service만 Scale Out
```

---

### 장애 격리

한 서비스 장애가 전체 시스템 장애로 이어지지 않는다.

---

예시

```text
Notification Service 장애

↓

회원가입 가능

↓

로그인 가능

↓

알림만 실패
```

---

# MSA를 사용하는 이유

## 유연성

서비스별로 다른 기술 스택 사용 가능

---

예시

```text
User Service
Kotlin

Payment Service
Java

AI Service
Python
```

---

## 확장성

필요한 서비스만 Scale Out 가능

---

## 복원력

일부 서비스 장애가 전체 시스템 장애로 확산되지 않음

---

## 배포 효율성

작은 단위로 배포 가능

---

# MSA 주요 특징

## 서비스 독립성

각 서비스는

```text
Code

Database

API
```

를 독립적으로 관리한다.

---

## 분산 처리

서비스가 여러 서버에 분산된다.

---

예시

```text
Server A
User Service

Server B
TimeDeal Service

Server C
Notification Service
```

---

## 경량 통신

서비스 간 API 통신

---

대표 방식

```text
REST API

GraphQL

gRPC
```

---

# RPC

Remote Procedure Call

---

## 개념

원격 서버의 함수를 마치 로컬 함수처럼 호출하는 방식

---

예시

```text
User Service

↓

User 조회 요청

↓

TimeDeal Service
```

---

## 대표 기술

```text
gRPC
```

---

# Monolithic Architecture

## 정의

모든 기능을 하나의 애플리케이션으로 개발하는 방식

---

예시

```text
User

Order

Payment

Notification
```

---

모두 하나의 프로젝트

---

# 장점

## 개발 단순

초기 개발 용이

---

## 테스트 단순

하나의 프로젝트만 실행

---

## 트랜잭션 관리 쉬움

단일 DB 사용

---

# 단점

## 확장성 부족

일부 기능만 확장 불가능

---

## 배포 부담

작은 수정도 전체 재배포

---

## 장애 전파

하나의 오류가 전체 서비스 영향

---

# MSA vs Monolithic

| 항목 | Monolithic | MSA |
|--------|--------|--------|
| 배포 | 전체 배포 | 서비스별 배포 |
| 확장 | 전체 확장 | 서비스별 확장 |
| 장애 | 전체 영향 | 부분 영향 |
| 개발 | 단순 | 복잡 |
| 운영 | 단순 | 복잡 |

---

# Event Driven Microservice (EDM)

## 정의

이벤트(Event)를 기반으로 서비스 간 통신하는 아키텍처

---

## 목적

서비스 간 결합도를 낮추기 위함

---

# 작동 방식

## 1. 이벤트 발생

```text
회원가입 성공
```

---

## 2. 이벤트 발행

```text
UserCreatedEvent
```

---

## 3. 메시지 브로커 전달

```text
Kafka

RabbitMQ
```

---

## 4. 다른 서비스 처리

```text
Notification Service

↓

환영 메시지 발송
```

---

# EDM 장점

## 느슨한 결합

서비스 간 의존성 감소

---

## 확장성

독립 확장 가능

---

## 장애 격리

일부 소비자 장애 허용

---

# 프로젝트 적용

## 프로젝트

### 우리동네 기반 타임딜 공동구매 플랫폼

---

# 왜 MSA를 선택했는가?

## 이유 1

서비스 역할 분리

---

```text
User Service

TimeDeal Service

Order Service

Notification Service
```

---

## 이유 2

독립 배포

---

User Service 수정

↓

User Service만 배포

---

## 이유 3

트래픽 대응

---

타임딜 마감 직전

↓

로그인 폭주

↓

User Service만 확장

---

# User Service

## 담당자

최다울

---

## 담당 기능

```text
회원가입

로그인

인증

GraphQL 조회
```

---

# User Service Database

```text
MySQL
```

회원 원본 데이터 저장

---

```text
Redis
```

세션 및 토큰 저장

---

# User Service 통신

```text
GraphQL

REST API
```

사용

---

# EDM 적용 예시

회원가입

↓

UserCreatedEvent

↓

Notification Service

↓

환영 메시지 발송

---

로그인

↓

LoginSuccessEvent

↓

Analytics Service

↓

로그 분석

---

# README 반영

## 이미 반영된 부분

```markdown
Micro Service

User Service

GraphQL

MySQL

Redis
```

---

## 추가 반영 추천

```markdown
본 프로젝트는 Microservice Architecture(MSA)를 적용하여 User Service, 공동구매 Service 등을 독립적으로 구성하였다.

이를 통해 서비스별 독립 배포와 확장성을 확보하였다.
```

---

# 발표 예상 질문

## Q1. 왜 MSA를 사용했나요?

### 답변

서비스를 독립적으로 개발 및 배포할 수 있고 특정 서비스만 확장할 수 있기 때문입니다.

---

## Q2. Monolithic과 MSA의 차이는 무엇인가요?

### 답변

Monolithic은 모든 기능이 하나의 애플리케이션에 포함되지만 MSA는 기능별로 독립된 서비스를 구성합니다.

---

## Q3. User Service를 분리한 이유는 무엇인가요?

### 답변

회원가입, 로그인, 인증은 다른 기능과 독립성이 높고 트래픽 특성이 다르기 때문에 별도 서비스로 분리하였습니다.

---

## Q4. EDM이란 무엇인가요?

### 답변

이벤트 기반으로 서비스 간 통신하는 아키텍처이며 서비스 간 결합도를 낮출 수 있습니다.

---

## Q5. 현재 프로젝트에 EDM을 적용할 수 있나요?

### 답변

회원가입 성공 이벤트 발생 시 Notification Service가 환영 메시지를 발송하는 구조로 적용할 수 있습니다.

---

# 실무 관점

실제 AWS 환경에서는

```text
API Gateway

↓

User Service

↓

MySQL
```

구조와

```text
Kafka

RabbitMQ
```

기반 Event Driven Architecture를 함께 사용하는 경우가 많다.

특히 대규모 서비스에서는 MSA와 EDM 조합이 대표적인 아키텍처 패턴이다.

---

# 생활 비유

Monolithic은 하나의 대형 백화점과 같다.

문제가 발생하면 백화점 전체가 영향을 받는다.

MSA는 여러 개의 전문 상가가 모인 쇼핑몰과 같다.

한 매장이 공사 중이어도 다른 매장은 정상 운영된다.

따라서 유지보수와 확장성이 훨씬 뛰어나다.

---

# 한 줄 요약

12주차에서는 Microservice Architecture(MSA)와 Monolithic Architecture의 차이를 학습하였으며, 서비스 독립성·확장성·복원성을 확보하기 위해 MSA를 적용하는 이유와 Event Driven Microservice(EDM) 개념을 이해하였다.