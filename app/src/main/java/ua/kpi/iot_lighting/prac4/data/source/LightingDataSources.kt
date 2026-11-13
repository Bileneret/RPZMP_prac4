package ua.kpi.iot_lighting.prac4.data.source

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto

interface LightingLocalDataSource {
    fun observeDevices(): Flow<List<LightDeviceEntity>>
    suspend fun getDevices(): List<LightDeviceEntity>
    suspend fun replaceAll(devices: List<LightDeviceEntity>)
    suspend fun upsert(device: LightDeviceEntity)
    suspend fun setPower(id: Long, poweredOn: Boolean, lastUpdatedAt: Long)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}

interface LightingRemoteDataSource {
    suspend fun getLights(): List<LightDto>
    suspend fun createLight(
        name: String,
        zone: String,
        powerW: Int,
        latitude: Double,
        longitude: Double,
    ): LightDto
    suspend fun setPower(id: Long, poweredOn: Boolean): LightDto
    suspend fun deleteLight(id: Long)
    suspend fun getFaultMode(): String
    suspend fun setFaultMode(mode: String)
}
