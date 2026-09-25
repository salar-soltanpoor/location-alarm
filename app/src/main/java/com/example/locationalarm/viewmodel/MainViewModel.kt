package com.example.locationalarm.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locationalarm.data.model.AlarmState
import com.example.locationalarm.data.model.Destination
import com.example.locationalarm.data.repository.AlarmRepository
import com.example.locationalarm.service.LocationAlarmService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AlarmRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val targetDestination: StateFlow<Destination> = repository.targetDestination
    val alarmState: StateFlow<AlarmState> = repository.alarmState

    private val _latitudeInput = MutableStateFlow(Destination.DEFAULT.latitude.toString())
    val latitudeInput: StateFlow<String> = _latitudeInput.asStateFlow()

    private val _longitudeInput = MutableStateFlow(Destination.DEFAULT.longitude.toString())
    val longitudeInput: StateFlow<String> = _longitudeInput.asStateFlow()

    private val _destinationNameInput = MutableStateFlow(Destination.DEFAULT.name)
    val destinationNameInput: StateFlow<String> = _destinationNameInput.asStateFlow()

    private val _radiusInput = MutableStateFlow(Destination.DEFAULT.radiusMeters)
    val radiusInput: StateFlow<Float> = _radiusInput.asStateFlow()

    init {
        viewModelScope.launch {
            repository.targetDestination.collect { dest ->
                _latitudeInput.value = dest.latitude.toString()
                _longitudeInput.value = dest.longitude.toString()
                _destinationNameInput.value = dest.name
                _radiusInput.value = dest.radiusMeters
            }
        }
    }

    fun onLatitudeChanged(latStr: String) {
        _latitudeInput.value = latStr
        latStr.toDoubleOrNull()?.let { lat ->
            if (lat in -90.0..90.0) {
                repository.updateCoordinates(latitude = lat, longitude = repository.targetDestination.value.longitude)
            }
        }
    }

    fun onLongitudeChanged(lngStr: String) {
        _longitudeInput.value = lngStr
        lngStr.toDoubleOrNull()?.let { lng ->
            if (lng in -180.0..180.0) {
                repository.updateCoordinates(latitude = repository.targetDestination.value.latitude, longitude = lng)
            }
        }
    }

    fun onDestinationNameChanged(name: String) {
        _destinationNameInput.value = name
        repository.updateCoordinates(
            latitude = repository.targetDestination.value.latitude,
            longitude = repository.targetDestination.value.longitude,
            name = name
        )
    }

    fun onRadiusChanged(radiusMeters: Float) {
        _radiusInput.value = radiusMeters
        repository.updateRadius(radiusMeters)
    }

    fun onMapLocationSelected(lat: Double, lng: Double) {
        val roundedLat = String.format(Locale.US, "%.5f", lat).toDoubleOrNull() ?: lat
        val roundedLng = String.format(Locale.US, "%.5f", lng).toDoubleOrNull() ?: lng
        _latitudeInput.value = roundedLat.toString()
        _longitudeInput.value = roundedLng.toString()
        repository.updateCoordinates(latitude = roundedLat, longitude = roundedLng)
    }

    fun applyPresetDestination(name: String, lat: Double, lng: Double) {
        _destinationNameInput.value = name
        _latitudeInput.value = lat.toString()
        _longitudeInput.value = lng.toString()
        repository.updateDestination(
            Destination(
                latitude = lat,
                longitude = lng,
                name = name,
                radiusMeters = repository.targetDestination.value.radiusMeters
            )
        )
    }

    fun toggleLocationAlarm() {
        when (alarmState.value) {
            is AlarmState.Idle -> startLocationAlarmService()
            is AlarmState.Active, is AlarmState.Triggered -> stopLocationAlarmService()
        }
    }

    fun startLocationAlarmService() {
        val intent = Intent(context, LocationAlarmService::class.java).apply {
            action = LocationAlarmService.ACTION_START_SERVICE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopLocationAlarmService() {
        val intent = Intent(context, LocationAlarmService::class.java).apply {
            action = LocationAlarmService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }
}
