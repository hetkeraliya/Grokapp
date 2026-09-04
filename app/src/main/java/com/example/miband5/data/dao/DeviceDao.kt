package com.example.miband5.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.miband5.data.entity.Device

@Dao
interface DeviceDao {
    @Insert
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Query("SELECT * FROM devices ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): Device?

    @Query("SELECT * FROM devices")
    suspend fun getAll(): List<Device>
}
