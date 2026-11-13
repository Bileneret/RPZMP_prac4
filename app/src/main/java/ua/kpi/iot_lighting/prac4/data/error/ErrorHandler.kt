package ua.kpi.iot_lighting.prac4.data.error

import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.UiError

object ErrorHandler {
    fun toUiError(throwable: Throwable): UiError {
        return when (throwable) {
            is DomainErrorException -> throwable.uiError
            is HttpException -> mapHttpError(throwable)
            is SocketTimeoutException -> UiError.Network(
                message = "Сервер не відповів вчасно (таймаут)",
                isRetryable = true,
            )
            is UnknownHostException, is ConnectException, is IOException -> UiError.Network(
                message = "Немає зʼєднання з сервером — показано локальний кеш",
                isRetryable = true,
            )
            is JsonSyntaxException, is JsonParseException -> UiError.Server(
                code = 502,
                message = "Некоректний формат даних від сервера",
                isRetryable = false,
            )
            else -> UiError.Unknown(
                message = throwable.message?.takeIf { it.isNotBlank() }
                    ?: "Неочікувана помилка операції",
            )
        }
    }

    fun isTransient(throwable: Throwable): Boolean {
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is IOException, -> true
            is HttpException -> throwable.code() == 429 || throwable.code() >= 500
            is DomainErrorException -> throwable.uiError.isRetryable
            else -> false
        }
    }

    private fun mapHttpError(ex: HttpException): UiError {
        val code = ex.code()
        return when (code) {
            401, 403 -> UiError.Unauthorized(
                message = if (code == 401) {
                    "Сесію відхилено або закінчився токен (HTTP 401)"
                } else {
                    "Недостатньо прав для цієї дії (HTTP 403)"
                },
            )
            400, 422 -> UiError.Validation(
                message = "Сервер відхилив параметри запиту (HTTP $code)",
            )
            429 -> UiError.Server(
                code = code,
                message = "Забагато запитів — повторюємо спробу (HTTP 429)",
                isRetryable = true,
            )
            in 500..599 -> UiError.Server(
                code = code,
                message = "Тимчасовий збій сервера (HTTP $code)",
                isRetryable = true,
            )
            else -> UiError.Server(
                code = code,
                message = "Помилка HTTP $code",
                isRetryable = false,
            )
        }
    }
}
