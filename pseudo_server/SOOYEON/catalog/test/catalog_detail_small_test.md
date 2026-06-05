# 타임딜 상세 페이지 Small Test

Small Test는 Redis, RDBMS 같은 외부 저장소에 직접 연결하지 않고 순수한 판단 로직을 검증하는 테스트입니다.

## 테스트 1. 존재하지 않는 타임딜을 조회하면 오류를 반환한다

```text
// Given - 요청한 dealId에 해당하는 타임딜이 없다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - DEAL_NOT_FOUND 결과를 반환해야 한다.
```

## 테스트 2. 삭제된 상품의 타임딜은 조회할 수 없다

```text
// Given - 타임딜은 존재하지만 연결된 상품이 삭제 상태이다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - PRODUCT_NOT_AVAILABLE 결과를 반환해야 한다.
```

## 테스트 3. 할인율은 원가와 딜 가격으로 계산된다

```text
// Given - 원가가 26,000원이고 딜 가격이 18,900원인 상품이 있다.
// When - 상세 페이지 조회를 요청한다.
// Then - 할인율은 ((26000 - 18900) / 26000 * 100).toInt() = 27 이어야 한다.
```

## 테스트 4. 사용자 동네와 공동구매 지역이 다르면 isAvailableForUser가 false이다

```text
// Given - 공동구매 가능 지역은 MAPO_YEONNAM이고 사용자의 지역은 GANGNAM_YEOKSAM이다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 region.isAvailableForUser는 false여야 한다.
```

## 테스트 5. 사용자 동네와 공동구매 지역이 같으면 isAvailableForUser가 true이다

```text
// Given - 공동구매 가능 지역과 사용자의 지역이 모두 MAPO_YEONNAM이다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 region.isAvailableForUser는 true여야 한다.
```
