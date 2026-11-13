package ua.kpi.iot_lighting.prac4.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice

@Entity(tableName = "light_devices")
data class LightDeviceEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val zone: String,
    val powerW: Int,
    val isPoweredOn: Boolean,
    val latitude: Double = LightDevice.DEFAULT_LAT,
    val longitude: Double = LightDevice.DEFAULT_LNG,
    val lastUpdatedAt: Long = 0L,
)
