package com.example.snispoofing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.snispoofing.data.model.TlsClientHelloMaker
import com.example.snispoofing.ui.viewmodel.SniSpoofViewModel

@Composable
fun InspectorScreen(
    viewModel: SniSpoofViewModel
) {
    val config by viewModel.currentConfig.collectAsState()

    var sniInput by remember(config) { mutableStateOf(config.fakeSni) }
    var decoyBytes by remember(sniInput) {
        mutableStateOf(TlsClientHelloMaker.generateRandomDecoy(sniInput))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "DECOY TLS INSPECTOR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        decoyBytes = TlsClientHelloMaker.generateRandomDecoy(sniInput)
                    },
                    modifier = Modifier.testTag("btn_regen_decoy")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate Decoy")
                }
            }
        }

        // SNI Customizer Input Card
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
                        text = "DECOY TARGET SNI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = sniInput,
                        onValueChange = {
                            sniInput = it
                            if (it.isNotBlank()) {
                                decoyBytes = TlsClientHelloMaker.generateRandomDecoy(it)
                            }
                        },
                        label = { Text("SNI Domain Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_inspector_sni"),
                        singleLine = true
                    )

                    Text(
                        text = "Packet Length: ${decoyBytes.size} Bytes (Fixed ClientHello Template)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Binary Structure Fields Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TLS 1.3 RECORD STRUCTURE BREAKDOWN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    StructureRow("Record Header", "0x16 0x03 0x01 (Handshake Record)")
                    StructureRow("Client Random", "32 Bytes Cryptographic Entropy [11..43)")
                    StructureRow("Session ID", "32 Bytes Session Identifier [44..76)")
                    StructureRow("Target SNI Ext", "server_name: '$sniInput' (${sniInput.length} bytes)")
                    StructureRow("Key Share Ext", "32 Bytes ECDHE Key Exchange Share")
                    StructureRow("Padding Ext", "${TlsClientHelloMaker.MAX_SNI_LEN - sniInput.length} Bytes Zero-Padding")
                }
            }
        }
    }
}

@Composable
fun StructureRow(label: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
