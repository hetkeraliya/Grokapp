package com.example.miband5.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.miband5.data.entity.ManualWorkout

@Dao
interface ManualWorkoutDao {
    @Insert
    suspend fun insert(workout: ManualWorkout)

    @Query("SELECT * FROM manual_workouts ORDER BY date DESC")
    suspend fun getAll(): List<ManualWorkout>

    @Query("SELECT * FROM manual_workouts WHERE date = :date")
    suspend fun getByDate(date: String): List<ManualWorkout>
}
