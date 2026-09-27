package com.example.snispoofing.data.model

enum class TunnelStatus {
    ACTIVE,
    CLOSED,
    FAILED
}

data class ConnectionTunnel(
    val id: String,
    val clientEndpoint: String,
    val destEndpoint: String,
    var bytesUp: Long = 0L,
    var bytesDown: Long = 0L,
    val setupTimeMs: Double = 0.0,
    var status: TunnelStatus = TunnelStatus.ACTIVE,
    val startTimeMs: Long = System.currentTimeMillis(),
    var lastActiveMs: Long = System.currentTimeMillis()
)
