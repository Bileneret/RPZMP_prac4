package ua.kpi.iot_lighting.prac4.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ua.kpi.iot_lighting.prac4.MainDispatcherRule
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.TokenPayload
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private class FakeAuthRepo(
        private val onLogin: suspend (String, String) -> Result<TokenPayload> = { email, _ ->
            Result.success(TokenPayload(email, Role.USER, 100L, 9999L))
        },
    ) : AuthRepository {
        private val session = MutableStateFlow<TokenPayload?>(null)
        var loginCalls = 0
        override val currentSession: StateFlow<TokenPayload?> = session
        override suspend fun restoreSession() = Unit
        override suspend fun login(email: String, password: String): Result<TokenPayload> {
            loginCalls += 1
            val res = onLogin(email, password)
            session.value = res.getOrNull()
            return res
        }
        override suspend fun logout() { session.value = null }
        override fun tokenHeader(): String? = null
    }

    @Test
    fun `invalid form sets validation errors without hitting repository`() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeAuthRepo()
        val vm = LoginViewModel(repo)

        vm.submit()
        assertNotNull(vm.state.value.emailError)
        assertNotNull(vm.state.value.passwordError)
        assertEquals(0, repo.loginCalls)
    }

    @Test
    fun `demo admin login succeeds and unauthorized surfaces error`() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeAuthRepo { email, password ->
            if (password == "wrong") {
                Result.failure(DomainErrorException(UiError.Unauthorized("Bad credentials")))
            } else {
                Result.success(TokenPayload(email, Role.ADMIN, 100L, 9999L))
            }
        }
        val vm = LoginViewModel(repo)
        vm.fillDemoAdmin()
        vm.onPasswordChange("wrong")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.error is UiError.Unauthorized)

        vm.fillDemoAdmin()
        vm.submit()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals("", vm.state.value.email)
        assertEquals("", vm.state.value.password)
        assertEquals(Role.ADMIN, repo.currentSession.value?.role)
    }
}
