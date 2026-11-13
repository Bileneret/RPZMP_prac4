package ua.kpi.iot_lighting.prac4.domain.logic

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.domain.model.LightingTotals

@OptIn(ExperimentalCoroutinesApi::class)
class PowerSimulatorTest {
    @Test
    fun `livePowerW emits 0 when activePowerW is 0 and updates immediately when base changes`() = runTest {
        val totals = MutableStateFlow(LightingTotals(totalCount = 2, activeCount = 0, activePowerW = 0, totalPowerW = 200))
        val sequence = listOf(0.95, 1.05, 1.00).iterator()
        val simulator = PowerSimulator(
            baseTotalsFlow = totals,
            scope = backgroundScope,
            intervalMs = 3_000L,
            jitterFactor = { if (sequence.hasNext()) sequence.next() else 1.02 },
        )

        runCurrent()
        assertEquals(0, simulator.livePowerW.value)

        totals.value = LightingTotals(totalCount = 2, activeCount = 2, activePowerW = 200, totalPowerW = 200)
        runCurrent()
        assertEquals(190, simulator.livePowerW.value)

        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(210, simulator.livePowerW.value)

        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(200, simulator.livePowerW.value)
    }

    @Test
    fun `default jitter stays within five percent bounds`() = runTest {
        val totals = MutableStateFlow(LightingTotals(totalCount = 1, activeCount = 1, activePowerW = 1000, totalPowerW = 1000))
        val simulator = PowerSimulator(
            baseTotalsFlow = totals,
            scope = backgroundScope,
            intervalMs = 3_000L,
        )
        runCurrent()
        repeat(10) {
            val reading = simulator.livePowerW.value
            assertTrue("Expected reading $reading in 950..1050", reading in 950..1050)
            advanceTimeBy(3_000L)
            runCurrent()
        }
    }
}
