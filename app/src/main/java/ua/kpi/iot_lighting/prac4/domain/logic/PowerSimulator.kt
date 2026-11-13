package ua.kpi.iot_lighting.prac4.domain.logic

import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import ua.kpi.iot_lighting.prac4.domain.model.LightingTotals

/**
 * Гарячий телеметричний потік [StateFlow], який імітує коливання навантаження (±5%)
 * кожні [intervalMs] мілісекунд та миттєво реагує на зміну активних вузлів (ПЗ13).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PowerSimulator(
    baseTotalsFlow: Flow<LightingTotals>,
    scope: CoroutineScope,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val jitterFactor: () -> Double = { 0.95 + Random.nextDouble() * 0.10 },
) {
    private val tickFlow: Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(intervalMs)
        }
    }

    val livePowerW: StateFlow<Int> = baseTotalsFlow
        .map { it.activePowerW }
        .distinctUntilChanged()
        .flatMapLatest { basePowerW ->
            if (basePowerW <= 0) {
                flowOf(0)
            } else {
                tickFlow.map {
                    (basePowerW * jitterFactor()).roundToInt().coerceAtLeast(1)
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = 0,
        )

    companion object {
        const val DEFAULT_INTERVAL_MS = 3_000L
    }
}
