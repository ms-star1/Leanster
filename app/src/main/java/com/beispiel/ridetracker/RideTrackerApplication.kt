package com.beispiel.ridetracker

import android.app.Application
import org.maplibre.android.MapLibre

class RideTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}