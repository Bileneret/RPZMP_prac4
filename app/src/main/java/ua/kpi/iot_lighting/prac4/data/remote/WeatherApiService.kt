package ua.kpi.iot_lighting.prac4.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import ua.kpi.iot_lighting.prac4.data.remote.dto.WeatherResponseDto

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") latitude: Double = ServerConfig.DEFAULT_LAT,
        @Query("longitude") longitude: Double = ServerConfig.DEFAULT_LNG,
        @Query("current") current: String = "temperature_2m,cloud_cover,is_day,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
    ): WeatherResponseDto
}
