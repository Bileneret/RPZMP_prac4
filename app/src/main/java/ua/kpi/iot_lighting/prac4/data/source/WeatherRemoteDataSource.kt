package ua.kpi.iot_lighting.prac4.data.source

import ua.kpi.iot_lighting.prac4.data.remote.WeatherApiService
import ua.kpi.iot_lighting.prac4.data.remote.dto.WeatherResponseDto

interface WeatherRemoteDataSource {
    suspend fun fetchCurrentWeather(): WeatherResponseDto
}

class OpenMeteoWeatherDataSource(
    private val api: WeatherApiService,
) : WeatherRemoteDataSource {
    override suspend fun fetchCurrentWeather(): WeatherResponseDto = api.currentWeather()
}
