package com.example.snispoofing.data.model

data class ConfigSettings(
    val id: Int = 1,
    val listenHost: String = "127.0.0.1",
    val listenPort: Int = 40443,
    val connectHost: String = "188.114.98.0",
    val connectPort: Int = 443,
    val fakeSni: String = "auth.vercel.com",
    val bypassMethod: String = "wrong_seq", // wrong_seq, fake_client_hello, sni_split, custom_decoy
    val dataMode: String = "TLS"
)

enum class ProxyState {
    STOPPED,
    STARTING,
    RUNNING,
    DEGRADED,
    TESTING
}

enum class BypassVerdict(val label: String, val description: String) {
    READY("Ready", "Proxy listener idle, awaiting traffic"),
    WORKING("Bypass Confirmed", "The decoy TLS ClientHello is accepted by destination"),
    STRUGGLING("Bypass Degrading", "Network or DPI middlebox is dropping some decoys"),
    BLOCKED("Bypass Blocked", "Destination or DPI is interfering with decoys");
}

data class ProxyStats(
    val totalTunnels: Int = 0,
    val activeTunnels: Int = 0,
    val failedHandshakes: Int = 0,
    val connectFailures: Int = 0,
    val bytesUp: Long = 0L,
    val bytesDown: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val medianSetupMs: Double = 0.0,
    val verdict: BypassVerdict = BypassVerdict.READY
)
