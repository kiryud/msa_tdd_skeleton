# Announce Service - Medium Test (통합 테스트)

> Spring 컨텍스트 + H2 인메모리 DB + Redis Mock 사용. 실제 레이어 간 연동 검증.

---

## DB 저장 검증

### TC-M-01: 알림 발송 후 MySQL 저장 확인
```
Given : userId=1, type=ORDER_CONFIRMED, orderId=5
When  : POST /notifications/send 호출
Then  : HTTP 200
        notifications 테이블에 레코드 1건 저장됨
        저장된 is_read = false
        저장된 type = ORDER_CONFIRMED
        저장된 user_id = 1
```

### TC-M-02: 알림 발송 후 Redis unread count 증가 확인
```
Given : userId=2, Redis "notification:unread:2" = 3
When  : POST /notifications/send 호출 (userId=2)
Then  : HTTP 200
        Redis "notification:unread:2" = 4
```

### TC-M-03: 알림 읽음 처리 후 DB 상태 변경 확인
```
Given : notificationId=1 (isRead=false, userId=1) DB에 저장된 상태
When  : PUT /notifications/1/read 호출
Then  : HTTP 200
        notifications 테이블 해당 레코드 is_read = true
```

### TC-M-04: 알림 읽음 처리 후 Redis unread count 감소 확인
```
Given : notificationId=1 (isRead=false, userId=1)
        Redis "notification:unread:1" = 2
When  : PUT /notifications/1/read 호출
Then  : HTTP 200
        Redis "notification:unread:1" = 1
```

---

## 알림 목록 조회

### TC-M-05: 내 알림 목록 조회 - 여러 건
```
Given : userId=1의 알림 4건 DB에 저장
        (ORDER_CONFIRMED 2건, DEAL_SUCCESS 1건, DEADLINE_ALERT 1건)
When  : GET /notifications/user/1 호출
Then  : HTTP 200
        응답 배열 크기 4
        모두 userId=1의 알림
        createdAt 기준 최신순 정렬
```

### TC-M-06: 알림 없는 유저 목록 조회
```
Given : userId=99 알림 없음
When  : GET /notifications/user/99 호출
Then  : HTTP 200
        응답 빈 배열 []
```

### TC-M-07: unread count 조회 - Redis 값 반환
```
Given : userId=1, Redis "notification:unread:1" = 5
When  : GET /notifications/user/1/unread-count 호출
Then  : HTTP 200
        응답 { count: 5 }
```

---

## 공구 배치 알림 통합 검증

### TC-M-08: 딜 성사 배치 알림 발송 후 DB + Redis 모두 저장 확인
```
Given : dealId=1, type=DEAL_SUCCESS
        participantUserIds = [1, 2, 3]
When  : POST /notifications/deal-result 호출
Then  : HTTP 200
        notifications 테이블에 3건 저장됨
        각 userId (1, 2, 3)의 Redis unread count 각각 1씩 증가
        반환 발송 건수 = 3
```

### TC-M-09: 존재하지 않는 알림 읽음 처리
```
Given : notificationId=999 DB에 없음
When  : PUT /notifications/999/read 호출
Then  : HTTP 404
        응답 body에 에러 메시지 포함
```
