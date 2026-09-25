package com.example.locationalarm.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.locationalarm.data.model.AlarmState
import com.example.locationalarm.data.model.Destination
import com.example.locationalarm.data.repository.AlarmRepository
import com.example.locationalarm.util.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationAlarmService : Service() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var notificationHelper: AlarmNotificationHelper

    @Inject
    lateinit var audioVibrationManager: AudioVibrationManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var targetDestination: Destination? = null
    private var isAlarmTriggered = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationAlarmService onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        serviceScope.launch {
            alarmRepository.targetDestination.collectLatest { destination ->
                targetDestination = destination
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_START_SERVICE -> startLocationTracking()
            ACTION_STOP_SERVICE -> stopLocationTracking()
            ACTION_DISMISS_ALARM -> dismissAlarm()
        }

        return START_STICKY
    }

    private fun startLocationTracking() {
        isAlarmTriggered = false
        val currentDest = targetDestination ?: Destination.DEFAULT
        alarmRepository.setServiceActive(currentDest)

        val notification = notificationHelper.buildForegroundNotification(
            destinationName = currentDest.name,
            distanceMeters = null
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    AlarmNotificationHelper.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(
                    AlarmNotificationHelper.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            }
        } else {
            startForeground(AlarmNotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        requestLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation ?: return
                val currentDest = targetDestination ?: return

                val distanceMeters = LocationUtils.calculateDistanceMeters(
                    startLat = lastLocation.latitude,
                    startLng = lastLocation.longitude,
                    endLat = currentDest.latitude,
                    endLng = currentDest.longitude
                )

                Log.d(TAG, "New location update: Lat ${lastLocation.latitude}, Lng ${lastLocation.longitude}. Distance to ${currentDest.name}: $distanceMeters m")

                alarmRepository.updateCurrentDistance(distanceMeters)

                // Update foreground notification
                val updatedNotification = notificationHelper.buildForegroundNotification(
                    destinationName = currentDest.name,
                    distanceMeters = distanceMeters
                )
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(AlarmNotificationHelper.SERVICE_NOTIFICATION_ID, updatedNotification)

                // Check trigger condition
                if (distanceMeters <= currentDest.radiusMeters && !isAlarmTriggered) {
                    isAlarmTriggered = true
                    triggerAlarm(currentDest, distanceMeters)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates", e)
        }
    }

    private fun triggerAlarm(destination: Destination, distanceMeters: Float) {
        Log.d(TAG, "DESTINATION REACHED! Triggering alarm for ${destination.name} at $distanceMeters meters")
        alarmRepository.triggerAlarm(destination, distanceMeters)
        audioVibrationManager.startAlarm()
        notificationHelper.triggerAlarmNotification(destination.name, distanceMeters)
    }

    private fun dismissAlarm() {
        Log.d(TAG, "Dismissing active alarm sound/vibration")
        audioVibrationManager.stopAlarm()
        notificationHelper.cancelAlarmNotification()
    }

    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking service...")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        audioVibrationManager.stopAlarm()
        notificationHelper.cancelAlarmNotification()
        alarmRepository.stopAlarm()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationAlarmService onDestroy")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        audioVibrationManager.stopAlarm()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationAlarmService"

        const val ACTION_START_SERVICE = "com.example.locationalarm.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.locationalarm.action.STOP_SERVICE"
        const val ACTION_DISMISS_ALARM = "com.example.locationalarm.action.DISMISS_ALARM"

        private const val UPDATE_INTERVAL_MS = 5000L
        private const val FASTEST_INTERVAL_MS = 2000L
    }
}
