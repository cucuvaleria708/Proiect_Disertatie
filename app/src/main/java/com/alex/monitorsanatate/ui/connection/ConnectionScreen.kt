package com.alex.monitorsanatate.ui.connection

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.ui.components.ConnectionStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.selectedMethod.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val manualIp by viewModel.manualIp.collectAsStateWithLifecycle()
    val manualPort by viewModel.manualPort.collectAsStateWithLifecycle()
    val ledState by viewModel.ledState.collectAsStateWithLifecycle()
    val ledRequestStatus by viewModel.ledRequestStatus.collectAsStateWithLifecycle()

    // BLE permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Conexiune") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "test",
                    onClick = { viewModel.selectTab("test") },
                    label = { Text("Test LED") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = selectedTab == "wifi",
                    onClick = { viewModel.selectTab("wifi") },
                    label = { Text("WiFi") },
                    leadingIcon = {
                        Icon(Icons.Filled.Wifi, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = selectedTab == "ble",
                    onClick = { viewModel.selectTab("ble") },
                    label = { Text("BLE") },
                    leadingIcon = {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null)
                    }
                )
            }

            // Test LED tab
            if (selectedTab == "test") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Test LED ESP32",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { viewModel.updateManualIp(it) },
                            label = { Text("IP Address ESP32") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { viewModel.updateManualPort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.sendLedCommand(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ledState)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("LED ON")
                            }
                            Button(
                                onClick = { viewModel.sendLedCommand(false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!ledState)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("LED OFF")
                            }
                        }

                        if (ledRequestStatus.isNotEmpty()) {
                            Text(
                                text = ledRequestStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // WiFi manual connection
            if (selectedTab == "wifi") {
                ConnectionStatusBadge(connectionState = connectionState)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Conexiune manuală WiFi",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { viewModel.updateManualIp(it) },
                            label = { Text("IP Address ESP32") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { viewModel.updateManualPort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        when (connectionState) {
                            is ConnectionState.Connected -> {
                                OutlinedButton(
                                    onClick = { viewModel.disconnect() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Deconectare")
                                }
                            }
                            is ConnectionState.Connecting -> {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Se conectează...")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { viewModel.connectManualWifi() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Conectare")
                                }
                            }
                        }
                    }
                }
            }

            // BLE device list
            if (selectedTab == "ble") {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scanare dispozitive BLE")
                }

                if (connectionState is ConnectionState.Connected) {
                    OutlinedButton(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deconectare")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Dispozitive găsite:",
                    style = MaterialTheme.typography.titleSmall
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredDevices) { device ->
                        DeviceCard(
                            device = device,
                            isConnected = connectionState is ConnectionState.Connected &&
                                (connectionState as ConnectionState.Connected).device.address == device.address,
                            onClick = { viewModel.connectToDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInfo,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            device.rssi?.let { rssi ->
                Text(
                    text = "$rssi dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
