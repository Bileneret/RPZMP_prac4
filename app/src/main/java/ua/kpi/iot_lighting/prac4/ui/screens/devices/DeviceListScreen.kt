package ua.kpi.iot_lighting.prac4.ui.screens.devices

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.util.Locale
import kotlinx.coroutines.launch
import ua.kpi.iot_lighting.prac4.domain.model.DeviceSortOption
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import ua.kpi.iot_lighting.prac4.domain.model.Role
import ua.kpi.iot_lighting.prac4.domain.model.StatusFilter
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.ui.components.ConfirmDialog
import ua.kpi.iot_lighting.prac4.ui.components.ErrorBanner
import ua.kpi.iot_lighting.prac4.ui.components.FilterSortBar
import ua.kpi.iot_lighting.prac4.ui.components.LocationStatusBanner
import ua.kpi.iot_lighting.prac4.ui.components.MapPanel
import ua.kpi.iot_lighting.prac4.ui.components.TelemetrySummaryCard
import ua.kpi.iot_lighting.prac4.ui.theme.AmberContainer
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.AzureAccent
import ua.kpi.iot_lighting.prac4.ui.theme.AzureContainer
import ua.kpi.iot_lighting.prac4.ui.theme.CanvasBase
import ua.kpi.iot_lighting.prac4.ui.theme.CrimsonError
import ua.kpi.iot_lighting.prac4.ui.theme.EmeraldContainer
import ua.kpi.iot_lighting.prac4.ui.theme.EmeraldSuccess
import ua.kpi.iot_lighting.prac4.ui.theme.OutlineSubtle
import ua.kpi.iot_lighting.prac4.ui.theme.SlateMuted
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceCard
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceElevated
import ua.kpi.iot_lighting.prac4.ui.theme.TextSecondary
import ua.kpi.iot_lighting.prac4.ui.util.AddressSuggestion
import ua.kpi.iot_lighting.prac4.ui.util.Formatters
import ua.kpi.iot_lighting.prac4.ui.util.Messages
import ua.kpi.iot_lighting.prac4.ui.util.StreetGeocoder
import ua.kpi.iot_lighting.prac4.ui.viewmodel.DeviceListUiState

/**
 * Головний екран моніторингу світильників (ПЗ13–ПЗ16).
 * Поєднує інтерактивну карту, GPS-статус, багаторівневі фільтри/сортування
 * та двосторонньо синхронізований перелік вузлів освітлення.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    uiState: DeviceListUiState,
    userRole: Role,
    userEmail: String,
    onSync: () -> Unit,
    onTogglePower: (LightDevice) -> Unit,
    onAddDevice: (name: String, zone: String, powerW: Int, latitude: Double, longitude: Double) -> Unit,
    onDeleteDevice: (Long) -> Unit,
    onSelectDevice: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onZoneSelected: (String?) -> Unit,
    onMinPowerChange: (Int) -> Unit,
    onSortOptionChange: (DeviceSortOption) -> Unit,
    onResetFilters: () -> Unit,
    onLocationPermissionResult: (Boolean) -> Unit,
    onSyncPermissionFromSystem: () -> Unit,
    onRefreshLocation: () -> Unit,
    onSetFaultMode: (String) -> Unit,
    onDismissError: () -> Unit,
    onOpenAbout: () -> Unit,
    onLogout: () -> Unit,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var isPickingLocationOnMap by rememberSaveable { mutableStateOf(false) }
    var pickedLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var pickedLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var draftZone by rememberSaveable { mutableStateOf(Messages.DEMO_ZONES.first()) }
    var draftPowerText by rememberSaveable { mutableStateOf("100") }
    var draftLatText by rememberSaveable { mutableStateOf(LightDevice.DEFAULT_LAT.toString()) }
    var draftLngText by rememberSaveable { mutableStateOf(LightDevice.DEFAULT_LNG.toString()) }
    var draftResolvedAddress by rememberSaveable { mutableStateOf<String?>(null) }
    var showFaultDialog by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteDevice by remember { mutableStateOf<LightDevice?>(null) }
    var mapExpandedPortrait by rememberSaveable { mutableStateOf(false) }
    var focusUserSeq by remember { mutableLongStateOf(0L) }
    val pickedMapPoint = if (pickedLat != null && pickedLng != null) {
        GeoPoint(pickedLat!!, pickedLng!!)
    } else {
        null
    }

    // Системна кнопка «Назад» під час вибору точки на мапі скасовує режим і повертає діалог із чернеткою (N2)
    BackHandler(enabled = isPickingLocationOnMap) {
        isPickingLocationOnMap = false
        showAddDialog = true
    }

    val handlePickLocationOnMap: (Double, Double) -> Unit = { lat, lng ->
        pickedLat = lat
        pickedLng = lng
        draftLatText = String.format(Locale.US, "%.6f", lat)
        draftLngText = String.format(Locale.US, "%.6f", lng)
        isPickingLocationOnMap = false
        showAddDialog = true
        coroutineScope.launch {
            val suggestion = StreetGeocoder.reverseGeocode(lat, lng)
            draftResolvedAddress = suggestion.displayAddress
            if (draftName.isBlank()) {
                draftName = suggestion.suggestedName
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionResult(granted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onSyncPermissionFromSystem()
    }

    val requestLocationPermissions = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    LaunchedEffect(uiState.selectedDeviceId, uiState.selectionSeq) {
        val selectedId = uiState.selectedDeviceId ?: return@LaunchedEffect
        val index = uiState.filteredDevices.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        containerColor = CanvasBase,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasBase),
                title = {
                    Column {
                        Text(
                            text = "IoT Lighting",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                        )
                        Text(
                            text = "Роль: ${userRole.displayName} · $userEmail",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (userRole.canManageCatalog) {
                        IconButton(
                            onClick = {
                                val currentPoint = (uiState.locationState as? LocationState.Available)?.point
                                draftName = ""
                                draftZone = Messages.DEMO_ZONES.first()
                                draftPowerText = "100"
                                draftLatText = (currentPoint?.latitude ?: LightDevice.DEFAULT_LAT).toString()
                                draftLngText = (currentPoint?.longitude ?: LightDevice.DEFAULT_LNG).toString()
                                draftResolvedAddress = null
                                pickedLat = null
                                pickedLng = null
                                isPickingLocationOnMap = false
                                showAddDialog = true
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Додати світильник",
                                tint = AmberPrimary,
                            )
                        }
                        IconButton(onClick = { showFaultDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Режим збою сервера",
                                tint = if (uiState.faultMode == "none") TextSecondary else AmberPrimary,
                            )
                        }
                    }
                    IconButton(onClick = onSync, enabled = !uiState.isSyncing) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AmberPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Оновити",
                            )
                        }
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Про систему IoT освітлення",
                        )
                    }
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Вийти з акаунта",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            val isLandscape = maxWidth >= 600.dp

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TelemetrySummaryCard(
                            totals = uiState.totals,
                            livePowerW = uiState.livePowerW,
                            weather = uiState.weather,
                            isWeatherFallback = uiState.isWeatherCachedFallback,
                            weatherStatusNote = uiState.weatherStatusNote,
                            recommendation = uiState.recommendation,
                            lastSyncedAtMillis = uiState.lastSyncedAtMillis,
                            nearbyCount = uiState.nearbyDistancesMeters.size,
                        )
                        LocationStatusBanner(
                            locationState = uiState.locationState,
                            nearbyCount = uiState.nearbyDistancesMeters.size,
                            onRequestPermission = requestLocationPermissions,
                            onRefreshLocation = onRefreshLocation,
                            onFocusUserLocation = { focusUserSeq += 1L },
                        )
                        MapPanel(
                            devices = uiState.filteredDevices,
                            selectedDeviceId = uiState.selectedDeviceId,
                            selectionSeq = uiState.selectionSeq,
                            locationState = uiState.locationState,
                            nearbyDistancesMeters = uiState.nearbyDistancesMeters,
                            onSelectDevice = { onSelectDevice(it) },
                            onRequestFocusUserSeq = focusUserSeq,
                            isExpanded = false,
                            showExpandButton = false,
                            onToggleExpand = {},
                            isPickingLocation = isPickingLocationOnMap,
                            pickedPoint = pickedMapPoint,
                            onPickLocationOnMap = handlePickLocationOnMap,
                            onCancelPickLocation = {
                                isPickingLocationOnMap = false
                                showAddDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (uiState.error != null) {
                            ErrorBanner(
                                error = uiState.error,
                                onRetry = if (uiState.error.isRetryable) onSync else null,
                                onDismiss = onDismissError,
                            )
                        }
                        FilterSortBar(
                            filterSort = uiState.filterSort,
                            availableZones = uiState.availableZones,
                            filteredCount = uiState.filteredDevices.size,
                            totalCount = uiState.allDevices.size,
                            maxDevicePowerW = uiState.maxDevicePowerW,
                            onSearchQueryChange = onSearchQueryChange,
                            onStatusFilterChange = onStatusFilterChange,
                            onZoneSelected = onZoneSelected,
                            onMinPowerChange = onMinPowerChange,
                            onSortOptionChange = onSortOptionChange,
                            onResetFilters = onResetFilters,
                        )
                        DeviceLazyList(
                            devices = uiState.filteredDevices,
                            totalDeviceCount = uiState.allDevices.size,
                            isSyncing = uiState.isSyncing,
                            syncError = uiState.error,
                            selectedDeviceId = uiState.selectedDeviceId,
                            pendingDeviceIds = uiState.pendingDeviceIds,
                            nearbyDistancesMeters = uiState.nearbyDistancesMeters,
                            canManageCatalog = userRole.canManageCatalog,
                            listState = listState,
                            onSelectDevice = onSelectDevice,
                            onTogglePower = onTogglePower,
                            onRequestDeleteDevice = { pendingDeleteDevice = it },
                            onResetFilters = onResetFilters,
                            onSync = onSync,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.error != null) {
                        ErrorBanner(
                            error = uiState.error,
                            onRetry = if (uiState.error.isRetryable) onSync else null,
                            onDismiss = onDismissError,
                        )
                    }

                    TelemetrySummaryCard(
                        totals = uiState.totals,
                        livePowerW = uiState.livePowerW,
                        weather = uiState.weather,
                        isWeatherFallback = uiState.isWeatherCachedFallback,
                        weatherStatusNote = uiState.weatherStatusNote,
                        recommendation = uiState.recommendation,
                        lastSyncedAtMillis = uiState.lastSyncedAtMillis,
                        nearbyCount = uiState.nearbyDistancesMeters.size,
                    )

                    LocationStatusBanner(
                        locationState = uiState.locationState,
                        nearbyCount = uiState.nearbyDistancesMeters.size,
                        onRequestPermission = requestLocationPermissions,
                        onRefreshLocation = onRefreshLocation,
                        onFocusUserLocation = { focusUserSeq += 1L },
                    )

                    MapPanel(
                        devices = uiState.filteredDevices,
                        selectedDeviceId = uiState.selectedDeviceId,
                        selectionSeq = uiState.selectionSeq,
                        locationState = uiState.locationState,
                        nearbyDistancesMeters = uiState.nearbyDistancesMeters,
                        onSelectDevice = { onSelectDevice(it) },
                        onRequestFocusUserSeq = focusUserSeq,
                        isExpanded = mapExpandedPortrait,
                        showExpandButton = true,
                        onToggleExpand = { mapExpandedPortrait = !mapExpandedPortrait },
                        isPickingLocation = isPickingLocationOnMap,
                        pickedPoint = pickedMapPoint,
                        onPickLocationOnMap = handlePickLocationOnMap,
                        onCancelPickLocation = {
                            isPickingLocationOnMap = false
                            showAddDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (mapExpandedPortrait) 340.dp else 210.dp),
                    )

                    FilterSortBar(
                        filterSort = uiState.filterSort,
                        availableZones = uiState.availableZones,
                        filteredCount = uiState.filteredDevices.size,
                        totalCount = uiState.allDevices.size,
                        maxDevicePowerW = uiState.maxDevicePowerW,
                        onSearchQueryChange = onSearchQueryChange,
                        onStatusFilterChange = onStatusFilterChange,
                        onZoneSelected = onZoneSelected,
                        onMinPowerChange = onMinPowerChange,
                        onSortOptionChange = onSortOptionChange,
                        onResetFilters = onResetFilters,
                    )

                    DeviceLazyList(
                        devices = uiState.filteredDevices,
                        totalDeviceCount = uiState.allDevices.size,
                        isSyncing = uiState.isSyncing,
                        syncError = uiState.error,
                        selectedDeviceId = uiState.selectedDeviceId,
                        pendingDeviceIds = uiState.pendingDeviceIds,
                        nearbyDistancesMeters = uiState.nearbyDistancesMeters,
                        canManageCatalog = userRole.canManageCatalog,
                        listState = listState,
                        onSelectDevice = onSelectDevice,
                        onTogglePower = onTogglePower,
                        onRequestDeleteDevice = { pendingDeleteDevice = it },
                        onResetFilters = onResetFilters,
                        onSync = onSync,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val currentPoint = (uiState.locationState as? LocationState.Available)?.point
        AddDeviceDialog(
            name = draftName,
            zone = draftZone,
            powerText = draftPowerText,
            latText = draftLatText,
            lngText = draftLngText,
            resolvedAddress = draftResolvedAddress,
            onNameChange = { draftName = it },
            onZoneChange = { draftZone = it },
            onPowerTextChange = { draftPowerText = it },
            onLatTextChange = { draftLatText = it },
            onLngTextChange = { draftLngText = it },
            onResolvedAddressChange = { draftResolvedAddress = it },
            onCoordinatesSelected = { lat, lng ->
                pickedLat = lat
                pickedLng = lng
                draftLatText = String.format(Locale.US, "%.6f", lat)
                draftLngText = String.format(Locale.US, "%.6f", lng)
            },
            userGpsLat = currentPoint?.latitude,
            userGpsLng = currentPoint?.longitude,
            onPickOnMap = {
                showAddDialog = false
                isPickingLocationOnMap = true
            },
            onDismiss = {
                isPickingLocationOnMap = false
                showAddDialog = false
            },
            onConfirm = { name, zone, powerW, lat, lng ->
                isPickingLocationOnMap = false
                showAddDialog = false
                onAddDevice(name, zone, powerW, lat, lng)
            },
        )
    }

    pendingDeleteDevice?.let { target ->
        ConfirmDialog(
            title = "Видалити світильник?",
            message = "Світильник «${target.name}» буде видалено на сервері та з локального кешу.",
            confirmLabel = "Видалити",
            onConfirm = {
                pendingDeleteDevice = null
                onDeleteDevice(target.id)
            },
            onDismiss = { pendingDeleteDevice = null },
        )
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Вийти з акаунта?",
            message = "Поточну сесію буде завершено. Для подальшої роботи знадобиться повторний вхід.",
            confirmLabel = "Вийти",
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false },
        )
    }

    if (showFaultDialog) {
        FaultModeDialog(
            currentMode = uiState.faultMode,
            onDismiss = { showFaultDialog = false },
            onSelectMode = { mode ->
                showFaultDialog = false
                onSetFaultMode(mode)
            },
        )
    }
}

/**
 * Прокручуваний список світильників із підтримкою станів завантаження, помилки та порожніх фільтрів (ПЗ14).
 */
@Composable
private fun DeviceLazyList(
    devices: List<LightDevice>,
    totalDeviceCount: Int,
    isSyncing: Boolean,
    syncError: UiError?,
    selectedDeviceId: Long?,
    pendingDeviceIds: Set<Long>,
    nearbyDistancesMeters: Map<Long, Int>,
    canManageCatalog: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSelectDevice: (Long?) -> Unit,
    onTogglePower: (LightDevice) -> Unit,
    onRequestDeleteDevice: (LightDevice) -> Unit,
    onResetFilters: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (devices.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (totalDeviceCount == 0 && isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AmberPrimary,
                    )
                    Text(
                        text = "Завантаження світильників із сервера...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                } else if (totalDeviceCount == 0 && syncError != null) {
                    Text(
                        text = syncError.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonError,
                    )
                    OutlinedButton(onClick = onSync) {
                        Text("Повторити синхронізацію")
                    }
                } else {
                    Text(
                        text = if (totalDeviceCount == 0) {
                            "Список світильників порожній"
                        } else {
                            "Нічого не знайдено за обраними фільтрами"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    if (totalDeviceCount > 0) {
                        OutlinedButton(onClick = onResetFilters) {
                            Text("Скинути фільтри")
                        }
                    } else {
                        OutlinedButton(onClick = onSync) {
                            Text("Оновити з сервера")
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                isSelected = device.id == selectedDeviceId,
                isPending = device.id in pendingDeviceIds,
                nearbyDistanceMeters = nearbyDistancesMeters[device.id],
                canManageCatalog = canManageCatalog,
                onSelect = { onSelectDevice(device.id) },
                onTogglePower = { onTogglePower(device) },
                onDelete = { onRequestDeleteDevice(device) },
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: LightDevice,
    isSelected: Boolean,
    isPending: Boolean,
    nearbyDistanceMeters: Int?,
    canManageCatalog: Boolean,
    onSelect: () -> Unit,
    onTogglePower: () -> Unit,
    onDelete: () -> Unit,
) {
    val containerColor = when {
        isSelected -> AzureContainer.copy(alpha = 0.72f)
        else -> SurfaceCard
    }
    val borderColor = when {
        isSelected -> AzureAccent
        device.isPoweredOn -> AmberPrimary.copy(alpha = 0.45f)
        else -> OutlineSubtle
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = if (device.isPoweredOn) AmberContainer else SurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (device.isPoweredOn) AmberPrimary else SlateMuted,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = device.zone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AzureAccent,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Text(
                                text = Formatters.formatPower(device.powerW),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (device.isPoweredOn) AmberPrimary else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = if (device.isPoweredOn) "Увімкнено" else "Вимкнено",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (device.isPoweredOn) AmberPrimary else TextSecondary,
                                fontWeight = if (device.isPoweredOn) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AmberPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Switch(
                            checked = device.isPoweredOn,
                            onCheckedChange = { onTogglePower() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CanvasBase,
                                checkedTrackColor = AmberPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceElevated,
                            ),
                        )
                    }
                    if (canManageCatalog) {
                        IconButton(
                            onClick = onDelete,
                            enabled = !isPending,
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Видалити світильник",
                                tint = CrimsonError.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = Formatters.formatCoordinates(device.latitude, device.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    // Бейдж «ПОРУЧ» у нижньому рядку, щоб не стискатися і не переноситися по літерах (N11)
                    if (nearbyDistanceMeters != null) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "ПОРУЧ · ${Formatters.formatDistance(nearbyDistanceMeters)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Оновлено: ${Formatters.formatTimeOnly(device.lastUpdatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Діалог створення світильника з вибором координат на мапі (A4)
 * та адресною прив'язкою через Nominatim/OSM і перевірені вулиці Кривого Рогу (N14).
 */
@Composable
private fun AddDeviceDialog(
    name: String,
    zone: String,
    powerText: String,
    latText: String,
    lngText: String,
    resolvedAddress: String?,
    onNameChange: (String) -> Unit,
    onZoneChange: (String) -> Unit,
    onPowerTextChange: (String) -> Unit,
    onLatTextChange: (String) -> Unit,
    onLngTextChange: (String) -> Unit,
    onResolvedAddressChange: (String?) -> Unit,
    onCoordinatesSelected: (Double, Double) -> Unit,
    userGpsLat: Double?,
    userGpsLng: Double?,
    onPickOnMap: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, zone: String, powerW: Int, latitude: Double, longitude: Double) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var addressQuery by rememberSaveable { mutableStateOf("") }
    var isSearchingAddress by remember { mutableStateOf(false) }
    var addressSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Новий світильник") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it); validationError = null },
                    label = { Text("Назва світильника") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Messages.DEMO_ZONES) { preset ->
                        FilterChip(
                            selected = zone == preset,
                            onClick = { onZoneChange(preset); validationError = null },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                OutlinedTextField(
                    value = zone,
                    onValueChange = { onZoneChange(it); validationError = null },
                    label = { Text("Зона") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = powerText,
                    onValueChange = { onPowerTextChange(it); validationError = null },
                    label = { Text("Потужність (Вт)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = addressQuery,
                    onValueChange = { query ->
                        addressQuery = query
                        addressSuggestions = StreetGeocoder.verifiedKryvyiRihStreets
                            .filter { it.displayAddress.contains(query.trim(), ignoreCase = true) }
                            .take(4)
                    },
                    label = { Text("Пошук вулиці або об'єкта (Кривий Ріг)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isSearchingAddress = true
                                    addressSuggestions = StreetGeocoder.searchByAddress(addressQuery)
                                    isSearchingAddress = false
                                }
                            },
                            enabled = !isSearchingAddress,
                        ) {
                            if (isSearchingAddress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AmberPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Знайти адресу",
                                    tint = AmberPrimary,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (addressSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        addressSuggestions.forEach { suggestion ->
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, OutlineSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCoordinatesSelected(suggestion.latitude, suggestion.longitude)
                                        onResolvedAddressChange(suggestion.displayAddress)
                                        onZoneChange(suggestion.suggestedZone)
                                        if (name.isBlank()) {
                                            onNameChange(suggestion.suggestedName)
                                        }
                                        addressQuery = suggestion.displayAddress
                                        addressSuggestions = emptyList()
                                        validationError = null
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = suggestion.displayAddress,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = suggestion.suggestedZone,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AzureAccent,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
                if (resolvedAddress != null) {
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AzureAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Адреса: $resolvedAddress",
                                style = MaterialTheme.typography.labelSmall,
                                color = AzureAccent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { onNameChange("Ліхтар $resolvedAddress") },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            ) {
                                Text("У назву", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { onLatTextChange(it); validationError = null },
                        label = { Text("Широта") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { onLngTextChange(it); validationError = null },
                        label = { Text("Довгота") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (userGpsLat != null && userGpsLng != null) {
                    OutlinedButton(
                        onClick = {
                            onLatTextChange(userGpsLat.toString())
                            onLngTextChange(userGpsLng.toString())
                            scope.launch {
                                val resolved = StreetGeocoder.reverseGeocode(userGpsLat, userGpsLng)
                                onResolvedAddressChange(resolved.displayAddress)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Підставити мої GPS-координати")
                    }
                }
                OutlinedButton(
                    onClick = onPickOnMap,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Вказати точку на мапі")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val power = powerText.trim().toIntOrNull()
                    val lat = latText.trim().replace(',', '.').toDoubleOrNull()
                    val lng = lngText.trim().replace(',', '.').toDoubleOrNull()
                    when {
                        name.isBlank() -> validationError = "Вкажіть назву світильника"
                        zone.isBlank() -> validationError = "Вкажіть зону"
                        power == null || power <= 0 -> validationError = "Потужність має бути > 0 Вт"
                        lat == null || lat !in -90.0..90.0 -> validationError = "Некоректна широта"
                        lng == null || lng !in -180.0..180.0 -> validationError = "Некоректна довгота"
                        else -> onConfirm(name.trim(), zone.trim(), power, lat, lng)
                    }
                },
            ) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}

@Composable
private fun FaultModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelectMode: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Симуляція збоїв mock-сервера") },
        text = {
            Column {
                Messages.FAULT_MODES.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onSelectMode(mode) },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}
