package com.example.snispoofing.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.snispoofing.data.model.ConfigSettings

@Entity(tableName = "config_settings")
data class ConfigEntity(
    @PrimaryKey val id: Int = 1,
    val listenHost: String,
    val listenPort: Int,
    val connectHost: String,
    val connectPort: Int,
    val fakeSni: String,
    val bypassMethod: String,
    val dataMode: String
) {
    fun toDomain(): ConfigSettings = ConfigSettings(
        id = id,
        listenHost = listenHost,
        listenPort = listenPort,
        connectHost = connectHost,
        connectPort = connectPort,
        fakeSni = fakeSni,
        bypassMethod = bypassMethod,
        dataMode = dataMode
    )

    companion object {
        fun fromDomain(settings: ConfigSettings): ConfigEntity = ConfigEntity(
            id = settings.id,
            listenHost = settings.listenHost,
            listenPort = settings.listenPort,
            connectHost = settings.connectHost,
            connectPort = settings.connectPort,
            fakeSni = settings.fakeSni,
            bypassMethod = settings.bypassMethod,
            dataMode = settings.dataMode
        )
    }
}
