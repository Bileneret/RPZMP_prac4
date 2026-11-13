package ua.kpi.iot_lighting.prac4.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice

class GeoCalculatorTest {
    @Test
    fun `distanceMeters between identical points is zero`() {
        val dist = GeoCalculator.distanceMeters(47.9635, 33.4325, 47.9635, 33.4325)
        assertEquals(0.0, dist, 0.01)
    }

    @Test
    fun `distanceMeters matches expected Kryvyi Rih span within tolerance`() {
        val dist = GeoCalculator.distanceMeters(47.9105, 33.3918, 47.8965, 33.3762)
        assertTrue("Expected ~1940m, got $dist", dist in 1800.0..2100.0)
    }

    @Test
    fun `findNearbyDistances filters devices by 1500m radius around Pokrovskyi district GPS`() {
        val userGps = GeoPoint(47.963458, 33.432501)
        val devices = listOf(
            LightDevice(1L, "5-й Зарічний #14", "Вулиця", 120, true, latitude = 47.9642, longitude = 33.4336),
            LightDevice(2L, "ТРЦ Терра Зарічний", "Паркування", 150, true, latitude = 47.9618, longitude = 33.4302),
            LightDevice(7L, "Парк Шахтарський", "Парк", 85, true, latitude = 47.9548, longitude = 33.4252),
            LightDevice(9L, "Вузол КРЕС", "Вулиця", 140, true, latitude = 47.9495, longitude = 33.4198),
            LightDevice(14L, "Сонячна Галерея", "Паркування", 110, true, latitude = 47.9288, longitude = 33.4148),
        )

        val nearby = GeoCalculator.findNearbyDistances(devices, userGps)
        assertTrue(1L in nearby)
        assertTrue(2L in nearby)
        assertTrue(7L in nearby)
        assertFalse(9L in nearby)
        assertFalse(14L in nearby)
        assertEquals(emptyMap<Long, Int>(), GeoCalculator.findNearbyDistances(devices, null))
    }
}
