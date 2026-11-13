package ua.kpi.iot_lighting.prac4.data.auth

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.TokenPayload

object MockTokenFactory {
    private val gson = Gson()
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val urlDecoder = Base64.getUrlDecoder()
    private const val HEADER_JSON = """{"alg":"HS256","typ":"JWT"}"""
    private const val MOCK_SALT = "kpi-iot-lighting-prac4-demo"

    private data class WirePayload(
        @SerializedName("sub") val sub: String,
        @SerializedName("role") val role: String,
        @SerializedName("iat") val iat: Long,
        @SerializedName("exp") val exp: Long,
    )

    fun createToken(
        subject: String,
        role: Role,
        nowEpochSec: Long = System.currentTimeMillis() / 1000L,
        ttlSec: Long = DEFAULT_TTL_SEC,
    ): String {
        val header = encodeBase64Url(HEADER_JSON)
        val payloadJson = gson.toJson(
            WirePayload(
                sub = subject,
                role = role.wireName,
                iat = nowEpochSec,
                exp = nowEpochSec + ttlSec,
            ),
        )
        val payload = encodeBase64Url(payloadJson)
        val signature = sign("$header.$payload")
        return "$header.$payload.$signature"
    }

    fun parseAndVerify(
        token: String?,
        nowEpochSec: Long = System.currentTimeMillis() / 1000L,
        verifySignature: Boolean = false,
    ): TokenPayload? {
        if (token.isNullOrBlank()) return null
        val parts = token.split('.')
        if (parts.size != 3) return null

        val headerPart = parts[0]
        val payloadPart = parts[1]
        val sigPart = parts[2]
        if (headerPart.isBlank() || payloadPart.isBlank() || sigPart.isBlank()) return null

        if (verifySignature && sign("$headerPart.$payloadPart") != sigPart) {
            return null
        }

        val wire = runCatching {
            val json = decodeBase64Url(payloadPart)
            gson.fromJson(json, WirePayload::class.java)
        }.getOrNull() ?: return null

        if (wire.sub.isBlank() || wire.exp <= 0L) return null
        val payload = TokenPayload(
            subject = wire.sub,
            role = Role.fromWire(wire.role),
            issuedAtEpochSec = wire.iat,
            expiresAtEpochSec = wire.exp,
        )
        return payload.takeUnless { it.isExpired(nowEpochSec) }
    }

    private fun encodeBase64Url(raw: String): String {
        return urlEncoder.encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeBase64Url(encoded: String): String {
        return String(urlDecoder.decode(encoded), StandardCharsets.UTF_8)
    }

    private fun sign(signingInput: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$signingInput.$MOCK_SALT".toByteArray(StandardCharsets.UTF_8))
        return urlEncoder.encodeToString(digest)
    }

    const val DEFAULT_TTL_SEC = 86_400L
}
