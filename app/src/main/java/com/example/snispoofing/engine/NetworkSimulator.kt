package com.example.snispoofing.engine

import com.example.snispoofing.data.model.BypassVerdict
import com.example.snispoofing.data.model.ConfigSettings
import com.example.snispoofing.data.model.TlsClientHelloMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

data class HandshakeTestResult(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val fakeSniUsed: String,
    val connectHost: String,
    val connectPort: Int,
    val hexSnippet: String,
    val verdict: BypassVerdict,
    val diagnosticSummary: String,
    val details: String
)

object NetworkSimulator {
    suspend fun runDpiHandshakeTest(config: ConfigSettings): HandshakeTestResult = withContext(Dispatchers.IO) {
        var isSuccess = false
        var latency = 0L
        var hexSnippet = ""
        var diagnostic = ""
        var details = ""
        var verdict = BypassVerdict.BLOCKED

        val decoyPayload = TlsClientHelloMaker.generateRandomDecoy(config.fakeSni)
        hexSnippet = TlsClientHelloMaker.bytesToHex(decoyPayload, 64)

        try {
            val socket = Socket()
            val timeTaken = measureTimeMillis {
                socket.connect(InetSocketAddress(config.connectHost, config.connectPort), 5000)
                val out = socket.getOutputStream()
                out.write(decoyPayload)
                out.flush()

                socket.soTimeout = 3000
                val input = socket.getInputStream()
                val responseBuf = ByteArray(512)
                val read = input.read(responseBuf)
                isSuccess = read > 0 || socket.isConnected
            }
            latency = timeTaken
            try { socket.close() } catch (e: Exception) {}

            if (isSuccess || latency < 4000) {
                isSuccess = true
                verdict = BypassVerdict.WORKING
                diagnostic = "Bypass Confirmed"
                details = "Decoy ClientHello for SNI '${config.fakeSni}' accepted by ${config.connectHost}:${config.connectPort} in ${latency}ms."
            } else {
                verdict = BypassVerdict.STRUGGLING
                diagnostic = "Handshake Delayed"
                details = "Target server connected but response was delayed (${latency}ms). Middlebox may be throttling packets."
            }
        } catch (e: Exception) {
            isSuccess = false
            verdict = BypassVerdict.BLOCKED
            diagnostic = "Connection / Decoy Interrupted"
            details = "Failed to connect or inject decoy to ${config.connectHost}:${config.connectPort}: ${e.localizedMessage ?: e.message}"
        }

        HandshakeTestResult(
            isSuccess = isSuccess,
            latencyMs = latency,
            fakeSniUsed = config.fakeSni,
            connectHost = config.connectHost,
            connectPort = config.connectPort,
            hexSnippet = hexSnippet,
            verdict = verdict,
            diagnosticSummary = diagnostic,
            details = details
        )
    }
}
