# 민상 타임딜 상세 페이지 담당 문서

## 담당 주제

동네 기반 타임딜 공동구매 플랫폼의 `타임딜 상세 페이지 조회 기능`을 담당합니다.

이 기능은 사용자가 공동구매 목록에서 특정 타임딜을 선택했을 때, 상품 정보, 가격, 판매자 정보, 판매처 위치, 공동구매 현황(최대 인원, 현재 참여 인원)을 조합해서 반환하는 역할입니다.

## 문서 읽는 순서

1. `timedeal_detail.md`
   - 서비스 개요
   - 내가 담당한 기능
   - Redis, RDBMS, TDD 적용 이유
   - 상세 조회 의사코드

2. `timedeal_detail_spring_boot_design.md`
   - Spring Boot 계층형 아키텍처 적용
   - `@RestController`, `@Service`, `@Repository`, `@Entity`, `@Transactional(readOnly = true)` 활용 계획
   - DTO 분리, fetch join, 읽기 전용 트랜잭션 보강

3. `test/timedeal_detail_small_test.md`
   - 외부 저장소 없이 순수 판단 로직 검증

4. `test/timedeal_detail_medium_test.md`
   - Redis, RDBMS 조회 흐름 검증

5. `test/timedeal_detail_spring_boot_test.md`
   - Spring Boot 계층, 어노테이션, 읽기 전용 트랜잭션, DTO 분리 보강 테스트

## 점수 보강 포인트

- 기술 적용도: Spring Boot, Redis, RDBMS, TDD를 담당 기능 안에 연결
- TDD: small / medium / Spring Boot 보강 테스트로 분리
- 트러블슈팅: RDBMS와 Redis 데이터 불일치, N+1 문제, 읽기 전용 트랜잭션 처리 정의
- 문서 구성: 기존 의사코드와 Spring Boot 설계 보강 문서를 분리
