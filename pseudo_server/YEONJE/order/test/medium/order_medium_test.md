# Order Service - Medium Test (통합 테스트)

> Spring 컨텍스트 + H2 인메모리 DB 사용. 실제 레이어 간 연동 검증.

---

## DB 저장 검증

### TC-M-01: 주문 생성 후 MySQL 저장 확인
```
Given : 재고 10개 (Redis Mock), userId=1, dealId=1, quantity=2
When  : POST /orders 호출
Then  : HTTP 200
        orders 테이블에 레코드 1건 저장됨
        저장된 status = CONFIRMED
        저장된 quantity = 2
```

### TC-M-02: 주문 취소 후 DB 상태 변경 확인
```
Given : orderId=1 (CONFIRMED, quantity=3) DB에 저장된 상태
When  : DELETE /orders/1 호출
Then  : HTTP 200
        orders 테이블 해당 레코드 status = CANCELLED
```

### TC-M-03: 주문 단건 조회
```
Given : orderId=1 DB에 저장된 상태
When  : GET /orders/1 호출
Then  : HTTP 200
        응답 body에 orderId, dealId, quantity, status 포함
```

### TC-M-04: 존재하지 않는 주문 조회
```
Given : orderId=999 DB에 없음
When  : GET /orders/999 호출
Then  : HTTP 404
        응답 body에 에러 메시지 포함
```

---

## 유저별 주문 목록

### TC-M-05: 내 주문 목록 조회 - 여러 건
```
Given : userId=1의 주문 3건 DB에 저장 (dealId 각각 다름)
When  : GET /orders/user/1 호출
Then  : HTTP 200
        응답 배열 크기 3
        모두 userId=1의 주문
```

### TC-M-06: 주문 없는 유저 목록 조회
```
Given : userId=99 주문 없음
When  : GET /orders/user/99 호출
Then  : HTTP 200
        응답 빈 배열 []
```

---

## 중복 주문 통합 검증

### TC-M-07: 중복 주문 시도 - DB + Redis 모두 변경 없음
```
Given : userId=1, dealId=1 CONFIRMED 주문 DB에 존재
        Redis 재고 5
When  : POST /orders { userId:1, dealId:1, quantity:1 }
Then  : HTTP 409
        DB 주문 건수 변화 없음
        Redis 재고 5 유지
```
