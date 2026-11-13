package ua.kpi.iot_lighting.prac4.data.source

import kotlinx.coroutines.flow.Flow
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceDao
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceEntity

class RoomLightingLocalDataSource(
    private val dao: LightDeviceDao,
) : LightingLocalDataSource {
    override fun observeDevices(): Flow<List<LightDeviceEntity>> = dao.observeAll()

    override suspend fun getDevices(): List<LightDeviceEntity> = dao.getAll()

    override suspend fun replaceAll(devices: List<LightDeviceEntity>) = dao.replaceAll(devices)

    override suspend fun upsert(device: LightDeviceEntity) = dao.upsert(device)

    override suspend fun setPower(id: Long, poweredOn: Boolean, lastUpdatedAt: Long) {
        dao.setPower(id = id, poweredOn = poweredOn, lastUpdatedAt = lastUpdatedAt)
    }

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun clearAll() = dao.clearAll()
}
