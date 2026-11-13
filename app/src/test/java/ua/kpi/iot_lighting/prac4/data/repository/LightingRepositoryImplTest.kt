package ua.kpi.iot_lighting.prac4.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac4.data.local.LightDeviceEntity
import ua.kpi.iot_lighting.prac4.data.remote.dto.LightDto
import ua.kpi.iot_lighting.prac4.data.source.LightingLocalDataSource
import ua.kpi.iot_lighting.prac4.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight

class LightingRepositoryImplTest {

    private class FakeLocal : LightingLocalDataSource {
        val items = MutableStateFlow<List<LightDeviceEntity>>(emptyList())
        override fun observeDevices(): Flow<List<LightDeviceEntity>> = items
        override suspend fun getDevices() = items.value
        override suspend fun replaceAll(devices: List<LightDeviceEntity>) { items.value = devices }
        override suspend fun upsert(device: LightDeviceEntity) {
            items.value = items.value.filterNot { it.id == device.id } + device
        }
        override suspend fun setPower(id: Long, poweredOn: Boolean, lastUpdatedAt: Long) {
            items.value = items.value.map {
                if (it.id == id) it.copy(isPoweredOn = poweredOn, lastUpdatedAt = lastUpdatedAt) else it
            }
        }
        override suspend fun deleteById(id: Long) {
            items.value = items.value.filterNot { it.id == id }
        }
        override suspend fun clearAll() { items.value = emptyList() }
    }

    private class MemoryCache : CacheMetadata {
        var syncTs: Long? = null
        var weather: WeatherInsight? = null
        override fun lastSyncedAtMillis() = syncTs
        override fun markSyncedNow(timestampMillis: Long) { syncTs = timestampMillis }
        override fun clearSyncTimestamp() { syncTs = null }
        override fun readWeatherCache() = weather
        override fun saveWeatherCache(insight: WeatherInsight) { weather = insight }
        override fun clearAll() { syncTs = null; weather = null }
    }

    @Test
    fun `syncDevices writes remote data to local store and records timestamp`() = runTest {
        val local = FakeLocal()
        val cache = MemoryCache()
        val remote = object : LightingRemoteDataSource {
            override suspend fun getLights() = listOf(
                LightDto(1L, "Lamp 1", "Вулиця", 120, true, 47.91, 33.39, 500L),
            )
            override suspend fun createLight(name: String, zone: String, powerW: Int, latitude: Double, longitude: Double) =
                LightDto(2L, name, zone, powerW, true, latitude, longitude, 600L)
            override suspend fun setPower(id: Long, poweredOn: Boolean) =
                LightDto(id, "Lamp 1", "Вулиця", 120, poweredOn, 47.91, 33.39, 700L)
            override suspend fun deleteLight(id: Long) = Unit
            override suspend fun getFaultMode() = "none"
            override suspend fun setFaultMode(mode: String) = Unit
        }
        val repo = LightingRepositoryImpl(
            local = local,
            remote = remote,
            cacheMetadata = cache,
            networkChecker = object : NetworkChecker { override fun isOnline() = true },
            errorLogger = object : ErrorLogger { override fun logError(tag: String, error: Throwable, context: String?) = Unit },
            clockMillis = { 10_000L },
        )

        assertTrue(repo.syncDevices().isSuccess)
        val devices = repo.observeDevices().first()
        assertEquals(1, devices.size)
        assertEquals(10_000L, repo.lastSyncedAtMillis())

        assertTrue(repo.setPower(1L, false).isSuccess)
        val updated = repo.observeDevices().first().single()
        assertFalse(updated.isPoweredOn)
        assertEquals(700L, updated.lastUpdatedAt)
    }

    @Test
    fun `addDevice validates invalid arguments before calling network`() = runTest {
        val repo = LightingRepositoryImpl(
            local = FakeLocal(),
            remote = object : LightingRemoteDataSource {
                override suspend fun getLights() = emptyList<LightDto>()
                override suspend fun createLight(name: String, zone: String, powerW: Int, latitude: Double, longitude: Double) =
                    error("should not be called")
                override suspend fun setPower(id: Long, poweredOn: Boolean) = error("unused")
                override suspend fun deleteLight(id: Long) = Unit
                override suspend fun getFaultMode() = "none"
                override suspend fun setFaultMode(mode: String) = Unit
            },
            cacheMetadata = MemoryCache(),
            networkChecker = object : NetworkChecker { override fun isOnline() = true },
            errorLogger = object : ErrorLogger { override fun logError(tag: String, error: Throwable, context: String?) = Unit },
        )

        val result = repo.addDevice("", "Вулиця", 0, 47.91, 33.39)
        assertTrue(result.isFailure)
        val uiErr = (result.exceptionOrNull() as DomainErrorException).uiError
        assertTrue(uiErr is UiError.Validation)
    }

    @Test
    fun `offline sync leaves local cache intact and returns Network error`() = runTest {
        val local = FakeLocal().apply {
            items.value = listOf(LightDeviceEntity(1L, "Cached", "Парк", 50, true))
        }
        val repo = LightingRepositoryImpl(
            local = local,
            remote = object : LightingRemoteDataSource {
                override suspend fun getLights() = error("should not be called")
                override suspend fun createLight(name: String, zone: String, powerW: Int, latitude: Double, longitude: Double) = error("unused")
                override suspend fun setPower(id: Long, poweredOn: Boolean) = error("unused")
                override suspend fun deleteLight(id: Long) = Unit
                override suspend fun getFaultMode() = "none"
                override suspend fun setFaultMode(mode: String) = Unit
            },
            cacheMetadata = MemoryCache(),
            networkChecker = object : NetworkChecker { override fun isOnline() = false },
            errorLogger = object : ErrorLogger { override fun logError(tag: String, error: Throwable, context: String?) = Unit },
        )

        val res = repo.syncDevices()
        assertTrue(res.isFailure)
        assertEquals(1, repo.observeDevices().first().size)
    }
}
