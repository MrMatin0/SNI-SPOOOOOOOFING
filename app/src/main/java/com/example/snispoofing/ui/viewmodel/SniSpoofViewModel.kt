package com.example.snispoofing.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.snispoofing.data.db.AppDatabase
import com.example.snispoofing.data.db.ConfigEntity
import com.example.snispoofing.data.model.ConfigSettings
import com.example.snispoofing.data.model.ConnectionTunnel
import com.example.snispoofing.data.model.LogEntry
import com.example.snispoofing.data.model.LogLevel
import com.example.snispoofing.data.model.ProxyState
import com.example.snispoofing.data.model.ProxyStats
import com.example.snispoofing.engine.HandshakeTestResult
import com.example.snispoofing.engine.NetworkSimulator
import com.example.snispoofing.engine.ProxyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val POPULAR_SNI_PRESETS = listOf(
    "auth.vercel.com",
    "mci.ir",
    "speedtest.net",
    "wikipedia.org",
    "cloudflare.com",
    "cdn.discordapp.com"
)

val BYPASS_STRATEGIES = listOf(
    "wrong_seq" to "Wrong Sequence Decoy (WinDivert / Kernel Fast-Path)",
    "fake_client_hello" to "Decoy TLS 1.3 ClientHello Injection",
    "sni_split" to "SNI Record Fragmentation",
    "custom_decoy" to "Custom Entropy Decoy Record"
)

class SniSpoofViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val configDao = db.configDao()
    private val logDao = db.logDao()

    val engine = ProxyEngine()

    val proxyState: StateFlow<ProxyState> = engine.proxyState
    val proxyStats: StateFlow<ProxyStats> = engine.proxyStats
    val activeTunnels: StateFlow<List<ConnectionTunnel>> = engine.activeTunnels
    val logs: StateFlow<List<LogEntry>> = engine.logs

    private val _currentConfig = MutableStateFlow(ConfigSettings())
    val currentConfig: StateFlow<ConfigSettings> = _currentConfig.asStateFlow()

    private val _testResult = MutableStateFlow<HandshakeTestResult?>(null)
    val testResult: StateFlow<HandshakeTestResult?> = _testResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    init {
        viewModelScope.launch {
            configDao.getConfigFlow().collect { saved ->
                if (saved != null) {
                    _currentConfig.value = saved.toDomain()
                } else {
                    val defaultConfig = ConfigSettings()
                    configDao.saveConfig(ConfigEntity.fromDomain(defaultConfig))
                    _currentConfig.value = defaultConfig
                }
            }
        }
    }

    fun toggleProxy() {
        if (proxyState.value == ProxyState.RUNNING) {
            engine.stopProxy()
        } else {
            engine.startProxy(_currentConfig.value)
        }
    }

    fun updateConfig(newConfig: ConfigSettings) {
        _currentConfig.value = newConfig
        viewModelScope.launch {
            configDao.saveConfig(ConfigEntity.fromDomain(newConfig))
        }
        engine.log(LogLevel.INFO, "Configuration updated", "Target: ${newConfig.connectHost}:${newConfig.connectPort} | Fake SNI: ${newConfig.fakeSni}")
    }

    fun applySniPreset(sni: String) {
        val updated = _currentConfig.value.copy(fakeSni = sni)
        updateConfig(updated)
    }

    fun runDpiTest() {
        if (_isTesting.value) return
        _isTesting.value = true
        engine.log(LogLevel.INFO, "Running DPI Handshake Test", "Testing target ${_currentConfig.value.connectHost}:${_currentConfig.value.connectPort}")

        viewModelScope.launch {
            val result = NetworkSimulator.runDpiHandshakeTest(_currentConfig.value)
            _testResult.value = result
            _isTesting.value = false

            val level = if (result.isSuccess) LogLevel.SUCCESS else LogLevel.ERROR
            engine.log(level, "DPI Test: ${result.diagnosticSummary}", result.details)
        }
    }

    fun clearLogs() {
        engine.clearLogs()
    }
}
