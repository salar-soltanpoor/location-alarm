package com.example.locationalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.locationalarm.AlarmActivity
import com.example.locationalarm.MainActivity
import com.example.locationalarm.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for sticky foreground service updates
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Location Alarm Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active background tracking status"
                setShowBadge(false)
            }

            // Channel for high priority alert when destination is reached
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Destination Reached Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers full screen alarm alert when approaching target destination"
                setBypassDnd(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun buildForegroundNotification(destinationName: String, distanceMeters: Float?): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_MAIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = Intent(context, LocationAlarmService::class.java).apply {
            action = LocationAlarmService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_STOP,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (distanceMeters != null) {
            val distText = if (distanceMeters >= 1000) {
                String.format("%.2f km", distanceMeters / 1000)
            } else {
                "${distanceMeters.toInt()} meters"
            }
            "Distance to $destinationName: $distText"
        } else {
            "Acquiring location for $destinationName…"
        }

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Location Alarm Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun triggerAlarmNotification(destinationName: String, distanceMeters: Float) {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION_NAME, destinationName)
            putExtra(EXTRA_DISTANCE_METERS, distanceMeters)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_ALARM,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = if (distanceMeters >= 1000) {
            String.format("%.2f km", distanceMeters / 1000)
        } else {
            "${distanceMeters.toInt()} meters"
        }

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setContentTitle("Target Reached: $destinationName")
            .setContentText("You are $distText away from your destination!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    fun cancelAlarmNotification() {
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "location_alarm_service_channel"
        const val ALARM_CHANNEL_ID = "location_alarm_trigger_channel"

        const val SERVICE_NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002

        const val REQUEST_CODE_MAIN = 2001
        const val REQUEST_CODE_STOP = 2002
        const val REQUEST_CODE_ALARM = 2003

        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
        const val EXTRA_DISTANCE_METERS = "extra_distance_meters"
    }
}
