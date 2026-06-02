# 타임딜 공동구매 Small Test

Small Test는 Redis, RDBMS 같은 외부 저장소에 직접 연결하지 않고 순수한 판단 로직을 검증하는 테스트입니다.

## 테스트 1. 마감 시간이 지난 공동구매에는 참여할 수 없다

```text
// Given - 현재 시간이 공동구매 마감 시간보다 늦다.
// When - 사용자가 공동구매 참여를 요청한다.
// Then - DEAL_DEADLINE_PASSED 결과를 반환해야 한다.
```

## 테스트 2. 이미 참여한 사용자는 다시 참여할 수 없다

```text
// Given - userId가 이미 참여자 목록에 존재한다.
// When - 같은 userId로 다시 참여 요청을 보낸다.
// Then - ALREADY_JOINED 결과를 반환해야 한다.
```

## 테스트 3. 최대 참여 인원이 찬 경우 참여할 수 없다

```text
// Given - 현재 참여 인원이 maxParticipants와 같다.
// When - 새로운 사용자가 참여 요청을 보낸다.
// Then - DEAL_FULL 결과를 반환해야 한다.
```

## 테스트 4. 사용자 동네와 공동구매 지역이 다르면 참여할 수 없다

```text
// Given - 공동구매 가능 지역은 A동이고 사용자의 지역은 B동이다.
// When - 사용자가 공동구매 참여를 요청한다.
// Then - REGION_NOT_MATCHED 결과를 반환해야 한다.
```

## 테스트 5. 최소 인원이 모이면 성사 가능 상태가 된다

```text
// Given - 최소 참여 인원이 5명이고 현재 참여 인원이 4명이다.
// When - 한 명이 추가로 참여한다.
// Then - 공동구매 상태는 READY_TO_CONFIRM 상태가 되어야 한다.
```
