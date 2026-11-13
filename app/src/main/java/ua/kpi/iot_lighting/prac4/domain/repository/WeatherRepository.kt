package ua.kpi.iot_lighting.prac4.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.domain.model.WeatherSyncResult

interface WeatherRepository {
    suspend fun insight(): Result<WeatherInsight>

    fun observeWeatherPeriodic(
        intervalMs: Long = DEFAULT_WEATHER_INTERVAL_MS,
    ): Flow<WeatherSyncResult>

    companion object {
        const val DEFAULT_WEATHER_INTERVAL_MS = 60_000L
    }
}
