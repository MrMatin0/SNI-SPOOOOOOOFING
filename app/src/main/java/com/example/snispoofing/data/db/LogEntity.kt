package com.example.snispoofing.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.snispoofing.data.model.LogEntry
import com.example.snispoofing.data.model.LogLevel

@Entity(tableName = "packet_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val level: String,
    val title: String,
    val detail: String?
) {
    fun toDomain(): LogEntry = LogEntry(
        id = id,
        timestampMs = timestampMs,
        level = try { LogLevel.valueOf(level) } catch (e: Exception) { LogLevel.INFO },
        title = title,
        detail = detail
    )

    companion object {
        fun fromDomain(entry: LogEntry): LogEntity = LogEntity(
            timestampMs = entry.timestampMs,
            level = entry.level.name,
            title = entry.title,
            detail = entry.detail
        )
    }
}
