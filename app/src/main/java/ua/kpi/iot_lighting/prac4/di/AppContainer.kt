package ua.kpi.iot_lighting.prac4.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import ua.kpi.iot_lighting.prac4.data.auth.TokenStorage
import ua.kpi.iot_lighting.prac4.data.auth.TokenStore
import ua.kpi.iot_lighting.prac4.data.error.AndroidErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.AndroidNetworkChecker
import ua.kpi.iot_lighting.prac4.data.error.ErrorLogger
import ua.kpi.iot_lighting.prac4.data.error.NetworkChecker
import ua.kpi.iot_lighting.prac4.data.local.AppDatabase
import ua.kpi.iot_lighting.prac4.data.local.CacheMetadata
import ua.kpi.iot_lighting.prac4.data.local.SharedPreferencesCacheMetadata
import ua.kpi.iot_lighting.prac4.data.location.FusedLocationDataSource
import ua.kpi.iot_lighting.prac4.data.location.LocationDataSource
import ua.kpi.iot_lighting.prac4.data.remote.RetrofitFactory
import ua.kpi.iot_lighting.prac4.data.repository.AuthRepositoryImpl
import ua.kpi.iot_lighting.prac4.data.repository.LightingRepositoryImpl
import ua.kpi.iot_lighting.prac4.data.repository.WeatherRepositoryImpl
import ua.kpi.iot_lighting.prac4.data.source.OpenMeteoWeatherDataSource
import ua.kpi.iot_lighting.prac4.data.source.RetrofitLightingRemoteDataSource
import ua.kpi.iot_lighting.prac4.data.source.RoomLightingLocalDataSource
import ua.kpi.iot_lighting.prac4.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac4.domain.logic.PowerSimulator
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac4.domain.repository.LightingRepository
import ua.kpi.iot_lighting.prac4.domain.repository.WeatherRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tokenStorage: TokenStorage by lazy { TokenStore(appContext) }
    val cacheMetadata: CacheMetadata by lazy { SharedPreferencesCacheMetadata(appContext) }
    val networkChecker: NetworkChecker by lazy { AndroidNetworkChecker(appContext) }
    val errorLogger: ErrorLogger by lazy { AndroidErrorLogger(appContext) }

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val lightingApi by lazy {
        RetrofitFactory.createLightingApi(
            bearerHeaderProvider = { authRepository.tokenHeader() },
        )
    }

    private val weatherApi by lazy {
        RetrofitFactory.createWeatherApi()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            api = lightingApi,
            tokenStorage = tokenStorage,
            networkChecker = networkChecker,
            errorLogger = errorLogger,
        )
    }

    val lightingRepository: LightingRepository by lazy {
        LightingRepositoryImpl(
            local = RoomLightingLocalDataSource(database.lightDeviceDao()),
            remote = RetrofitLightingRemoteDataSource(lightingApi),
            cacheMetadata = cacheMetadata,
            networkChecker = networkChecker,
            errorLogger = errorLogger,
        )
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            remote = OpenMeteoWeatherDataSource(weatherApi),
            cacheMetadata = cacheMetadata,
            networkChecker = networkChecker,
            errorLogger = errorLogger,
        )
    }

    val locationDataSource: LocationDataSource by lazy {
        FusedLocationDataSource(appContext)
    }

    val powerSimulator: PowerSimulator by lazy {
        PowerSimulator(
            baseTotalsFlow = lightingRepository.observeDevices().map {
                LightingAdvisor.computeTotals(it)
            },
            scope = appScope,
        )
    }
}
