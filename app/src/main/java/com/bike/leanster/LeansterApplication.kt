package com.bike.leanster

import android.app.Application
import com.bike.leanster.database.AppDatabase
import org.maplibre.android.MapLibre

class LeansterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        AppDatabase.getInstance(this)
    }
}
