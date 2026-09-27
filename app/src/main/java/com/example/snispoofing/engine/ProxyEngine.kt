package com.example.snispoofing.engine

import com.example.snispoofing.data.model.BypassVerdict
import com.example.snispoofing.data.model.ConfigSettings
import com.example.snispoofing.data.model.ConnectionTunnel
import com.example.snispoofing.data.model.LogEntry
import com.example.snispoofing.data.model.LogLevel
import com.example.snispoofing.data.model.ProxyState
import com.example.snispoofing.data.model.ProxyStats
import com.example.snispoofing.data.model.TlsClientHelloMaker
import com.example.snispoofing.data.model.TunnelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.update
import java.util.concurrent.ArrayBlockingQueue
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

private object BufferPool {
    private val pool = ArrayBlockingQueue<ByteArray>(100)
    fun acquire(): ByteArray = pool.poll() ?: ByteArray(32768)
    fun release(buffer: ByteArray) { pool.offer(buffer) }
}

class ProxyEngine {
    private val _proxyState = MutableStateFlow(ProxyState.STOPPED)
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    private val _proxyStats = MutableStateFlow(ProxyStats())
    val proxyStats: StateFlow<ProxyStats> = _proxyStats.asStateFlow()

    private val _activeTunnels = MutableStateFlow<List<ConnectionTunnel>>(emptyList())
    val activeTunnels: StateFlow<List<ConnectionTunnel>> = _activeTunnels.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var engineJob: Job? = null
    private var speedMonitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val activeTunnelMap = ConcurrentHashMap<String, ConnectionTunnel>()
    private val totalTunnelsCounter = AtomicLong(0)
    private val bytesUpCounter = AtomicLong(0)
    private val bytesDownCounter = AtomicLong(0)
    private val failedHandshakesCounter = AtomicLong(0)
    private val connectFailuresCounter = AtomicLong(0)

    private val tunnelIdCounter = AtomicLong(System.currentTimeMillis())
    private var lastBytesUp = 0L
    private var lastBytesDown = 0L
    private var lastCheckTime = System.currentTimeMillis()
    private val latencyList = mutableListOf<Double>()

    fun log(level: LogLevel, title: String, detail: String? = null) {
        val newEntry = LogEntry(level = level, title = title, detail = detail)
        _logs.update { (listOf(newEntry) + it).take(200) }
    }

    fun startProxy(config: ConfigSettings) {
        if (_proxyState.value == ProxyState.RUNNING) return

        _proxyState.value = ProxyState.STARTING
        log(LogLevel.INFO, "Starting SNI Proxy", "Listening on ${config.listenHost}:${config.listenPort} -> Target ${config.connectHost}:${config.connectPort}")

        engineJob = scope.launch {
            try {
                val bindAddr = try {
                    InetAddress.getByName(config.listenHost)
                } catch (e: Exception) {
                    InetAddress.getByName("0.0.0.0")
                }
                serverSocket = ServerSocket(config.listenPort, 50, bindAddr)
                _proxyState.value = ProxyState.RUNNING
                log(LogLevel.SUCCESS, "Proxy Listener Active", "Port ${config.listenPort} bound successfully. Fake SNI: ${config.fakeSni}")

                startSpeedMonitor()

                while (isActive && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch {
                            handleConnection(clientSocket, config)
                        }
                    } catch (e: Exception) {
                        if (isActive && serverSocket?.isClosed == false) {
                            log(LogLevel.WARN, "Accept error", e.message)
                        }
                    }
                }
            } catch (e: Exception) {
                _proxyState.value = ProxyState.STOPPED
                log(LogLevel.ERROR, "Failed to start proxy server", e.localizedMessage ?: e.message)
            }
        }
    }

    fun stopProxy() {
        if (_proxyState.value == ProxyState.STOPPED) return
        log(LogLevel.INFO, "Stopping proxy service...")
        engineJob?.cancel()
        speedMonitorJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
        activeTunnelMap.values.forEach { it.status = TunnelStatus.CLOSED }
        updateTunnelsList()
        _proxyState.value = ProxyState.STOPPED
        log(LogLevel.SUCCESS, "Proxy service stopped")
    }

    private fun startSpeedMonitor() {
        speedMonitorJob = scope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val elapsedSec = maxOf(0.001, (now - lastCheckTime) / 1000.0)

                val currentUp = bytesUpCounter.get()
                val currentDown = bytesDownCounter.get()

                val upRate = ((currentUp - lastBytesUp) / elapsedSec).toLong()
                val downRate = ((currentDown - lastBytesDown) / elapsedSec).toLong()

                lastBytesUp = currentUp
                lastBytesDown = currentDown
                lastCheckTime = now

                val total = totalTunnelsCounter.get().toInt()
                val active = activeTunnelMap.size
                val failedHandshakes = failedHandshakesCounter.get().toInt()
                val connectFailures = connectFailuresCounter.get().toInt()

                val medianLat = synchronized(latencyList) {
                    if (latencyList.isNotEmpty()) {
                        val sorted = latencyList.sorted()
                        sorted[sorted.size / 2]
                    } else 0.0
                }

                val verdict = when {
                    active > 0 || total > 0 && failedHandshakes == 0 -> BypassVerdict.WORKING
                    failedHandshakes > 0 && active > 0 -> BypassVerdict.STRUGGLING
                    failedHandshakes > 0 && active == 0 -> BypassVerdict.BLOCKED
                    else -> BypassVerdict.READY
                }

                _proxyStats.value = ProxyStats(
                    totalTunnels = total,
                    activeTunnels = active,
                    failedHandshakes = failedHandshakes,
                    connectFailures = connectFailures,
                    bytesUp = currentUp,
                    bytesDown = currentDown,
                    uploadSpeedBps = upRate,
                    downloadSpeedBps = downRate,
                    medianSetupMs = medianLat,
                    verdict = verdict
                )

                updateTunnelsList()
            }
        }
    }

    private suspend fun handleConnection(clientSocket: Socket, config: ConfigSettings) = withContext(Dispatchers.IO) {
        clientSocket.tcpNoDelay = true
        val tunnelId = "tunnel_${tunnelIdCounter.incrementAndGet()}"
        val clientEndpoint = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
        val destEndpoint = "${config.connectHost}:${config.connectPort}"

        var setupTimeMs = 0.0
        var destSocket: Socket? = null

        val tunnel = ConnectionTunnel(
            id = tunnelId,
            clientEndpoint = clientEndpoint,
            destEndpoint = destEndpoint,
            status = TunnelStatus.ACTIVE
        )

        activeTunnelMap[tunnelId] = tunnel
        totalTunnelsCounter.incrementAndGet()
        updateTunnelsList()

        try {
            val setupDuration = measureTimeMillis {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.soTimeout = 15000
                socket.connect(java.net.InetSocketAddress(config.connectHost, config.connectPort), 10000)
                destSocket = socket
            }

            setupTimeMs = setupDuration.toDouble()
            tunnel.lastActiveMs = System.currentTimeMillis()
            synchronized(latencyList) {
                if (latencyList.size > 50) latencyList.removeAt(0)
                latencyList.add(setupTimeMs)
            }

            log(LogLevel.SUCCESS, "Tunnel Established", "Client $clientEndpoint -> $destEndpoint ($setupTimeMs ms setup)")

            destSocket?.let { targetSocket ->
                val clientIn = clientSocket.getInputStream()
                val clientOut = clientSocket.getOutputStream()
                val destIn = targetSocket.getInputStream()
                val destOut = targetSocket.getOutputStream()

                val jobUp = launch {
                    relayUploadStream(clientIn, destOut, tunnel, config)
                }
                val jobDown = launch {
                    relayDownloadStream(destIn, clientOut, tunnel)
                }

                jobUp.join()
                jobDown.join()
            }

        } catch (e: Exception) {
            failedHandshakesCounter.incrementAndGet()
            tunnel.status = TunnelStatus.FAILED
            log(LogLevel.WARN, "Handshake / Relay Interrupted", "Client $clientEndpoint: ${e.localizedMessage ?: e.message}")
        } finally {
            try { clientSocket.close() } catch (ignored: Exception) {}
            try { destSocket?.close() } catch (ignored: Exception) {}
            tunnel.status = TunnelStatus.CLOSED
            activeTunnelMap.remove(tunnelId)
            updateTunnelsList()
        }
    }

    private suspend fun relayUploadStream(
        input: InputStream,
        output: OutputStream,
        tunnel: ConnectionTunnel,
        config: ConfigSettings
    ) = withContext(Dispatchers.IO) {
        val buffer = BufferPool.acquire()
        var isFirstPacket = true

        try {
            while (isActive) {
                val read = input.read(buffer)
                if (read <= 0) break

                if (isFirstPacket) {
                    isFirstPacket = false

                    // Check if initial packet is a TLS ClientHello
                    val isTlsClientHello = read > 5 &&
                            buffer[0] == 0x16.toByte() &&
                            buffer[1] == 0x03.toByte()

                    if (isTlsClientHello) {
                        applyBypassStrategy(buffer, read, output, config)
                    } else {
                        output.write(buffer, 0, read)
                        output.flush()
                    }
                } else {
                    output.write(buffer, 0, read)
                    output.flush()
                }

                tunnel.bytesUp += read
                bytesUpCounter.addAndGet(read.toLong())
                tunnel.lastActiveMs = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Stream closed or broken
        } finally {
            BufferPool.release(buffer)
        }
    }

    private suspend fun applyBypassStrategy(
        buffer: ByteArray,
        length: Int,
        output: OutputStream,
        config: ConfigSettings
    ) {
        when (config.bypassMethod) {
            "sni_split" -> {
                // Split TLS ClientHello into 2 chunks to bypass DPI SNI detection
                val splitOffset = minOf(14, length - 1)
                if (splitOffset > 0) {
                    output.write(buffer, 0, splitOffset)
                    output.flush()
                    delay(5)
                    output.write(buffer, splitOffset, length - splitOffset)
                    output.flush()
                } else {
                    output.write(buffer, 0, length)
                    output.flush()
                }
            }
            "fake_client_hello" -> {
                // Send decoy TLS ClientHello before forwarding client's real TLS ClientHello
                val decoy = TlsClientHelloMaker.generateRandomDecoy(config.fakeSni)
                try {
                    output.write(decoy)
                    output.flush()
                    delay(10)
                } catch (e: Exception) {
                    // Ignore decoy write error if middlebox closes connection
                }
                // Now split real ClientHello
                val splitOffset = minOf(5, length - 1)
                output.write(buffer, 0, splitOffset)
                output.flush()
                delay(2)
                output.write(buffer, splitOffset, length - splitOffset)
                output.flush()
            }
            "wrong_seq" -> {
                // Fragment TLS ClientHello into small packets (Record Header vs Payload)
                val headerLen = minOf(5, length)
                output.write(buffer, 0, headerLen)
                output.flush()
                delay(3)
                if (length > headerLen) {
                    val remaining = length - headerLen
                    val half = headerLen + (remaining / 2)
                    output.write(buffer, headerLen, half - headerLen)
                    output.flush()
                    delay(3)
                    output.write(buffer, half, length - half)
                    output.flush()
                }
            }
            "custom_decoy" -> {
                // Custom entropy record + ClientHello split
                val splitOffset = minOf(20, length - 1)
                output.write(buffer, 0, splitOffset)
                output.flush()
                delay(5)
                output.write(buffer, splitOffset, length - splitOffset)
                output.flush()
            }
            else -> {
                // Default SNI record split
                val splitOffset = minOf(5, length - 1)
                output.write(buffer, 0, splitOffset)
                output.flush()
                delay(2)
                output.write(buffer, splitOffset, length - splitOffset)
                output.flush()
            }
        }
    }

    private suspend fun relayDownloadStream(
        input: InputStream,
        output: OutputStream,
        tunnel: ConnectionTunnel
    ) = withContext(Dispatchers.IO) {
        val buffer = BufferPool.acquire()
        try {
            while (isActive) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()

                tunnel.bytesDown += read
                bytesDownCounter.addAndGet(read.toLong())
                tunnel.lastActiveMs = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Stream closed or broken
        } finally {
            BufferPool.release(buffer)
        }
    }

    private fun updateTunnelsList() {
        _activeTunnels.value = activeTunnelMap.values.sortedByDescending { it.lastActiveMs }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
