package main.user

/**
 * [KAN-5] User-Service 비즈니스 로직
 * Coroutine 비동기 스레딩을 적용하여 타임딜 마감 직전 선착순 대규모 트래픽 분산 제어 명분 구축.
 */
class UserService {

    // suspend 키워드를 붙여 코루틴 비동기 무중단(Non-blocking) 함수로 정의
    suspend fun loginAndIssueToken(userId: String, userPw: String): String {
        // 1. MySQL에서 유저 계정 일치 여부 무결성 검증 수행 예정
        
        // 2. 대규모 트래픽 환경에서 병목을 방지하기 위해 Redis(In-Memory)에 세션 토큰 발행 처리 예정
        val sessionToken = "SUCCESS_TOKEN_ISSUED_BY_REDIS_FOR_${userId}"
        
        return sessionToken
    }
}