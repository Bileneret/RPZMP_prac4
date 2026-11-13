package ua.kpi.iot_lighting.prac4.ui.screens.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.iot_lighting.prac4.ui.theme.AmberPrimary
import ua.kpi.iot_lighting.prac4.ui.theme.CanvasBase
import ua.kpi.iot_lighting.prac4.ui.theme.OutlineSubtle
import ua.kpi.iot_lighting.prac4.ui.theme.TextSecondary

/** Посилання на репозиторій практичної роботи №4. */
private const val GITHUB_REPO_URL = "https://github.com/Bileneret/RPZMP_prac4"

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val features = listOf(
    FeatureItem(
        icon = Icons.Default.Bolt,
        title = "Телеметрія в реальному часі",
        description = "Динамічне відстеження активного навантаження освітлювальної мережі та миттєвий перерахунок показників.",
    ),
    FeatureItem(
        icon = Icons.Default.Map,
        title = "Інтерактивна карта і список",
        description = "Двостороння синхронізація між переліком світильників і маркерами на карті міста.",
    ),
    FeatureItem(
        icon = Icons.Default.FilterList,
        title = "Пошук, фільтрація та сортування",
        description = "Швидкий відбір світильників за назвою, станом живлення, зоною та потужністю з синхронним оновленням карти.",
    ),
    FeatureItem(
        icon = Icons.Default.MyLocation,
        title = "GPS-позиція та вузли поруч",
        description = "Визначення розташування користувача та підсвічування світильників у радіусі 1,5 км.",
    ),
    FeatureItem(
        icon = Icons.Default.Cloud,
        title = "Погодний радник і прогноз витрат",
        description = "Рекомендації на основі природної освітленості та розрахунок добового споживання енергії й вартості.",
    ),
    FeatureItem(
        icon = Icons.Default.Security,
        title = "Рольовий доступ та офлайн-кеш",
        description = "Розмежування прав адміністратора і користувача та збереження даних у локальному сховищі.",
    ),
)

private val techStack = listOf(
    "Мова" to "Kotlin",
    "UI" to "Jetpack Compose, Material 3",
    "Архітектура" to "MVVM, Repository, Data Source",
    "Асинхронність" to "Coroutines, Flow, StateFlow",
    "Карти та GPS" to "Google Maps, OSM, FusedLocation",
    "Мережа" to "Retrofit, Gson",
    "Сховище" to "Room, KSP",
    "Авторизація" to "JWT (mock), Interceptor",
)

/**
 * Екран «Про застосунок»: опис практичної роботи №4, основних можливостей,
 * технологічного стеку та джерел даних (у єдиному стилі з ПР2 та ПР3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Про застосунок",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "IoT Lighting",
                style = MaterialTheme.typography.headlineMedium,
                color = AmberPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Система освітлення з IoT",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader("Про проєкт")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Практична робота №4 · Варіант 13",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Недяк Олександр Сергійович · група ТВ-32",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android-застосунок для моніторингу та керування міськими світильниками Кривого Рогу з картою вулиць, геолокацією, фільтрацією, сортуванням і потоковою телеметрією.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Можливості")
            Spacer(modifier = Modifier.height(10.dp))
            features.forEachIndexed { index, feature ->
                FeatureRow(feature)
                if (index < features.lastIndex) {
                    ThinDivider()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Технології")
            Spacer(modifier = Modifier.height(10.dp))
            techStack.forEachIndexed { index, (label, value) ->
                TechRow(label = label, value = value)
                if (index < techStack.lastIndex) {
                    ThinDivider()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Дані")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Джерело світильників: локальний HTTP-сервер із вузлами освітлення на реальних вулицях Кривого Рогу. Погодні умови надходять із публічного сервісу Open-Meteo, а результати кешуються в базі Room для роботи офлайн.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { uriHandler.openUri(GITHUB_REPO_URL) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = CanvasBase,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GitHub-репозиторій",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = GITHUB_REPO_URL.removePrefix("https://"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AmberPrimary,
    )
}

@Composable
private fun FeatureRow(feature: FeatureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = AmberPrimary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = OutlineSubtle.copy(alpha = 0.6f),
    )
}
