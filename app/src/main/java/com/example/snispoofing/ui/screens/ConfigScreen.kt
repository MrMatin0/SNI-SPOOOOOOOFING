package com.example.snispoofing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.snispoofing.ui.viewmodel.BYPASS_STRATEGIES
import com.example.snispoofing.ui.viewmodel.POPULAR_SNI_PRESETS
import com.example.snispoofing.ui.viewmodel.SniSpoofViewModel

@Composable
fun ConfigScreen(
    viewModel: SniSpoofViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val config by viewModel.currentConfig.collectAsState()

    var listenHost by remember(config) { mutableStateOf(config.listenHost) }
    var listenPortText by remember(config) { mutableStateOf(config.listenPort.toString()) }
    var connectHost by remember(config) { mutableStateOf(config.connectHost) }
    var connectPortText by remember(config) { mutableStateOf(config.connectPort.toString()) }
    var fakeSni by remember(config) { mutableStateOf(config.fakeSni) }
    var selectedMethod by remember(config) { mutableStateOf(config.bypassMethod) }

    var isSaved by remember { mutableStateOf(false) }

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
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "PROXY CONFIGURATION",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Listener Settings
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
                        text = "LOCAL LISTENER",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = listenHost,
                        onValueChange = { listenHost = it },
                        label = { Text("Listen Interface / Host") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_listen_host"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = listenPortText,
                        onValueChange = { listenPortText = it },
                        label = { Text("Listen Port") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_listen_port"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        // Target Destination Settings
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
                        text = "DESTINATION ENDPOINT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = connectHost,
                        onValueChange = { connectHost = it },
                        label = { Text("Target IP / Hostname (CONNECT_IP)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_connect_host"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = connectPortText,
                        onValueChange = { connectPortText = it },
                        label = { Text("Target Port (CONNECT_PORT)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_connect_port"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        // Fake SNI & Presets
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
                        text = "DECOY FAKE SNI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = fakeSni,
                        onValueChange = { fakeSni = it },
                        label = { Text("Decoy Server Name Indication (FAKE_SNI)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_fake_sni"),
                        singleLine = true
                    )

                    Text(
                        text = "Popular Decoy Presets:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(POPULAR_SNI_PRESETS) { preset ->
                            FilterChip(
                                selected = fakeSni == preset,
                                onClick = { fakeSni = preset },
                                label = {
                                    Text(
                                        preset,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bypass Strategy
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
                        text = "BYPASS METHOD / STRATEGY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    BYPASS_STRATEGIES.forEach { (key, title) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == key,
                                onClick = { selectedMethod = key }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Save & Apply Button
        item {
            Button(
                onClick = {
                    val lPort = listenPortText.toIntOrNull() ?: 40443
                    val cPort = connectPortText.toIntOrNull() ?: 443
                    val newCfg = config.copy(
                        listenHost = listenHost.trim(),
                        listenPort = lPort,
                        connectHost = connectHost.trim(),
                        connectPort = cPort,
                        fakeSni = fakeSni.trim(),
                        bypassMethod = selectedMethod
                    )
                    viewModel.updateConfig(newCfg)
                    isSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_config_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isSaved) "SAVED & APPLIED" else "SAVE CONFIGURATION",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
