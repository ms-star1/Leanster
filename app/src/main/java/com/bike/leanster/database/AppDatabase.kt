package com.bike.leanster.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bike.leanster.database.entities.CornerEntity
import com.bike.leanster.database.entities.CornerPbEntity
import com.bike.leanster.database.entities.SessionCornerEntity

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
                    "leanster.db"
                ).build().also { INSTANCE = it }
            }
    }
}
