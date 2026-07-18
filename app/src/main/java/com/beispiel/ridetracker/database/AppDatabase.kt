package com.beispiel.ridetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beispiel.ridetracker.database.entities.CornerEntity
import com.beispiel.ridetracker.database.entities.CornerPbEntity
import com.beispiel.ridetracker.database.entities.SessionCornerEntity

@Database(
    entities = [CornerEntity::class, CornerPbEntity::class, SessionCornerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cornerDao(): CornerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ridetracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
