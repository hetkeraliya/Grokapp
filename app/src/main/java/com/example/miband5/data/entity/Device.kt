package com.example.miband5.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Paired device record. */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val identifier: String, // MAC address
    val model: String = "Mi Band 5",
    val lastSynced: Long = 0 // epoch seconds
)
