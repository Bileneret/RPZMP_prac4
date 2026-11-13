package ua.kpi.iot_lighting.prac4.data.repository

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.kpi.iot_lighting.prac4.data.auth.MockTokenFactory
import ua.kpi.iot_lighting.prac4.data.auth.TokenStorage
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.remote.LightingApiService
import ua.kpi.iot_lighting.prac4.data.remote.dto.CreateLightRequestDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.FaultModeDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LoginRequestDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LoginResponseDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.PowerUpdateRequestDto
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.UiError

class AuthRepositoryImplTest {

    private class MemoryTokenStorage(var stored: String? = null) : TokenStorage {
        override fun read(): String? = stored
        override fun write(token: String?) { stored = token }
        override fun clear() { stored = null }
    }

    private fun buildRepo(
        online: Boolean = true,
        storage: MemoryTokenStorage = MemoryTokenStorage(),
        clockEpoch: () -> Long = { 1_000_000L },
        onLogin: suspend (LoginRequestDto) -> LoginResponseDto = { req ->
            val role = if (req.email.startsWith("admin")) "admin" else "user"
            val token = MockTokenFactory.createToken(req.email, Role.fromWire(role), nowEpochSec = 1_000_000L)
            LoginResponseDto(token = token, email = req.email, role = role)
        },
    ): AuthRepositoryImpl {
        val fakeApi = object : LightingApiService {
            override suspend fun login(request: LoginRequestDto) = onLogin(request)
            override suspend fun getLights(): List<LightDto> = emptyList()
            override suspend fun createLight(request: CreateLightRequestDto) = error("unused")
            override suspend fun setPower(id: Long, request: PowerUpdateRequestDto) = error("unused")
            override suspend fun deleteLight(id: Long) = Unit
            override suspend fun getFaultMode() = FaultModeDto("none")
            override suspend fun setFaultMode(request: FaultModeDto) = request
        }
        return AuthRepositoryImpl(
            api = fakeApi,
            tokenStorage = storage,
            networkChecker = object : NetworkChecker { override fun isOnline() = online },
            errorLogger = object : ErrorLogger { override fun logError(tag: String, error: Throwable, context: String?) = Unit },
            clockEpochSec = clockEpoch,
        )
    }

    @Test
    fun `login stores token and populates currentSession on success`() = runTest {
        val storage = MemoryTokenStorage()
        val repo = buildRepo(storage = storage)

        val result = repo.login("admin@kpi.ua", "admin123")
        assertTrue(result.isSuccess)
        assertEquals(Role.ADMIN, repo.currentSession.value?.role)
        assertTrue(repo.tokenHeader()?.startsWith("Bearer ") == true)
        assertEquals(storage.stored, repo.tokenHeader()?.removePrefix("Bearer "))
    }

    @Test
    fun `login offline returns Network error without throwing`() = runTest {
        val repo = buildRepo(online = false)
        val result = repo.login("operator@kpi.ua", "1234")
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as DomainErrorException).uiError
        assertTrue(err is UiError.Network)
    }

    @Test
    fun `login 401 returns Unauthorized error`() = runTest {
        val repo = buildRepo {
            throw HttpException(Response.error<LoginResponseDto>(401, "".toResponseBody()))
        }
        val result = repo.login("operator@kpi.ua", "wrong")
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as DomainErrorException).uiError
        assertTrue(err is UiError.Unauthorized)
    }

    @Test
    fun `restoreSession clears expired tokens and logout wipes session`() = runTest {
        val validToken = MockTokenFactory.createToken("operator@kpi.ua", Role.USER, nowEpochSec = 100L, ttlSec = 500L)
        val expiredToken = MockTokenFactory.createToken("old@kpi.ua", Role.USER, nowEpochSec = 10L, ttlSec = 20L)

        val storage = MemoryTokenStorage(expiredToken)
        val repo = buildRepo(storage = storage, clockEpoch = { 300L })
        repo.restoreSession()
        assertNull(repo.currentSession.value)
        assertNull(storage.stored)

        storage.stored = validToken
        repo.restoreSession()
        assertEquals("operator@kpi.ua", repo.currentSession.value?.subject)

        repo.logout()
        assertNull(repo.currentSession.value)
        assertNull(storage.stored)
    }

    @Test
    fun `io exception during login maps to Network error`() = runTest {
        val repo = buildRepo { throw IOException("socket closed") }
        val result = repo.login("operator@kpi.ua", "1234")
        assertTrue((result.exceptionOrNull() as DomainErrorException).uiError is UiError.Network)
    }
}
