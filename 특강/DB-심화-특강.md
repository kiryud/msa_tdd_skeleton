# DB 심화 특강 정리

## 학습 주제

### Transaction & ACID

### MongoDB (Document Store)

### Redis 실무 활용

### Data Lake Architecture

### Cache Pattern

### Distributed Lock

---

# 1. Transaction (트랜잭션)

## 정의

트랜잭션(Transaction)은 데이터베이스에서 수행되는 여러 작업을 하나의 논리적인 작업 단위로 묶는 것이다.

---

### 목적

모든 작업이 성공하거나,

모든 작업이 실패해야 한다.

---

### 예시

공동구매 참여

```text
주문 생성

↓

재고 차감

↓

결제 기록 저장
```

---

중간에 실패하면?

```text
주문 생성 성공

재고 차감 성공

결제 저장 실패
```

↓

문제 발생

---

따라서

```text
모두 성공

또는

모두 취소
```

되어야 한다.

---

# ACID

트랜잭션이 지켜야 하는 4가지 특성

---

## A : Atomicity (원자성)

### All Or Nothing

모든 작업이 성공하거나

모두 실패해야 한다.

---

### 예시

```text
User 저장 성공

Redis 저장 실패
```

↓

Rollback

↓

전체 취소

---

## C : Consistency (일관성)

DB의 규칙을 반드시 유지해야 한다.

---

### 예시

```text
User 존재 X

↓

Order 생성
```

불가능

---

외래키(FK) 제약 조건 유지

---

## I : Isolation (격리성)

동시에 실행되는 트랜잭션이 서로 영향을 주면 안 된다.

---

### 예시

```text
타임딜 참여

유저 A

유저 B

동시 참여
```

↓

Lock

↓

순차 처리

---

## D : Durability (지속성)

Commit된 데이터는 영구 보존된다.

---

### 예시

```text
Commit

↓

서버 다운
```

↓

데이터 유지

---

# TCL

Transaction Control Language

---

## START TRANSACTION

트랜잭션 시작

```sql
START TRANSACTION;
```

---

## COMMIT

작업 확정

```sql
COMMIT;
```

---

## ROLLBACK

작업 취소

```sql
ROLLBACK;
```

---

# 프로젝트 적용

## User Service

회원가입

```text
User 저장

↓

Redis 세션 저장

↓

Commit
```

---

중간 실패

↓

Rollback

---

# 2. MongoDB

## 정의

MongoDB는 Document 기반 NoSQL 데이터베이스이다.

---

## 특징

### Document Store

JSON 형태 저장

---

예시

```json
{
  "name":"daoul",
  "email":"test@test.com"
}
```

---

### Schema-less

고정 스키마 없음

---

### No Join

데이터를 하나의 Document로 저장

---

# RDBMS의 한계

MySQL

```text
1 게임

↓

10명 참여자

↓

300개 이벤트
```

---

조회 시

```text
JOIN

JOIN

JOIN
```

발생

---

# MongoDB 방식

```json
{
  "game":"KR_123",
  "participants":[...],
  "timeline":[...]
}
```

---

한 번에 조회 가능

---

# Data Lake Architecture

## 구조

```text
Riot API

↓

MongoDB

↓

Batch

↓

MySQL
```

---

## 목적

원본 JSON 보존

---

### 장점

파싱 실패

↓

MongoDB 원본 재사용

↓

API 재호출 불필요

---

# NoSQL 유형

## 1. Key-Value

예시

```text
Redis
```

---

## 2. Document Store

예시

```text
MongoDB
```

---

## 3. Column Family

예시

```text
Cassandra

HBase
```

---

## 4. Graph Store

예시

```text
Neo4j
```

---

# BASE

NoSQL 철학

---

## Basically Available

항상 응답

---

## Soft State

상태 변화 허용

---

## Eventual Consistency

최종 일관성 보장

---

# 3. Redis

## 정의

인메모리(In-Memory) 기반 Key-Value 데이터베이스

---

## 특징

```text
매우 빠름

캐시

세션

분산락

실시간 처리
```

---

# Look-Aside Cache

가장 대표적인 캐시 패턴

---

## 구조

```text
Client

↓

Redis

↓

MySQL
```

---

## 동작

### Cache Hit

```text
Redis 존재
```

↓

즉시 반환

---

### Cache Miss

```text
Redis 없음
```

↓

MySQL 조회

↓

Redis 저장

↓

반환

---

# 프로젝트 적용

## 인기 공동구매 목록

```text
Redis 조회

↓

Hit

↓

즉시 반환
```

---

# Redis 실무 활용 사례

## 1. Distributed Lock

### 목적

동시성 문제 해결

---

### 예시

타임딜 선착순

```text
100개 쿠폰

↓

10만명 동시 클릭
```

---

문제

```text
중복 발급
```

---

Redis

```text
SETNX

Lock 획득

↓

한 명만 처리
```

---

### 프로젝트 적용

```text
공동구매 참여

선착순 이벤트

타임딜 마감
```

---

# 2. Response Caching

### 목적

API 응답 캐싱

---

예시

```text
인기 공동구매 목록
```

---

Redis 저장

↓

빠른 응답

---

# 3. Counter

### 목적

조회수

좋아요

참여자 수

---

예시

```text
Redis INCR
```

---

장점

DB 부하 감소

---

# 4. Session Store

### 목적

로그인 유지

---

예시

```text
JWT

Refresh Token

Session
```

Redis 저장

---

### 프로젝트 적용

User Service

```text
로그인

세션 저장

인증 처리
```

---

# 5. Sorted Set (ZSet)

### 목적

실시간 순위

랭킹

---

예시

```text
실시간 인기 공동구매

실시간 검색어
```

---

명령

```text
ZADD

ZREVRANGE
```

---

# 프로젝트 적용

## 우리동네 기반 타임딜 공동구매 플랫폼

---

## MySQL

저장

```text
회원 정보

주문 정보

결제 정보
```

---

## Redis

캐시

```text
인기 공구

세션

토큰

실시간 참여자 수
```

---

## Distributed Lock

동시성 제어

```text
선착순 참여

중복 참여 방지
```

---

## MongoDB

확장 시

```text
원본 위치 데이터

로그 데이터

이벤트 데이터
```

저장 가능

---

# README 반영 추천

## Redis 사용 이유

```markdown
Redis를 활용하여 로그인 세션 관리, 인기 공동구매 캐싱, 실시간 참여자 수 집계 및 선착순 이벤트 동시성 제어를 수행하였다.
```

---

## Transaction 사용 이유

```markdown
회원가입 및 주문 생성 과정에서 데이터 정합성을 보장하기 위해 트랜잭션을 적용하였다.
```

---

## Distributed Lock 사용 이유

```markdown
타임딜 마감 직전 발생하는 동시 요청 환경에서 Redis 기반 Distributed Lock을 활용하여 중복 참여 문제를 방지할 수 있도록 설계하였다.
```

---

# 발표 예상 질문

## Q1. ACID란 무엇인가요?

트랜잭션이 지켜야 하는 원자성, 일관성, 격리성, 지속성의 4가지 특성입니다.

---

## Q2. Redis를 사용하는 이유는 무엇인가요?

캐싱, 세션 저장, 실시간 처리, 동시성 제어를 위해 사용합니다.

---

## Q3. Cache Hit와 Cache Miss란 무엇인가요?

Redis에 데이터가 존재하면 Cache Hit, 존재하지 않으면 Cache Miss입니다.

---

## Q4. Distributed Lock은 왜 필요한가요?

선착순 이벤트나 타임딜에서 중복 처리 문제를 방지하기 위해 필요합니다.

---

## Q5. MongoDB를 사용하는 이유는 무엇인가요?

JSON 형태의 유연한 데이터 저장과 대용량 원본 데이터 보관에 적합하기 때문입니다.

---

# 생활 비유

MySQL은 은행 금고이다.

정확하게 보관하지만 꺼내는 데 시간이 걸린다.

Redis는 계산대 서랍이다.

자주 사용하는 돈을 넣어두고 즉시 꺼낼 수 있다.

MongoDB는 창고 전체를 박스째 보관하는 물류창고이다.

원본 형태 그대로 보관하기 때문에 나중에 다시 가공하기 쉽다.

---

# 한 줄 요약

트랜잭션(ACID)은 데이터 정합성을 보장하고, Redis는 캐싱·세션·동시성 제어를 담당하며, MongoDB는 대용량 원본 데이터를 유연하게 저장하는 NoSQL 데이터베이스이다.