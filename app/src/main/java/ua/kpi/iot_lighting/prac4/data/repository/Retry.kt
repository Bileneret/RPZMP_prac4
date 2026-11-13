package ua.kpi.iot_lighting.prac4.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler

suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 250L,
    backoffFactor: Double = 2.0,
    delayFn: suspend (Long) -> Unit = { delay(it) },
    shouldRetry: (Throwable) -> Boolean = ErrorHandler::isTransient,
    onRetry: (attempt: Int, error: Throwable) -> Unit = { _, _ -> },
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts має бути >= 1" }
    var currentDelay = initialDelayMs
    var lastError: Throwable? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            lastError = t
            if (attempt >= maxAttempts || !shouldRetry(t)) {
                throw t
            }
            onRetry(attempt, t)
            delayFn(currentDelay)
            currentDelay = (currentDelay * backoffFactor).toLong().coerceAtLeast(0L)
        }
    }
    throw lastError ?: IllegalStateException("Retry exhausted without exception")
}
