package com.example.locationalarm.ui.screens.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationalarm.util.PermissionUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("MissingPermission")
@Composable
fun MapPicker(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    destinationName: String,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val targetLatLng = LatLng(latitude, longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLatLng, 14f)
    }

    val markerState = remember(targetLatLng) {
        MarkerState(position = targetLatLng)
    }

    var hasLocationPermission by remember {
        mutableStateOf(PermissionUtils.hasForegroundLocationPermission(context))
    }

    // Keep camera and marker updated when target destination changes externally
    LaunchedEffect(targetLatLng) {
        markerState.position = targetLatLng
        if (!cameraPositionState.isMoving && cameraPositionState.position.target != targetLatLng) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(targetLatLng),
            )
        }
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
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
                Marker(
                    state = markerState,
                    title = destinationName,
                    snippet = "Radius: ${radiusMeters.toInt()}m",
                    draggable = true,
                )
                Circle(
                    center = targetLatLng,
                    radius = radiusMeters.toDouble(),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    strokeColor = MaterialTheme.colorScheme.primary,
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

            // Floating My Location Button
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    onLocationSelected(it.latitude, it.longitude)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(20.dp),
                    )
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
                    text = "Tap map or drag marker to set destination • Radius: ${radiusMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
