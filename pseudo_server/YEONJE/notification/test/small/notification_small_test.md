# Announce Service - Small Test (단위 테스트)

> 외부 의존성(DB, Redis) 없이 순수 도메인 로직만 검증. Mock 객체 사용.

---

## 알림 발송 유효성 검증

### TC-S-01: 정상 알림 발송 - 주문 확인
```
Given : userId=1, type=ORDER_CONFIRMED, orderId=10
        title="주문이 확인되었습니다", content="딜 #1 주문이 완료되었습니다."
When  : sendNotification(request) 호출
Then  : NotificationRepository.save 1회 호출됨
        Redis INCR "notification:unread:1" 1회 호출됨
        반환된 notificationId != null
```

### TC-S-02: 정상 알림 발송 - 공구 성사
```
Given : userId=2, type=DEAL_SUCCESS, dealId=5
        title="공구가 성사되었습니다!", content="딜 #5 결제를 진행해 주세요."
When  : sendNotification(request) 호출
Then  : Notification.isRead = false 로 저장됨
        Redis INCR 호출됨
        SSE Publish 비동기 호출됨
```

### TC-S-03: 정상 알림 발송 - 공구 실패
```
Given : userId=3, type=DEAL_FAILED, dealId=5
        title="공구가 실패했습니다", content="딜 #5 최소 인원 미달."
When  : sendNotification(request) 호출
Then  : Notification.type = DEAL_FAILED 로 저장됨
        Redis INCR 호출됨
```

---

## 알림 읽음 처리 로직

### TC-S-04: 정상 읽음 처리
```
Given : notificationId=1, userId=1
        해당 알림 isRead=false, userId=1
When  : markAsRead(1, 1) 호출
Then  : NotificationRepository.updateIsRead(1, true) 호출됨
        Redis DECR "notification:unread:1" 1회 호출됨
```

### TC-S-05: 이미 읽은 알림 재읽음 처리
```
Given : notificationId=1, isRead=true (이미 읽음)
When  : markAsRead(1, 1) 호출
Then  : 예외 발생 - "이미 읽은 알림입니다"
        Redis DECR 호출 안 됨
```

### TC-S-06: 존재하지 않는 알림 읽음 처리
```
Given : notificationId=999 (존재하지 않음)
When  : markAsRead(999, 1) 호출
Then  : 예외 발생 - "알림을 찾을 수 없습니다"
```

### TC-S-07: 다른 유저의 알림 읽음 처리 시도 (권한 없음)
```
Given : notificationId=1, 알림 소유자 userId=1
        요청자 userId=2
When  : markAsRead(1, 2) 호출
Then  : 예외 발생 - "접근 권한이 없습니다"
        DB 업데이트 안 됨
        Redis DECR 호출 안 됨
```

---

## Redis unread count 경계 처리

### TC-S-08: unread count 0인 상태에서 읽음 처리
```
Given : notificationId=1, isRead=false
        Redis "notification:unread:1" = 0 (이미 0)
When  : markAsRead(1, 1) 호출
Then  : DB isRead = true 로 업데이트됨
        Redis DECR 호출 안 됨 (음수 방지)
        예외 발생 없음
```

---

## 배치 알림 발송 로직

### TC-S-09: 공구 성사 배치 알림 - 참여자 수만큼 발송
```
Given : dealId=1, type=DEAL_SUCCESS
        participantUserIds = [1, 2, 3] (3명)
When  : sendDealResultNotification(1, DEAL_SUCCESS, [1,2,3]) 호출
Then  : sendNotification 3회 호출됨
        각 알림 title = "공구가 성사되었습니다!"
        반환 발송 건수 = 3
```

### TC-S-10: 공구 실패 배치 알림 - title/content 구분
```
Given : dealId=2, type=DEAL_FAILED
        participantUserIds = [4, 5]
When  : sendDealResultNotification(2, DEAL_FAILED, [4,5]) 호출
Then  : 각 알림 type = DEAL_FAILED
        각 알림 title = "공구가 아쉽게 실패했습니다"
        반환 발송 건수 = 2
```

### TC-S-11: 참여자 0명인 딜 배치 알림
```
Given : dealId=3, type=DEAL_FAILED
        participantUserIds = [] (아무도 없음)
When  : sendDealResultNotification(3, DEAL_FAILED, []) 호출
Then  : sendNotification 호출 안 됨
        반환 발송 건수 = 0
        예외 발생 없음
```

---

## 알림 타입 매핑 검증

### TC-S-12: 알림 타입별 title/content 자동 생성 확인
```
Given : type=ORDER_CONFIRMED, orderId=7
When  : 알림 생성 요청
Then  : title에 "주문" 또는 "확인" 포함
        content에 orderId=7 참조 포함
        type = ORDER_CONFIRMED 로 저장됨
```
