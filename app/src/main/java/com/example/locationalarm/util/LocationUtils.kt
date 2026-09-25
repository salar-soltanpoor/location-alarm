package com.example.locationalarm.util

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    /**
     * Calculates geodesic distance in meters between two geographical coordinates
     * using the Haversine formula (pure Kotlin, works in both JVM unit tests and Android runtime).
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val earthRadiusMeters = 6371000.0

        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadiusMeters * c).toFloat()
    }

    /**
     * Formats distance in meters into human readable display string.
     */
    fun formatDistance(distanceMeters: Float?): String {
        if (distanceMeters == null) return "Calculating distance…"
        return if (distanceMeters >= 1000f) {
            String.format(Locale.US, "%.2f km", distanceMeters / 1000f)
        } else {
            "${distanceMeters.toInt()} m"
        }
    }
}
