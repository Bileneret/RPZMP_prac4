package ua.kpi.iot_lighting.prac4.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.kpi.iot_lighting.prac4.BuildConfig
import ua.kpi.iot_lighting.prac4.data.remote.ServerConfig
import ua.kpi.iot_lighting.prac4.domain.logic.GeoCalculator
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.AzureAccent
import ua.kpi.iot_lighting.prac4.ui.theme.CanvasBase
import ua.kpi.iot_lighting.prac4.ui.theme.EmeraldSuccess
import ua.kpi.iot_lighting.prac4.ui.theme.OutlineSubtle
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceCard
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceElevated
import ua.kpi.iot_lighting.prac4.ui.theme.TextSecondary
import ua.kpi.iot_lighting.prac4.ui.util.Formatters

private const val CITY_OVERVIEW_LAT = 47.9420
private const val CITY_OVERVIEW_LNG = 33.4050
private const val CITY_OVERVIEW_ZOOM = 11.5f
private const val DISTRICT_DEFAULT_ZOOM = 13.8f
private const val USER_FOCUS_ZOOM = 14.1f
private const val DEVICE_FOCUS_ZOOM = 15.3f

@Composable
fun MapPanel(
    devices: List<LightDevice>,
    selectedDeviceId: Long?,
    selectionSeq: Long,
    locationState: LocationState,
    nearbyDistancesMeters: Map<Long, Int>,
    onSelectDevice: (Long) -> Unit,
    onRequestFocusUserSeq: Long,
    isExpanded: Boolean,
    showExpandButton: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    isPickingLocation: Boolean = false,
    pickedPoint: GeoPoint? = null,
    onPickLocationOnMap: ((Double, Double) -> Unit)? = null,
    onCancelPickLocation: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val defaultLatLng = remember { LatLng(ServerConfig.DEFAULT_LAT, ServerConfig.DEFAULT_LNG) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, DISTRICT_DEFAULT_ZOOM)
    }

    var useGoogleMapsSdk by rememberSaveable { mutableStateOf(BuildConfig.HAS_MAPS_KEY) }
    var targetLat by rememberSaveable { mutableDoubleStateOf(ServerConfig.DEFAULT_LAT) }
    var targetLng by rememberSaveable { mutableDoubleStateOf(ServerConfig.DEFAULT_LNG) }
    var targetZoom by rememberSaveable { mutableFloatStateOf(DISTRICT_DEFAULT_ZOOM) }
    var animateCamera by remember { mutableStateOf(false) }
    var autoFocusedUserOnce by rememberSaveable { mutableStateOf(false) }

    val selectedDevice = remember(devices, selectedDeviceId) {
        devices.firstOrNull { it.id == selectedDeviceId }
    }
    val userPoint: GeoPoint? = (locationState as? LocationState.Available)?.point

    // При першому отриманні реальних GPS-координат автоматично центруємо карту на користувачі
    LaunchedEffect(userPoint?.latitude, userPoint?.longitude) {
        if (userPoint != null && !autoFocusedUserOnce && selectedDeviceId == null && !isPickingLocation) {
            autoFocusedUserOnce = true
            animateCamera = true
            targetLat = userPoint.latitude
            targetLng = userPoint.longitude
            targetZoom = USER_FOCUS_ZOOM
            if (useGoogleMapsSdk) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(userPoint.latitude, userPoint.longitude),
                        USER_FOCUS_ZOOM,
                    ),
                    durationMs = 500,
                )
            }
        }
    }

    LaunchedEffect(selectedDevice?.id, selectionSeq) {
        if (selectedDevice != null) {
            animateCamera = true
            targetLat = selectedDevice.latitude
            targetLng = selectedDevice.longitude
            targetZoom = DEVICE_FOCUS_ZOOM
            if (useGoogleMapsSdk) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(selectedDevice.latitude, selectedDevice.longitude),
                        DEVICE_FOCUS_ZOOM,
                    ),
                    durationMs = 500,
                )
            }
        }
    }

    LaunchedEffect(onRequestFocusUserSeq) {
        if (onRequestFocusUserSeq > 0L && userPoint != null) {
            animateCamera = true
            targetLat = userPoint.latitude
            targetLng = userPoint.longitude
            targetZoom = USER_FOCUS_ZOOM
            if (useGoogleMapsSdk) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(userPoint.latitude, userPoint.longitude),
                        USER_FOCUS_ZOOM,
                    ),
                    durationMs = 500,
                )
            }
        }
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = SurfaceCard,
        border = BorderStroke(
            width = if (isPickingLocation) 2.dp else 1.dp,
            color = if (isPickingLocation) AmberPrimary else OutlineSubtle,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useGoogleMapsSdk) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL,
                    ),
                    uiSettings = MapUiSettings(
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = true,
                        mapToolbarEnabled = false,
                    ),
                    onMapClick = { latLng ->
                        if (isPickingLocation) {
                            onPickLocationOnMap?.invoke(latLng.latitude, latLng.longitude)
                        }
                    },
                ) {
                    devices.forEach { device ->
                        key(device.id) {
                            val isSelected = device.id == selectedDeviceId
                            val markerState = remember {
                                MarkerState(position = LatLng(device.latitude, device.longitude))
                            }
                            LaunchedEffect(device.latitude, device.longitude) {
                                markerState.position = LatLng(device.latitude, device.longitude)
                            }
                            // N17: у режимі вибору точки InfoWindow приховуємо і в Google-гілці,
                            // щоб підказка банера не перекривалася карткою раніше обраного світильника
                            LaunchedEffect(isSelected, selectionSeq, isPickingLocation) {
                                if (isSelected && !isPickingLocation) {
                                    markerState.showInfoWindow()
                                } else {
                                    markerState.hideInfoWindow()
                                }
                            }

                            val hue = when {
                                isSelected -> BitmapDescriptorFactory.HUE_AZURE
                                device.isPoweredOn -> BitmapDescriptorFactory.HUE_YELLOW
                                else -> BitmapDescriptorFactory.HUE_VIOLET
                            }
                            val statusText = if (device.isPoweredOn) "УВІМК" else "ВИМК"
                            val nearbySuffix = nearbyDistancesMeters[device.id]?.let { dist ->
                                " · Поруч ${Formatters.formatDistance(dist)}"
                            } ?: ""

                            Marker(
                                state = markerState,
                                title = device.name,
                                snippet = "${device.zone} · $statusText · ${device.powerW} Вт$nearbySuffix",
                                icon = BitmapDescriptorFactory.defaultMarker(hue),
                                zIndex = if (isSelected) 2f else 1f,
                                onClick = {
                                    markerState.showInfoWindow()
                                    onSelectDevice(device.id)
                                    true
                                },
                            )
                        }
                    }

                    if (userPoint != null) {
                        val userMarkerState = remember {
                            MarkerState(position = LatLng(userPoint.latitude, userPoint.longitude))
                        }
                        LaunchedEffect(userPoint.latitude, userPoint.longitude) {
                            userMarkerState.position = LatLng(userPoint.latitude, userPoint.longitude)
                        }
                        Marker(
                            state = userMarkerState,
                            title = "Моє розташування",
                            snippet = Formatters.formatCoordinates(userPoint.latitude, userPoint.longitude),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                            zIndex = 3f,
                        )
                        Circle(
                            center = LatLng(userPoint.latitude, userPoint.longitude),
                            radius = GeoCalculator.DEFAULT_NEARBY_RADIUS_METERS,
                            fillColor = AzureAccent.copy(alpha = 0.14f),
                            strokeColor = AzureAccent.copy(alpha = 0.78f),
                            strokeWidth = 3f,
                        )
                    }

                    if (pickedPoint != null) {
                        val pickedState = remember {
                            MarkerState(position = LatLng(pickedPoint.latitude, pickedPoint.longitude))
                        }
                        LaunchedEffect(pickedPoint.latitude, pickedPoint.longitude) {
                            pickedState.position = LatLng(pickedPoint.latitude, pickedPoint.longitude)
                        }
                        Marker(
                            state = pickedState,
                            title = "Обрана точка",
                            snippet = Formatters.formatCoordinates(pickedPoint.latitude, pickedPoint.longitude),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            zIndex = 4f,
                        )
                    }
                }
            } else {
                InteractiveStreetTileMap(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    userPoint = userPoint,
                    pickedPoint = pickedPoint,
                    nearbyDistancesMeters = nearbyDistancesMeters,
                    centerLat = targetLat,
                    centerLng = targetLng,
                    zoom = targetZoom,
                    animateCamera = animateCamera,
                    isPickingLocation = isPickingLocation,
                    onPanLatLng = { newLat, newLng ->
                        animateCamera = false
                        // Межі охоплюють увесь Кривий Ріг включно з Інгульцем (№9: 47.6835, 33.1635)
                        targetLat = newLat.coerceIn(47.60, 48.25)
                        targetLng = newLng.coerceIn(33.05, 33.80)
                    },
                    onZoomChange = { newZoom ->
                        animateCamera = false
                        targetZoom = newZoom.coerceIn(10.5f, 17.5f)
                    },
                    onSelectDevice = onSelectDevice,
                    onPickLocationOnMap = onPickLocationOnMap,
                )
            }

            // Верхній лівий бейдж карти
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = SurfaceElevated.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                onClick = {
                    if (BuildConfig.HAS_MAPS_KEY) {
                        useGoogleMapsSdk = !useGoogleMapsSdk
                    }
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (useGoogleMapsSdk) {
                            "Google Maps · Кривий Ріг (${devices.size})"
                        } else {
                            "Карта м. Кривий Ріг (${devices.size})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Банер режиму вибору точки на мапі (A4)
            if (isPickingLocation) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp, start = 12.dp, end = 52.dp),
                    color = SurfaceCard.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, AmberPrimary),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Торкніться точки на мапі для нового світильника",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (onCancelPickLocation != null) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Скасувати вибір на мапі",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onCancelPickLocation() },
                            )
                        }
                    }
                }
            }

            // Панель кнопок керування камерою (праворуч вгорі)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showExpandButton) {
                    SmallFloatingActionButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(34.dp),
                        containerColor = SurfaceCard.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = if (isExpanded) "Згорнути карту" else "Розгорнути карту",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                SmallFloatingActionButton(
                    onClick = {
                        animateCamera = true
                        targetLat = CITY_OVERVIEW_LAT
                        targetLng = CITY_OVERVIEW_LNG
                        targetZoom = CITY_OVERVIEW_ZOOM
                        if (useGoogleMapsSdk) {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(CITY_OVERVIEW_LAT, CITY_OVERVIEW_LNG),
                                        CITY_OVERVIEW_ZOOM,
                                    ),
                                    durationMs = 450,
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(34.dp),
                    containerColor = SurfaceCard.copy(alpha = 0.92f),
                    contentColor = AmberPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Огляд усього міста",
                        modifier = Modifier.size(17.dp),
                    )
                }

                if (userPoint != null) {
                    SmallFloatingActionButton(
                        onClick = {
                            animateCamera = true
                            targetLat = userPoint.latitude
                            targetLng = userPoint.longitude
                            targetZoom = USER_FOCUS_ZOOM
                            if (useGoogleMapsSdk) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        update = CameraUpdateFactory.newLatLngZoom(
                                            LatLng(userPoint.latitude, userPoint.longitude),
                                            USER_FOCUS_ZOOM,
                                        ),
                                        durationMs = 450,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(34.dp),
                        containerColor = SurfaceCard.copy(alpha = 0.92f),
                        contentColor = AzureAccent,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "До моєї позиції",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            if (!useGoogleMapsSdk) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            animateCamera = true
                            targetZoom = (targetZoom - 0.8f).coerceIn(10.5f, 17.5f)
                        },
                        modifier = Modifier.size(34.dp),
                        containerColor = SurfaceCard.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Віддалити", modifier = Modifier.size(17.dp))
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            animateCamera = true
                            targetZoom = (targetZoom + 0.8f).coerceIn(10.5f, 17.5f)
                        },
                        modifier = Modifier.size(34.dp),
                        containerColor = SurfaceCard.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Наблизити", modifier = Modifier.size(17.dp))
                    }
                }

                // Обов'язкова атрибуція OpenStreetMap (B4)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 88.dp, bottom = 10.dp),
                    color = SurfaceCard.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }

            // Інтерактивне вікно InfoWindow для обраного маркера (приховане під час вибору точки — N5)
            if (!useGoogleMapsSdk && selectedDevice != null && !isPickingLocation) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .widthIn(max = 265.dp),
                    color = SurfaceCard.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, AzureAccent),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = selectedDevice.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val stateWord = if (selectedDevice.isPoweredOn) "Увімкнено" else "Вимкнено"
                        val nearbyNote = nearbyDistancesMeters[selectedDevice.id]?.let {
                            " · Поруч ${Formatters.formatDistance(it)}"
                        } ?: ""
                        Text(
                            text = "${selectedDevice.zone} · $stateWord · ${selectedDevice.powerW} Вт$nearbyNote",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (nearbyNote.isNotEmpty()) EmeraldSuccess else AmberPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Повноцінна інтерактивна вулична карта Web Mercator (EPSG:3857) із підвантаженням
 * та кешуванням реальних тайлів вулиць м. Кривий Ріг (OpenStreetMap / Carto Voyager),
 * колом радіуса 1500 м (ПЗ16), маркером геопозиції користувача та маркерами світильників (ПЗ14–15).
 */
@Composable
private fun InteractiveStreetTileMap(
    devices: List<LightDevice>,
    selectedDevice: LightDevice?,
    userPoint: GeoPoint?,
    pickedPoint: GeoPoint?,
    nearbyDistancesMeters: Map<Long, Int>,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    animateCamera: Boolean,
    isPickingLocation: Boolean,
    onPanLatLng: (newLat: Double, newLng: Double) -> Unit,
    onZoomChange: (newZoom: Float) -> Unit,
    onSelectDevice: (Long) -> Unit,
    onPickLocationOnMap: ((Double, Double) -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tileLoadTick by remember { mutableIntStateOf(0) }

    val animLat = remember { Animatable(centerLat.toFloat()) }
    val animLng = remember { Animatable(centerLng.toFloat()) }
    val animZoom = remember { Animatable(zoom) }

    LaunchedEffect(centerLat, centerLng, zoom, animateCamera) {
        if (animateCamera) {
            launch { animLat.animateTo(centerLat.toFloat(), tween(durationMillis = 280)) }
            launch { animLng.animateTo(centerLng.toFloat(), tween(durationMillis = 280)) }
            launch { animZoom.animateTo(zoom, tween(durationMillis = 280)) }
        } else {
            animLat.snapTo(centerLat.toFloat())
            animLng.snapTo(centerLng.toFloat())
            animZoom.snapTo(zoom)
        }
    }

    val renderLat = if (animateCamera) animLat.value.toDouble() else centerLat
    val renderLng = if (animateCamera) animLng.value.toDouble() else centerLng
    val renderZoom = if (animateCamera) animZoom.value else zoom

    val tileZoom = renderZoom.toInt().coerceIn(10, 18)
    val scale = 2.0.pow((renderZoom - tileZoom).toDouble()).toFloat()
    val tileSizePx = 256f * scale

    // rememberUpdatedState виключає перезапуск pointerInput під час панорамування/анімацій (A1–A3)
    val currentDevices by rememberUpdatedState(devices)
    val currentLat by rememberUpdatedState(renderLat)
    val currentLng by rememberUpdatedState(renderLng)
    val currentZoom by rememberUpdatedState(renderZoom)
    val currentTileZoom by rememberUpdatedState(tileZoom)
    val currentTileSizePx by rememberUpdatedState(tileSizePx)
    val currentOnPanLatLng by rememberUpdatedState(onPanLatLng)
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentOnSelectDevice by rememberUpdatedState(onSelectDevice)
    val currentIsPicking by rememberUpdatedState(isPickingLocation)
    val currentOnPickLocation by rememberUpdatedState(onPickLocationOnMap)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val tZoom = currentTileZoom
                        val tSize = currentTileSizePx
                        val cTileX = WebMercator.lonToTileX(currentLng, tZoom)
                        val cTileY = WebMercator.latToTileY(currentLat, tZoom)

                        if (currentIsPicking && currentOnPickLocation != null) {
                            val tapTileX = cTileX + (tapOffset.x - cx) / tSize
                            val tapTileY = cTileY + (tapOffset.y - cy) / tSize
                            val tapLng = WebMercator.tileXToLon(tapTileX, tZoom)
                            val tapLat = WebMercator.tileYToLat(tapTileY, tZoom)
                            currentOnPickLocation?.invoke(tapLat, tapLng)
                            return@detectTapGestures
                        }

                        var bestId: Long? = null
                        var bestDist = 56f
                        currentDevices.forEach { d ->
                            val dTileX = WebMercator.lonToTileX(d.longitude, tZoom)
                            val dTileY = WebMercator.latToTileY(d.latitude, tZoom)
                            val x = cx + ((dTileX - cTileX) * tSize).toFloat()
                            val y = cy + ((dTileY - cTileY) * tSize).toFloat()
                            val distPin = minOf(
                                hypot(tapOffset.x - x, tapOffset.y - y),
                                hypot(tapOffset.x - x, tapOffset.y - (y - 16f)),
                            )
                            if (distPin < bestDist) {
                                bestDist = distPin
                                bestId = d.id
                            }
                        }
                        if (bestId != null) {
                            currentOnSelectDevice(bestId!!)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val tZoom = currentTileZoom
                        val tSize = currentTileSizePx
                        if (gestureZoom != 1f && gestureZoom > 0f) {
                            val zoomDelta = (ln(gestureZoom.toDouble()) / ln(2.0)).toFloat()
                            val nextZoom = (currentZoom + zoomDelta).coerceIn(10.5f, 17.5f)
                            currentOnZoomChange(nextZoom)
                        }
                        if (pan != Offset.Zero) {
                            val cTileX = WebMercator.lonToTileX(currentLng, tZoom) - pan.x / tSize
                            val cTileY = WebMercator.latToTileY(currentLat, tZoom) - pan.y / tSize
                            val newLng = WebMercator.tileXToLon(cTileX, tZoom)
                            val newLat = WebMercator.tileYToLat(cTileY, tZoom)
                            currentOnPanLatLng(newLat, newLng)
                        }
                    }
                },
        ) {
            // 1. Фон підкладки до завантаження тайлів
            drawRect(color = Color(0xFF1A232C))
            val observeTick = tileLoadTick // підписка на завершення завантаження тайлів
            if (observeTick < 0) return@Canvas

            val cx = size.width / 2f
            val cy = size.height / 2f
            val centerTileX = WebMercator.lonToTileX(renderLng, tileZoom)
            val centerTileY = WebMercator.latToTileY(renderLat, tileZoom)
            val maxTileIndex = (1 shl tileZoom) - 1

            val minTileX = floor(centerTileX - cx / tileSizePx).toInt().coerceIn(0, maxTileIndex)
            val maxTileX = ceil(centerTileX + cx / tileSizePx).toInt().coerceIn(0, maxTileIndex)
            val minTileY = floor(centerTileY - cy / tileSizePx).toInt().coerceIn(0, maxTileIndex)
            val maxTileY = ceil(centerTileY + cy / tileSizePx).toInt().coerceIn(0, maxTileIndex)

            // 2. Рендер реальних вуличних тайлів OpenStreetMap / Carto
            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    val left = cx + ((tx - centerTileX) * tileSizePx).toFloat()
                    val top = cy + ((ty - centerTileY) * tileSizePx).toFloat()
                    val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
                    val dstSize = IntSize(ceil(tileSizePx).toInt() + 1, ceil(tileSizePx).toInt() + 1)

                    val bitmap = StreetTileCache.getOrLoad(
                        context = context,
                        scope = scope,
                        z = tileZoom,
                        x = tx,
                        y = ty,
                        onLoaded = { tileLoadTick += 1 },
                    )
                    if (bitmap != null) {
                        drawImage(
                            image = bitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                        )
                    } else {
                        // Якщо точний тайл ще вантажиться, пробуємо батьківський тайл z-1
                        val parentZ = tileZoom - 1
                        if (parentZ >= 10) {
                            val parentBitmap = StreetTileCache.getCached(parentZ, tx / 2, ty / 2)
                            if (parentBitmap != null) {
                                val srcX = (tx % 2) * 128
                                val srcY = (ty % 2) * 128
                                drawImage(
                                    image = parentBitmap,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(128, 128),
                                    dstOffset = dstOffset,
                                    dstSize = dstSize,
                                )
                            }
                        }
                    }
                }
            }

            // Легке тонування для високої контрастності маркерів освітлення
            drawRect(color = CanvasBase.copy(alpha = 0.14f))

            // 3. Коло радіуса 1500 м навколо реальної геопозиції користувача (ПЗ16)
            if (userPoint != null) {
                val uTileX = WebMercator.lonToTileX(userPoint.longitude, tileZoom)
                val uTileY = WebMercator.latToTileY(userPoint.latitude, tileZoom)
                val ux = cx + ((uTileX - centerTileX) * tileSizePx).toFloat()
                val uy = cy + ((uTileY - centerTileY) * tileSizePx).toFloat()
                val uOffset = Offset(ux, uy)

                val metersPerPixel = (156543.03392 * cos(Math.toRadians(userPoint.latitude))) /
                    2.0.pow(renderZoom.toDouble())
                val radiusPx = (GeoCalculator.DEFAULT_NEARBY_RADIUS_METERS / metersPerPixel).toFloat()

                drawCircle(
                    color = AzureAccent.copy(alpha = 0.16f),
                    radius = radiusPx,
                    center = uOffset,
                )
                drawCircle(
                    color = Color(0xFF0288D1).copy(alpha = 0.88f),
                    radius = radiusPx,
                    center = uOffset,
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)),
                    ),
                )

                // Синій GPS-маркер користувача з білою обводкою
                drawCircle(
                    color = AzureAccent.copy(alpha = 0.30f),
                    radius = 22f,
                    center = uOffset,
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = uOffset,
                )
                drawCircle(
                    color = Color(0xFF1976D2),
                    radius = 9f,
                    center = uOffset,
                )
            }

            // 4. Маркери-шпильки світильників (вимкнені -> увімкнені -> обраний)
            val ordered = devices.sortedWith(
                compareBy<LightDevice> { it.id == selectedDevice?.id }.thenBy { it.isPoweredOn },
            )
            ordered.forEach { device ->
                val isSelected = device.id == selectedDevice?.id
                val isNearby = device.id in nearbyDistancesMeters
                val dTileX = WebMercator.lonToTileX(device.longitude, tileZoom)
                val dTileY = WebMercator.latToTileY(device.latitude, tileZoom)
                val x = cx + ((dTileX - centerTileX) * tileSizePx).toFloat()
                val y = cy + ((dTileY - centerTileY) * tileSizePx).toFloat()

                if (x < -48f || x > size.width + 48f || y < -48f || y > size.height + 48f) {
                    return@forEach
                }

                val pinColor = when {
                    isSelected -> Color(0xFF00B0FF)
                    device.isPoweredOn -> Color(0xFFFFB300)
                    else -> Color(0xFF7E57C2)
                }

                // Зелене кільце для об'єктів у радіусі 1.5 км (ПЗ16)
                if (isNearby) {
                    drawCircle(
                        color = EmeraldSuccess.copy(alpha = 0.25f),
                        radius = if (isSelected) 24f else 18f,
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = EmeraldSuccess,
                        radius = if (isSelected) 24f else 18f,
                        center = Offset(x, y),
                        style = Stroke(width = 3f),
                    )
                }

                // Тінь під шпилькою
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = if (isSelected) 8f else 6f,
                    center = Offset(x, y + 2f),
                )

                // Форма класичної картографічної шпильки (Pin)
                val headRadius = if (isSelected) 15f else 11.5f
                val headCenterY = y - headRadius * 1.45f
                val pointerPath = Path().apply {
                    moveTo(x - headRadius * 0.72f, headCenterY + headRadius * 0.45f)
                    lineTo(x + headRadius * 0.72f, headCenterY + headRadius * 0.45f)
                    lineTo(x, y)
                    close()
                }
                drawPath(path = pointerPath, color = Color(0xFF161B22))
                drawCircle(
                    color = Color(0xFF161B22),
                    radius = headRadius + 2.2f,
                    center = Offset(x, headCenterY),
                )
                drawPath(path = pointerPath, color = pinColor)
                drawCircle(
                    color = pinColor,
                    radius = headRadius,
                    center = Offset(x, headCenterY),
                )
                drawCircle(
                    color = if (device.isPoweredOn) Color(0xFFFFF8E1) else Color(0xFFEDE7F6),
                    radius = headRadius * 0.42f,
                    center = Offset(x, headCenterY),
                )
            }

            // 5. Тимчасовий маркер обраної на мапі точки для додавання світильника (A4)
            if (pickedPoint != null) {
                val pTileX = WebMercator.lonToTileX(pickedPoint.longitude, tileZoom)
                val pTileY = WebMercator.latToTileY(pickedPoint.latitude, tileZoom)
                val px = cx + ((pTileX - centerTileX) * tileSizePx).toFloat()
                val py = cy + ((pTileY - centerTileY) * tileSizePx).toFloat()
                val headR = 14f
                val headY = py - headR * 1.45f
                val pickPath = Path().apply {
                    moveTo(px - headR * 0.72f, headY + headR * 0.45f)
                    lineTo(px + headR * 0.72f, headY + headR * 0.45f)
                    lineTo(px, py)
                    close()
                }
                drawCircle(color = EmeraldSuccess.copy(alpha = 0.25f), radius = 22f, center = Offset(px, py))
                drawPath(path = pickPath, color = Color(0xFF161B22))
                drawCircle(color = Color(0xFF161B22), radius = headR + 2.2f, center = Offset(px, headY))
                drawPath(path = pickPath, color = EmeraldSuccess)
                drawCircle(color = EmeraldSuccess, radius = headR, center = Offset(px, headY))
                drawCircle(color = Color.White, radius = headR * 0.4f, center = Offset(px, headY))
            }
        }
    }
}

private object WebMercator {
    fun lonToTileX(lon: Double, z: Int): Double {
        val n = (1 shl z).toDouble()
        return ((lon + 180.0) / 360.0) * n
    }

    fun latToTileY(lat: Double, z: Int): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val rad = Math.toRadians(clamped)
        val n = (1 shl z).toDouble()
        return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n
    }

    fun tileXToLon(tileX: Double, z: Int): Double {
        val n = (1 shl z).toDouble()
        return tileX / n * 360.0 - 180.0
    }

    fun tileYToLat(tileY: Double, z: Int): Double {
        val n = (1 shl z).toDouble()
        val m = Math.PI - 2.0 * Math.PI * tileY / n
        return Math.toDegrees(atan(sinh(m)))
    }
}

private object StreetTileCache {
    private val memoryCache = LruCache<String, ImageBitmap>(180)
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    fun getCached(z: Int, x: Int, y: Int): ImageBitmap? {
        return memoryCache.get("$z/$x/$y")
    }

    fun getOrLoad(
        context: Context,
        scope: CoroutineScope,
        z: Int,
        x: Int,
        y: Int,
        onLoaded: () -> Unit,
    ): ImageBitmap? {
        val key = "$z/$x/$y"
        memoryCache.get(key)?.let { return it }

        if (inFlight.add(key)) {
            val cacheDir = File(context.cacheDir, "osm_street_tiles_v2")
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmap = fetchTileWithDiskCache(cacheDir, z, x, y)
                    if (bitmap != null) {
                        memoryCache.put(key, bitmap)
                        withContext(Dispatchers.Main) {
                            onLoaded()
                        }
                    }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return null
    }

    private fun fetchTileWithDiskCache(
        cacheDir: File,
        z: Int,
        x: Int,
        y: Int,
    ): ImageBitmap? {
        val file = File(cacheDir, "${z}_${x}_${y}.png")
        if (file.isFile && file.length() > 100L) {
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()?.let { return it }
        }

        val urls = listOf(
            "https://tile.openstreetmap.org/$z/$x/$y.png",
        )
        for (urlStr in urls) {
            val bytes = runCatching {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    setRequestProperty(
                        "User-Agent",
                        "IoTLightingPrac4/1.0 (KPI RPZMP Variant13; Android)",
                    )
                }
                try {
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { it.readBytes() }
                    } else {
                        null
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()

            if (bytes != null && bytes.size > 100) {
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                if (decoded != null) {
                    runCatching {
                        cacheDir.mkdirs()
                        file.writeBytes(bytes)
                    }
                    return decoded
                }
            }
        }
        return null
    }
}
