package ua.kpi.iot_lighting.prac4.domain.model

data class LightDevice(
    val id: Long,
    val name: String,
    val zone: String,
    val powerW: Int,
    val isPoweredOn: Boolean,
    val latitude: Double = DEFAULT_LAT,
    val longitude: Double = DEFAULT_LNG,
    val lastUpdatedAt: Long = 0L,
) {
    companion object {
        const val DEFAULT_LAT = 47.9635
        const val DEFAULT_LNG = 33.4325
    }
}
