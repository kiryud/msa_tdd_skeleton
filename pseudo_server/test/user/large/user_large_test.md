# User Service - Large Test

## 개요

- 실제 운영 환경과 유사한 End-to-End(E2E) 시나리오 검증
- 로그인, 세션 발급, 프로필 조회 전체 흐름 검증
- User Service 라이프사이클 기반 시스템 테스트 수행
- 향후 회원가입 기능 확장 고려

---

# Test Case 01 - 사용자 로그인 시나리오

## 목적

- 사용자 인증 기능 검증
- Access Token 발급 검증

## Given

- 등록된 사용자 정보 존재
- 올바른 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- 사용자 인증 성공
- Access Token 발급
- 로그인 상태 진입

---

# Test Case 02 - Redis 세션 저장 시나리오

## 목적

- 로그인 상태 유지 검증
- Redis 세션 관리 검증

## Given

- 로그인 성공 사용자

## When

- Access Token 발급 수행

## Then

- Redis 세션 정보 저장
- 인증 상태 유지 가능

---

# Test Case 03 - GraphQL 프로필 조회 시나리오

## 목적

- GraphQL 기반 프로필 조회 검증
- 사용자 정보 반환 검증

## Given

- 로그인 완료 사용자
- 유효한 Access Token 보유

## When

- GraphQL Query 요청

## Then

- nickname 반환
- baseLocation 반환
- UserRegisterDto 반환
- 비밀번호 정보 미반환

---

# Test Case 04 - 인증 실패 시나리오

## 목적

- 잘못된 로그인 요청 차단 검증
- 인증 실패 처리 검증

## Given

- 등록된 사용자 정보 존재
- 잘못된 비밀번호 입력

## When

- 로그인 요청 수행

## Then

- 인증 실패
- 토큰 미발급
- 세션 미생성

---

# Test Case 05 - 세션 만료 시나리오

## 목적

- Redis 세션 만료 정책 검증
- 재인증 흐름 검증

## Given

- 로그인 완료 사용자
- Redis 저장 세션 존재

## When

- 세션 TTL(Time To Live) 만료

## Then

- Redis 세션 삭제
- 재로그인 필요

---

# Test Case 06 - 전체 사용자 흐름 검증

## 목적

- User Service 전체 흐름 검증
- End-to-End 시나리오 검증

## Given

- 플랫폼 사용자

## When

1. 로그인 수행
2. Access Token 발급
3. Redis 세션 저장
4. GraphQL 프로필 조회
5. 세션 유지

## Then

- 전체 과정 정상 수행
- 사용자 인증 상태 유지
- 프로필 조회 가능
- 서비스 이용 가능 상태 유지

---

# Large Test 범위

## 포함

- 로그인
- Access Token 발급
- Redis 세션 관리
- GraphQL 프로필 조회
- 인증 상태 유지
- 사용자 전체 이용 흐름

## 제외

- AWS 인프라 장애 상황
- Kubernetes 오토스케일링
- 다중 리전 배포
- 외부 결제 시스템
- 외부 마이크로서비스 통신

---

# 적용 기술

- Kotlin Data Class
- Coroutine
- GraphQL
- MySQL
- Redis

---

# 기대 효과

- 실제 사용자 시나리오 검증
- End-to-End 흐름 검증
- 인증 시스템 안정성 확보
- 서비스 전체 흐름 신뢰성 확보
- 운영 환경 이전 문제 발견