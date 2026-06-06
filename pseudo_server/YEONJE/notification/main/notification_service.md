# Announce Service (알림 서비스)

## 구상한 서비스

동네 기반 타임딜 공동구매 플랫폼에서 발생하는 주요 이벤트(딜 성사, 딜 실패, 주문 확인, 마감 임박)를 사용자에게 실시간으로 전달하는 서비스입니다.

Order Service, TimeDeal Service, Creation Service 등 다른 마이크로서비스로부터 이벤트를 수신하여 해당 사용자에게 알림을 발송하고, 알림 이력을 관리합니다.

## 내가 담당할 기능

제가 담당할 기능은 `공동구매 이벤트 알림 발송 및 관리 기능`입니다.

이 기능은 아래 이벤트들을 처리합니다.

- 공구 성사 알림 (DEAL_SUCCESS): 최소 인원 달성 → 참여자 전원에게 발송
- 공구 실패 알림 (DEAL_FAILED): 마감까지 최소 인원 미달 → 참여자 전원에게 발송
- 주문 확인 알림 (ORDER_CONFIRMED): 주문 서비스에서 주문 생성 완료 시 발송
- 마감 임박 알림 (DEADLINE_ALERT): 딜 마감 30분 전 참여자에게 발송

## 이 기능을 선택한 이유

알림 서비스는 주문·딜·참여 서비스의 부산물(이벤트)을 받아서 실시간으로 사용자에게 전달하는 구조입니다.

단순 CRUD가 아닌 Redis Pub/Sub, Coroutine 비동기 처리, SSE(Server-Sent Events) 실시간 스트리밍을 동시에 적용할 수 있어 기술 적용도 점수에 유리합니다. 또한 Order Service와 연동되기 때문에 서비스 간 이벤트 흐름을 시연할 수 있습니다.

## 사용 기술과 이유

### Kotlin / Spring Boot

알림 발송 API와 서비스 계층 구성에 사용합니다. Coroutine과의 조합으로 비동기 알림 발송을 간결하게 표현할 수 있습니다.

### Redis Pub/Sub

다른 마이크로서비스(Order, TimeDeal)에서 발행된 이벤트를 Subscribe하여 알림 처리를 트리거합니다. `notification:{userId}` 채널로 실시간 알림을 발행하면 SSE 연결 중인 클라이언트가 즉시 수신합니다.

### Redis (unread count)

`notification:unread:{userId}` 키에 INCR/DECR 원자 연산으로 읽지 않은 알림 수를 빠르게 관리합니다. 매번 DB를 집계하지 않아도 됩니다.

### MySQL

알림 발송 이력(발송 시각, 수신자, 내용, 읽음 여부)을 영구 저장합니다. 사용자가 이전 알림을 조회할 때 사용됩니다.

### SSE (Server-Sent Events)

클라이언트가 `/notifications/subscribe/{userId}`에 SSE 연결을 유지하면, 새 알림 발생 시 서버가 즉시 Push합니다. WebSocket보다 구현이 단순하고 단방향 알림에 적합합니다.

### Coroutine

알림 발송은 요청 흐름에서 분리되어야 합니다. Order Service가 주문 완료 후 알림 API를 호출해도, 알림 실패가 주문 응답에 영향을 주면 안 됩니다. Coroutine으로 비동기 발송하여 독립성을 보장합니다.

## 데이터 구조

```kotlin
enum class NotificationType {
    DEAL_SUCCESS,        // 공구 성사
    DEAL_FAILED,         // 공구 실패
    ORDER_CONFIRMED,     // 주문 확인
    DEADLINE_ALERT       // 마감 임박
}

data class Notification(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val dealId: Long?,
    val orderId: Long?,
    val title: String,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime
)

data class SendNotificationRequest(
    val userId: Long,
    val type: NotificationType,
    val dealId: Long? = null,
    val orderId: Long? = null,
    val title: String,
    val content: String
)

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)
```

## DB 스키마

```sql
-- MySQL
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(50)  NOT NULL,  -- NotificationType
    deal_id     BIGINT,
    order_id    BIGINT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_user_unread (user_id, is_read)
);
```

```
Redis Key 설계
notification:unread:{userId}    → INCR/DECR (읽지 않은 알림 수)
notification:channel:{userId}   → Pub/Sub 채널 (실시간 push)
```

## 핵심 의사코드

```text
// 알림 발송 (내부 API - 다른 서비스에서 호출)
// 주문 확인, 공구 성사/실패 등 이벤트 발생 시 해당 서비스가 호출한다.

/*
    인자    : request (userId, type, dealId?, orderId?, title, content)
    반환값  : 발송된 알림 ID
    역할    : DB에 알림 이력 저장 → Redis unread count 증가 → SSE Publish
*/

function sendNotification(request):
    notification = Notification(
        userId    = request.userId,
        type      = request.type,
        dealId    = request.dealId,
        orderId   = request.orderId,
        title     = request.title,
        content   = request.content,
        isRead    = false,
        createdAt = currentTime()
    )

    saved = NotificationRepository.save(notification)

    Redis.INCR("notification:unread:" + request.userId)

    async {
        Redis.PUBLISH("notification:channel:" + request.userId, toJson(saved))
    }

    return saved.id
```

```text
// 알림 읽음 처리

/*
    인자    : notificationId, userId
    반환값  : 성공/실패
    역할    : 알림 isRead = true, Redis unread count 감소
*/

function markAsRead(notificationId, userId):
    notification = NotificationRepository.findById(notificationId)

    if notification does not exist:
        return NOT_FOUND

    if notification.userId != userId:
        return FORBIDDEN

    if notification.isRead:
        return ALREADY_READ

    NotificationRepository.updateIsRead(notificationId, true)

    currentUnread = Redis.GET("notification:unread:" + userId)
    if currentUnread > 0:
        Redis.DECR("notification:unread:" + userId)

    return SUCCESS
```

```text
// 공구 성사/실패 시 참여자 전원 알림 (배치 발송)

/*
    인자    : dealId, type (DEAL_SUCCESS or DEAL_FAILED), participantUserIds
    반환값  : 발송 건수
    역할    : 참여자 목록에 대해 비동기로 알림 발송
*/

function sendDealResultNotification(dealId, type, participantUserIds):
    title   = if type == DEAL_SUCCESS then "공구가 성사되었습니다!" else "공구가 아쉽게 실패했습니다"
    content = if type == DEAL_SUCCESS
              then "딜 #" + dealId + " 공동구매가 성사되었습니다. 결제를 진행해 주세요."
              else "딜 #" + dealId + " 공동구매 최소 인원에 도달하지 못했습니다."

    async {
        for each userId in participantUserIds:
            sendNotification(SendNotificationRequest(
                userId  = userId,
                type    = type,
                dealId  = dealId,
                title   = title,
                content = content
            ))
    }

    return participantUserIds.size
```

## API 설계

```text
POST /notifications/send
- 알림 발송 (내부 서비스 전용)
- Body: SendNotificationRequest

POST /notifications/deal-result
- 딜 성사/실패 참여자 전원 알림 (내부 서비스 전용)
- Body: { dealId, type, participantUserIds }

GET /notifications/user/{userId}
- 내 알림 목록 조회
- Response: List<NotificationResponse>

PUT /notifications/{id}/read
- 알림 읽음 처리
- Response: 200 OK

GET /notifications/user/{userId}/unread-count
- 읽지 않은 알림 수 조회 (Redis)
- Response: { count: Int }

GET /notifications/subscribe/{userId}
- SSE 연결 (실시간 알림 수신)
- Response: text/event-stream
```

## Jira 이슈 예시

```text
[알림/설계] 알림 서비스 구조 설계 및 기술 스택 선정
[알림/설계] Redis Pub/Sub + SSE 실시간 알림 흐름 설계
[알림/기능] 알림 발송 API 작성 (내부 호출용)
[알림/기능] 공구 성사/실패 배치 알림 발송 기능 작성
[알림/기능] 알림 읽음 처리 및 unread count Redis 연동
[알림/기능] SSE 실시간 알림 push 구현
[알림/TDD] 알림 발송 단위 테스트 작성
[알림/TDD] 읽음 처리 로직 단위 테스트 작성
[알림/TDD] 동시 다중 알림 발송 동시성 테스트 작성
```
