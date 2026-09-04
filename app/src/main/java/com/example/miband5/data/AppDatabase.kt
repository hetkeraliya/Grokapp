package com.example.miband5.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miband5.data.dao.DailyStatsDao
import com.example.miband5.data.dao.DeviceDao
import com.example.miband5.data.dao.HrSampleDao
import com.example.miband5.data.dao.ManualWorkoutDao
import com.example.miband5.data.dao.WorkoutDao
import com.example.miband5.data.entity.DailyStats
import com.example.miband5.data.entity.Device
import com.example.miband5.data.entity.HrSample
import com.example.miband5.data.entity.ManualWorkout
import com.example.miband5.data.entity.Workout

@Database(
    entities = [
        DailyStats::class,
        HrSample::class,
        Workout::class,
        ManualWorkout::class,
        Device::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun hrSampleDao(): HrSampleDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun manualWorkoutDao(): ManualWorkoutDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "miband5.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
