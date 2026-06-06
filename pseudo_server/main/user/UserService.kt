package main.user

/**
 * [KAN-5] User-Service 비즈니스 로직
 * Coroutine 비동기 스레딩을 적용하여 로그인 및 세션 발급 처리 수행
 */
class UserService {

    suspend fun loginAndIssueToken(
        userId: String,
        userPw: String
    ): String {

        // 입력값 검증
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }

        if (userPw.isBlank()) {
            throw IllegalArgumentException("Password is required")
        }

        // MySQL 사용자 검증 수행 예정

        // Redis 세션 저장 수행 예정
        val sessionToken =
            "SUCCESS_TOKEN_ISSUED_BY_REDIS_FOR_$userId"

        return sessionToken
    }
}