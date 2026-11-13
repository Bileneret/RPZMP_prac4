package ua.kpi.iot_lighting.prac4.domain.logic

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import ua.kpi.iot_lighting.prac4.domain.model.GeoPoint
import ua.kpi.iot_lighting.prac4.domain.model.LightDevice

/**
 * Геопросторові розрахунки за формулою Haversine (ПЗ16).
 */
object GeoCalculator {
    const val EARTH_RADIUS_METERS = 6_371_000.0
    const val DEFAULT_NEARBY_RADIUS_METERS = 1_500.0

    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
            cos(radLat1) * cos(radLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun findNearbyDistances(
        devices: List<LightDevice>,
        userPoint: GeoPoint?,
        radiusMeters: Double = DEFAULT_NEARBY_RADIUS_METERS,
    ): Map<Long, Int> {
        val origin = userPoint ?: return emptyMap()
        return buildMap {
            for (device in devices) {
                val meters = distanceMeters(
                    lat1 = origin.latitude,
                    lon1 = origin.longitude,
                    lat2 = device.latitude,
                    lon2 = device.longitude,
                )
                if (meters <= radiusMeters) {
                    put(device.id, meters.roundToInt())
                }
            }
        }
    }
}
