package com.example.locationalarm.data.model

/**
 * Represents the current operational state of the location alarm system.
 */
sealed interface AlarmState {
    /**
     * System is idle; no destination or background location monitoring active.
     */
    data object Idle : AlarmState

    /**
     * Location alarm is actively tracking in background.
     * @param destination Target destination being monitored.
     * @param currentDistanceMeters Last measured distance in meters, or null if location not yet fixed.
     */
    data class Active(
        val destination: Destination,
        val currentDistanceMeters: Float? = null
    ) : AlarmState

    /**
     * Destination radius reached! Alarm tone and vibration actively playing.
     * @param destination Target destination that triggered the alarm.
     * @param triggerDistanceMeters Exact distance in meters when alarm triggered.
     */
    data class Triggered(
        val destination: Destination,
        val triggerDistanceMeters: Float
    ) : AlarmState
}
