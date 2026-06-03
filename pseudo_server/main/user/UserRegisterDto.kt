package main.user

/**
 * [KAN-5] 우리동네 타임딜 회원가입 요청 DTO
 * Kotlin Data Class를 활용하여 보일러플레이트 코드를 배제하고 객체의 불변성을 보장함.
 */
data class UserRegisterDto(
    val userId: String,
    val passwordEncrypted: String,
    val nickname: String,
    val baseLocation: String // 사용자의 GPS 기반 동네 인증 정보
)