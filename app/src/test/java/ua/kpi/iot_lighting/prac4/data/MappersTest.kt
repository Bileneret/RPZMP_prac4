package ua.kpi.iot_lighting.prac4.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac4.data.remote.dto.CurrentWeatherDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.remote.dto.WeatherResponseDto
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice

class MappersTest {
    @Test
    fun `LightDeviceEntity maps back and forth preserving coordinates and timestamps`() {
        val domain = LightDevice(
            id = 42L,
            name = "Опора #42",
            zone = "Вулиця",
            powerW = 150,
            isPoweredOn = true,
            latitude = 47.915,
            longitude = 33.401,
            lastUpdatedAt = 999_000L,
        )
        val roundTrip = domain.toEntity().toDomain()
        assertEquals(domain, roundTrip)
    }

    @Test
    fun `LightDto falls back to Kryvyi Rih defaults when coordinates omitted`() {
        val dto = LightDto(id = 7L, name = " Новий ", zone = " Фасад ", powerW = 90, isPoweredOn = false)
        val entity = dto.toEntity(nowMillis = 123_456L)
        assertEquals("Новий", entity.name)
        assertEquals("Фасад", entity.zone)
        assertEquals(LightDevice.DEFAULT_LAT, entity.latitude, 0.0001)
        assertEquals(LightDevice.DEFAULT_LNG, entity.longitude, 0.0001)
        assertEquals(123_456L, entity.lastUpdatedAt)
        assertEquals(LightDeviceEntity::class, entity::class)
    }

    @Test
    fun `WeatherResponseDto maps night and cloudiness accurately`() {
        val dto = WeatherResponseDto(
            current = CurrentWeatherDto(
                temperatureC = 6.4,
                cloudCoverPercent = 85,
                isDay = 0,
                windSpeedKmh = 12.5,
            ),
        )
        val insight = dto.toDomain(nowMillis = 55_000L)
        assertFalse(insight.isDay)
        assertEquals(85, insight.cloudCoverPercent)
        assertTrue(insight.summary.contains("Темний час доби"))
    }
}
