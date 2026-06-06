# Order Service - 주문 서비스

## 서비스 개요
타임딜 공동구매 플랫폼에서 선착순 주문을 처리하는 마이크로서비스.
동시 다발적인 주문 요청에서 재고 정합성을 보장하는 것이 핵심.

## 기술 스택 및 사용 이유

| 기술 | 사용 이유 |
|------|----------|
| Spring Boot + Kotlin | 팀 표준 스택, data class로 DTO 간결하게 표현 |
| MySQL (RDBMS) | 주문 데이터는 ACID 트랜잭션 필수 (결제·정산 연동) |
| Redis (In-Memory) | 재고 DECR 원자 연산으로 락 없이 선착순 보장, 동시성 이슈 방지 |
| Coroutine | 주문 요청 비동기 처리, 대량 트래픽에서 메인 스레드 블로킹 방지 |

## DB 설계

### MySQL - orders 테이블
```
orders
- id          : Long (PK, auto increment)
- user_id     : Long (주문자 ID)
- deal_id     : Long (공구방 ID)
- quantity    : Int  (주문 수량, 1 이상)
- status      : Enum [PENDING, CONFIRMED, CANCELLED]
- created_at  : DateTime
- updated_at  : DateTime
```

### MySQL - deals 테이블 (수량 제한 정보)
```
deals
- id          : Long (PK)
- min_quantity : Int (최소 주문 수량, 기본 1, nullable → null이면 제한 없음)
- max_quantity : Int (최대 주문 수량, nullable → null이면 제한 없음)

※ 핫딜/타임딜/공동구매 유형에 따라 min/max 값이 다르게 설정됨
   예) 공동구매: minQuantity=5, maxQuantity=null (최소 수량 강제, 상한 없음)
       타임딜:   minQuantity=1, maxQuantity=3    (1인당 최대 3개)
       핫딜:     minQuantity=1, maxQuantity=1    (1인당 1개만)
```

### Redis
```
key   : "deal:{dealId}:stock"
value : 잔여 재고 수량 (Int)
연산  : DECRBY {quantity}  → 원자적 재고 감소
        INCRBY {quantity}  → 취소 시 재고 복구

key   : "deal:{dealId}:limit"
value : Hash { minQuantity: Int, maxQuantity: Int }
연산  : HGETALL → 주문 생성 시 수량 제한 조회 (deals 테이블 캐싱)
용도  : DB 조회 없이 수량 제한 빠르게 검증 (고트래픽 대비)
```

## API 명세

### POST /orders - 주문 생성 (선착순)
```
Request:
{
  "userId"   : Long,
  "dealId"   : Long,
  "quantity" : Int    // 1 이상
}

처리 흐름:
1. quantity 유효성 검증 (1 이상)
2. Redis에서 deal 수량 제한 조회 (deal:{dealId}:limit)
   - minQuantity 존재 시: quantity < minQuantity 이면 예외
   - maxQuantity 존재 시: quantity > maxQuantity 이면 예외
3. 중복 주문 확인 (같은 userId + dealId 이미 존재 시 예외)
4. Redis DECRBY quantity → 결과가 0 미만이면 INCRBY로 롤백 후 예외
5. MySQL orders 테이블에 PENDING 상태로 저장
6. 저장 성공 시 CONFIRMED 상태로 업데이트
7. 응답 반환

Response 200:
{
  "orderId"  : Long,
  "status"   : "CONFIRMED",
  "quantity" : Int
}

Response 400 - 수량 0 이하:
{ "error": "수량은 1 이상이어야 합니다" }

Response 400 - 최소 주문 수량 미달:
{ "error": "최소 주문 수량은 {minQuantity}개 이상이어야 합니다" }

Response 400 - 최대 주문 수량 초과:
{ "error": "최대 주문 수량은 {maxQuantity}개 이하이어야 합니다" }

Response 409 - 재고 부족:
{ "error": "재고가 부족합니다" }

Response 409 - 중복 주문:
{ "error": "이미 해당 공구방에 주문이 존재합니다" }
```

### DELETE /orders/{orderId} - 주문 취소
```
처리 흐름:
1. orderId로 주문 조회 (없으면 404)
2. 이미 CANCELLED 상태면 예외
3. 상태를 CANCELLED로 업데이트
4. Redis INCRBY quantity → 재고 복구

Response 200:
{ "orderId": Long, "status": "CANCELLED" }

Response 404 - 주문 없음:
{ "error": "주문을 찾을 수 없습니다" }

Response 400 - 이미 취소됨:
{ "error": "이미 취소된 주문입니다" }
```

### GET /orders/{orderId} - 주문 단건 조회
```
Response 200:
{
  "orderId"   : Long,
  "dealId"    : Long,
  "quantity"  : Int,
  "status"    : String,
  "createdAt" : DateTime
}
```

### GET /orders/user/{userId} - 내 주문 목록
```
Response 200:
[
  { "orderId": Long, "dealId": Long, "quantity": Int, "status": String },
  ...
]
```
