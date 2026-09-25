package com.example.locationalarm.ui.screens.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable

@Composable
fun ForegroundLocationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        title = { Text(text = "Location Permission Required") },
        text = {
            Text(
                text = "Location Alarm needs access to your device's location to calculate distance to your target destination and wake you up when you arrive."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BackgroundLocationRationaleDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text(text = "Allow Always In Background") },
        text = {
            Text(
                text = "To ensure you don't miss your bus stop when your phone screen is off or when using other apps (like Spotify or Netflix), please enable 'Allow all the time' location permission in System Settings."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text(text = "Notification Permission Required") },
        text = {
            Text(
                text = "Notifications are required to display the continuous distance tracker and trigger the full-screen alarm alert when you reach your destination."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Enable Notifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
