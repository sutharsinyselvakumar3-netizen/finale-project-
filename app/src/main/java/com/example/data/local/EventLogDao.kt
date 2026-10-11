package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EventLogEntity): Long

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<EventLogEntity>>

    @Query("DELETE FROM event_logs")
    suspend fun clearLogs()
}
