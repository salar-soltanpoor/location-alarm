package com.example.locationalarm.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locationalarm.data.model.AlarmState
import com.example.locationalarm.ui.screens.components.BackgroundLocationRationaleDialog
import com.example.locationalarm.ui.screens.components.CoordinateInput
import com.example.locationalarm.ui.screens.components.ForegroundLocationRationaleDialog
import com.example.locationalarm.ui.screens.components.MapPicker
import com.example.locationalarm.ui.screens.components.NotificationRationaleDialog
import com.example.locationalarm.ui.screens.components.RadiusSlider
import com.example.locationalarm.util.LocationUtils
import com.example.locationalarm.util.PermissionUtils
import com.example.locationalarm.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetDestination by viewModel.targetDestination.collectAsState()
    val alarmState by viewModel.alarmState.collectAsState()

    val latitudeInput by viewModel.latitudeInput.collectAsState()
    val longitudeInput by viewModel.longitudeInput.collectAsState()
    val destinationNameInput by viewModel.destinationNameInput.collectAsState()
    val radiusInput by viewModel.radiusInput.collectAsState()

    var showForegroundRationale by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    // Permission Launchers
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            checkAndStartAlarmService(
                context = context,
                viewModel = viewModel,
                onShowBackgroundRationale = { showBackgroundRationale = true },
                onShowNotificationRationale = { showNotificationRationale = true }
            )
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkAndStartAlarmService(
                context = context,
                viewModel = viewModel,
                onShowBackgroundRationale = { showBackgroundRationale = true },
                onShowNotificationRationale = { showNotificationRationale = true }
            )
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startLocationAlarmService()
        }
    }

    // Permission Rationale Dialogs
    if (showForegroundRationale) {
        ForegroundLocationRationaleDialog(
            onConfirm = {
                showForegroundRationale = false
                foregroundLocationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showForegroundRationale = false }
        )
    }

    if (showBackgroundRationale) {
        BackgroundLocationRationaleDialog(
            onOpenSettings = {
                showBackgroundRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    PermissionUtils.openAppSettings(context)
                }
            },
            onDismiss = { showBackgroundRationale = false }
        )
    }

    if (showNotificationRationale) {
        NotificationRationaleDialog(
            onConfirm = {
                showNotificationRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showNotificationRationale = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Alarm", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Status Card
            StatusCard(alarmState = alarmState)

            // Map Picker
            MapPicker(
                latitude = targetDestination.latitude,
                longitude = targetDestination.longitude,
                radiusMeters = radiusInput,
                destinationName = targetDestination.name,
                onLocationSelected = { lat, lng ->
                    viewModel.onMapLocationSelected(lat, lng)
                }
            )

            // Destination Name & Coordinate Form
            CoordinateInput(
                destinationName = destinationNameInput,
                latitudeStr = latitudeInput,
                longitudeStr = longitudeInput,
                onNameChanged = { viewModel.onDestinationNameChanged(it) },
                onLatitudeChanged = { viewModel.onLatitudeChanged(it) },
                onLongitudeChanged = { viewModel.onLongitudeChanged(it) },
                onPresetSelected = { name, lat, lng ->
                    viewModel.applyPresetDestination(name, lat, lng)
                }
            )

            // Radius Slider
            RadiusSlider(
                radiusMeters = radiusInput,
                onRadiusChanged = { viewModel.onRadiusChanged(it) }
            )

            // Start/Stop Action Button
            val isRunning = alarmState !is AlarmState.Idle
            val buttonColor by animateColorAsState(
                targetValue = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "buttonColor"
            )

            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.stopLocationAlarmService()
                    } else {
                        checkAndStartAlarmService(
                            context = context,
                            viewModel = viewModel,
                            onShowForegroundRationale = { showForegroundRationale = true },
                            onShowBackgroundRationale = { showBackgroundRationale = true },
                            onShowNotificationRationale = { showNotificationRationale = true }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.AlarmOff else Icons.Default.Alarm,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "STOP LOCATION ALARM" else "START LOCATION ALARM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusCard(alarmState: AlarmState) {
    val (statusTitle, statusColor, icon) = when (alarmState) {
        is AlarmState.Idle -> Triple("Alarm Inactive", Color.Gray, Icons.Default.LocationSearching)
        is AlarmState.Active -> Triple("Alarm Active", Color(0xFF2E7D32), Icons.Default.Alarm)
        is AlarmState.Triggered -> Triple("DESTINATION REACHED!", Color(0xFFD32F2F), Icons.Default.NotificationsActive)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                when (alarmState) {
                    is AlarmState.Idle -> {
                        Text(
                            text = "Set target and press Start to monitor location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is AlarmState.Active -> {
                        Text(
                            text = "Target: ${alarmState.destination.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Remaining Distance: ${LocationUtils.formatDistance(alarmState.currentDistanceMeters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is AlarmState.Triggered -> {
                        Text(
                            text = "Arrived at ${alarmState.destination.name}!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun checkAndStartAlarmService(
    context: android.content.Context,
    viewModel: MainViewModel,
    onShowForegroundRationale: () -> Unit = {},
    onShowBackgroundRationale: () -> Unit = {},
    onShowNotificationRationale: () -> Unit = {}
) {
    if (!PermissionUtils.hasForegroundLocationPermission(context)) {
        onShowForegroundRationale()
        return
    }

    if (!PermissionUtils.hasNotificationPermission(context)) {
        onShowNotificationRationale()
        return
    }

    if (!PermissionUtils.hasBackgroundLocationPermission(context)) {
        onShowBackgroundRationale()
        return
    }

    viewModel.startLocationAlarmService()
}
