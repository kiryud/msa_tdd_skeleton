# 동네 기반 타임딜 공동구매 플랫폼 — 딜 생성

## 구상한 서비스

동네 기반 타임딜 공동구매 플랫폼은 같은 지역에 있는 사용자들이 제한 시간 안에 공동구매에 참여해서 생활용품, 식재료, 배달 음식 등을 함께 구매할 수 있도록 돕는 서비스입니다.

이 문서는 그중에서 **공동구매 딜(목록)을 최초로 생성하는 부분**을 다룹니다. 참여 처리, 위치 기반 탐색, 결제/정산은 각각 다른 담당자가 맡습니다.

## 내가 담당할 기능

제가 담당할 기능은 `공동구매 딜 최초 생성 기능`입니다.

이 기능은 생필품 타임딜, 판매자 직접 등록, 배달비 절약용 음식 배달 공구처럼 **형태가 다른 여러 딜을 우리 시스템에 처음 등록**하고, 초기 상태(OPEN)로 저장한 뒤 "딜 생성됨" 이벤트를 발행하는 역할을 합니다.

생성 시 서버에서 처리하는 일은 아래와 같습니다.

- 딜 타입 판별 (생필품 / 판매자 / 배달 공구)
- 타입별 입력 유효성 검증 (마감 시간, 가격, 최소값 등)
- 성공 조건 설정 (인원 / 수량 / 금액 중 1개 + 최소값)
- 정원(capacity, 최대값) 설정
- 초기 상태 OPEN으로 저장
- 같은 요청의 중복 생성 방지 (멱등성)
- 딜 생성 이벤트 발행 (참여 서비스 연동)

이 서비스에서 만든 딜이 참여 처리 서비스의 입력이 됩니다.

## 이 기능을 선택한 이유

참여 처리 기능이 이미 만들어진 딜을 받아서 쓰는 쪽이라면, 딜 생성은 **참여·위치·결제 서비스가 쓸 딜을 처음 만들어내는 쪽**입니다.

단순한 등록(CRUD)으로 끝내지 않기 위해 아래 세 가지를 핵심 축으로 잡았습니다.

- **타입 다형성**: 생필품/판매자/배달 공구를 전략 패턴으로 처리 → 타입 추가 시 기존 코드 무수정 (개방-폐쇄 원칙)
- **멱등성**: 생성 버튼 따닥 클릭으로 인한 중복 딜 생성 방지
- **이벤트 발행(커밋 후)**: 저장이 커밋된 뒤에만 이벤트가 나가도록 묶어 참여 서비스에 전달

이 구성으로 전략 패턴, 멱등성, 이벤트 정합이라는 기술 복합 사용 명분과 TDD 케이스를 동시에 확보할 수 있다고 판단했습니다.

## 사용 기술과 이유

### Kotlin / Spring Boot

딜 생성 API와 서비스 계층을 구성하기 위해 사용합니다. data class로 요청/응답 DTO를 간결하게 표현합니다.

### RDBMS

딜 원본 데이터(타입, 마감 시간, 성공 조건, 정원, 상태)는 기록으로 남아야 하므로 RDBMS에 저장합니다. 이 딜 테이블은 생성 서비스가 소유하며, 다른 서비스는 dealId로 참조만 합니다.

### 전략 패턴 (Strategy)

타입마다 검증 규칙과 성공 조건이 다릅니다. 타입별 전략 객체로 분리하면 타입 분기를 없애고, 각 전략을 순수 로직으로 떼어내 Small Test로 검증할 수 있습니다.

### 이벤트 발행 (트랜잭션 커밋 후)

딜이 생성되면 `DealCreatedEvent`를 발행해 참여 서비스가 받습니다. 스프링의 `ApplicationEventPublisher`로 발행하고 `@TransactionalEventListener(AFTER_COMMIT)`에서 처리하여, 저장이 **커밋된 뒤에만** 이벤트가 나가도록 합니다. 저장이 롤백되면 이벤트도 발행되지 않습니다.

### 멱등성

같은 `Idempotency-Key`로 들어온 중복 생성 요청은 한 건만 만들고 기존 결과를 돌려줍니다. DB unique 제약(또는 Redis SETNX)으로 보장합니다.

### TDD

타입별 유효성 검증, 성공 조건 빌드, 멱등성 판단을 테스트 우선으로 검증하기 위해 사용합니다.

## 데이터 구조 예시

```kotlin
enum class DealType {
    GROCERY,
    SELLER,
    FOOD_DELIVERY
}

enum class SuccessMetric {
    HEADCOUNT,
    QUANTITY,
    AMOUNT
}

enum class DealStatus {
    OPEN,
    READY_TO_CONFIRM,
    SUCCESS,
    FAILED,
    CLOSED
}

data class SuccessCriterion(
    val metric: SuccessMetric,
    val min: Long
)

data class Deal(
    val dealId: Long,
    val dealType: DealType,
    val title: String,
    val regionCode: String,
    val hostId: Long,
    val price: Long,
    val capacity: Long?,
    val successCriterion: SuccessCriterion,
    val deadline: LocalDateTime,
    val status: DealStatus
)

data class CreateDealRequest(
    val dealType: DealType,
    val title: String,
    val regionCode: String,
    val price: Long,
    val capacity: Long?,
    val metric: SuccessMetric,
    val min: Long,
    val deadline: LocalDateTime
)

data class CreateDealResponse(
    val dealId: Long,
    val dealType: DealType,
    val status: DealStatus,
    val message: String
)

data class DealCreatedEvent(
    val dealId: Long,
    val dealType: DealType,
    val regionCode: String,
    val capacity: Long?,
    val successCriterion: SuccessCriterion,
    val deadline: LocalDateTime,
    val hostId: Long
)
```

> 참여 서비스 코드와의 매핑: 인원(HEADCOUNT) 타입일 때 `successCriterion.min` = `minParticipants`, `capacity` = `maxParticipants`. 즉 참여 서비스는 기존 필드 의미 그대로 받아갈 수 있습니다.

## 핵심 의사코드

```text
// 공동구매 딜 최초 생성
// 사용자(또는 판매자)가 딜 등록 버튼을 눌렀을 때 실행된다.

/*
    인자    : request, idempotencyKey, authUserId(토큰에서 추출)
    반환값  : 생성된 dealId, 딜 타입, 상태
    역할    : 타입별로 검증한 뒤 OPEN 상태의 딜을 저장하고 생성 이벤트를 발행한다.
*/

function createDeal(request, idempotencyKey, authUserId):

    existing = DealRepository.findByIdempotencyKey(idempotencyKey)
    if existing exists:
        return toResponse(existing)

    strategy = DealCreationStrategyFactory.get(request.dealType)

    if currentTime() >= request.deadline:
        return DEADLINE_IN_PAST
    if request.price <= 0:
        return INVALID_PRICE
    if request.min < 1:
        return INVALID_MIN
    if request.capacity != null and request.capacity < request.min:
        return CAPACITY_LESS_THAN_MIN

    validation = strategy.validate(request)
    if validation is invalid:
        return validation.error

    criterion = strategy.buildSuccessCriterion(request)

    deal = Deal(
        dealType = request.dealType,
        title = request.title,
        regionCode = request.regionCode,
        hostId = authUserId,
        price = request.price,
        capacity = request.capacity,
        successCriterion = criterion,
        deadline = request.deadline,
        status = OPEN
    )

    saved = DealRepository.save(deal)
    EventPublisher.publish(DealCreatedEvent(saved))

    return toResponse(saved)
```

## API 예시

```text
POST /deals
- 공동구매 딜 생성 요청 (헤더: Authorization, Idempotency-Key)

GET /deals/{dealId}
- 딜 정보 조회 (참여 서비스가 마감/정원/성공조건 확인에 사용)
```

## 참여 서비스와의 연동 계약

- **이벤트**: 생성 직후 `DealCreatedEvent` 발행 → 참여 서비스가 수신
- **조회**: 참여 서비스는 참여 검증 시 `GET /deals/{dealId}`로 deadline / capacity / successCriterion 조회
- **상태 경계**: 생성 서비스는 `OPEN`으로만 생성. 이후 `READY_TO_CONFIRM` / `SUCCESS` / `FAILED` 전이는 참여 서비스가 처리
- **엔티티 소유**: `Deal` 엔티티 = 생성 서비스 소유, `Participation` 엔티티 = 참여 서비스 소유(dealId로 참조)
- **판정 기준**: 성공 최소값(`successCriterion.min`)과 정원(`capacity`)은 생성 서비스가 정해서 넘기고, 그 값으로 성공/실패 판정·초과 방지는 참여 서비스가 수행

## Jira 이슈 예시

```text
[Creation/설계] 공동구매 딜 생성 기능 구조 설계
[Creation/설계] DealCreationStrategy 인터페이스 및 타입별 전략 설계
[Creation/기능] 딜 타입별 유효성 검증 작성
[Creation/기능] 성공 조건(인원/수량/금액 + 최소값) 설정 작성
[Creation/기능] 딜 생성 멱등성 처리 작성
[Creation/기능] DealCreatedEvent 발행(커밋 후) 작성
[Creation/TDD] 타입별 유효성 검증 테스트 작성
[Creation/TDD] 멱등성 중복 생성 방지 테스트 작성
[Creation/TDD] 생성 -> 이벤트 발행 -> 참여 서비스 수신 흐름 테스트 작성
```
