package com.example.locationalarm.ui.screens.components

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationalarm.R
import com.example.locationalarm.util.PermissionUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapPicker(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    destinationName: String,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    isAlarmActive: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val targetLatLng = LatLng(latitude, longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLatLng, 14f)
    }

    val markerState = remember(targetLatLng) {
        MarkerState(position = targetLatLng)
    }

    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val hasLocationPermission = remember(context) {
        PermissionUtils.hasForegroundLocationPermission(context)
    }

    // Fetch user location if permissions granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Custom map vector styling options
    val mapStyleOptions = remember(context) {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
        } catch (e: Exception) {
            null
        }
    }

    // Auto camera positioning logic when target changes or alarm is active
    LaunchedEffect(targetLatLng, isAlarmActive, userLocation) {
        markerState.position = targetLatLng

        if (isAlarmActive && userLocation != null) {
            // Bound both user location and target destination inside camera view
            val bounds = LatLngBounds.builder()
                .include(userLocation!!)
                .include(targetLatLng)
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 120),
            )
        } else if (!cameraPositionState.isMoving && cameraPositionState.position.target != targetLatLng) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(targetLatLng),
            )
        }
    }

    val mapProperties = remember(hasLocationPermission, selectedMapType, mapStyleOptions) {
        MapProperties(
            mapType = selectedMapType,
            isBuildingEnabled = true,
            isIndoorEnabled = true,
            isTrafficEnabled = true,
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = if (selectedMapType == MapType.NORMAL) mapStyleOptions else null,
        )
    }

    val mapUiSettings = remember(hasLocationPermission) {
        MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = hasLocationPermission,
            mapToolbarEnabled = true,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Google Maps Compose View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = { latLng ->
                    onLocationSelected(latLng.latitude, latLng.longitude)
                },
            ) {
                // Custom Destination Marker
                Marker(
                    state = markerState,
                    title = destinationName,
                    snippet = "Geofence Radius: ${radiusMeters.toInt()}m",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    draggable = true,
                )

                // Geofence Radius Circle
                Circle(
                    center = targetLatLng,
                    radius = radiusMeters.toDouble(),
                    fillColor = Color(0x330088FF),
                    strokeColor = Color(0xFF0088FF),
                    strokeWidth = 3f,
                )
            }

            // Sync marker drag end position with destination state
            LaunchedEffect(markerState.position) {
                val distanceMeters = FloatArray(1)
                android.location.Location.distanceBetween(
                    markerState.position.latitude,
                    markerState.position.longitude,
                    targetLatLng.latitude,
                    targetLatLng.longitude,
                    distanceMeters
                )
                if (distanceMeters[0] > 1f) {
                    onLocationSelected(
                        markerState.position.latitude,
                        markerState.position.longitude,
                    )
                }
            }

            // Map Type Switcher Overlay Chips
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val mapTypes = listOf(
                    "Default" to MapType.NORMAL,
                    "Hybrid" to MapType.HYBRID,
                    "Terrain" to MapType.TERRAIN,
                    "Satellite" to MapType.SATELLITE,
                )
                mapTypes.forEach { (label, type) ->
                    val isSelected = selectedMapType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMapType = type },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            // Custom Control FABs (Fit Bounds & My Location)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fit user location & destination bounds button
                if (userLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                val bounds = LatLngBounds.builder()
                                    .include(userLocation!!)
                                    .include(targetLatLng)
                                    .build()
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 120),
                                )
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusWeak,
                            contentDescription = "Fit Bounds",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Floating My Location Button
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        userLocation = LatLng(it.latitude, it.longitude)
                                        onLocationSelected(it.latitude, it.longitude)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Map Overlay Banner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            ) {
                Text(
                    text = "Tap map or drag destination marker • Radius: ${radiusMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
