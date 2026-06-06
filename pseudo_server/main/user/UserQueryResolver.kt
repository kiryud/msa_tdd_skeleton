package main.user

/**
 * [KAN-5] User-Service 내 GraphQL Resolver 설계
 * GraphQL을 활용하여 필요한 사용자 프로필 정보를 선택적으로 조회하는 역할 수행
 */
class UserQueryResolver {

    fun getUserProfile(userId: String): UserRegisterDto {

        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID is required")
        }

        return UserRegisterDto(
            userId = userId,
            passwordEncrypted = "PROTECTED",
            nickname = "타임딜메이트",
            baseLocation = "우리동네 기반 위치정보"
        )
    }
}