# Announce Service - Large Test (동시성 / E2E 테스트)

> 실제 Redis + MySQL 연동. 대량 동시 알림 발송 시 정합성 및 실시간 전달 검증.
> 이 테스트가 Announce Service의 핵심 기술 시연 포인트.

---

## 동시 알림 발송 - Redis 원자성 보장

### TC-L-01: 100명 동시 배치 알림 발송 → Redis unread count 정합성
```
Given : dealId=1, type=DEAL_SUCCESS
        participantUserIds = 100명 (userId 1~100)
        각 userId의 초기 Redis unread count = 0

When  : POST /notifications/deal-result 호출
        100건의 sendNotification이 Coroutine으로 동시 실행

Then  : notifications 테이블에 정확히 100건 저장됨
        userId 1~100 각각의 Redis "notification:unread:{id}" = 1
        count 음수 발생 없음
        Redis INCR 원자성으로 누락 없이 100회 실행됨
```

### TC-L-02: 동일 유저에게 동시 다중 알림 → unread count 누락 없음
```
Given : userId=1, 초기 unread count = 0
        10개의 알림 발송 요청 동시 발생 (Coroutine 10개 launch)

When  : 10개 sendNotification 동시 실행

Then  : notifications 테이블 userId=1 알림 = 10건
        Redis "notification:unread:1" = 10
        count 누락 없음 (Redis INCR 원자성 보장)
```

---

## 동시 읽음 처리 - 중복 차감 방지

### TC-L-03: 동일 알림에 대해 동시 읽음 처리 요청 → 한 번만 처리
```
Given : notificationId=1, isRead=false, userId=1
        Redis "notification:unread:1" = 5
        읽음 처리 요청 3개 동시 발생 (네트워크 중복 클릭 시뮬레이션)

When  : PUT /notifications/1/read 3개 동시 호출

Then  : 성공 응답 1건만
        나머지 2건은 409 (이미 읽은 알림) 또는 처리 무시
        notifications 테이블 is_read = true (1회만 업데이트)
        Redis "notification:unread:1" = 4 (1만 차감, 음수 없음)
```

---

## SSE 실시간 전달 E2E

### TC-L-04: SSE 연결 유지 중 알림 발송 → 즉시 수신 확인
```
Given : userId=1이 GET /notifications/subscribe/1 SSE 연결 중
        dealId=2, type=DEAL_SUCCESS

When  : POST /notifications/send 호출 (userId=1, type=DEAL_SUCCESS)
        Redis PUBLISH "notification:channel:1" 실행됨

Then  : SSE 연결된 userId=1 클라이언트가 3초 내 이벤트 수신
        수신된 이벤트의 type = DEAL_SUCCESS
        수신된 이벤트의 dealId = 2
        DB 저장 완료 후 이벤트 발행됨 (정합성 보장)
```