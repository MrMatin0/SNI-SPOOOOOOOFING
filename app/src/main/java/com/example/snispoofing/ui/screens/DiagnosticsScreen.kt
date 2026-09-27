package com.example.snispoofing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snispoofing.data.model.BypassVerdict
import com.example.snispoofing.ui.theme.AccentEmerald
import com.example.snispoofing.ui.theme.ErrorRose
import com.example.snispoofing.ui.theme.WarningAmber
import com.example.snispoofing.ui.viewmodel.SniSpoofViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: SniSpoofViewModel
) {
    val config by viewModel.currentConfig.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "DIAGNOSTICS & TESTING",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live DPI Handshake Tester Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LIVE DPI DECOY TESTER",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Connects to ${config.connectHost}:${config.connectPort} using fake SNI '${config.fakeSni}' to verify if DPI middleboxes drop or accept decoy packets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.runDpiTest() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_dpi_test_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TESTING HANDSHAKE...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("EXECUTE BYPASS TEST", fontWeight = FontWeight.Bold)
                        }
                    }

                    testResult?.let { res ->
                        HorizontalDivider()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (res.isSuccess) AccentEmerald.copy(alpha = 0.15f) else ErrorRose.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (res.isSuccess) AccentEmerald else ErrorRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = res.diagnosticSummary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.isSuccess) AccentEmerald else ErrorRose
                                    )
                                }
                                Text(
                                    text = res.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Latency: ${res.latencyMs} ms | Decoy SNI: ${res.fakeSniUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Support & Donation Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ErrorRose,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "SUPPORT FREE INTERNET PROJECTS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "If this tool helps you reach the free internet, consider supporting future development.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    SupportRow("USDT (BEP20)", "0x76a768B53Ca77B43086946315f0BDF21156bF424") {
                        clipboardManager.setText(AnnotatedString("0x76a768B53Ca77B43086946315f0BDF21156bF424"))
                    }

                    SupportRow("USDT (TRC20)", "TU5gKvKqcXPn8itp1DouBCwcqGHMemBm8o") {
                        clipboardManager.setText(AnnotatedString("TU5gKvKqcXPn8itp1DouBCwcqGHMemBm8o"))
                    }

                    SupportRow("Telegram", "@patterniha  ·  @projectXhttp") {
                        clipboardManager.setText(AnnotatedString("@patterniha"))
                    }
                }
            }
        }

        // Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALL EVENT LOGS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearLogs() }) {
                    Text("Clear All")
                }
            }
        }

        // Logs List
        items(logs, key = { it.id }) { log ->
            LogCardItem(log)
        }
    }
}

@Composable
fun SupportRow(label: String, address: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = onCopy) {
            Text("Copy")
        }
    }
}
