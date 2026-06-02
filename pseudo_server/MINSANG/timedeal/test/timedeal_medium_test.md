# 타임딜 공동구매 Medium Test

Medium Test는 서비스 로직과 Redis, RDBMS 저장 흐름이 함께 동작하는지 확인하는 테스트입니다.

## 테스트 1. 참여 성공 시 Redis에 사용자 ID가 저장된다

```text
// Given - 공동구매가 OPEN 상태이고 참여 인원이 최대 인원보다 적다.
// When - 사용자가 공동구매 참여를 요청한다.
// Then - Redis의 timedeal:{dealId}:participants 키에 userId가 저장되어야 한다.
```

## 테스트 2. 참여 성공 시 RDBMS에 참여 이력이 저장된다

```text
// Given - 사용자가 공동구매 참여 조건을 모두 만족한다.
// When - joinTimeDeal 함수가 실행된다.
// Then - ParticipationRepository에 JOINED 상태의 참여 이력이 저장되어야 한다.
```

## 테스트 3. 참여 취소 시 Redis 참여자 목록에서 제거된다

```text
// Given - Redis 참여자 목록에 userId가 저장되어 있다.
// When - 사용자가 공동구매 참여 취소를 요청한다.
// Then - Redis 참여자 목록에서 userId가 제거되어야 한다.
```

## 테스트 4. 참여 취소 시 RDBMS에 취소 이력이 저장된다

```text
// Given - 사용자가 이미 공동구매에 참여한 상태이다.
// When - cancelTimeDeal 함수가 실행된다.
// Then - ParticipationRepository에 CANCELED 상태의 이력이 저장되어야 한다.
```

## 테스트 5. 현재 참여 인원을 Redis 기준으로 조회한다

```text
// Given - Redis 참여자 목록에 3명의 사용자가 저장되어 있다.
// When - 현재 참여 인원 조회를 요청한다.
// Then - 참여 인원 수는 3으로 반환되어야 한다.
```
