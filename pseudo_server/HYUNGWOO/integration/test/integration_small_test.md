# 통합 Small Test

Small Test는 외부 저장소에 접근하지 않고, 함수에 들어온 인자나 (목으로 대체한) 다른 함수의 결과를 받아 **반환값을 검사**하는 수준의 테스트입니다. `integration.md`의 의사코드 함수를 대상으로 합니다.

> AMOUNT(배달 공구) 관련 케이스는 주문에 amount(또는 unitPrice) 필드 추가가 선행되어야 실행 가능하다(order_service.md는 현재 quantity만 저장). 그 전까지는 미구현 경로다.

## 테스트 1. 성공 판정기는 인원 기준으로 SUCCESS/FAILED를 반환한다

```text
// Given - metric=HEADCOUNT, min=10, currentValue=10
// When  - evaluateSuccess(SuccessCriterion(HEADCOUNT, 10), 10)을 호출한다.
// Then  - SUCCESS를 반환해야 한다.
// Given - 같은 조건에서 currentValue=9
// Then  - FAILED를 반환해야 한다.
```

## 테스트 2. 성공 판정기는 수량 기준으로 SUCCESS/FAILED를 반환한다

```text
// Given - metric=QUANTITY, min=50, currentValue=50
// When  - evaluateSuccess(SuccessCriterion(QUANTITY, 50), 50)을 호출한다.
// Then  - SUCCESS를 반환해야 한다.
// Given - currentValue=49
// Then  - FAILED를 반환해야 한다.
```

## 테스트 3. 성공 판정기는 금액 기준으로 SUCCESS/FAILED를 반환한다

```text
// Given - metric=AMOUNT, min=30000, currentValue=30000
// When  - evaluateSuccess(SuccessCriterion(AMOUNT, 30000), 30000)을 호출한다.
// Then  - SUCCESS를 반환해야 한다.
// Given - currentValue=29999
// Then  - FAILED를 반환해야 한다.
```

## 테스트 4. 선착순 모델은 metric에 따라 결정된다

```text
// Given - metric=HEADCOUNT
// When  - resolveParticipationModel(HEADCOUNT)을 호출한다.
// Then  - JOIN을 반환해야 한다.
// Given - metric=QUANTITY 또는 AMOUNT
// When  - resolveParticipationModel(metric)을 호출한다.
// Then  - ORDER를 반환해야 한다.
```

## 테스트 5. 누적값 집계는 metric에 맞는 소스를 선택한다

```text
// Given - SCARD/주문 집계 함수가 목으로 대체되어 있다.
// Given - deal.metric=HEADCOUNT
// When  - getCurrentValue(deal)을 호출한다.
// Then  - 참여자 SCARD 결과가 반환되어야 한다(주문 집계는 호출되지 않는다).
// Given - deal.metric=QUANTITY
// When  - getCurrentValue(deal)을 호출한다.
// Then  - 주문 수량 합 결과가 반환되어야 한다.
// Given - deal.metric=AMOUNT
// When  - getCurrentValue(deal)을 호출한다.
// Then  - 주문 금액 합 결과가 반환되어야 한다.
```

## 테스트 6. 생성 이벤트는 metric에 맞게 read-model로 매핑된다

```text
// Given - DealCreatedEvent(metric=QUANTITY, min=50, capacity=100, deadline, regionCode)
// When  - DealReadModel.from(event)를 호출한다.
// Then  - metric=QUANTITY, min=50, capacity=100이 그대로 매핑되어야 한다.
```

## 테스트 7. 상세 진행도는 metric에 맞는 단위로 계산된다

```text
// Given - SCARD/재고/금액 집계가 목으로 대체되어 있다.
// Given - metric=HEADCOUNT, 참여자 수=3
// When  - buildProgress(deal)을 호출한다.
// Then  - unit=HEADCOUNT, current=3이어야 한다.
// Given - metric=QUANTITY, capacity=100, 남은 재고=40
// When  - buildProgress(deal)을 호출한다.
// Then  - unit=QUANTITY, current=60(=capacity-재고)이어야 한다.
// Given - metric=AMOUNT, 주문 금액 합=32000
// When  - buildProgress(deal)을 호출한다.
// Then  - unit=AMOUNT, current=32000이어야 한다.
```

## 테스트 8. 지역 일치는 공용 순수 함수로 판정된다

```text
// Given - userRegionCode != dealRegionCode
// When  - isRegionMatched(userRegionCode, dealRegionCode)를 호출한다.
// Then  - false를 반환해야 한다.
// Given - userRegionCode == dealRegionCode
// When  - isRegionMatched(userRegionCode, dealRegionCode)를 호출한다.
// Then  - true를 반환해야 한다.
```

> 할인율(discountRate) 계산은 상세 조회 서비스 내부 로직이며 `catalog_detail_small_test`에서 이미 검증하므로 통합 테스트에서는 중복하지 않는다.
