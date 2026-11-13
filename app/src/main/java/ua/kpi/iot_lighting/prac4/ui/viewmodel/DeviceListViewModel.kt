package ua.kpi.iot_lighting.prac4.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac4.data.location.LocationDataSource
import ua.kpi.iot_lighting.prac4.domain.logic.GeoCalculator
import ua.kpi.iot_lighting.prac4.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac4.domain.logic.PowerSimulator
import ua.kpi.iot_lighting.prac4.domain.model.DeviceFilterSortLogic
import ua.kpi.iot_lighting.prac4.domain.model.DeviceSortOption
import ua.kpi.iot_lighting.prac4.domain.model.FilterSortState
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.LightingTotals
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import ua.kpi.iot_lighting.prac4.domain.model.StatusFilter
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.domain.model.WeatherSyncResult
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac4.domain.repository.LightingRepository
import ua.kpi.iot_lighting.prac4.domain.repository.WeatherRepository

data class DeviceListUiState(
    val allDevices: List<LightDevice> = emptyList(),
    val filteredDevices: List<LightDevice> = emptyList(),
    val availableZones: List<String> = emptyList(),
    val totals: LightingTotals = LightingTotals.EMPTY,
    val livePowerW: Int = 0,
    val recommendation: String = "",
    val weather: WeatherInsight? = null,
    val isWeatherCachedFallback: Boolean = false,
    val weatherStatusNote: String? = null,
    val filterSort: FilterSortState = FilterSortState(),
    val selectedDeviceId: Long? = null,
    val selectionSeq: Long = 0L,
    val locationState: LocationState = LocationState.PermissionRequired,
    val nearbyDistancesMeters: Map<Long, Int> = emptyMap(),
    val maxDevicePowerW: Int = 250,
    val pendingDeviceIds: Set<Long> = emptySet(),
    val isSyncing: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val faultMode: String = "none",
    val error: UiError? = null,
)

private data class ControlUiState(
    val selectedDeviceId: Long? = null,
    val selectionSeq: Long = 0L,
    val pendingDeviceIds: Set<Long> = emptySet(),
    val isSyncing: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val faultMode: String = "none",
    val error: UiError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DeviceListViewModel(
    private val lightingRepository: LightingRepository,
    private val weatherRepository: WeatherRepository,
    private val authRepository: AuthRepository,
    private val locationDataSource: LocationDataSource,
    private val powerSimulator: PowerSimulator,
    private val weatherIntervalMs: Long = WeatherRepository.DEFAULT_WEATHER_INTERVAL_MS,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    autoSyncOnStart: Boolean = true,
) : ViewModel() {

    private val rawFilterState = MutableStateFlow(FilterSortState())
    private val controlState = MutableStateFlow(ControlUiState())
    private val permissionGrantedFlow = MutableStateFlow(locationDataSource.hasLocationPermission())
    private val manualLocationOverride = MutableStateFlow<LocationState?>(null)

    private val debouncedQueryFlow: Flow<String> = rawFilterState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .debounce { query -> if (query.isBlank()) 0L else searchDebounceMs }

    private val effectiveFilterFlow: Flow<FilterSortState> = combine(
        rawFilterState,
        debouncedQueryFlow,
    ) { raw, debouncedQuery ->
        raw.copy(searchQuery = debouncedQuery)
    }

    private val weatherFlow: Flow<WeatherSyncResult?> = weatherRepository
        .observeWeatherPeriodic(intervalMs = weatherIntervalMs)
        .map<WeatherSyncResult, WeatherSyncResult?> { it }
        .onStart { emit(null) }

    /** Реактивний потік координат користувача, що перезапускається при зміні системного дозволу (ПЗ16). */
    private val locationStreamFlow: Flow<LocationState> = permissionGrantedFlow
        .flatMapLatest { granted ->
            manualLocationOverride.value = null
            locationDataSource.observeLocation(hasPermission = granted)
        }

    private val effectiveLocationFlow: Flow<LocationState> = combine(
        locationStreamFlow,
        manualLocationOverride,
    ) { streamed, override ->
        when {
            streamed is LocationState.PermissionRequired -> streamed
            streamed is LocationState.Available -> streamed
            override is LocationState.Available -> override
            else -> override ?: streamed
        }
    }

    /**
     * Об'єднаний стан екрана (ПЗ13): синхронно комбінує каталог Room, потік погоди Open-Meteo,
     * геолокацію FusedLocation, «гарячий» потік телеметрії PowerSimulator та фільтри з debounce (ПЗ15).
     */
    val state: StateFlow<DeviceListUiState> = combine(
        lightingRepository.observeDevices(),
        weatherFlow,
        effectiveLocationFlow,
        powerSimulator.livePowerW,
        combine(effectiveFilterFlow, controlState) { filter, control -> filter to control },
    ) { devices, weatherResult, locationState, livePowerW, (filterSort, control) ->
        val filtered = DeviceFilterSortLogic.apply(devices, filterSort)
        val totals = LightingAdvisor.computeTotals(devices)
        val weatherInsight = when (weatherResult) {
            is WeatherSyncResult.Fresh -> weatherResult.insight
            is WeatherSyncResult.CachedFallback -> weatherResult.insight
            null -> null
        }
        val isFallback = weatherResult is WeatherSyncResult.CachedFallback
        val weatherNote = (weatherResult as? WeatherSyncResult.CachedFallback)?.message
        val userPoint: GeoPoint? = (locationState as? LocationState.Available)?.point
        val nearby = GeoCalculator.findNearbyDistances(devices, userPoint)
        val zones = devices.map { it.zone }.distinct().sorted()
        val maxPower = (devices.maxOfOrNull { it.powerW } ?: 250).coerceAtLeast(100)

        DeviceListUiState(
            allDevices = devices,
            filteredDevices = filtered,
            availableZones = zones,
            totals = totals,
            livePowerW = livePowerW,
            recommendation = LightingAdvisor.recommendation(totals, weatherInsight),
            weather = weatherInsight,
            isWeatherCachedFallback = isFallback,
            weatherStatusNote = weatherNote,
            filterSort = filterSort.copy(searchQuery = rawFilterState.value.searchQuery),
            selectedDeviceId = control.selectedDeviceId,
            selectionSeq = control.selectionSeq,
            locationState = locationState,
            nearbyDistancesMeters = nearby,
            maxDevicePowerW = maxPower,
            pendingDeviceIds = control.pendingDeviceIds,
            isSyncing = control.isSyncing,
            lastSyncedAtMillis = control.lastSyncedAtMillis,
            faultMode = control.faultMode,
            error = control.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DeviceListUiState(),
    )

    init {
        viewModelScope.launch {
            val cachedTs = lightingRepository.lastSyncedAtMillis()
            if (cachedTs != null) {
                controlState.update { it.copy(lastSyncedAtMillis = cachedTs) }
            }
            if (autoSyncOnStart) {
                syncNow()
            }
        }
    }

    /** Синхронізує список світильників із сервером із одноразовим автоповтором за порожнього кешу (ПЗ13). */
    fun syncNow() {
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true, error = null) }
            var result = lightingRepository.syncDevices()
            // Якщо первинний запит після логіна обірвався за порожнього кешу, повторюємо один раз після паузи
            if (result.isFailure && state.value.allDevices.isEmpty()) {
                delay(350L)
                result = lightingRepository.syncDevices()
            }
            val fault = lightingRepository.faultMode().getOrNull()
            val lastSynced = lightingRepository.lastSyncedAtMillis()
            controlState.update { prev ->
                prev.copy(
                    isSyncing = false,
                    lastSyncedAtMillis = lastSynced ?: prev.lastSyncedAtMillis,
                    faultMode = fault ?: prev.faultMode,
                    error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                )
            }
        }
    }

    fun togglePower(device: LightDevice) {
        if (device.id in controlState.value.pendingDeviceIds) return
        viewModelScope.launch {
            controlState.update {
                it.copy(pendingDeviceIds = it.pendingDeviceIds + device.id, error = null)
            }
            val result = lightingRepository.setPower(device.id, !device.isPoweredOn)
            val lastSynced = lightingRepository.lastSyncedAtMillis()
            controlState.update { prev ->
                prev.copy(
                    pendingDeviceIds = prev.pendingDeviceIds - device.id,
                    lastSyncedAtMillis = lastSynced ?: prev.lastSyncedAtMillis,
                    error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                )
            }
        }
    }

    fun addDevice(
        name: String,
        zone: String,
        powerW: Int,
        latitude: Double,
        longitude: Double,
    ) {
        viewModelScope.launch {
            controlState.update { it.copy(isSyncing = true, error = null) }
            val result = lightingRepository.addDevice(
                name = name,
                zone = zone,
                powerW = powerW,
                latitude = latitude,
                longitude = longitude,
            )
            val lastSynced = lightingRepository.lastSyncedAtMillis()
            controlState.update { prev ->
                prev.copy(
                    isSyncing = false,
                    lastSyncedAtMillis = lastSynced ?: prev.lastSyncedAtMillis,
                    selectedDeviceId = result.getOrNull()?.id ?: prev.selectedDeviceId,
                    error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                )
            }
        }
    }

    fun deleteDevice(id: Long) {
        viewModelScope.launch {
            controlState.update {
                it.copy(pendingDeviceIds = it.pendingDeviceIds + id, error = null)
            }
            val result = lightingRepository.deleteDevice(id)
            controlState.update { prev ->
                prev.copy(
                    pendingDeviceIds = prev.pendingDeviceIds - id,
                    selectedDeviceId = if (prev.selectedDeviceId == id) null else prev.selectedDeviceId,
                    error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                )
            }
        }
    }

    fun selectDevice(deviceId: Long?) {
        controlState.update {
            it.copy(
                selectedDeviceId = deviceId,
                selectionSeq = it.selectionSeq + 1L,
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        rawFilterState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChange(statusFilter: StatusFilter) {
        rawFilterState.update { it.copy(statusFilter = statusFilter) }
    }

    fun onZoneSelected(zone: String?) {
        rawFilterState.update { current ->
            current.copy(selectedZone = if (current.selectedZone == zone) null else zone)
        }
    }

    fun onMinPowerChange(minPowerW: Int) {
        rawFilterState.update { it.copy(minPowerW = minPowerW.coerceAtLeast(0)) }
    }

    fun onSortOptionChange(sortOption: DeviceSortOption) {
        rawFilterState.update { it.copy(sortOption = sortOption) }
    }

    fun resetFilters() {
        rawFilterState.value = FilterSortState()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val effectiveGranted = granted || locationDataSource.hasLocationPermission()
        permissionGrantedFlow.value = effectiveGranted
        if (effectiveGranted) {
            refreshLocation()
        }
    }

    fun syncPermissionFromSystem() {
        val granted = locationDataSource.hasLocationPermission()
        if (granted != permissionGrantedFlow.value) {
            permissionGrantedFlow.value = granted
            if (granted) {
                refreshLocation()
            }
        }
    }

    fun onAppResume() {
        syncPermissionFromSystem()
        if (state.value.allDevices.isEmpty() && !controlState.value.isSyncing) {
            syncNow()
        }
    }

    fun refreshLocation() {
        if (!locationDataSource.hasLocationPermission()) {
            permissionGrantedFlow.value = false
            return
        }
        viewModelScope.launch {
            manualLocationOverride.value = LocationState.Loading
            manualLocationOverride.value = locationDataSource.fetchCurrentLocation()
        }
    }

    fun setFaultMode(mode: String) {
        viewModelScope.launch {
            controlState.update { it.copy(error = null) }
            val result = lightingRepository.setFaultMode(mode)
            controlState.update { prev ->
                prev.copy(
                    faultMode = if (result.isSuccess) mode else prev.faultMode,
                    error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                )
            }
        }
    }

    fun clearError() {
        controlState.update { it.copy(error = null) }
    }

    fun logout() {
        viewModelScope.launch {
            lightingRepository.clearLocalCache()
            authRepository.logout()
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
