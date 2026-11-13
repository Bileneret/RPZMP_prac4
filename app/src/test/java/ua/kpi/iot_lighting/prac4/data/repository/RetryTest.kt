package ua.kpi.iot_lighting.prac4.data.repository

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryTest {
    @Test
    fun `retries transient errors and succeeds on third attempt`() = runTest {
        var calls = 0
        val delays = mutableListOf<Long>()
        val result = retryWithBackoff(
            maxAttempts = 3,
            initialDelayMs = 100L,
            backoffFactor = 2.0,
            delayFn = { delays.add(it) },
        ) {
            calls += 1
            if (calls < 3) throw IOException("transient $calls")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(listOf(100L, 200L), delays)
    }

    @Test
    fun `does not retry non-transient error`() = runTest {
        var calls = 0
        val thrown = runCatching {
            retryWithBackoff(
                maxAttempts = 3,
                delayFn = {},
            ) {
                calls += 1
                throw IllegalArgumentException("bad")
            }
        }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)
        assertEquals(1, calls)
    }

    @Test
    fun `propagates CancellationException immediately`() = runTest {
        var calls = 0
        val thrown = runCatching {
            retryWithBackoff(maxAttempts = 3, delayFn = {}) {
                calls += 1
                throw CancellationException("cancelled")
            }
        }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
        assertEquals(1, calls)
    }
}
