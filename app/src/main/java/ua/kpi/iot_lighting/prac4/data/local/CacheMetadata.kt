package ua.kpi.iot_lighting.prac4.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import ua.kpi.iot_lighting.prac4.domain.model.WeatherInsight

interface CacheMetadata {
    fun lastSyncedAtMillis(): Long?
    fun markSyncedNow(timestampMillis: Long = System.currentTimeMillis())
    fun clearSyncTimestamp()

    fun readWeatherCache(): WeatherInsight?
    fun saveWeatherCache(insight: WeatherInsight)
    fun clearAll()
}

class SharedPreferencesCacheMetadata(
    private val prefs: SharedPreferences,
    private val gson: Gson = Gson(),
) : CacheMetadata {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    override fun lastSyncedAtMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_SYNC_MS, -1L)
        return value.takeIf { it > 0L }
    }

    override fun markSyncedNow(timestampMillis: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC_MS, timestampMillis) }
    }

    override fun clearSyncTimestamp() {
        prefs.edit { remove(KEY_LAST_SYNC_MS) }
    }

    override fun readWeatherCache(): WeatherInsight? {
        val json = prefs.getString(KEY_WEATHER_JSON, null) ?: return null
        return runCatching { gson.fromJson(json, WeatherInsight::class.java) }.getOrNull()
    }

    override fun saveWeatherCache(insight: WeatherInsight) {
        prefs.edit { putString(KEY_WEATHER_JSON, gson.toJson(insight)) }
    }

    override fun clearAll() {
        prefs.edit {
            remove(KEY_LAST_SYNC_MS)
            remove(KEY_WEATHER_JSON)
        }
    }

    companion object {
        private const val PREFS_NAME = "iot_lighting_prac4_cache"
        private const val KEY_LAST_SYNC_MS = "last_synced_at_ms"
        private const val KEY_WEATHER_JSON = "last_weather_insight"
    }
}
