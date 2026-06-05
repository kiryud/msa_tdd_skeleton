# 타임딜 상세 페이지 Large Test

Large Test는 여러 기능과 저장소, 외부 연동이 연결된 전체 흐름을 검증하는 테스트입니다.

## 테스트 1. 동시에 여러 사용자가 조회해도 현재 참여 인원이 일관되게 반환된다

```text
// Given - Redis의 timedeal:{dealId}:participants 키에 30명의 userId가 저장되어 있다.
// When - 100명의 사용자가 동시에 상세 페이지 조회를 요청한다.
// Then - 모든 응답의 participants.currentParticipants는 30이어야 한다.
// Then - 조회 중 데이터 불일치나 오류가 발생하면 안 된다.
```

## 테스트 2. 참여 처리 직후 상세 조회 시 현재 참여 인원이 즉시 반영된다

```text
// Given - 공동구매에 23명이 참여한 상태이다.
// When - 한 명이 참여를 완료한 직후 상세 페이지 조회를 요청한다.
// Then - 응답의 participants.currentParticipants는 24여야 한다.
// Then - Redis 기반 조회이므로 RDBMS 반영 여부와 관계없이 즉시 반영되어야 한다.
```

## 테스트 3. 타임딜 상태가 READY_TO_CONFIRM으로 바뀐 직후 상세 조회에 반영된다

```text
// Given - 최소 참여 인원 달성으로 타임딜 상태가 READY_TO_CONFIRM으로 변경되었다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 status는 READY_TO_CONFIRM이어야 한다.
// Then - 이전 캐시나 stale 데이터가 반환되면 안 된다.
```

## 테스트 4. 마감 이후 상태가 SUCCESS인 타임딜도 상세 조회가 가능하다

```text
// Given - 타임딜 상태가 SUCCESS로 확정되었다.
// When - 사용자가 상세 페이지 조회를 요청한다.
// Then - 응답의 status는 SUCCESS여야 한다.
// Then - 상품, 판매자, 참여 인원 정보도 함께 반환되어야 한다.
```

## 테스트 5. 상세 조회부터 참여하기까지 전체 흐름이 연결된다

```text
// Given - 공동구매가 OPEN 상태로 생성되어 있다.
// When - 사용자가 상세 페이지를 조회한다.
// When - isJoined가 false임을 확인하고 참여 요청을 보낸다.
// When - 참여 완료 후 다시 상세 페이지를 조회한다.
// Then - 두 번째 조회 응답의 isJoined는 true여야 한다.
// Then - participants.currentParticipants는 이전보다 1 증가해야 한다.
```
