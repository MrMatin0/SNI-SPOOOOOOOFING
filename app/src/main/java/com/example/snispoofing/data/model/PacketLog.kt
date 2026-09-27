package com.example.snispoofing.data.model

import java.util.concurrent.atomic.AtomicLong

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    DEBUG
}

private val logIdGenerator = AtomicLong(System.currentTimeMillis())

data class LogEntry(
    val id: Long = logIdGenerator.incrementAndGet(),
    val timestampMs: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val title: String,
    val detail: String? = null
)

object DpiDiagnostic {
    fun diagnose(reason: String?): Pair<String, String> {
        if (reason.isNullOrBlank()) {
            return Pair("connection interrupted during handshake", "no reply from destination")
        }
        val lower = reason.lowercase()
        return when {
            "timeout" in lower || "no response" in lower -> {
                Pair("no reply to decoy ClientHello", "the DPI or destination middlebox dropped the injected packet")
            }
            "outbound" in lower -> {
                if ("after fake sent" in lower) {
                    Pair("local socket transmitted early", "client application retried or closed handshake early")
                } else {
                    Pair("packet altered locally", "another proxy, VPN, or network filter modified the outgoing stream")
                }
            }
            "no syn sent" in lower -> {
                Pair("reply arrived before SYN sent", "middlebox or proxy answered on behalf of target")
            }
            "syn-ack" in lower -> {
                Pair("handshake reply sequence mismatch", "DPI or network interference on SYN-ACK stream")
            }
            "seq" in lower -> {
                Pair("TCP sequence number mismatch from peer", "middlebox or firewall rewritten TCP packet stream")
            }
            "ack" in lower -> {
                Pair("TCP acknowledgement number mismatch", "DPI middlebox altered TCP ACK header")
            }
            else -> {
                Pair("unexpected packet during handshake", "DPI filter or network peer interference detected")
            }
        }
    }
}
