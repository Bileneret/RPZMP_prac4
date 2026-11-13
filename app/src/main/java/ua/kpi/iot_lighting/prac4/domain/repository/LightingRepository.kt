package ua.kpi.iot_lighting.prac4.domain.repository

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice

interface LightingRepository {
    fun observeDevices(): Flow<List<LightDevice>>

    suspend fun syncDevices(): Result<Unit>

    suspend fun setPower(id: Long, poweredOn: Boolean): Result<Unit>

    suspend fun addDevice(
        name: String,
        zone: String,
        powerW: Int,
        latitude: Double = LightDevice.DEFAULT_LAT,
        longitude: Double = LightDevice.DEFAULT_LNG,
    ): Result<LightDevice>

    suspend fun deleteDevice(id: Long): Result<Unit>

    suspend fun lastSyncedAtMillis(): Long?

    suspend fun clearLocalCache()

    suspend fun faultMode(): Result<String>

    suspend fun setFaultMode(mode: String): Result<Unit>
}
