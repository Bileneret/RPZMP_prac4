package ua.kpi.iot_lighting.prac4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.iot_lighting.prac4.di.AppContainer
import ua.kpi.iot_lighting.prac4.ui.screens.about.AboutScreen
import ua.kpi.iot_lighting.prac4.ui.screens.devices.DeviceListScreen
import ua.kpi.iot_lighting.prac4.ui.screens.login.LoginScreen
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.CanvasBase
import ua.kpi.iot_lighting.prac4.ui.theme.IoTLightingTheme
import ua.kpi.iot_lighting.prac4.ui.viewmodel.DeviceListViewModel
import ua.kpi.iot_lighting.prac4.ui.viewmodel.LoginViewModel
import ua.kpi.iot_lighting.prac4.ui.viewmodel.viewModelWithFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as Prac4Application).container
        setContent {
            IoTLightingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CanvasBase,
                ) {
                    AppRoot(container = container)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(container: AppContainer) {
    var sessionRestored by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container.authRepository.restoreSession()
        sessionRestored = true
    }

    val session by container.authRepository.currentSession.collectAsStateWithLifecycle()

    if (!sessionRestored) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AmberPrimary)
        }
        return
    }

    // Системна кнопка «Назад» на екрані «Про застосунок» повертає до основного екрана замість виходу (N1)
    BackHandler(enabled = showAbout) {
        showAbout = false
    }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }

    val activeSession = session
    if (activeSession == null) {
        val loginVm: LoginViewModel = viewModelWithFactory {
            LoginViewModel(container.authRepository)
        }
        val loginState by loginVm.state.collectAsStateWithLifecycle()
        LoginScreen(
            uiState = loginState,
            onEmailChange = loginVm::onEmailChange,
            onPasswordChange = loginVm::onPasswordChange,
            onFillOperator = loginVm::fillDemoOperator,
            onFillAdmin = loginVm::fillDemoAdmin,
            onSubmit = loginVm::submit,
        )
    } else {
        val deviceVm: DeviceListViewModel = viewModelWithFactory {
            DeviceListViewModel(
                lightingRepository = container.lightingRepository,
                weatherRepository = container.weatherRepository,
                authRepository = container.authRepository,
                locationDataSource = container.locationDataSource,
                powerSimulator = container.powerSimulator,
            )
        }
        val uiState by deviceVm.state.collectAsStateWithLifecycle()
        DeviceListScreen(
            uiState = uiState,
            userRole = activeSession.role,
            userEmail = activeSession.subject,
            onSync = deviceVm::syncNow,
            onTogglePower = deviceVm::togglePower,
            onAddDevice = deviceVm::addDevice,
            onDeleteDevice = deviceVm::deleteDevice,
            onSelectDevice = deviceVm::selectDevice,
            onSearchQueryChange = deviceVm::onSearchQueryChange,
            onStatusFilterChange = deviceVm::onStatusFilterChange,
            onZoneSelected = deviceVm::onZoneSelected,
            onMinPowerChange = deviceVm::onMinPowerChange,
            onSortOptionChange = deviceVm::onSortOptionChange,
            onResetFilters = deviceVm::resetFilters,
            onLocationPermissionResult = deviceVm::onLocationPermissionResult,
            onSyncPermissionFromSystem = deviceVm::onAppResume,
            onRefreshLocation = deviceVm::refreshLocation,
            onSetFaultMode = deviceVm::setFaultMode,
            onDismissError = deviceVm::clearError,
            onOpenAbout = { showAbout = true },
            onLogout = deviceVm::logout,
        )
    }
}
