package ua.kpi.iot_lighting.prac4.data.error

import android.content.Context
import android.util.Log
import java.io.File

interface ErrorLogger {
    fun logError(tag: String, error: Throwable, context: String? = null)
}

/**
 * Журнал помилок у Logcat та файл `error_log.txt` у внутрішній теці застосунку
 * (відповідно до вимог ПЗ12–ПЗ13 з автоматичною ротацією після 256 КБ).
 */
class AndroidErrorLogger(appContext: Context? = null) : ErrorLogger {
    private val file: File? = appContext?.let { File(it.filesDir, ERROR_LOG_NAME) }
    private val rotatedFile: File? = appContext?.let { File(it.filesDir, ROTATED_LOG_NAME) }

    override fun logError(tag: String, error: Throwable, context: String?) {
        val prefix = context?.let { "[$it] " } ?: ""
        val line = "$prefix${error::class.simpleName}: ${error.message}"
        runCatching {
            Log.w(tag, line, error)
        }
        val target = file ?: return
        val rotated = rotatedFile ?: return
        runCatching {
            if (target.exists() && target.length() > MAX_LOG_BYTES) {
                target.renameTo(rotated)
            }
            target.appendText("${System.currentTimeMillis()} [$tag] $line\n")
        }
    }

    fun readAll(): String =
        if (file != null && file.exists()) file.readText() else ""

    private companion object {
        const val ERROR_LOG_NAME = "error_log.txt"
        const val ROTATED_LOG_NAME = "error_log.1.txt"
        const val MAX_LOG_BYTES = 256 * 1024L
    }
}
