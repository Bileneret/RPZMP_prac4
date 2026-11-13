package ua.kpi.iot_lighting.prac4.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ua.kpi.iot_lighting.prac4.MainDispatcherRule
import ua.kpi.iot_lighting.prac4.data.location.LocationDataSource
import ua.kpi.iot_lighting.prac4.domain.logic.LightingAdvisor
import ua.kpi.iot_lighting.prac4.domain.logic.PowerSimulator
import ua.kpi.iot_lighting.prac4.domain.model.DeviceSortOption
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.StatusFilter
import ua.kpi.iot_lighting.prac4.domain.model.TokenPayload
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.domain.model.WeatherSyncResult
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac4.domain.repository.LightingRepository
import ua.kpi.iot_lighting.prac4.domain.repository.WeatherRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val seed = listOf(
        LightDevice(1L, "Ліхтар Центр", "Вулиця", 120, true, 47.9108, 33.3924, 100L),
        LightDevice(2L, "Склад Секція Б", "Склад", 200, false, 47.9154, 33.3976, 300L),
        LightDevice(3L, "Алея Південь", "Парк", 80, true, 47.9268, 33.4125, 200L),
    )

    private class FakeLightingRepo(initial: List<LightDevice>) : LightingRepository {
        val devices = MutableStateFlow(initial)
        var cleared = false
        override fun observeDevices(): Flow<List<LightDevice>> = devices
        override suspend fun syncDevices() = Result.success(Unit)
        override suspend fun setPower(id: Long, poweredOn: Boolean): Result<Unit> {
            devices.value = devices.value.map {
                if (it.id == id) it.copy(isPoweredOn = poweredOn, lastUpdatedAt = 999L) else it
            }
            return Result.success(Unit)
        }
        override suspend fun addDevice(name: String, zone: String, powerW: Int, latitude: Double, longitude: Double): Result<LightDevice> {
            val d = LightDevice(99L, name, zone, powerW, true, latitude, longitude, 500L)
            devices.value = devices.value + d
            return Result.success(d)
        }
        override suspend fun deleteDevice(id: Long): Result<Unit> {
            devices.value = devices.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
        override suspend fun lastSyncedAtMillis() = 5000L
        override suspend fun clearLocalCache() { cleared = true; devices.value = emptyList() }
        override suspend fun faultMode() = Result.success("none")
        override suspend fun setFaultMode(mode: String) = Result.success(Unit)
    }

    @Test
    fun `5-flow combine handles debounced search multi-filter sort selection and GPS nearby badges`() = runTest(mainDispatcherRule.dispatcher) {
        val lightingRepo = FakeLightingRepo(seed)
        val weatherInsight = WeatherInsight(16.0, 20, true, 5.0, 100L, "Ясно")
        val weatherRepo = object : WeatherRepository {
            override suspend fun insight() = Result.success(weatherInsight)
            override fun observeWeatherPeriodic(intervalMs: Long) = flowOf(WeatherSyncResult.Fresh(weatherInsight))
        }
        val authRepo = object : AuthRepository {
            override val currentSession: StateFlow<TokenPayload?> = MutableStateFlow(TokenPayload("admin@kpi.ua", Role.ADMIN, 1L, 99999L))
            override suspend fun restoreSession() = Unit
            override suspend fun login(email: String, password: String) = error("unused")
            override suspend fun logout() = Unit
            override fun tokenHeader(): String? = null
        }
        val locationFlow = MutableStateFlow<LocationState>(LocationState.PermissionRequired)
        val locationDs = object : LocationDataSource {
            var granted = false
            override fun hasLocationPermission() = granted
            override fun observeLocation(hasPermission: Boolean, intervalMs: Long) = locationFlow
            override suspend fun fetchCurrentLocation() = locationFlow.value
        }
        val powerSim = PowerSimulator(
            baseTotalsFlow = lightingRepo.observeDevices().map { LightingAdvisor.computeTotals(it) },
            scope = backgroundScope,
            intervalMs = 3_000L,
            jitterFactor = { 1.0 },
        )

        val vm = DeviceListViewModel(
            lightingRepository = lightingRepo,
            weatherRepository = weatherRepo,
            authRepository = authRepo,
            locationDataSource = locationDs,
            powerSimulator = powerSim,
            searchDebounceMs = 250L,
            autoSyncOnStart = false,
        )

        val collectorJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect {}
        }
        runCurrent()

        // Initial combined values
        assertEquals(3, vm.state.value.filteredDevices.size)
        assertEquals(200, vm.state.value.livePowerW)
        assertEquals(weatherInsight, vm.state.value.weather)

        // Debounce test: typing should NOT filter at 100ms, only at >= 250ms
        vm.onSearchQueryChange("Склад")
        advanceTimeBy(100L)
        runCurrent()
        assertEquals("Склад", vm.state.value.filterSort.searchQuery)
        assertEquals(3, vm.state.value.filteredDevices.size)

        advanceTimeBy(200L)
        runCurrent()
        assertEquals(listOf(2L), vm.state.value.filteredDevices.map { it.id })

        // Clearing search applies immediately (0ms debounce)
        vm.onSearchQueryChange("")
        runCurrent()
        assertEquals(3, vm.state.value.filteredDevices.size)

        // Multi-filter + sort
        vm.onStatusFilterChange(StatusFilter.ON)
        vm.onSortOptionChange(DeviceSortOption.BY_POWER_DESC)
        runCurrent()
        assertEquals(listOf(1L, 3L), vm.state.value.filteredDevices.map { it.id })
        vm.resetFilters()
        runCurrent()
        assertEquals(3, vm.state.value.filteredDevices.size)

        // Bidirectional selection increments selectionSeq even for same device
        vm.selectDevice(1L)
        runCurrent()
        val firstSeq = vm.state.value.selectionSeq
        vm.selectDevice(1L)
        runCurrent()
        assertEquals(1L, vm.state.value.selectedDeviceId)
        assertEquals(firstSeq + 1L, vm.state.value.selectionSeq)

        // GPS location updates nearbyDistancesMeters
        locationDs.granted = true
        locationFlow.value = LocationState.Available(GeoPoint(47.9105, 33.3918), 8f, 1000L)
        vm.onLocationPermissionResult(true)
        runCurrent()
        assertTrue(1L in vm.state.value.nearbyDistancesMeters)
        assertTrue(2L in vm.state.value.nearbyDistancesMeters)
        assertTrue(3L !in vm.state.value.nearbyDistancesMeters)

        // Toggle power updates lastUpdatedAt & sorts to top with BY_LAST_UPDATED
        vm.togglePower(seed[0])
        vm.onSortOptionChange(DeviceSortOption.BY_LAST_UPDATED)
        runCurrent()
        assertEquals(1L, vm.state.value.filteredDevices.first().id)

        collectorJob.cancel()
    }
}
