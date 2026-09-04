package com.example.miband5.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw per-minute HR + intensity sample. Kept so historical HR-zone
 * breakdowns can be recomputed. Pruned after ~90 days (see SyncCoordinator).
 */
@Entity(tableName = "hr_samples")
data class HrSample(
    @PrimaryKey val timestamp: Long, // epoch seconds
    val heartRate: Int,
    val rawIntensity: Int = 0
)
