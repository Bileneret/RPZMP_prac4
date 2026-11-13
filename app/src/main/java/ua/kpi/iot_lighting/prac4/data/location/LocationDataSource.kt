package ua.kpi.iot_lighting.prac4.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LocationState
import kotlin.coroutines.resume

interface LocationDataSource {
    fun hasLocationPermission(): Boolean

    fun observeLocation(
        hasPermission: Boolean,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
    ): Flow<LocationState>

    suspend fun fetchCurrentLocation(): LocationState

    companion object {
        const val DEFAULT_INTERVAL_MS = 10_000L
    }
}

/**
 * Джерело геолокації на базі [FusedLocationProviderClient] (ПЗ16).
 * Забезпечує безпечну перевірку дозволів та реактивний потік координат через `callbackFlow`.
 */
class FusedLocationDataSource(
    private val context: Context,
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : LocationDataSource {

    /** Повертає true, якщо надано точний або наближений дозвіл на геолокацію. */
    override fun hasLocationPermission(): Boolean {
        val appContext = context.applicationContext
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Запускає періодичні оновлення GPS та знімає слухач при закритті потоку `awaitClose`. */
    @SuppressLint("MissingPermission")
    override fun observeLocation(
        hasPermission: Boolean,
        intervalMs: Long,
    ): Flow<LocationState> = callbackFlow {
        if (!hasPermission || !hasLocationPermission()) {
            trySend(LocationState.PermissionRequired)
            awaitClose { }
            return@callbackFlow
        }

        trySend(LocationState.Loading)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis((intervalMs / 2L).coerceAtLeast(2_000L))
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(location.toLocationState())
            }
        }

        try {
            client.lastLocation.addOnSuccessListener { cached ->
                if (cached != null) {
                    trySend(cached.toLocationState())
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    trySend(
                        LocationState.Unavailable(
                            error.localizedMessage ?: "Служба геолокації недоступна",
                        ),
                    )
                }
        } catch (_: SecurityException) {
            trySend(LocationState.PermissionRequired)
        } catch (ex: Exception) {
            trySend(
                LocationState.Unavailable(
                    ex.localizedMessage ?: "Не вдалося запустити GPS-оновлення",
                ),
            )
        }

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(): LocationState {
        if (!hasLocationPermission()) return LocationState.PermissionRequired

        val tokenSource = CancellationTokenSource()
        return try {
            val fresh = suspendCancellableCoroutine<Location?> { cont ->
                cont.invokeOnCancellation { tokenSource.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
            if (fresh != null) {
                fresh.toLocationState()
            } else {
                val last = suspendCancellableCoroutine<Location?> { cont ->
                    client.lastLocation
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
                last?.toLocationState()
                    ?: LocationState.Unavailable("Не вдалося отримати поточні координати GPS")
            }
        } catch (_: SecurityException) {
            LocationState.PermissionRequired
        }
    }

    private fun Location.toLocationState(): LocationState.Available {
        return LocationState.Available(
            point = GeoPoint(latitude = latitude, longitude = longitude),
            accuracyMeters = accuracy,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
}
