# Order Service - Small Test (단위 테스트)

> 외부 의존성(DB, Redis) 없이 순수 도메인 로직만 검증. Mock 객체 사용.

---

## 재고 감소 로직

### TC-S-01: 정상 주문 - 단일 수량
```
Given : 재고 10개, userId=1, dealId=1, quantity=1
When  : 주문 생성 요청
Then  : Redis 재고 9로 감소
        주문 상태 CONFIRMED
```

### TC-S-02: 정상 주문 - 여러 개 주문 (재고 충분)
```
Given : 재고 10개, userId=1, dealId=1, quantity=5
When  : 주문 생성 요청
Then  : Redis 재고 5로 감소
        주문 상태 CONFIRMED
        저장된 quantity = 5
```

### TC-S-03: 재고보다 많은 수량 주문 (재고 부족)
```
Given : 재고 3개, userId=1, dealId=1, quantity=5
When  : 주문 생성 요청
Then  : 예외 발생 - "재고가 부족합니다"
        Redis 재고 롤백되어 3 유지 (INCRBY 복구)
        주문 저장 안 됨
```

### TC-S-04: 재고 정확히 소진 (경계값)
```
Given : 재고 5개, userId=1, dealId=1, quantity=5
When  : 주문 생성 요청
Then  : Redis 재고 0으로 감소
        주문 상태 CONFIRMED
```

### TC-S-05: 재고 0인 상태에서 주문
```
Given : 재고 0개, userId=1, dealId=1, quantity=1
When  : 주문 생성 요청
Then  : 예외 발생 - "재고가 부족합니다"
        주문 저장 안 됨
```

---

## 수량 유효성 검증

### TC-S-06: 수량 0 주문
```
Given : 재고 10개, userId=1, dealId=1, quantity=0
When  : 주문 생성 요청
Then  : 예외 발생 - "수량은 1 이상이어야 합니다"
        Redis DECRBY 호출 안 됨
```

### TC-S-07: 수량 음수 주문
```
Given : 재고 10개, userId=1, dealId=1, quantity=-3
When  : 주문 생성 요청
Then  : 예외 발생 - "수량은 1 이상이어야 합니다"
        Redis DECRBY 호출 안 됨
```

---

## 중복 주문 방지

### TC-S-08: 같은 유저가 같은 공구방에 이미 주문한 상태에서 추가 주문
```
Given : userId=1, dealId=1로 CONFIRMED 주문 이미 존재
        재고 5개, quantity=2
When  : 같은 userId=1, dealId=1로 주문 생성 요청
Then  : 예외 발생 - "이미 해당 공구방에 주문이 존재합니다"
        Redis DECRBY 호출 안 됨
        기존 주문 변경 없음
```

### TC-S-09: 취소된 주문이 있는 유저의 재주문 (허용)
```
Given : userId=1, dealId=1로 CANCELLED 주문 존재
        재고 5개, quantity=2
When  : 같은 userId=1, dealId=1로 주문 생성 요청
Then  : 정상 처리 - 주문 CONFIRMED
        Redis 재고 3으로 감소
```

---

## 주문 취소 로직

### TC-S-10: 정상 취소
```
Given : orderId=1, status=CONFIRMED, quantity=3
        Redis 재고 현재 2
When  : 주문 취소 요청
Then  : 주문 상태 CANCELLED로 변경
        Redis 재고 5로 복구 (INCRBY 3)
```

### TC-S-11: 이미 취소된 주문 재취소
```
Given : orderId=1, status=CANCELLED
When  : 주문 취소 요청
Then  : 예외 발생 - "이미 취소된 주문입니다"
        Redis 재고 변경 없음
```

### TC-S-12: 존재하지 않는 주문 취소
```
Given : orderId=999 (존재하지 않음)
When  : 주문 취소 요청
Then  : 예외 발생 - "주문을 찾을 수 없습니다"
```

---

## 최소 주문 수량 제한

### TC-S-13: 최소 수량 미달 주문 (공동구매)
```
Given : dealId=1, minQuantity=5, maxQuantity=null
        재고 10개, userId=1, quantity=3
When  : 주문 생성 요청
Then  : 예외 발생 - "최소 주문 수량은 5개 이상이어야 합니다"
        Redis DECRBY 호출 안 됨
        주문 저장 안 됨
```

### TC-S-14: 최소 수량 정확히 충족 (경계값)
```
Given : dealId=1, minQuantity=5, maxQuantity=null
        재고 10개, userId=1, quantity=5
When  : 주문 생성 요청
Then  : 정상 처리 - 주문 CONFIRMED
        Redis 재고 5로 감소
```

### TC-S-15: 최소 수량 제한 없는 딜 (minQuantity=null)
```
Given : dealId=1, minQuantity=null, maxQuantity=null
        재고 10개, userId=1, quantity=1
When  : 주문 생성 요청
Then  : 정상 처리 - 주문 CONFIRMED (최소 수량 검증 스킵)
```

---

## 최대 주문 수량 제한

### TC-S-16: 최대 수량 초과 주문 (타임딜)
```
Given : dealId=1, minQuantity=1, maxQuantity=3
        재고 10개, userId=1, quantity=5
When  : 주문 생성 요청
Then  : 예외 발생 - "최대 주문 수량은 3개 이하이어야 합니다"
        Redis DECRBY 호출 안 됨
        주문 저장 안 됨
```

### TC-S-17: 최대 수량 정확히 충족 (경계값)
```
Given : dealId=1, minQuantity=1, maxQuantity=3
        재고 10개, userId=1, quantity=3
When  : 주문 생성 요청
Then  : 정상 처리 - 주문 CONFIRMED
        Redis 재고 7로 감소
```

### TC-S-18: 최대 수량 1개 제한 딜에서 2개 주문 (핫딜)
```
Given : dealId=1, minQuantity=1, maxQuantity=1
        재고 10개, userId=1, quantity=2
When  : 주문 생성 요청
Then  : 예외 발생 - "최대 주문 수량은 1개 이하이어야 합니다"
        Redis DECRBY 호출 안 됨
```

### TC-S-19: 최소·최대 동시 위반 (최소 검증이 먼저)
```
Given : dealId=1, minQuantity=3, maxQuantity=5
        재고 10개, userId=1, quantity=1
When  : 주문 생성 요청
Then  : 예외 발생 - "최소 주문 수량은 3개 이상이어야 합니다"  (최소 검증 우선)
        Redis DECRBY 호출 안 됨
```

### TC-S-20: 수량 제한 범위 내 정상 주문 (min·max 모두 설정)
```
Given : dealId=1, minQuantity=2, maxQuantity=5
        재고 10개, userId=1, quantity=3
When  : 주문 생성 요청
Then  : 정상 처리 - 주문 CONFIRMED
        Redis 재고 7로 감소
```
