package ua.kpi.iot_lighting.prac4.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ua.kpi.iot_lighting.prac4.domain.model.TokenPayload

interface AuthRepository {
    val currentSession: StateFlow<TokenPayload?>

    suspend fun restoreSession()

    suspend fun login(email: String, password: String): Result<TokenPayload>

    suspend fun logout()

    fun tokenHeader(): String?
}
