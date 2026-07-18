package com.beispiel.ridetracker.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_corners",
    foreignKeys = [ForeignKey(
        entity = CornerEntity::class,
        parentColumns = ["id"],
        childColumns = ["cornerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cornerId"), Index("sessionId")]
)
data class SessionCornerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val cornerId: Long,
    val leanAchieved: Float,    // absolute value, degrees
    val speedAtPeak: Double,    // km/h
    val cornerTimestamp: Long
)
