# User Service - Medium Test

## 개요

- User Service 내부 컴포넌트 간 상호작용 검증
- MySQL, Redis, GraphQL 연동 시나리오 검증
- 단일 서비스 내부 통합 테스트 관점
- 실제 운영 환경 이전 단계 검증 목적

---

# Test Case 01 - 로그인 사용자 조회 검증

## 목적

- User Service와 MySQL 연동 검증
- 사용자 계정 조회 로직 검증

## Given

- MySQL에 사용자 정보 저장
- userId 존재

## When

- 로그인 요청 수행

## Then

- 사용자 정보 정상 조회
- 인증 절차 진행

---

# Test Case 02 - 로그인 후 세션 저장 검증

## 목적

- Redis 세션 저장 로직 검증
- 로그인 상태 유지 검증

## Given

- 사용자 인증 성공

## When

- Access Token 발급

## Then

- Redis 세션 저장
- 로그인 상태 유지 가능

---

# Test Case 03 - 잘못된 로그인 요청 검증

## 목적

- 인증 실패 처리 검증

## Given

- 존재하지 않는 사용자
또는
- 잘못된 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- 인증 실패
- Redis 세션 미생성
- 예외 응답 반환

---

# Test Case 04 - GraphQL 프로필 조회 검증

## 목적

- GraphQL Resolver와 User Service 연동 검증

## Given

- 사용자 정보 존재

## When

- GraphQL Query 요청

## Then

- User Service 호출
- 사용자 프로필 조회
- nickname 반환
- baseLocation 반환

---

# Test Case 05 - 세션 만료 검증

## 목적

- Redis 세션 만료 정책 검증

## Given

- Redis에 로그인 세션 존재

## When

- TTL(Time To Live) 만료

## Then

- 세션 자동 제거
- 재로그인 필요

---

# Medium Test 범위

## 포함

- User Service ↔ MySQL 연동
- User Service ↔ Redis 연동
- GraphQL Resolver ↔ User Service 연동
- 인증 및 세션 처리 검증

## 제외

- 모바일 클라이언트
- API Gateway
- 외부 마이크로서비스 통신
- AWS 인프라

---

# 적용 기술

- Kotlin
- Coroutine
- GraphQL
- MySQL
- Redis

---

# 기대 효과

- 서비스 내부 통합 동작 검증
- 로그인 인증 로직 검증
- Redis 세션 관리 검증
- GraphQL 조회 기능 검증
- 실제 운영 환경 이전 문제 발견