package ua.kpi.iot_lighting.prac4.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponseDto(
    @SerializedName("current") val current: CurrentWeatherDto?,
)

data class CurrentWeatherDto(
    @SerializedName("time") val time: String? = null,
    @SerializedName("temperature_2m") val temperatureC: Double? = null,
    @SerializedName("cloud_cover") val cloudCoverPercent: Int? = null,
    @SerializedName("is_day") val isDay: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeedKmh: Double? = null,
)
