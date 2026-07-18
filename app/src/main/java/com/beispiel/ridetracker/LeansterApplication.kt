package com.beispiel.ridetracker

import android.app.Application
import com.beispiel.ridetracker.database.AppDatabase
import org.maplibre.android.MapLibre

class LeansterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        AppDatabase.getInstance(this)
    }
}
