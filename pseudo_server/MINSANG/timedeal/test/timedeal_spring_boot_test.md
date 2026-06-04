# 타임딜 공동구매 Spring Boot 보강 테스트

이 문서는 기존 small / medium / large 테스트에 Spring Boot 계층 구조와 어노테이션 활용 검증을 추가한 테스트 설계입니다.

## Controller 계층 테스트

### 테스트 1. 참여 요청은 Controller에서 Service로 위임된다

```text
// Given - 사용자가 POST /timedeals/{dealId}/join 요청을 보낸다.
// When - TimeDealController의 joinTimeDeal 메서드가 호출된다.
// Then - Controller는 직접 참여 가능 여부를 판단하지 않는다.
// Then - TimeDealService.joinTimeDeal 메서드로 요청을 위임해야 한다.
```

### 테스트 2. 요청 Body는 DTO로 변환된다

```text
// Given - 클라이언트가 JSON 형태의 참여 요청을 보낸다.
// When - @RequestBody가 요청 데이터를 받는다.
// Then - JoinDealRequest DTO로 변환되어야 한다.
// Then - Entity가 Controller 요청 객체로 직접 사용되면 안 된다.
```

## Service 계층 테스트

### 테스트 3. Service는 참여 가능 여부를 순서대로 검증한다

```text
// Given - 타임딜 참여 요청이 들어온다.
// When - TimeDealService.joinTimeDeal이 실행된다.
// Then - 타임딜 존재 여부를 확인해야 한다.
// Then - 마감 시간 여부를 확인해야 한다.
// Then - 지역 일치 여부를 확인해야 한다.
// Then - 중복 참여 여부를 확인해야 한다.
// Then - 정원 초과 여부를 확인해야 한다.
```

### 테스트 4. RDBMS 저장 실패 시 Redis 참여 등록을 보상 처리한다

```text
// Given - Redis에는 참여자 등록이 성공했다.
// Given - RDBMS 참여 이력 저장 중 오류가 발생한다.
// When - joinTimeDeal 작업이 실패한다.
// Then - Redis에서 해당 userId를 제거해야 한다.
// Then - 최종 응답은 JOIN_FAILED를 반환해야 한다.
```

## Repository / Entity 계층 테스트

### 테스트 5. TimeDeal Entity는 상태 변경 메서드로만 상태를 바꾼다

```text
// Given - TimeDeal 상태가 OPEN이다.
// When - 최소 참여 인원이 달성되어 readyToConfirm 메서드가 호출된다.
// Then - 상태는 READY_TO_CONFIRM으로 변경되어야 한다.
```

### 테스트 6. Participation 이력은 참여와 취소 상태를 구분해 저장된다

```text
// Given - 사용자가 공동구매에 참여한 뒤 취소한다.
// When - ParticipationRepository에 이력이 저장된다.
// Then - JOINED 이력과 CANCELED 이력이 구분되어 저장되어야 한다.
```

## 동시성 / Lock 테스트

### 테스트 7. @DealJoinLock은 같은 타임딜의 동시 참여 요청을 순서대로 처리한다

```text
// Given - dealId가 같은 참여 요청 100개가 동시에 들어온다.
// When - @DealJoinLock이 적용된 joinTimeDeal이 실행된다.
// Then - 한 순간에 하나의 요청만 참여 인원 확정 구간에 들어가야 한다.
// Then - 최종 참여 인원은 maxParticipants를 넘지 않아야 한다.
```

### 테스트 8. 같은 userId의 따닥 요청은 한 번만 성공한다

```text
// Given - 같은 사용자가 참여 버튼을 빠르게 여러 번 누른다.
// When - 동일 userId로 여러 참여 요청이 들어온다.
// Then - 첫 번째 요청만 성공해야 한다.
// Then - 나머지 요청은 ALREADY_JOINED를 반환해야 한다.
```

## Transaction 테스트

### 테스트 9. 참여 이력 저장과 상태 변경은 하나의 트랜잭션으로 처리된다

```text
// Given - 참여 요청으로 최소 인원이 달성된다.
// When - 참여 이력 저장 후 상태 변경 중 오류가 발생한다.
// Then - 참여 이력 저장도 롤백되어야 한다.
// Then - 공동구매 상태도 이전 상태로 유지되어야 한다.
```

## 인증 / 보안 테스트

### 테스트 10. 인증되지 않은 사용자는 참여할 수 없다

```text
// Given - Authorization 헤더가 없는 요청이다.
// When - 사용자가 공동구매 참여 API를 호출한다.
// Then - 참여 로직은 실행되지 않아야 한다.
// Then - UNAUTHORIZED 결과를 반환해야 한다.
```

### 테스트 11. 요청 Body의 userId보다 토큰의 사용자 정보를 우선한다

```text
// Given - JWT 토큰의 userId는 1이고 요청 Body의 userId는 2이다.
// When - 사용자가 공동구매 참여를 요청한다.
// Then - 서버는 Body의 userId를 그대로 믿으면 안 된다.
// Then - 토큰에서 검증된 userId 1을 기준으로 참여 처리해야 한다.
```

## Scheduler 테스트

### 테스트 12. @Scheduled는 마감된 타임딜을 자동 확정한다

```text
// Given - deadline이 지난 OPEN 상태의 타임딜이 있다.
// When - closeExpiredDeals 스케줄러가 실행된다.
// Then - 최소 참여 인원을 달성한 타임딜은 SUCCESS가 되어야 한다.
// Then - 최소 참여 인원을 달성하지 못한 타임딜은 FAILED가 되어야 한다.
```
