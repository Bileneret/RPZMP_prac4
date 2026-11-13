package ua.kpi.iot_lighting.prac4.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val ukLocale = Locale.forLanguageTag("uk-UA")

    fun formatPower(watts: Int): String = "$watts Вт"

    fun formatTimestamp(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0L) return "—"
        val formatter = SimpleDateFormat("dd.MM HH:mm", ukLocale)
        return formatter.format(Date(epochMillis))
    }

    fun formatTimeOnly(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0L) return "—"
        val formatter = SimpleDateFormat("HH:mm:ss", ukLocale)
        return formatter.format(Date(epochMillis))
    }

    fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format(ukLocale, "%.2f км", meters / 1000.0)
        } else {
            "$meters м"
        }
    }

    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
    }

    fun formatKwh(kwh: Double): String {
        return "${String.format(ukLocale, "%.2f", kwh)} кВт·год"
    }

    fun formatMoney(uah: Double): String {
        return "${String.format(ukLocale, "%.2f", uah)} ₴"
    }
}
