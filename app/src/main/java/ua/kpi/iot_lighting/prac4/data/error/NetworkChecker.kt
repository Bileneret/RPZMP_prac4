package ua.kpi.iot_lighting.prac4.data.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkChecker {
    fun isOnline(): Boolean
}

class AndroidNetworkChecker(
    private val context: Context,
) : NetworkChecker {
    override fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
