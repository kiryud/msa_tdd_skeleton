# User Service TDD

## 개요

- 우리동네 기반 타임딜 공동구매 플랫폼 User Service 대상 TDD 시나리오 정의
- 로그인, 세션 관리, 프로필 조회 기능 대상 테스트 우선 개발(TDD) 적용
- 성공 및 실패 시나리오 기반 기능 명세 검증

---

# Test Case 01 - DTO 생성 검증

## Given

- userId 존재
- passwordEncrypted 존재
- nickname 존재
- baseLocation 존재

## When

- UserRegisterDto 객체 생성

## Then

- DTO 정상 생성
- 필드 값 정상 저장

---

# Test Case 02 - 로그인 성공

## Given

- 등록된 사용자 정보 존재
- 올바른 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- 사용자 인증 성공
- Access Token 발급
- Redis 세션 저장 예정

---

# Test Case 03 - 빈 아이디 로그인 실패

## Given

- 비어있는 userId
- 정상 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- IllegalArgumentException 발생
- 로그인 실패

---

# Test Case 04 - 빈 비밀번호 로그인 실패

## Given

- 정상 userId
- 비어있는 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- IllegalArgumentException 발생
- 로그인 실패

---

# Test Case 05 - 프로필 조회 성공

## Given

- 존재하는 사용자 ID

## When

- GraphQL Query 기반 프로필 조회 수행

## Then

- nickname 반환
- baseLocation 반환
- UserRegisterDto 반환

---

# Test Strategy

## Small Test

- DTO 생성 검증
- 로그인 검증 로직
- 입력값 검증
- 예외 처리 검증
- GraphQL Resolver 반환 검증

## Medium Test

- User Service ↔ MySQL 연동 검증
- User Service ↔ Redis 세션 저장 검증
- GraphQL Resolver ↔ User Service 연동 검증

## Large Test

- 로그인 → 토큰 발급 → 세션 저장 → 프로필 조회 전체 시나리오 검증
- 실제 서비스 환경 기반 End-to-End 시나리오 검증

---

# TDD 적용 목적

- 로그인 기능 안정성 확보
- 입력값 검증 로직 확인
- 예외 상황 사전 검증
- 리팩토링 시 기능 보장
- 서비스 품질 향상