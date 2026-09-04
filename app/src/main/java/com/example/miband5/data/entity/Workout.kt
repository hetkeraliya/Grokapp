package com.example.miband5.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Device-detected workout (from the band). muscle_tags is a JSON array. */
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long, // epoch seconds
    val endTime: Long? = null,
    val kind: String = "", // e.g. "running", "walking"
    val name: String = "",
    val minutes: Int = 0,
    val distanceMeters: Int = 0,
    val calories: Int = 0,
    val muscleTags: String = "[]" // JSON array of muscle names
)
