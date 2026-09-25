package com.example.locationalarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.locationalarm.service.AlarmNotificationHelper
import com.example.locationalarm.service.LocationAlarmService
import com.example.locationalarm.ui.screens.AlarmTriggeredScreen
import com.example.locationalarm.ui.theme.LocationAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnOnScreenAndKeyguard()

        val destinationName = intent.getStringExtra(AlarmNotificationHelper.EXTRA_DESTINATION_NAME)
            ?: "Target Destination"
        val distanceMeters = intent.getFloatExtra(AlarmNotificationHelper.EXTRA_DISTANCE_METERS, 0f)

        setContent {
            LocationAlarmTheme {
                AlarmTriggeredScreen(
                    destinationName = destinationName,
                    distanceMeters = distanceMeters,
                    onDismissAlarm = {
                        dismissAlarmAndFinish()
                    }
                )
            }
        }
    }

    private fun turnOnScreenAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    private fun dismissAlarmAndFinish() {
        val stopServiceIntent = Intent(this, LocationAlarmService::class.java).apply {
            action = LocationAlarmService.ACTION_STOP_SERVICE
        }
        startService(stopServiceIntent)
        finish()
    }
}
