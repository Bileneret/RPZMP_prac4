package ua.kpi.iot_lighting.prac4.data.repository

import java.io.IOException
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac4.data.remote.dto.CurrentWeatherDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.WeatherResponseDto
import ua.kpi.iot_lighting.prac4.data.source.WeatherRemoteDataSource
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.domain.model.WeatherSyncResult

class WeatherRepositoryImplTest {

    private class MemoryWeatherCache : CacheMetadata {
        var cachedWeather: WeatherInsight? = null
        override fun lastSyncedAtMillis(): Long? = null
        override fun markSyncedNow(timestampMillis: Long) = Unit
        override fun clearSyncTimestamp() = Unit
        override fun readWeatherCache() = cachedWeather
        override fun saveWeatherCache(insight: WeatherInsight) { cachedWeather = insight }
        override fun clearAll() { cachedWeather = null }
    }

    @Test
    fun `observeWeatherPeriodic emits fresh then retries transient failure and falls back to cache without terminating`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cache = MemoryWeatherCache()
        var callCount = 0

        val remote = object : WeatherRemoteDataSource {
            override suspend fun fetchCurrentWeather(): WeatherResponseDto {
                callCount += 1
                // Tick 1: call 1 -> Fresh (cached)
                // Tick 2: calls 2, 3, 4 fail transiently -> 1 initial + 2 retries = 3 attempts -> CachedFallback
                // Tick 3: call 5 -> Fresh again (loop survived!)
                return when (callCount) {
                    1 -> WeatherResponseDto(CurrentWeatherDto(temperatureC = 14.0, cloudCoverPercent = 20, isDay = 1, windSpeedKmh = 5.0))
                    2, 3, 4 -> throw IOException("temporary weather outage #$callCount")
                    else -> WeatherResponseDto(CurrentWeatherDto(temperatureC = 15.0, cloudCoverPercent = 10, isDay = 1, windSpeedKmh = 4.0))
                }
            }
        }

        val repo = WeatherRepositoryImpl(
            remote = remote,
            cacheMetadata = cache,
            networkChecker = object : NetworkChecker { override fun isOnline() = true },
            errorLogger = object : ErrorLogger { override fun logError(tag: String, error: Throwable, context: String?) = Unit },
            ioDispatcher = dispatcher,
            clockMillis = { 123_000L },
        )

        val emissions = repo.observeWeatherPeriodic(intervalMs = 5_000L)
            .take(3)
            .toList()

        assertEquals(3, emissions.size)
        assertTrue(emissions[0] is WeatherSyncResult.Fresh)
        val fallback = emissions[1] as WeatherSyncResult.CachedFallback
        assertEquals(14.0, fallback.insight?.temperatureC ?: 0.0, 0.01)
        assertTrue(emissions[2] is WeatherSyncResult.Fresh)
        assertEquals(15.0, (emissions[2] as WeatherSyncResult.Fresh).insight.temperatureC, 0.01)
        assertEquals(5, callCount)
    }
}
