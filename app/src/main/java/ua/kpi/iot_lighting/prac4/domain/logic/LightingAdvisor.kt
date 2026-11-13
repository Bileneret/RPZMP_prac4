package ua.kpi.iot_lighting.prac4.domain.logic

import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.LightingTotals
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight

object LightingAdvisor {
    fun computeTotals(devices: List<LightDevice>): LightingTotals {
        val active = devices.filter { it.isPoweredOn }
        return LightingTotals(
            totalCount = devices.size,
            activeCount = active.size,
            activePowerW = active.sumOf { it.powerW },
            totalPowerW = devices.sumOf { it.powerW },
        )
    }

    fun recommendation(
        totals: LightingTotals,
        weather: WeatherInsight?,
    ): String {
        if (totals.totalCount == 0) {
            return "Список вузлів порожній — виконайте синхронізацію або додайте світильник"
        }
        if (weather == null) {
            return if (totals.activeCount == 0) {
                "Усі світильники вимкнено — навантаження 0 Вт"
            } else {
                "Працює ${totals.activeCount} із ${totals.totalCount} вузлів (${totals.activePowerW} Вт)"
            }
        }
        return when {
            weather.isDay && weather.cloudCoverPercent < 40 && totals.activeCount > 0 ->
                "Ясна світла пора (${weather.cloudCoverPercent}% хмар) — доцільно пригасити вуличні лінії"
            weather.isDay && weather.cloudCoverPercent >= 75 && totals.activeCount == 0 ->
                "Щільна хмарність (${weather.cloudCoverPercent}%) — перевірте вхідні та складські зони"
            !weather.isDay && totals.activeCount == 0 ->
                "Темний час доби — рекомендовано увімкнути вуличне та паркувальне освітлення"
            !weather.isDay && totals.activePowerW > 600 ->
                "Нічний режим активний (${totals.activePowerW} Вт); за високої хмарності тримайте резерв"
            else ->
                "Оптимальний баланс освітлення: ${totals.activeCount}/${totals.totalCount} у роботі"
        }
    }
}
