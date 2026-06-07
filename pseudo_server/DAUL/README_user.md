# User Service - 사용자 담당 문서

## 담당 주제

우리동네 기반 타임딜 공동구매 플랫폼의 `사용자 기능`을 담당한다. (Jira: [KAN-5])

로그인 및 세션 토큰 발급, GraphQL 기반 프로필 조회, 회원가입 요청 DTO를 제공한다. 현재는 의사코드 수준이며 MySQL 계정 검증과 Redis 세션 저장은 추후 추가 예정이다.

## 문서 읽는 순서

1. `user_service.md`
   - 서비스 개요, 담당 기능
   - Kotlin Data Class, Coroutine, GraphQL, MySQL/Redis(예정), TDD 적용 이유
   - 로그인/프로필 조회 의사코드
   - 다른 서비스와의 연동 계약 및 통합 시 확인 필요 사항

2. `user_spring_boot_design.md`
   - Spring Boot 계층형 아키텍처 적용
   - GraphQL Resolver(`@Controller`), `@Service`, `@Repository`/`@Entity`(예정) 활용 계획
   - Coroutine 로그인, Redis 세션(예정), 비밀번호 보안 보강

3. `user_small_test.md`
   - 외부 시스템 없이 DTO 생성, 로그인 검증, 입력값 검증, 예외, Resolver 반환 검증

4. `user_medium_test.md`
   - MySQL 조회, Redis 세션 저장/만료, GraphQL Resolver↔Service 연동 검증

5. `user_large_test.md`
   - 로그인 → 세션 발급 → GraphQL 프로필 조회 전체 흐름과 세션 만료/인증 실패 검증

6. `user_spring_boot_test.md`
   - GraphQL 위임, Coroutine, MySQL/Redis(예정), 비밀번호 비노출 보강 테스트

## 점수 보강 포인트

- 기술 적용도: GraphQL, Coroutine, MySQL/Redis(예정), TDD를 사용자 인증 흐름에 연결
- TDD: small / medium / large / Spring Boot 보강 테스트로 분리
- 트러블슈팅: 입력값 검증, 세션 TTL 만료, 비밀번호 비노출 정의
- 문서 구성: 의사코드 / Spring Boot 설계 보강 / 계층별 테스트 문서 분리
- 협업 과정: User Service가 세션 토큰·동네(baseLocation) 정보로 다른 서비스와 어떻게 연결되는지 명확화
