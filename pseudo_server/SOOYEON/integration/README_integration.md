# 통합 — 딜 라이프사이클 연동

> 딜 생성 · 상세 조회 · 참여 처리 · 주문 처리, 네 마이크로서비스를 하나로 연결하는 **글루(Glue) 계층**의 설계와 TDD를 담당합니다.

---

## 핵심 책임

| # | 책임 | 설명 |
|:-:|---|---|
| 1 | **선착순 모델 분기** | `metric`에 따라 참여(HEADCOUNT) 또는 주문(QUANTITY·AMOUNT)으로 단일 라우팅 |
| 2 | **metric 기준 마감 확정** | 참여 인원 / 주문 수량 합 / 주문 금액 합을 metric별로 집계해 성사 여부 판정 |

---

## 서비스 책임 경계

| 서비스 | 책임 | 소유 엔티티 | Redis 키 |
|---|---|:---:|---|
| 딜 생성 (Creation) | 딜 최초 등록 `OPEN`, 생성 이벤트 발행 | `Deal` | — |
| 상세 조회 (Detail) | 딜 + 상품 + 판매자 + 진행도 조합 응답 | `Product` `Seller` | 읽기 전용 |
| 참여 처리 (Participation) | 인원 기준 선착순 참여 / 취소 / 마감 | `Participation` | `timedeal:{dealId}:participants` |
| 주문 처리 (Order) | 수량 기준 선착순 주문 / 취소 | `Order` | `deal:{dealId}:stock` |
| **통합 계층** | metric 라우팅, 마감 확정, 진행도 집계 | `DealReadModel` | 읽기 전용 |

---

## 문서 읽는 순서

```
integration.md                     ← 의사코드, 라이프사이클, 경계 계약
integration_spring_boot_design.md  ← Spring Boot 계층 배치, Kotlin 코드
test/
  integration_small_test.md        ← 순수 함수 반환값 검증 (외부 I/O 없음)
  integration_medium_test.md       ← 저장소 I/O 정합 검증 (DB · Redis)
  integration_large_test.md        ← 전체 API 흐름 검증 (생성 → 마감)
  integration_spring_boot_test.md  ← Bean 계층 보강 테스트
```

---

## 테스트 크기 기준

| 크기 | 기준 | 주요 도구 |
|:---:|---|---|
| **Small** | 함수 인자 · 반환값 검사. 외부 I/O 없음 | 순수 함수 호출, Mock |
| **Medium** | DB / Redis I/O 발생. 인스턴스 간 요청 포함 | 실제 저장소 또는 Testcontainers |
| **Large** | POST / GET 이상의 API 호출. 서비스 간 전체 흐름 | MockMvc / RestAssured, 전체 Spring Context |

---

## 테스트 작성 원칙

- **Given → When → Then** 을 모든 케이스에 명시 — _무엇을_ / _언제_ / _어떻게 되는지_
- 케이스 순서: **⚠️ 예외 처리 → ✅ 기능 구현 → 🔄 리팩터링**
- 각 섹션 레이블: `E` (Exception) · `F` (Feature) · `R` (Refactoring)

> [!WARNING]
> **AMOUNT(배달 공구)** 케이스는 주문 서비스에 `amount` 또는 `unitPrice` 필드 추가가 선행되어야 합니다.
> 그 전까지 해당 케이스는 `[AMOUNT 미구현]` 으로 표시하고 비활성 상태로 둡니다.

---

## 점수 보강 포인트

| 항목 | 내용 |
|---|---|
| 기술 적용도 | 커밋 후 이벤트 수신, metric 전략, 마감 스케줄러, 서비스 간 집계 조회 연결 |
| TDD | 의사코드 기반 Small / Medium / Large / Spring Boot 테스트 계층 분리 |
| 트러블슈팅 | 선착순 모델 이중화, metric 무시 마감, 진행도 단위 불일치, 스케줄러 단일 실패 전파 |
| 문서 구성 | 의사코드 / 설계 / 테스트 문서 분리 |
| 협업 | `dealId` · 이벤트 · Redis 키 규약(`RedisKeyConstants`)으로 서비스 연결 명확화 |
