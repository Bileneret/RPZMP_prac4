package ua.kpi.iot_lighting.prac4.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac4.data.source.LightingLocalDataSource
import ua.kpi.iot_lighting.prac4.data.source.LightingRemoteDataSource
import ua.kpi.iot_lighting.prac4.data.toDomain
import ua.kpi.iot_lighting.prac4.data.toEntity
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.repository.LightingRepository

class LightingRepositoryImpl(
    private val local: LightingLocalDataSource,
    private val remote: LightingRemoteDataSource,
    private val cacheMetadata: CacheMetadata,
    private val networkChecker: NetworkChecker,
    private val errorLogger: ErrorLogger,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : LightingRepository {

    override fun observeDevices(): Flow<List<LightDevice>> {
        return local.observeDevices().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncDevices(): Result<Unit> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(
                    UiError.Network(
                        message = "Офлайн-режим — показано локальний кеш Room",
                        isRetryable = false,
                    ),
                ),
            )
        }
        return try {
            val remoteList = retryWithBackoff(
                onRetry = { attempt, err ->
                    errorLogger.logError("LightingRepository", err, "sync-retry-$attempt")
                },
            ) {
                remote.getLights()
            }
            val now = clockMillis()
            local.replaceAll(remoteList.map { it.toEntity(now) })
            cacheMetadata.markSyncedNow(now)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError("LightingRepository", t, "syncDevices")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun setPower(id: Long, poweredOn: Boolean): Result<Unit> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(
                    UiError.Network(
                        message = "Немає зʼєднання для перемикання вузла",
                        isRetryable = false,
                    ),
                ),
            )
        }
        return try {
            val updatedDto = retryWithBackoff(
                onRetry = { attempt, err ->
                    errorLogger.logError("LightingRepository", err, "power-retry-$attempt")
                },
            ) {
                remote.setPower(id = id, poweredOn = poweredOn)
            }
            val now = clockMillis()
            local.upsert(updatedDto.toEntity(now))
            cacheMetadata.markSyncedNow(now)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError("LightingRepository", t, "setPower($id)")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun addDevice(
        name: String,
        zone: String,
        powerW: Int,
        latitude: Double,
        longitude: Double,
    ): Result<LightDevice> {
        val trimmedName = name.trim()
        val trimmedZone = zone.trim()
        if (trimmedName.isEmpty() || trimmedZone.isEmpty() || powerW <= 0) {
            return Result.failure(
                DomainErrorException(
                    UiError.Validation("Перевірте назву, зону та додатну потужність (Вт)"),
                ),
            )
        }
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(
                    UiError.Network("Немає мережі для додавання світильника", isRetryable = false),
                ),
            )
        }
        return try {
            val created = retryWithBackoff {
                remote.createLight(
                    name = trimmedName,
                    zone = trimmedZone,
                    powerW = powerW,
                    latitude = latitude,
                    longitude = longitude,
                )
            }
            val now = clockMillis()
            val entity = created.toEntity(now)
            local.upsert(entity)
            cacheMetadata.markSyncedNow(now)
            Result.success(entity.toDomain())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError("LightingRepository", t, "addDevice")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun deleteDevice(id: Long): Result<Unit> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(
                    UiError.Network("Немає мережі для видалення вузла", isRetryable = false),
                ),
            )
        }
        return try {
            retryWithBackoff { remote.deleteLight(id) }
            local.deleteById(id)
            cacheMetadata.markSyncedNow(clockMillis())
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError("LightingRepository", t, "deleteDevice($id)")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun lastSyncedAtMillis(): Long? = cacheMetadata.lastSyncedAtMillis()

    override suspend fun clearLocalCache() {
        local.clearAll()
        cacheMetadata.clearAll()
    }

    override suspend fun faultMode(): Result<String> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(UiError.Network("Немає мережі", isRetryable = false)),
            )
        }
        return try {
            Result.success(remote.getFaultMode())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    override suspend fun setFaultMode(mode: String): Result<Unit> {
        if (!networkChecker.isOnline()) {
            return Result.failure(
                DomainErrorException(UiError.Network("Немає мережі", isRetryable = false)),
            )
        }
        return try {
            remote.setFaultMode(mode)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }
}
