package ua.kpi.iot_lighting.prac4.domain.model

sealed interface UiError {
    val message: String
    val isRetryable: Boolean

    data class Network(
        override val message: String,
        override val isRetryable: Boolean = true,
    ) : UiError

    data class Unauthorized(
        override val message: String,
    ) : UiError {
        override val isRetryable: Boolean = false
    }

    data class Validation(
        override val message: String,
    ) : UiError {
        override val isRetryable: Boolean = false
    }

    data class Server(
        val code: Int,
        override val message: String,
        override val isRetryable: Boolean = code >= 500 || code == 429,
    ) : UiError

    data class Cache(
        override val message: String,
    ) : UiError {
        override val isRetryable: Boolean = false
    }

    data class Unknown(
        override val message: String,
    ) : UiError {
        override val isRetryable: Boolean = false
    }
}

class DomainErrorException(val uiError: UiError) : Exception(uiError.message)
