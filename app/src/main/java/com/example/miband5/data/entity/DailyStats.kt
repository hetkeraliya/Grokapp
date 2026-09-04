package com.example.miband5.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-day aggregate. Schema matches the build brief exactly.
 * journal_tags / notes are JSON-encoded strings ([] = empty).
 */
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val steps: Int = 0,
    val heartRateAvg: Int? = null,
    val heartRateMin: Int? = null,
    val heartRateMax: Int? = null,
    val sleepMinutes: Int = 0,
    val deepSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val calories: Int = 0,
    val distanceMeters: Int = 0,
    val batteryLast: Int? = null,
    val stressAvg: Int? = null,
    val hrZoneLowMin: Int = 0,
    val hrZoneModerateMin: Int = 0,
    val hrZoneHighMin: Int = 0,
    val journalTags: String = "[]", // JSON array of tag strings
    val notes: String = ""
)
