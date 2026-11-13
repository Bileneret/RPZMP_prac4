package ua.kpi.iot_lighting.prac4.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LightDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("zone") val zone: String,
    @SerializedName("powerW") val powerW: Int,
    @SerializedName("isPoweredOn") val isPoweredOn: Boolean,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("lastUpdatedAt") val lastUpdatedAt: Long? = null,
)

data class CreateLightRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("zone") val zone: String,
    @SerializedName("powerW") val powerW: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
)

data class PowerUpdateRequestDto(
    @SerializedName("isPoweredOn") val isPoweredOn: Boolean,
)

data class FaultModeDto(
    @SerializedName("mode") val mode: String,
)
