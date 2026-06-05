# 타임딜 상세 페이지 Medium Test

Medium Test는 서비스 로직과 Redis, RDBMS 조회 흐름이 함께 동작하는지 확인하는 테스트입니다.

## 테스트 1. 현재 참여 인원은 Redis SCARD로 조회된다

```text
// Given - Redis의 timedeal:{dealId}:participants 키에 15명의 userId가 저장되어 있다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 participants.currentParticipants는 15여야 한다.
```

## 테스트 2. 이미 참여한 사용자의 isJoined는 true이다

```text
// Given - Redis 참여자 목록에 해당 userId가 저장되어 있다.
// When - 해당 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 isJoined는 true여야 한다.
```

## 테스트 3. 참여하지 않은 사용자의 isJoined는 false이다

```text
// Given - Redis 참여자 목록에 해당 userId가 없다.
// When - 해당 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 isJoined는 false여야 한다.
```

## 테스트 4. 타임딜과 연결된 상품·판매자 정보가 함께 반환된다

```text
// Given - dealId에 연결된 상품과 판매자 정보가 RDBMS에 저장되어 있다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답에 product 정보와 seller 정보가 모두 포함되어야 한다.
// Then - RDBMS 쿼리는 fetch join으로 한 번만 실행되어야 한다.
```

## 테스트 5. 판매처의 공동구매 가능 동네 목록이 응답에 포함된다

```text
// Given - 판매자의 availableRegions에 연남동, 연희동, 망원동이 저장되어 있다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 region.availableRegions에 세 동네가 모두 포함되어야 한다.
```
