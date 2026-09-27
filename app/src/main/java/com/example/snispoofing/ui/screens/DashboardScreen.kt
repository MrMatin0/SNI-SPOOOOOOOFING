package com.example.snispoofing.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snispoofing.data.model.BypassVerdict
import com.example.snispoofing.data.model.LogLevel
import com.example.snispoofing.data.model.ProxyState
import com.example.snispoofing.ui.theme.AccentEmerald
import com.example.snispoofing.ui.theme.ErrorRose
import com.example.snispoofing.ui.theme.WarningAmber
import com.example.snispoofing.ui.viewmodel.SniSpoofViewModel

@Composable
fun DashboardScreen(
    viewModel: SniSpoofViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToInspector: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHeader(onNavigateToDiagnostics) }
        item { HeroStatusCard(viewModel) }
        item { SpeedTelemetryGrid(viewModel) }
        item { ActiveConfigCard(viewModel, onNavigateToConfig) }
        item { QuickToolsRow(viewModel, onNavigateToInspector) }
        item { LogsHeader(viewModel) }

        items(logs.take(10), key = { it.id }) { log ->
            LogCardItem(log)
        }
    }
}

@Composable
fun DashboardHeader(onNavigateToDiagnostics: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SNI-SPOOFING",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "DPI Bypass & Decoy TLS Injection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onNavigateToDiagnostics,
            modifier = Modifier.testTag("btn_diagnostics")
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "Diagnostics")
        }
    }
}

@Composable
fun HeroStatusCard(viewModel: SniSpoofViewModel) {
    val proxyState by viewModel.proxyState.collectAsState()
    val stats by viewModel.proxyStats.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (proxyState) {
                ProxyState.RUNNING -> MaterialTheme.colorScheme.surfaceVariant
                ProxyState.STARTING -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Badge & Pulse Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when (proxyState) {
                                ProxyState.RUNNING -> AccentEmerald
                                ProxyState.STARTING -> WarningAmber
                                else -> ErrorRose
                            }
                        )
                )
                Text(
                    text = when (proxyState) {
                        ProxyState.RUNNING -> "SERVICE ONLINE"
                        ProxyState.STARTING -> "STARTING ENGINE..."
                        else -> "SERVICE OFFLINE"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (proxyState) {
                        ProxyState.RUNNING -> AccentEmerald
                        ProxyState.STARTING -> WarningAmber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Bypass Verdict Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (stats.verdict) {
                    BypassVerdict.WORKING -> AccentEmerald.copy(alpha = 0.15f)
                    BypassVerdict.STRUGGLING -> WarningAmber.copy(alpha = 0.15f)
                    BypassVerdict.BLOCKED -> ErrorRose.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (stats.verdict) {
                        BypassVerdict.WORKING -> AccentEmerald
                        BypassVerdict.STRUGGLING -> WarningAmber
                        BypassVerdict.BLOCKED -> ErrorRose
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (stats.verdict) {
                            BypassVerdict.WORKING -> Icons.Default.Shield
                            BypassVerdict.STRUGGLING -> Icons.Default.Warning
                            BypassVerdict.BLOCKED -> Icons.Default.Block
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (stats.verdict) {
                            BypassVerdict.WORKING -> AccentEmerald
                            BypassVerdict.STRUGGLING -> WarningAmber
                            BypassVerdict.BLOCKED -> ErrorRose
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = stats.verdict.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (stats.verdict) {
                            BypassVerdict.WORKING -> AccentEmerald
                            BypassVerdict.STRUGGLING -> WarningAmber
                            BypassVerdict.BLOCKED -> ErrorRose
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Start/Stop Big Toggle Button
            Button(
                onClick = { viewModel.toggleProxy() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(54.dp)
                    .testTag("toggle_proxy_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (proxyState == ProxyState.RUNNING) ErrorRose else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (proxyState == ProxyState.RUNNING) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (proxyState == ProxyState.RUNNING) "STOP PROXY" else "START PROXY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpeedTelemetryGrid(viewModel: SniSpoofViewModel) {
    val stats by viewModel.proxyStats.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upload Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Rate", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = formatSpeed(stats.uploadSpeedBps),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Total: ${formatBytes(stats.bytesUp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Download Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Rate", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = formatSpeed(stats.downloadSpeedBps),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Total: ${formatBytes(stats.bytesDown)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveConfigCard(viewModel: SniSpoofViewModel, onNavigateToConfig: () -> Unit) {
    val config by viewModel.currentConfig.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE CONFIGURATION",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onNavigateToConfig) {
                    Text("Edit Config")
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Listen Address", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${config.listenHost}:${config.listenPort}",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Target Destination", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${config.connectHost}:${config.connectPort}",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fake Decoy SNI", style = MaterialTheme.typography.bodyMedium)
                Text(
                    config.fakeSni,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun QuickToolsRow(viewModel: SniSpoofViewModel, onNavigateToInspector: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.runDpiTest() },
            modifier = Modifier
                .weight(1f)
                .testTag("btn_quick_dpi_test"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Test Bypass")
        }

        OutlinedButton(
            onClick = onNavigateToInspector,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_inspect_packet"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Packet Hex")
        }
    }
}

@Composable
fun LogsHeader(viewModel: SniSpoofViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LIVE ACTIVITY LOGS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = { viewModel.clearLogs() }) {
            Text("Clear")
        }
    }
}

@Composable
fun LogCardItem(log: com.example.snispoofing.data.model.LogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (log.level) {
                        LogLevel.SUCCESS -> AccentEmerald
                        LogLevel.ERROR -> ErrorRose
                        LogLevel.WARN -> WarningAmber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(log.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!log.detail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatSpeed(bps: Long): String {
    return when {
        bps >= 1_000_000 -> String.format("%.2f MB/s", bps / 1_000_000.0)
        bps >= 1_000 -> String.format("%.1f KB/s", bps / 1_000.0)
        else -> "$bps B/s"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
