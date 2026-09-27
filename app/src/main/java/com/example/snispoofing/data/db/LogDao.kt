package com.example.snispoofing.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM packet_logs ORDER BY timestampMs DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<LogEntity>>

    @Insert
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM packet_logs")
    suspend fun clearLogs()
}
