# Interview Question 03

## 주제

### RDBMS JOIN

### 데이터베이스 성능 최적화

---

# 질문 1

## RDBMS의 여러 JOIN 중 하나를 선택하여 그림과 함께 설명해주세요.

---

# INNER JOIN

INNER JOIN은 두 테이블에서 조건이 일치하는 데이터만 조회하는 JOIN 방식이다.

---

## 예시 테이블

### User

| user_id | nickname |
|----------|----------|
| 1 | daoul |
| 2 | minsang |
| 3 | suyeon |

---

### Order

| order_id | user_id | product |
|----------|----------|----------|
| 101 | 1 | 휴지 |
| 102 | 2 | 생수 |
| 103 | 4 | 라면 |

---

## INNER JOIN 수행

```sql
SELECT *
FROM User u
INNER JOIN Order o
ON u.user_id = o.user_id;
```

---

## JOIN 과정

```text
User                    Order

1 daoul             101  1  휴지
2 minsang           102  2  생수
3 suyeon            103  4  라면


          INNER JOIN

                ↓


1 daoul     101  휴지
2 minsang   102  생수
```

---

## 결과

조건이 일치하는 데이터만 조회된다.

```text
User 1 ↔ Order 101

User 2 ↔ Order 102
```

---

### 제외되는 데이터

```text
User 3

Order 103
```

JOIN 조건이 맞지 않으므로 결과에 포함되지 않는다.

---

## 시간 복잡도 관점

인덱스가 없으면

```text
O(N × M)
```

수준의 비용이 발생할 수 있다.

---

인덱스가 존재하면

```text
O(log N)
```

수준으로 크게 개선될 수 있다.

---

# 프로젝트 연계

## 우리동네 기반 타임딜 공동구매 플랫폼

예시

### User

```text
회원 정보
```

---

### TimeDeal

```text
공동구매 정보
```

---

### JOIN

```sql
SELECT *
FROM User
INNER JOIN TimeDeal
ON User.id = TimeDeal.owner_id
```

---

### 활용

```text
공구방 생성자 정보 조회

공구 참여자 조회

주문 내역 조회
```

---

# 질문 2

## RDBMS 데이터베이스는 자료를 빠르게 검색하기 위해 어떤 일을 하나요?

---

# 답변

데이터베이스는 대용량 데이터에서도 빠른 검색 성능을 제공하기 위해 다양한 최적화 기법을 사용한다.

대표적으로

- 인덱스(Index)
- 파티셔닝(Partitioning)
- 캐싱(Caching)
- 쿼리 최적화(Query Optimization)
- 정규화 및 비정규화

전략을 활용한다.

---

# 1. 인덱스 (Index)

## 개념

인덱스는 데이터 검색 속도를 높이기 위한 자료구조이다.

책의 맨 뒤에 있는 색인(Index)과 같은 역할을 수행한다.

---

## 인덱스가 없는 경우

```text
1
2
3
4
5
...
1000000
```

찾고 싶은 데이터를 처음부터 끝까지 검사

↓

Full Scan

↓

느림

---

## 인덱스가 있는 경우

```text
B-Tree

          50
        /    \
      25      75
     /  \    /  \
   ...
```

---

원하는 데이터 위치를 빠르게 탐색

---

## 시간 복잡도

### Full Scan

```text
O(N)
```

---

### B-Tree Index

```text
O(logN)
```

---

## MySQL 실제 사용 예시

```sql
CREATE INDEX idx_email
ON users(email);
```

---

## 프로젝트 적용

User Service

```text
email

nickname
```

컬럼에 인덱스를 적용하면 로그인 조회 속도가 크게 향상된다.

---

# 2. 파티셔닝 (Partitioning)

## 개념

데이터를 여러 조각으로 나누어 저장하는 방식이다.

---

## 예시

```text
Order Table

2024 데이터

2025 데이터

2026 데이터
```

---

### 검색

```sql
SELECT *
FROM orders
WHERE year = 2026;
```

---

2026 파티션만 조회

↓

성능 향상

---

## 장점

- 검색 범위 감소
- 대용량 데이터 처리

---

# 3. 캐싱 (Caching)

## 개념

자주 조회되는 데이터를 메모리에 저장하여 빠르게 응답하는 기법이다.

---

## 구조

```text
Application

↓

Redis Cache

↓

MySQL
```

---

## 과정

```text
요청

↓

Redis 확인

↓

있음

↓

즉시 응답
```

---

## 장점

- 디스크 I/O 감소
- 응답 속도 향상

---

# 프로젝트 적용

## User Service

```text
JWT

Session

Access Token
```

Redis 저장

---

## TimeDeal Service

```text
인기 공동구매

베스트 상품
```

Redis 캐싱

---

# 4. 쿼리 최적화 (Query Optimization)

## 개념

DBMS는 SQL을 실행하기 전에 가장 효율적인 실행 계획(Execution Plan)을 선택한다.

---

## 수행 과정

```text
SQL 입력

↓

Query Parser

↓

Optimizer

↓

Execution Plan 생성

↓

실행
```

---

## 고려 요소

- 인덱스 사용 여부
- JOIN 순서
- 데이터 통계 정보
- Connection Cost
- Isolation Level

---

## 예시

```sql
EXPLAIN
SELECT *
FROM users
WHERE email='test@test.com';
```

---

실행 계획을 확인할 수 있다.

---

# 5. 정규화 (Normalization)

## 개념

중복 데이터를 제거하여 데이터 무결성을 높이는 설계 기법이다.

---

## 장점

- 데이터 중복 감소
- 무결성 향상

---

## 단점

JOIN 증가

↓

조회 성능 감소 가능

---

# 6. 비정규화 (Denormalization)

## 개념

조회 성능 향상을 위해 일부 데이터를 의도적으로 중복 저장하는 기법이다.

---

## 장점

조회 성능 향상

---

## 단점

데이터 중복 발생

---

# 프로젝트 연계

## User Service

정규화

```text
User

UserAddress

UserRole
```

분리 저장

---

## TimeDeal Service

비정규화

```text
공구방

참여자 수

현재 인원
```

즉시 조회 가능하도록 일부 중복 저장

---

# 발표 예상 질문

## Q1. 인덱스란 무엇인가요?

### 답변

데이터 검색 속도를 향상시키기 위한 자료구조이며 MySQL에서는 주로 B-Tree 구조를 사용합니다.

---

## Q2. 인덱스가 없다면 어떻게 되나요?

### 답변

데이터를 처음부터 끝까지 모두 검색하는 Full Scan이 발생하여 성능이 크게 저하될 수 있습니다.

---

## Q3. Redis를 사용하는 이유는 무엇인가요?

### 답변

자주 조회되는 데이터를 메모리에 저장하여 응답 속도를 향상시키기 위해 사용합니다.

---

## Q4. User Service에서는 어떤 컬럼에 인덱스를 적용할 수 있나요?

### 답변

로그인 시 자주 조회되는 email 컬럼에 인덱스를 적용할 수 있습니다.

---

## Q5. JOIN은 왜 사용할까요?

### 답변

분리된 여러 테이블의 데이터를 논리적으로 연결하여 필요한 정보를 한 번에 조회하기 위해 사용합니다.

---

# 실무 관점

실제 서비스에서 성능 문제의 대부분은 알고리즘보다 데이터베이스 조회에서 발생한다.

특히

- Index 설계
- Redis Cache
- Query Optimization
- JOIN 최적화

는 백엔드 성능 개선의 핵심 요소이다.

---

# User Service 관점 핵심

현재 프로젝트의 User Service에서는

```text
MySQL
+
Redis
+
Email Index
```

구조를 사용하면

로그인 및 인증 요청을 빠르게 처리할 수 있다.

---

# 한 줄 요약

RDBMS는 Index, Partitioning, Cache, Query Optimization 등의 기술을 활용하여 검색 성능을 향상시키며, User Service에서는 특히 Email Index와 Redis Cache가 핵심적인 성능 최적화 수단이 된다.