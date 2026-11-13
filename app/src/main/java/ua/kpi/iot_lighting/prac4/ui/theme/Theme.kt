package ua.kpi.iot_lighting.prac4.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MidnightDuskScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = CanvasBase,
    primaryContainer = AmberContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AzureAccent,
    onSecondary = CanvasBase,
    secondaryContainer = AzureContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    background = CanvasBase,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    error = CrimsonError,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = TextPrimary,
)

@Composable
fun IoTLightingTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MidnightDuskScheme,
        typography = AppTypography,
        content = content,
    )
}
