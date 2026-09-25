package com.example.locationalarm.data.repository

import com.example.locationalarm.data.model.AlarmState
import com.example.locationalarm.data.model.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor() {

    private val _targetDestination = MutableStateFlow(Destination.DEFAULT)
    val targetDestination: StateFlow<Destination> = _targetDestination.asStateFlow()

    private val _alarmState = MutableStateFlow<AlarmState>(AlarmState.Idle)
    val alarmState: StateFlow<AlarmState> = _alarmState.asStateFlow()

    fun updateDestination(destination: Destination) {
        _targetDestination.value = destination
    }

    fun updateRadius(radiusMeters: Float) {
        _targetDestination.value = _targetDestination.value.copy(radiusMeters = radiusMeters)
    }

    fun updateCoordinates(latitude: Double, longitude: Double, name: String = _targetDestination.value.name) {
        _targetDestination.value = _targetDestination.value.copy(
            latitude = latitude,
            longitude = longitude,
            name = name
        )
    }

    fun setServiceActive(destination: Destination = _targetDestination.value) {
        _alarmState.value = AlarmState.Active(destination = destination, currentDistanceMeters = null)
    }

    fun updateCurrentDistance(distanceMeters: Float) {
        val currentState = _alarmState.value
        if (currentState is AlarmState.Active) {
            _alarmState.value = currentState.copy(currentDistanceMeters = distanceMeters)
        }
    }

    fun triggerAlarm(destination: Destination, triggerDistanceMeters: Float) {
        _alarmState.value = AlarmState.Triggered(
            destination = destination,
            triggerDistanceMeters = triggerDistanceMeters
        )
    }

    fun stopAlarm() {
        _alarmState.value = AlarmState.Idle
    }
}
