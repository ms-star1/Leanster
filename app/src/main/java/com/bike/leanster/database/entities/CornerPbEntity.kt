package com.bike.leanster.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "corner_pbs",
    foreignKeys = [ForeignKey(
        entity = CornerEntity::class,
        parentColumns = ["id"],
        childColumns = ["cornerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cornerId")]
)
data class CornerPbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cornerId: Long,
    val bestLean: Float,        // absolute value, degrees
    val bestSpeed: Double,      // km/h at peak lean
    val bestSessionId: String,
    val achievedAt: Long
)
