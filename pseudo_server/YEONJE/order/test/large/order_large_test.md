# Order Service - Large Test (동시성 테스트)

> 실제 Redis + MySQL 연동. 대량 동시 요청 시 선착순 정합성 검증.
> 이 테스트가 Order Service의 핵심 기술 시연 포인트.

---

## 동시성 - 선착순 보장

### TC-L-01: 100명 동시 주문, 재고 10개 → 정확히 10명만 성공
```
Given : dealId=1, Redis 재고 10
        서로 다른 userId 100명 준비
        각자 quantity=1로 동시 주문

When  : 100개 요청 동시 발생 (Coroutine 100개 launch)

Then  : 성공 응답(200) 정확히 10건
        실패 응답(409 재고부족) 정확히 90건
        Redis 최종 재고 = 0
        MySQL 저장된 CONFIRMED 주문 = 10건
        재고 음수 발생 없음 (Redis DECRBY 원자성 보장)
```

### TC-L-02: 100명 동시 주문, 재고 100개 → 전원 성공
```
Given : dealId=1, Redis 재고 100
        서로 다른 userId 100명, quantity=1

When  : 100개 요청 동시 발생

Then  : 성공 응답 100건
        Redis 최종 재고 = 0
        MySQL CONFIRMED 주문 = 100건
```

### TC-L-03: 동시 주문 + 취소 혼합 → 재고 정합성 유지
```
Given : dealId=1, Redis 재고 5
        주문 시도 10명 (quantity=1), 취소 시도 3명 (기존 주문 보유)

When  : 13개 요청 동시 발생

Then  : 최종 재고 = 초기재고(5) + 취소수량(3) - 성공주문수 로 계산 일치
        재고 음수 발생 없음
        취소된 주문 status = CANCELLED
```

### TC-L-04: 한 명이 여러 수량 동시 중복 요청 (race condition 방지)
```
Given : dealId=1, Redis 재고 10
        userId=1이 quantity=3 주문을 동시에 3번 요청 (네트워크 중복 클릭 시뮬레이션)

When  : 동일한 요청 3개 동시 발생

Then  : 성공 응답 1건만
        나머지 2건은 409 (중복 주문) 또는 재고 부족
        MySQL CONFIRMED 주문 userId=1 건수 = 1건
        Redis 재고 = 7 (quantity=3 차감)
```
