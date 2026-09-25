package com.example.locationalarm

import com.example.locationalarm.util.LocationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun calculateDistanceMeters_samePoint_returnsZero() {
        val lat = 40.7580
        val lng = -73.9855
        val distance = LocationUtils.calculateDistanceMeters(lat, lng, lat, lng)
        assertEquals(0f, distance, 0.1f)
    }

    @Test
    fun calculateDistanceMeters_timesSquareToCentralPark_returnsApproximateDistance() {
        // Times Square: (40.7580, -73.9855)
        // Central Park South: (40.7660, -73.9772)
        val distance = LocationUtils.calculateDistanceMeters(
            40.7580, -73.9855,
            40.7660, -73.9772
        )
        // Distance should be approximately 1100 - 1300 meters
        assertTrue("Distance $distance m should be > 1000m", distance > 1000f)
        assertTrue("Distance $distance m should be < 1500m", distance < 1500f)
    }

    @Test
    fun formatDistance_metersAndKilometers() {
        assertEquals("Calculating distance…", LocationUtils.formatDistance(null))
        assertEquals("450 m", LocationUtils.formatDistance(450f))
        assertEquals("1.50 km", LocationUtils.formatDistance(1500f))
    }
}
