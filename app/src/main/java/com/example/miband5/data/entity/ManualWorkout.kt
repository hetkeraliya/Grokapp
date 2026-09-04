package com.example.miband5.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Freestyle workout log (Phase 1 features: manual logging flow). */
@Entity(tableName = "manual_workouts")
data class ManualWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // ISO yyyy-MM-dd
    val label: String,
    val exercises: String = "[]" // JSON array of {name, sets, reps}
)
