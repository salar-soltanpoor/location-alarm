package com.example.locationalarm

import android.app.Application
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LocationAlarmApp : Application(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d(TAG, "Google Maps SDK initialized with LATEST renderer.")
            MapsInitializer.Renderer.LEGACY -> Log.d(TAG, "Google Maps SDK initialized with LEGACY renderer.")
        }
    }

    companion object {
        private const val TAG = "LocationAlarmApp"
    }
}
