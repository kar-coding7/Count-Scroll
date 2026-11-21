package com.kiru.countscroll.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "scroll_events",
    indices = [Index(value = ["packageName", "timestamp"])]
)
data class ScrollEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(
    tableName = "usage_sessions",
    indices = [Index(value = ["packageName", "startTime"])]
)
data class UsageSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String,
    val startTime: Long,
    val endTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "delay_logs")
data class DelayLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // "TUNDA" atau "KELUAR"
    val delayUnit: String,  // "MENIT" atau "SCROLL"
    val delayValue: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
