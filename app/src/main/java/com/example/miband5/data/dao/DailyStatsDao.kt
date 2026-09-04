package com.example.miband5.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.miband5.data.entity.DailyStats

@Dao
interface DailyStatsDao {
    @Upsert
    suspend fun upsert(stats: DailyStats)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStats?

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getRange(start: String, end: String): List<DailyStats>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAll(): List<DailyStats>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DailyStats?

    @Query("DELETE FROM daily_stats")
    suspend fun clear()
}
