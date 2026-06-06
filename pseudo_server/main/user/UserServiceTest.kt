package main.user

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class UserServiceTest {

    private val userService = UserService()

    @Test
    fun 로그인_성공() = runBlocking {

        val token =
            userService.loginAndIssueToken(
                "daoul",
                "1234"
            )

        assertTrue(
            token.contains("SUCCESS_TOKEN")
        )
    }

    @Test
    fun 빈아이디_로그인_실패() {

        assertFailsWith<IllegalArgumentException> {

            runBlocking {
                userService.loginAndIssueToken(
                    "",
                    "1234"
                )
            }
        }
    }
}