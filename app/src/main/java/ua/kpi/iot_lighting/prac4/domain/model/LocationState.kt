package ua.kpi.iot_lighting.prac4.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

sealed interface LocationState {
    data object PermissionRequired : LocationState
    data object Loading : LocationState
    data class Available(
        val point: GeoPoint,
        val accuracyMeters: Float,
        val updatedAtMillis: Long,
    ) : LocationState
    data class Unavailable(val reason: String) : LocationState
}
