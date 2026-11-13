package ua.kpi.iot_lighting.prac4.ui.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.kpi.iot_lighting.prac4.domain.logic.GeoCalculator

/**
 * Результат прямого або зворотного геокодування для прив'язки світильника до вулиці/адреси.
 */
data class AddressSuggestion(
    val displayAddress: String,
    val suggestedName: String,
    val suggestedZone: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Сервіс адресного геокодування через OpenStreetMap Nominatim (без API-ключа)
 * із кешуванням у пам'яті та офлайн-fallback по 25 перевірених адресах Кривого Рогу.
 */
object StreetGeocoder {
    private const val USER_AGENT = "KPI-IoTLighting-Prac4/1.0 (Academic Project)"
    private const val TIMEOUT_MS = 3500

    private val reverseCache = ConcurrentHashMap<String, AddressSuggestion>()
    private val searchCache = ConcurrentHashMap<String, List<AddressSuggestion>>()

    /** Статичний довідник 25 перевірених адрес та об'єктів Кривого Рогу для миттєвої підказки й офлайн-режиму. */
    val verifiedKryvyiRihStreets: List<AddressSuggestion> = listOf(
        AddressSuggestion("пр. Поштовий (Карнаватка)", "Магістраль пр. Поштовий (Карнаватка)", "Вулиця", 47.919217, 33.356511),
        AddressSuggestion("вул. Соборності (Соцмісто)", "Магістраль вул. Соборності (Соцмісто)", "Вулиця", 47.900649, 33.405048),
        AddressSuggestion("пр. Миру (95 квартал)", "Магістраль пр. Миру (95 квартал)", "Вулиця", 47.907563, 33.386073),
        AddressSuggestion("вул. Героїв АТО (96 квартал)", "Вулиця Героїв АТО (96 квартал)", "Вулиця", 47.906481, 33.408299),
        AddressSuggestion("вул. Віталія Матусевича", "Алея вул. Віталія Матусевича", "Вулиця", 47.904497, 33.394979),
        AddressSuggestion("пр. Металургів (Стріла)", "Магістраль пр. Металургів (Стріла)", "Вулиця", 47.897020, 33.394239),
        AddressSuggestion("вул. Кобилянського (Шахтопрохідка)", "Вулиця Кобилянського (Шахтопрохідка)", "Вулиця", 47.897400, 33.362596),
        AddressSuggestion("вул. Січеславська (КРЕС)", "Магістраль вул. Січеславської (КРЕС)", "Вулиця", 47.988740, 33.438355),
        AddressSuggestion("пр. Перемоги (Інгулець)", "Проспект Перемоги (Інгулець)", "Вулиця", 47.683482, 33.163453),
        AddressSuggestion("вул. Володимира Великого", "Вулиця Володимира Великого", "Вулиця", 47.945350, 33.426139),
        AddressSuggestion("вул. Генерала Радієвського", "Вулиця Генерала Радієвського", "Вулиця", 47.943293, 33.435135),
        AddressSuggestion("вул. Миколи Світальського (Дубова балка)", "Вулиця Миколи Світальського (Дубова балка)", "Вулиця", 47.971242, 33.447372),
        AddressSuggestion("ТРК «Терра», 5-й Зарічний, 11А", "Паркінг ТРК «Терра» (5-й Зарічний, 11А)", "Паркування", 48.001150, 33.482281),
        AddressSuggestion("ТРЦ «Сонячна Галерея», майдан Химиченка", "Паркінг ТРЦ «Сонячна Галерея» (майдан Химиченка)", "Паркування", 47.946885, 33.440868),
        AddressSuggestion("ТРЦ «Victory Plaza», Центральний пр.", "Паркінг ТРЦ «Victory Plaza» (Центральний пр.)", "Паркування", 47.904265, 33.357719),
        AddressSuggestion("вул. Братів Кожакіних, головний корпус КНУ", "Фасад головного корпусу КНУ (Братів Кожакіних)", "Фасад", 47.905689, 33.401417),
        AddressSuggestion("парк ім. Федора Мершавцева (Гданцівка)", "Алея парку ім. Федора Мершавцева (Гданцівка)", "Парк", 47.899752, 33.331585),
        AddressSuggestion("парк Героїв (Соцмісто)", "Алея парку Героїв (Соцмісто)", "Парк", 47.905447, 33.390146),
        AddressSuggestion("парк «Шахтарський» (Ставки)", "Алея парку «Шахтарський» (Ставки)", "Парк", 48.026650, 33.477442),
        AddressSuggestion("стадіон «Металург»", "Фасад стадіону «Металург»", "Фасад", 47.895629, 33.395118),
        AddressSuggestion("площа Захисників України", "Магістраль площі Захисників України", "Вулиця", 47.908416, 33.343479),
        AddressSuggestion("пр. 200-річчя Кривого Рогу", "Магістраль пр. 200-річчя Кривого Рогу", "Вулиця", 47.950239, 33.435728),
        AddressSuggestion("вул. Тбіліська (71 квартал)", "Вулиця Тбіліська (71 квартал)", "Вулиця", 47.880468, 33.364902),
        AddressSuggestion("вул. Саксаганська (Бажанове)", "Вулиця Саксаганська (Бажанове)", "Вулиця", 47.992298, 33.444048),
        AddressSuggestion("вокзал «Кривий Ріг-Головний»", "Фасад вокзалу «Кривий Ріг-Головний»", "Фасад", 47.912563, 33.451650),
    )

    suspend fun reverseGeocode(lat: Double, lng: Double): AddressSuggestion = withContext(Dispatchers.IO) {
        val cacheKey = String.format(Locale.US, "%.4f,%.4f", lat, lng)
        reverseCache[cacheKey]?.let { return@withContext it }

        val remote = runCatching {
            val url = String.format(
                Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f&accept-language=uk&zoom=18",
                lat,
                lng,
            )
            fetchJson(url)?.let { elem ->
                if (!elem.isJsonObject) return@let null
                parseReverseObject(elem.asJsonObject, lat, lng)
            }
        }.getOrNull()

        val resolved = remote ?: fallbackNearest(lat, lng)
        reverseCache[cacheKey] = resolved
        resolved
    }

    suspend fun searchByAddress(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            return@withContext verifiedKryvyiRihStreets.take(6)
        }
        val cacheKey = normalized.lowercase(Locale("uk"))
        searchCache[cacheKey]?.let { cached -> return@withContext cached }

        val localMatches = verifiedKryvyiRihStreets.filter { item ->
            item.displayAddress.contains(normalized, ignoreCase = true) ||
                item.suggestedName.contains(normalized, ignoreCase = true)
        }

        val remoteMatches = runCatching {
            val encoded = URLEncoder.encode("Кривий Ріг, $normalized", "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&accept-language=uk&limit=5&addressdetails=1"
            fetchJson(url)?.let { elem ->
                if (!elem.isJsonArray) return@let emptyList()
                elem.asJsonArray.mapNotNull { item ->
                    if (!item.isJsonObject) null else parseSearchObject(item.asJsonObject)
                }
            }.orEmpty()
        }.getOrDefault(emptyList())

        val combined = buildList {
            val seen = HashSet<String>()
            (localMatches + remoteMatches).forEach { suggestion ->
                val dedupeKey = suggestion.displayAddress.lowercase(Locale("uk"))
                if (seen.add(dedupeKey)) {
                    add(suggestion)
                }
            }
        }.take(6)

        if (combined.isNotEmpty()) {
            searchCache[cacheKey] = combined
        }
        combined
    }

    private fun fetchJson(urlString: String) = run {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    JsonParser.parseReader(reader)
                }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseReverseObject(obj: JsonObject, lat: Double, lng: Double): AddressSuggestion? {
        val addr = obj.getAsJsonObject("address") ?: return null
        val roadPart = extractPrimaryRoad(addr) ?: return null
        val number = addr.getAsStringOrNull("house_number")
        val district = addr.getAsStringOrNull("suburb") ?: addr.getAsStringOrNull("city_district")
        val baseStreet = if (number != null) "$roadPart, $number" else roadPart
        val fullAddress = if (district != null && !baseStreet.contains(district, ignoreCase = true)) {
            "$baseStreet ($district)"
        } else {
            baseStreet
        }
        val zone = inferZone(addr, fullAddress)
        return AddressSuggestion(
            displayAddress = fullAddress,
            suggestedName = "Ліхтар $baseStreet",
            suggestedZone = zone,
            latitude = lat,
            longitude = lng,
        )
    }

    private fun parseSearchObject(obj: JsonObject): AddressSuggestion? {
        val lat = obj.getAsStringOrNull("lat")?.toDoubleOrNull() ?: return null
        val lng = obj.getAsStringOrNull("lon")?.toDoubleOrNull() ?: return null
        val addr = obj.getAsJsonObject("address")
        val roadPart = addr?.let(::extractPrimaryRoad)
        val number = addr?.getAsStringOrNull("house_number")
        val district = addr?.getAsStringOrNull("suburb") ?: addr?.getAsStringOrNull("city_district")

        val baseStreet = when {
            roadPart != null && number != null -> "$roadPart, $number"
            roadPart != null -> roadPart
            else -> obj.getAsStringOrNull("name")
                ?: obj.getAsStringOrNull("display_name")?.substringBefore(',')
                ?: return null
        }
        val display = if (district != null && !baseStreet.contains(district, ignoreCase = true)) {
            "$baseStreet ($district)"
        } else {
            baseStreet
        }
        val zone = if (addr != null) inferZone(addr, display) else "Вулиця"
        return AddressSuggestion(
            displayAddress = display,
            suggestedName = "Ліхтар $baseStreet",
            suggestedZone = zone,
            latitude = lat,
            longitude = lng,
        )
    }

    private fun extractPrimaryRoad(addr: JsonObject): String? {
        val keys = listOf("road", "pedestrian", "footway", "amenity", "shop", "leisure", "suburb")
        return keys.firstNotNullOfOrNull { key -> addr.getAsStringOrNull(key) }
    }

    private fun inferZone(addr: JsonObject, label: String): String {
        val lower = label.lowercase(Locale("uk"))
        return when {
            lower.contains("паркінг") || lower.contains("паркуван") ||
                lower.contains("трк") || lower.contains("трц") -> "Паркування"
            addr.has("leisure") || lower.contains("парк") || lower.contains("сквер") -> "Парк"
            addr.has("amenity") || lower.contains("вокзал") || lower.contains("корпус") || lower.contains("стадіон") -> "Фасад"
            else -> "Вулиця"
        }
    }

    private fun fallbackNearest(lat: Double, lng: Double): AddressSuggestion {
        val nearest = verifiedKryvyiRihStreets.minBy { item ->
            GeoCalculator.distanceMeters(lat, lng, item.latitude, item.longitude)
        }
        val distMeters = GeoCalculator.distanceMeters(lat, lng, nearest.latitude, nearest.longitude).toInt()
        val label = if (distMeters <= 400) {
            nearest.displayAddress
        } else {
            "поруч із ${nearest.displayAddress}"
        }
        return AddressSuggestion(
            displayAddress = label,
            suggestedName = "Ліхтар ${nearest.displayAddress}",
            suggestedZone = nearest.suggestedZone,
            latitude = lat,
            longitude = lng,
        )
    }

    private fun JsonObject.getAsStringOrNull(key: String): String? {
        val elem = get(key) ?: return null
        if (elem.isJsonNull) return null
        return elem.asString.trim().takeIf { it.isNotEmpty() }
    }
}
