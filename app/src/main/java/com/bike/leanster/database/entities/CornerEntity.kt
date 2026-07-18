package com.bike.leanster.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "corners",
    indices = [Index(value = ["centroidLat", "centroidLng"])]
)
data class CornerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val centroidLat: Double,
    val centroidLng: Double,
    val entryHeading: Float,
    val direction: String,       // "LEFT" | "RIGHT"
    val leanCategory: String,    // "GENTLE" | "MEDIUM" | "AGGRESSIVE"
    val firstSeenAt: Long,
    val timesRidden: Int = 1
)
