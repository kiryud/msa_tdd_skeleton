# User Service - Small Test

## 개요

- 외부 시스템(MySQL, Redis, Network) 비의존 환경 기반 단위 테스트
- User Service 내부 순수 비즈니스 로직 검증
- Mock 객체 또는 메모리 기반 테스트 수행
- 로그인 및 프로필 조회 핵심 기능 검증

---

# Test Case 01 - DTO 생성 검증

## 목적

- UserRegisterDto 객체 생성 검증
- 사용자 정보 저장 구조 검증

## Given

- userId 값 존재
- passwordEncrypted 값 존재
- nickname 값 존재
- baseLocation 값 존재

## When

- UserRegisterDto 객체 생성

## Then

- UserRegisterDto 객체 정상 생성
- 각 필드 값 정상 저장

---

# Test Case 02 - 로그인 성공

## 목적

- 정상 로그인 요청 처리 검증
- Access Token 생성 검증

## Given

- 등록된 사용자 정보
- 올바른 비밀번호 입력

## When

- loginAndIssueToken() 호출

## Then

- 토큰 문자열 반환
- 사용자 인증 성공

---

# Test Case 03 - 빈 아이디 로그인 실패

## 목적

- 필수 입력값 검증 로직 확인

## Given

- 비어있는 userId
- 정상 비밀번호 입력

## When

- loginAndIssueToken() 호출

## Then

- IllegalArgumentException 발생
- 로그인 실패

---

# Test Case 04 - 빈 비밀번호 로그인 실패

## 목적

- 비밀번호 입력 검증 로직 확인

## Given

- 정상 userId
- 비어있는 비밀번호

## When

- loginAndIssueToken() 호출

## Then

- IllegalArgumentException 발생
- 로그인 실패

---

# Test Case 05 - 프로필 조회 성공

## 목적

- GraphQL Resolver 반환 데이터 검증

## Given

- 존재하는 userId

## When

- getUserProfile() 호출

## Then

- nickname 반환
- baseLocation 반환
- UserRegisterDto 객체 반환

---

# Small Test 범위

## 포함

- DTO 생성 검증
- 로그인 검증 로직
- 입력값 검증
- 예외 처리 검증
- GraphQL Resolver 반환 검증

## 제외

- MySQL 연결
- Redis 연결
- 외부 API 호출
- 네트워크 통신

---

# 기대 효과

- 로그인 기능 안정성 확보
- 입력값 검증 로직 확인
- 예외 상황 조기 발견
- 빠른 테스트 수행
- 리팩토링 시 기능 보장