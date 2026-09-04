package com.example.miband5.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.miband5.data.entity.Workout

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout)

    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    suspend fun getAll(): List<Workout>

    @Query("SELECT * FROM workouts WHERE startTime BETWEEN :from AND :to ORDER BY startTime ASC")
    suspend fun getRange(from: Long, to: Long): List<Workout>
}
