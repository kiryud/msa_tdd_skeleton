# 민상 타임딜 공동구매 담당 문서

## 담당 주제

동네 기반 타임딜 공동구매 플랫폼의 `타임딜 공동구매 참여 처리 기능`을 담당합니다.

이 기능은 사용자가 공동구매 방에서 참여하기 버튼을 눌렀을 때, 마감 시간, 중복 참여, 정원 초과, 지역 조건을 확인한 뒤 선착순으로 참여자를 확정하는 역할입니다.

## 문서 읽는 순서

1. `timedeal.md`
   - 서비스 개요
   - 내가 담당한 기능
   - Redis, RDBMS, Coroutine, TDD 적용 이유
   - 공동구매 참여 / 취소 / 마감 확정 의사코드

2. `timedeal_spring_boot_design.md`
   - Spring Boot 계층형 아키텍처 적용
   - `@RestController`, `@Service`, `@Repository`, `@Entity`, `@Transactional`, `@Scheduled` 활용 계획
   - JWT 인증, DTO 분리, Lock, 트랜잭션 보상 처리 보강

3. `test/timedeal_small_test.md`
   - 외부 저장소 없이 순수 판단 로직 검증

4. `test/timedeal_medium_test.md`
   - Redis, RDBMS 저장 흐름 검증

5. `test/timedeal_large_test.md`
   - 동시 참여, 마감 확정, 알림 요청까지 전체 흐름 검증

6. `test/timedeal_spring_boot_test.md`
   - Spring Boot 계층, 어노테이션, 인증, 트랜잭션, 스케줄러 보강 테스트

## 점수 보강 포인트

- 기술 적용도: Spring Boot, Redis, RDBMS, Coroutine, TDD를 담당 기능 안에 연결
- TDD: small / medium / large / Spring Boot 보강 테스트로 분리
- 트러블슈팅: 동시 참여, 중복 참여, 정원 초과, Redis/RDBMS 불일치 문제 정의
- 문서 구성: 기존 의사코드와 Spring Boot 설계 보강 문서를 분리
