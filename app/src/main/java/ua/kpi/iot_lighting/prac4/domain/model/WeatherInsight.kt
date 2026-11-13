package ua.kpi.iot_lighting.prac4.domain.model

import kotlin.math.roundToInt

data class WeatherInsight(
    val temperatureC: Double,
    val cloudCoverPercent: Int,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val fetchedAtMillis: Long,
    val summary: String,
    val daylightPercent: Int = if (!isDay) 0 else (100 - cloudCoverPercent * 0.85).roundToInt().coerceIn(5, 100),
)

sealed interface WeatherSyncResult {
    data class Fresh(val insight: WeatherInsight) : WeatherSyncResult
    data class CachedFallback(
        val insight: WeatherInsight?,
        val message: String,
    ) : WeatherSyncResult
}
