package com.kiru.countscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kiru.countscroll.data.local.entity.DelayLog
import com.kiru.countscroll.data.local.entity.ScrollEvent
import com.kiru.countscroll.data.local.entity.UsageSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CountScrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrollEvent(event: ScrollEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageSession(session: UsageSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelayLog(log: DelayLog)

    @Query("SELECT COUNT(*) FROM scroll_events WHERE timestamp >= :startOfDayMillis")
    fun observeTodayTotalScrolls(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM scroll_events WHERE packageName IN (:packageNames) AND timestamp >= :startOfDayMillis")
    fun observeTodayAppsScrolls(packageNames: List<String>, startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(endTime - startTime), 0) FROM usage_sessions WHERE startTime >= :startOfDayMillis")
    fun observeTodayTotalDurationMillis(startOfDayMillis: Long): Flow<Long>

    @Query("SELECT * FROM scroll_events WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp ASC")
    fun observeScrollEventsBetween(startMillis: Long, endMillis: Long): Flow<List<ScrollEvent>>

    @Query("SELECT * FROM usage_sessions WHERE startTime >= :startMillis AND startTime < :endMillis ORDER BY startTime ASC")
    fun observeUsageSessionsBetween(startMillis: Long, endMillis: Long): Flow<List<UsageSession>>

    @Query("SELECT * FROM delay_logs WHERE timestamp >= :startOfDayMillis ORDER BY timestamp DESC")
    fun observeDelayLogsSince(startOfDayMillis: Long): Flow<List<DelayLog>>

    @Query("DELETE FROM scroll_events")
    suspend fun clearAllScrollEvents()

    @Query("DELETE FROM usage_sessions")
    suspend fun clearAllUsageSessions()

    @Query("DELETE FROM delay_logs")
    suspend fun clearAllDelayLogs()
}
