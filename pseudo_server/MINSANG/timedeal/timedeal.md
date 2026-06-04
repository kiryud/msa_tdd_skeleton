# 동네 기반 타임딜 공동구매 플랫폼

## 구상한 서비스

동네 기반 타임딜 공동구매 플랫폼은 같은 지역에 있는 사용자들이 제한 시간 안에 공동구매에 참여해서 생활용품, 식재료, 배달 음식 등을 함께 구매할 수 있도록 돕는 서비스입니다.

사용자는 현재 위치나 등록된 동네를 기준으로 주변 공동구매 방을 확인하고, 원하는 타임딜에 참여할 수 있습니다. 공동구매는 정해진 마감 시간과 최대 참여 인원 안에서 진행되며, 최소 참여 인원을 넘기면 공동구매가 성사됩니다.

## 내가 담당할 기능

제가 담당할 기능은 `타임딜 공동구매 참여 처리 기능`입니다.

이 기능은 사용자가 공동구매 방에서 참여하기 버튼을 눌렀을 때, 서버에서 참여 가능 여부를 판단하고 선착순으로 참여자를 확정하는 역할을 합니다.

공동구매는 정해진 시간 안에 정해진 인원만 참여할 수 있기 때문에 아래 조건을 정확하게 확인해야 합니다.

- 마감 시간이 지나지 않았는지 확인
- 같은 사용자가 이미 참여했는지 확인
- 최대 참여 인원이 초과되지 않았는지 확인
- 사용자의 동네가 공동구매 가능 지역과 맞는지 확인
- 최소 참여 인원이 모였는지 확인

## 이 기능을 선택한 이유

상품 등록이나 댓글 작성 같은 기능은 대부분 CRUD 중심으로 끝날 수 있습니다. 반면 공동구매 참여 처리는 여러 사용자가 동시에 참여 버튼을 누를 수 있기 때문에 정원 초과, 중복 참여, 마감 시간 처리 같은 문제가 생길 수 있습니다.

그래서 Redis, RDBMS, Coroutine, TDD를 자연스럽게 적용할 수 있고, 이번 과제에서 요구하는 기술 설계 이유를 설명하기에도 적합하다고 생각했습니다.

## 사용 기술과 이유

### Kotlin / Spring Boot

공동구매 참여 API를 작성하기 위해 사용합니다. Kotlin의 data class를 활용하면 요청과 응답 객체를 간결하게 표현할 수 있고, Spring Boot를 사용하면 REST API와 서비스 계층을 구성하기 쉽습니다.

### Redis

선착순 참여자 목록과 현재 참여 인원 수를 빠르게 관리하기 위해 사용합니다. 동시에 많은 사용자가 참여하더라도 Redis의 원자적 연산을 활용하면 정원 초과와 중복 참여 문제를 줄일 수 있습니다.

### RDBMS

공동구매 방 정보, 참여 이력, 공동구매 성공 또는 실패 결과처럼 나중에 기록으로 남아야 하는 데이터를 저장하기 위해 사용합니다.

### Coroutine

참여 요청 처리 이후 참여 이력 저장, 알림 요청 같은 작업을 비동기로 분리하기 위해 사용합니다.

### TDD

참여 가능 여부 판단, 중복 참여 방지, 정원 초과 방지, 마감 확정 로직을 테스트 우선으로 검증하기 위해 사용합니다.

## 데이터 구조 예시

```kotlin
data class TimeDeal(
    val dealId: Long,
    val title: String,
    val regionCode: String,
    val minParticipants: Int,
    val maxParticipants: Int,
    val deadline: DateTime,
    val status: DealStatus
)

data class JoinDealRequest(
    val dealId: Long,
    val userId: Long,
    val userRegionCode: String
)

data class JoinDealResponse(
    val dealId: Long,
    val userId: Long,
    val currentParticipants: Int,
    val status: DealStatus,
    val message: String
)

enum class DealStatus {
    OPEN,
    READY_TO_CONFIRM,
    SUCCESS,
    FAILED,
    CLOSED
}
```

## 핵심 의사코드

```text
// 타임딜 공동구매 참여 처리
// 사용자가 공동구매 방에서 참여하기 버튼을 눌렀을 때 실행된다.

/*
    인자    : dealId, userId, userRegionCode
    반환값  : 참여 결과, 현재 참여 인원, 공동구매 상태
    역할    : 참여 가능 여부를 확인하고 선착순으로 참여자를 등록한다.
*/

function joinTimeDeal(dealId, userId, userRegionCode):
    deal = TimeDealRepository.findById(dealId)

    if deal does not exist:
        return DEAL_NOT_FOUND

    if deal.status is not OPEN:
        return DEAL_NOT_OPEN

    if currentTime() > deal.deadline:
        return DEAL_DEADLINE_PASSED

    if userRegionCode != deal.regionCode:
        return REGION_NOT_MATCHED

    redisKey = "timedeal:" + dealId + ":participants"

    if Redis.SISMEMBER(redisKey, userId):
        return ALREADY_JOINED

    currentCount = Redis.SCARD(redisKey)

    if currentCount >= deal.maxParticipants:
        return DEAL_FULL

    Redis.SADD(redisKey, userId)

    updatedCount = Redis.SCARD(redisKey)

    async {
        ParticipationRepository.save(dealId, userId, JOINED)
    }

    if updatedCount >= deal.minParticipants:
        TimeDealRepository.updateStatus(dealId, READY_TO_CONFIRM)

    return JoinDealResponse(
        dealId = dealId,
        userId = userId,
        currentParticipants = updatedCount,
        status = READY_TO_CONFIRM if updatedCount >= deal.minParticipants else OPEN,
        message = "공동구매 참여가 완료되었습니다."
    )
```

```text
// 타임딜 공동구매 참여 취소

/*
    인자    : dealId, userId
    반환값  : 취소 결과, 현재 참여 인원
    역할    : 참여자를 Redis 참여 목록에서 제거하고 참여 취소 이력을 저장한다.
*/

function cancelTimeDeal(dealId, userId):
    deal = TimeDealRepository.findById(dealId)

    if deal does not exist:
        return DEAL_NOT_FOUND

    if currentTime() > deal.deadline:
        return CANCEL_NOT_ALLOWED_AFTER_DEADLINE

    redisKey = "timedeal:" + dealId + ":participants"

    if Redis.SISMEMBER(redisKey, userId) is false:
        return PARTICIPATION_NOT_FOUND

    Redis.SREM(redisKey, userId)

    updatedCount = Redis.SCARD(redisKey)

    async {
        ParticipationRepository.save(dealId, userId, CANCELED)
    }

    if updatedCount < deal.minParticipants:
        TimeDealRepository.updateStatus(dealId, OPEN)

    return CANCEL_SUCCESS
```

```text
// 마감 시간 이후 공동구매 성공 또는 실패 확정

/*
    인자    : dealId
    반환값  : 공동구매 최종 상태
    역할    : 최소 참여 인원 달성 여부를 확인하고 성공/실패를 확정한다.
*/

function closeTimeDeal(dealId):
    deal = TimeDealRepository.findById(dealId)

    if deal does not exist:
        return DEAL_NOT_FOUND

    if currentTime() < deal.deadline:
        return DEAL_NOT_EXPIRED

    redisKey = "timedeal:" + dealId + ":participants"
    participantCount = Redis.SCARD(redisKey)

    if participantCount >= deal.minParticipants:
        TimeDealRepository.updateStatus(dealId, SUCCESS)

        async {
            NotificationService.sendSuccessMessage(dealId)
        }

        return DEAL_SUCCESS

    TimeDealRepository.updateStatus(dealId, FAILED)

    async {
        NotificationService.sendFailedMessage(dealId)
    }

    return DEAL_FAILED
```

## API 예시

```text
POST /timedeals/{dealId}/join
- 공동구매 참여 요청

DELETE /timedeals/{dealId}/participants/me
- 공동구매 참여 취소

GET /timedeals/{dealId}/participants/count
- 현재 참여 인원 조회

POST /timedeals/{dealId}/close
- 마감 시간이 지난 공동구매 성공/실패 확정
```

## Jira 이슈 예시

```text
[타임딜/설계] 공동구매 참여 처리 기능 구조 설계
[타임딜/기능] 선착순 공동구매 참여 기능 작성
[타임딜/기능] 공동구매 참여 취소 기능 작성
[타임딜/기능] 마감 이후 성공 실패 확정 기능 작성
[타임딜/TDD] 중복 참여 방지 테스트 작성
[타임딜/TDD] 정원 초과 방지 테스트 작성
[타임딜/TDD] 동시 참여 요청 테스트 작성
```
