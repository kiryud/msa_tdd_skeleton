# 연제 타임딜 공동구매 담당 문서

## 담당 주제

동네 기반 타임딜 공동구매 플랫폼의 `선착순 주문 처리 기능`을 담당합니다.

이 기능은 사용자가 공동구매 방에서 주문하기 버튼을 눌렀을 때, 수량 유효성, 중복 주문, 재고 소진 여부를 확인한 뒤 선착순으로 주문을 확정하는 역할입니다.

## 문서 읽는 순서

1. `order_service.md`
   - 서비스 개요
   - 내가 담당한 기능
   - Redis, RDBMS, Coroutine, TDD 적용 이유
   - 주문 생성 / 취소 / 조회 의사코드 및 API 명세

2. `order_small_test.md`
   - 외부 의존성(DB, Redis) 없이 순수 도메인 로직만 검증
   - 재고 감소, 수량 유효성, 중복 주문 방지, 취소 로직

3. `order_medium_test.md`
   - Spring 컨텍스트 + H2 인메모리 DB 사용
   - DB 저장 흐름, 상태 변경, 유저별 주문 목록 검증

4. `order_large_test.md`
   - 실제 Redis + MySQL 연동 동시성 테스트
   - 대량 동시 요청 시 선착순 정합성 및 재고 음수 방지 검증

## 점수 보강 포인트

- 기술 적용도: Spring Boot + Kotlin, Redis, MySQL, Coroutine, TDD를 담당 기능 안에 연결
- TDD: small / medium / large 테스트로 계층 분리, 총 16개 테스트 케이스
- 트러블슈팅: 동시 주문 경합, 중복 주문 race condition, Redis 재고 롤백(INCRBY 보상 처리), Redis-MySQL 정합성 문제 정의
- 문서 구성: 서비스 설계 문서와 테스트 문서를 역할별로 분리
