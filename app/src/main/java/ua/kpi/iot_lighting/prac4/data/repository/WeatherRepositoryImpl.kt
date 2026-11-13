package ua.kpi.iot_lighting.prac4.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac4.data.source.WeatherRemoteDataSource
import ua.kpi.iot_lighting.prac4.data.toDomain
import ua.kpi.iot_lighting.prac4.domain.model.DomainErrorException
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.domain.model.WeatherSyncResult
import ua.kpi.iot_lighting.prac4.domain.repository.WeatherRepository

class WeatherRepositoryImpl(
    private val remote: WeatherRemoteDataSource,
    private val cacheMetadata: CacheMetadata,
    private val networkChecker: NetworkChecker,
    private val errorLogger: ErrorLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : WeatherRepository {

    override suspend fun insight(): Result<WeatherInsight> {
        return try {
            val fresh = fetchAndCacheWeather(enableInternalRetry = true)
            Result.success(fresh)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            errorLogger.logError(TAG, t, "insight")
            Result.failure(DomainErrorException(ErrorHandler.toUiError(t)))
        }
    }

    /**
     * Періодичний холодний Flow погоди Open-Meteo (ПЗ13):
     * - Використовує retryWhen для тимчасових помилок;
     * - За остаточної невдачі перехоплює помилку у catch і повертає кеш,
     *   НЕ перериваючи цикл періодичних оновлень.
     */
    override fun observeWeatherPeriodic(intervalMs: Long): Flow<WeatherSyncResult> = flow {
        while (currentCoroutineContext().isActive) {
            val singleAttempt: Flow<WeatherSyncResult> = flow {
                val insight = fetchAndCacheWeather(enableInternalRetry = false)
                emit(WeatherSyncResult.Fresh(insight))
            }.retryWhen { cause, attempt ->
                errorLogger.logError(TAG, cause, "weather-retry-$attempt")
                val shouldRetry = ErrorHandler.isTransient(cause) && attempt < MAX_PERIODIC_RETRIES
                if (shouldRetry) {
                    delay(RETRY_BASE_DELAY_MS * (attempt + 1))
                }
                shouldRetry
            }.catch { cause ->
                errorLogger.logError(TAG, cause, "weather-periodic-fallback")
                val cached = cacheMetadata.readWeatherCache()
                val message = ErrorHandler.toUiError(cause).message
                emit(WeatherSyncResult.CachedFallback(insight = cached, message = message))
            }

            singleAttempt.collect { emit(it) }
            delay(intervalMs)
        }
    }.flowOn(ioDispatcher)

    private suspend fun fetchAndCacheWeather(enableInternalRetry: Boolean): WeatherInsight {
        if (!networkChecker.isOnline()) {
            throw DomainErrorException(
                UiError.Network(
                    message = "Погода офлайн — використовується збережений знімок",
                    isRetryable = false,
                ),
            )
        }
        val dto = if (enableInternalRetry) {
            retryWithBackoff(
                onRetry = { attempt, err ->
                    errorLogger.logError(TAG, err, "weather-retry-$attempt")
                },
            ) {
                remote.fetchCurrentWeather()
            }
        } else {
            remote.fetchCurrentWeather()
        }
        val insight = dto.toDomain(clockMillis())
        cacheMetadata.saveWeatherCache(insight)
        return insight
    }

    companion object {
        private const val TAG = "WeatherRepository"
        private const val MAX_PERIODIC_RETRIES = 2L
        private const val RETRY_BASE_DELAY_MS = 400L
    }
}
