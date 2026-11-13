package ua.kpi.iot_lighting.prac4.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight

class LightingAdvisorTest {
    private val sampleDevices = listOf(
        LightDevice(1L, "Lamp 1", "Вулиця", 120, isPoweredOn = true),
        LightDevice(2L, "Lamp 2", "Парк", 80, isPoweredOn = false),
        LightDevice(3L, "Lamp 3", "Фасад", 150, isPoweredOn = true),
    )

    @Test
    fun `computeTotals aggregates active power and daily energy forecast accurately`() {
        val totals = LightingAdvisor.computeTotals(sampleDevices)
        assertEquals(3, totals.totalCount)
        assertEquals(2, totals.activeCount)
        assertEquals(270, totals.activePowerW)
        assertEquals(350, totals.totalPowerW)
        // 270 Вт * 12 год / 1000 = 3.24 кВт·год; * 4.5 грн = 14.58 грн
        assertEquals(3.24, totals.energyKwhPerDay, 0.001)
        assertEquals(14.58, totals.costPerDay, 0.001)
    }

    @Test
    fun `recommendation warns during bright daylight with active lamps`() {
        val totals = LightingAdvisor.computeTotals(sampleDevices)
        val clearDay = WeatherInsight(
            temperatureC = 18.0,
            cloudCoverPercent = 15,
            isDay = true,
            windSpeedKmh = 6.0,
            fetchedAtMillis = 1000L,
            summary = "Ясно",
        )
        assertTrue(clearDay.daylightPercent > 80)
        val message = LightingAdvisor.recommendation(totals, clearDay)
        assertTrue(message.contains("пригасити"))
    }

    @Test
    fun `recommendation handles empty device list`() {
        val totals = LightingAdvisor.computeTotals(emptyList())
        val message = LightingAdvisor.recommendation(totals, null)
        assertTrue(message.contains("порожній"))
    }
}
