package com.example.locationalarm.data.model

/**
 * Represents a target destination configured by the user.
 *
 * @param latitude Target latitude in degrees (-90.0 to 90.0)
 * @param longitude Target longitude in degrees (-180.0 to 180.0)
 * @param name Display label or description for the destination
 * @param radiusMeters Distance radius threshold in meters (e.g., 200m to 2000m)
 */
data class Destination(
    val latitude: Double,
    val longitude: Double,
    val name: String = "Selected Target",
    val radiusMeters: Float = 500f
) {
    companion object {
        const val MIN_RADIUS_METERS = 200f
        const val MAX_RADIUS_METERS = 2000f
        const val DEFAULT_RADIUS_METERS = 500f

        // Default initial location (e.g. New York City / Central Station)
        val DEFAULT = Destination(
            latitude = 40.7580,
            longitude = -73.9855,
            name = "Times Square, NYC",
            radiusMeters = DEFAULT_RADIUS_METERS
        )
    }
}
