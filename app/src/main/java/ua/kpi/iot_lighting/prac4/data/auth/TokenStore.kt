package ua.kpi.iot_lighting.prac4.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenStore(
    private val prefs: SharedPreferences,
) : TokenStorage {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    override fun read(): String? {
        return prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    override fun write(token: String?) {
        prefs.edit {
            if (token.isNullOrBlank()) {
                remove(KEY_TOKEN)
            } else {
                putString(KEY_TOKEN, token)
            }
        }
    }

    override fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    companion object {
        private const val PREFS_NAME = "iot_lighting_prac4_auth"
        private const val KEY_TOKEN = "jwt_token"
    }
}
