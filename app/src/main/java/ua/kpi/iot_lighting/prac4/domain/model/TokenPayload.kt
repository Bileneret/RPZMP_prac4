package ua.kpi.iot_lighting.prac4.domain.model

data class TokenPayload(
    val subject: String,
    val role: Role,
    val issuedAtEpochSec: Long,
    val expiresAtEpochSec: Long,
) {
    fun isExpired(nowEpochSec: Long = System.currentTimeMillis() / 1000L): Boolean {
        return nowEpochSec >= expiresAtEpochSec
    }
}
