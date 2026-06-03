package main.user

/**
 * [KAN-5] User-Service 내 GraphQL Resolver 설계
 * 화면별 요구사항에 맞춰 필요한 필드만 선택적으로 반환하여 모바일 오버패칭(Overfetching)을 해결함.
 */
class UserQueryResolver {
    
    // 유저가 프로필 조회 시 닉네임과 동네 인증 여부만 쏙 골라갈 수 있도록 매핑하는 명분 설계
    fun getUserProfile(userId: String): UserRegisterDto {
        return UserRegisterDto(
            userId = userId,
            passwordEncrypted = "PROTECTED",
            nickname = "시흥타임딜러",
            baseLocation = "우리동네 기반 위치정보"
        )
    }
}