package ua.kpi.iot_lighting.prac4.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.iot_lighting.prac4.data.auth.MockTokenFactory
import ua.kpi.iot_lighting.prac4.data.auth.TokenStorage
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.remote.LightingApiService
import ua.kpi.iot_lighting.prac4.data.remote.dto.LoginRequestDto
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.TokenPayload
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: LightingApiService,
    private val tokenStorage: TokenStorage,
    private val networkChecker: NetworkChecker,
    private val errorLogger: ErrorLogger,
    private val clockEpochSec: () -> Long = { System.currentTimeMillis() / 1000L },
) : AuthRepository {

    private val _currentSession = MutableStateFlow<TokenPayload?>(null)
    override val currentSession: StateFlow<TokenPayload?> = _currentSession.asStateFlow()

    override suspend fun restoreSession() {
        val raw = tokenStorage.read()
        val payload = MockTokenFactory.parseAndVerify(raw, nowEpochSec = clockEpochSec())
        if (payload == null) {
            tokenStorage.clear()
        }
        _currentSession.value = payload
    }

    override suspend fun login(email: String, password: String): Result<TokenPayload> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(
                    UiError.Network(
                        message = "Немає зʼєднання з мережею для входу",
                        isRetryable = false,
                    ),
                ),
            )
        }

        return try {
            val response = api.login(LoginRequestDto(email = email.trim(), password = password))
            val parsed = MockTokenFactory.parseAndVerify(
                token = response.token,
                nowEpochSec = clockEpochSec(),
            ) ?: TokenPayload(
                subject = response.email,
                role = Role.fromWire(response.role),
                issuedAtEpochSec = clockEpochSec(),
                expiresAtEpochSec = clockEpochSec() + MockTokenFactory.DEFAULT_TTL_SEC,
            )
            tokenStorage.write(response.token)
            _currentSession.value = parsed
            Result.success(parsed)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError("AuthRepository", t, "login")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun logout() {
        tokenStorage.clear()
        _currentSession.value = null
    }

    override fun tokenHeader(): String? {
        val token = tokenStorage.read() ?: return null
        val parsed = MockTokenFactory.parseAndVerify(token, nowEpochSec = clockEpochSec())
        if (parsed == null) {
            tokenStorage.clear()
            _currentSession.value = null
            return null
        }
        return "Bearer $token"
    }
}
