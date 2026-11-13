package ua.kpi.iot_lighting.prac4.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ua.kpi.iot_lighting.prac4.domain.model.LightingTotals
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight
import ua.kpi.iot_lighting.prac4.ui.theme.AmberContainer
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.AzureAccent
import ua.kpi.iot_lighting.prac4.ui.theme.AzureContainer
import ua.kpi.iot_lighting.prac4.ui.theme.CrimsonContainer
import ua.kpi.iot_lighting.prac4.ui.theme.CrimsonError
import ua.kpi.iot_lighting.prac4.ui.theme.EmeraldContainer
import ua.kpi.iot_lighting.prac4.ui.theme.EmeraldSuccess
import ua.kpi.iot_lighting.prac4.ui.theme.OutlineSubtle
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceCard
import ua.kpi.iot_lighting.prac4.ui.theme.SurfaceElevated
import ua.kpi.iot_lighting.prac4.ui.theme.TextSecondary
import ua.kpi.iot_lighting.prac4.ui.util.Formatters

@Composable
fun TelemetrySummaryCard(
    totals: LightingTotals,
    livePowerW: Int,
    weather: WeatherInsight?,
    isWeatherFallback: Boolean,
    weatherStatusNote: String?,
    recommendation: String,
    lastSyncedAtMillis: Long?,
    nearbyCount: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, OutlineSubtle),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = AmberContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live: ${Formatters.formatPower(livePowerW)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AzureAccent,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Увімкнено ${totals.activeCount} з ${totals.totalCount} · ${totals.activePowerW} Вт",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "Прогноз: ${Formatters.formatKwh(totals.energyKwhPerDay)} · ${Formatters.formatMoney(totals.costPerDay)} за добу",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (weather != null) {
                        Icon(
                            imageVector = when {
                                isWeatherFallback -> Icons.Default.CloudOff
                                !weather.isDay -> Icons.Default.NightsStay
                                else -> Icons.Default.WbSunny
                            },
                            contentDescription = null,
                            tint = when {
                                isWeatherFallback -> AmberPrimary
                                !weather.isDay -> AzureAccent
                                else -> EmeraldSuccess
                            },
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${weather.temperatureC.roundToInt()}°C",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isWeatherFallback) AmberPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Деталі телеметрії",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (weather != null) {
                        Text(
                            text = "Денне світло: ${weather.daylightPercent}% · Кривий Ріг: ${weather.temperatureC.roundToInt()}°C, хмарність ${weather.cloudCoverPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = weather?.summary ?: "Погода завантажується...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWeatherFallback) AmberPrimary else TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Оновлено: ${Formatters.formatTimestamp(lastSyncedAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    if (!weatherStatusNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Погодний fallback: $weatherStatusNote",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberPrimary,
                        )
                    }
                    if (nearbyCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "У радіусі 1,5 км навколо вас: $nearbyCount світильників",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationStatusBanner(
    locationState: LocationState,
    nearbyCount: Int,
    onRequestPermission: () -> Unit,
    onRefreshLocation: () -> Unit,
    onFocusUserLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = when (locationState) {
            is LocationState.Available -> EmeraldContainer.copy(alpha = 0.65f)
            is LocationState.PermissionRequired -> AzureContainer.copy(alpha = 0.75f)
            is LocationState.Loading -> SurfaceElevated
            is LocationState.Unavailable -> AmberContainer.copy(alpha = 0.7f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            when (locationState) {
                is LocationState.Available -> EmeraldSuccess.copy(alpha = 0.45f)
                is LocationState.PermissionRequired -> AzureAccent.copy(alpha = 0.45f)
                else -> OutlineSubtle
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                when (locationState) {
                    is LocationState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AzureAccent,
                        )
                    }
                    is LocationState.Available -> {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            tint = AzureAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val label = when (locationState) {
                    LocationState.PermissionRequired -> "Надайте GPS-доступ для пошуку поруч (≤ 1,5 км)"
                    LocationState.Loading -> "Визначення координат пристрою..."
                    is LocationState.Available ->
                        "GPS ±${locationState.accuracyMeters.roundToInt()} м · поруч (≤ 1,5 км): $nearbyCount"
                    is LocationState.Unavailable -> locationState.reason
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when (locationState) {
                LocationState.PermissionRequired -> {
                    TextButton(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.textButtonColors(contentColor = AzureAccent),
                    ) {
                        Text("Надати GPS", style = MaterialTheme.typography.labelMedium)
                    }
                }
                is LocationState.Available -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onFocusUserLocation,
                            colors = ButtonDefaults.textButtonColors(contentColor = EmeraldSuccess),
                        ) {
                            Text("До мене", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Оновити GPS",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
                is LocationState.Unavailable -> {
                    TextButton(onClick = onRefreshLocation) {
                        Text("Повторити", style = MaterialTheme.typography.labelMedium)
                    }
                }
                LocationState.Loading -> Unit
            }
        }
    }
}

@Composable
fun ErrorBanner(
    error: UiError,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CrimsonContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CrimsonError,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Row {
                if (error.isRetryable && onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text("Повторити", color = AmberPrimary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("ОК", color = TextSecondary)
                }
            }
        }
    }
}

/**
 * Уніфікований діалог підтвердження для деструктивних дій
 * (видалення світильника, вихід із акаунта) у стилі Практичної №3.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = true,
) {
    var submitted by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(text = title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(
                enabled = !submitted,
                onClick = {
                    if (submitted) return@TextButton
                    submitted = true
                    onConfirm()
                },
            ) {
                Text(
                    text = confirmLabel,
                    color = if (isDestructive) CrimsonError else AmberPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !submitted,
                onClick = onDismiss,
            ) {
                Text("Скасувати")
            }
        },
    )
}
