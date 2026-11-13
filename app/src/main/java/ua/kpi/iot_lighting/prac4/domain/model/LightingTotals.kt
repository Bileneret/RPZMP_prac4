package ua.kpi.iot_lighting.prac4.domain.model

data class LightingTotals(
    val totalCount: Int,
    val activeCount: Int,
    val activePowerW: Int,
    val totalPowerW: Int,
    val energyKwhPerDay: Double = (activePowerW * HOURS_PER_DAY) / 1000.0,
    val costPerDay: Double = energyKwhPerDay * TARIFF_UAH_PER_KWH,
) {
    companion object {
        const val HOURS_PER_DAY = 12.0
        const val TARIFF_UAH_PER_KWH = 4.5

        val EMPTY = LightingTotals(
            totalCount = 0,
            activeCount = 0,
            activePowerW = 0,
            totalPowerW = 0,
            energyKwhPerDay = 0.0,
            costPerDay = 0.0,
        )
    }
}
