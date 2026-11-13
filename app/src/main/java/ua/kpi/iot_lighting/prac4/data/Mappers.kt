package ua.kpi.iot_lighting.prac4.data

import kotlin.math.roundToInt
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.WeatherResponseDto
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight

fun LightDeviceEntity.toDomain(): LightDevice = LightDevice(
    id = id,
    name = name,
    zone = zone,
    powerW = powerW,
    isPoweredOn = isPoweredOn,
    latitude = latitude,
    longitude = longitude,
    lastUpdatedAt = lastUpdatedAt,
)

fun LightDevice.toEntity(): LightDeviceEntity = LightDeviceEntity(
    id = id,
    name = name,
    zone = zone,
    powerW = powerW,
    isPoweredOn = isPoweredOn,
    latitude = latitude,
    longitude = longitude,
    lastUpdatedAt = lastUpdatedAt,
)

fun LightDto.toEntity(nowMillis: Long = System.currentTimeMillis()): LightDeviceEntity = LightDeviceEntity(
    id = id,
    name = name.trim(),
    zone = zone.trim(),
    powerW = powerW.coerceAtLeast(0),
    isPoweredOn = isPoweredOn,
    latitude = latitude ?: LightDevice.DEFAULT_LAT,
    longitude = longitude ?: LightDevice.DEFAULT_LNG,
    lastUpdatedAt = lastUpdatedAt ?: nowMillis,
)

fun LightDto.toDomain(nowMillis: Long = System.currentTimeMillis()): LightDevice =
    toEntity(nowMillis).toDomain()

fun WeatherResponseDto.toDomain(nowMillis: Long = System.currentTimeMillis()): WeatherInsight {
    val block = current
    val temp = block?.temperatureC ?: 0.0
    val clouds = (block?.cloudCoverPercent ?: 0).coerceIn(0, 100)
    val isDay = (block?.isDay ?: 1) == 1
    val wind = block?.windSpeedKmh ?: 0.0
    val skyLabel = when {
        !isDay -> "Темний час доби"
        clouds < 30 -> "Ясна світла пора"
        clouds < 70 -> "Мінлива хмарність"
        else -> "Щільна хмарність"
    }
    val summary = "$skyLabel · ${temp.roundToInt()}°C · хмарність $clouds%"
    return WeatherInsight(
        temperatureC = temp,
        cloudCoverPercent = clouds,
        isDay = isDay,
        windSpeedKmh = wind,
        fetchedAtMillis = nowMillis,
        summary = summary,
    )
}
