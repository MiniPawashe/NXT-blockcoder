package com.example.nxt.ui.dialogs

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.bluetooth.ConnectionState
import com.example.nxt.bluetooth.DiscoveredDevice

@Composable
fun BluetoothDialog(
    connectionState: ConnectionState,
    devices: List<DiscoveredDevice>,
    onAutoConnect: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (address: String, name: String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isScanning = connectionState is ConnectionState.Scanning

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("NXT Bluetooth Connection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = { if (isScanning) onStopScan() else onStartScan() }) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Sync else Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = if (isScanning) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Auto-Connect Prominent Banner
                if (connectionState !is ConnectionState.Connected) {
                    Button(
                        onClick = onAutoConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_auto_connect_nearest")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-Connect to Nearest NXT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Current Connection State Banner
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3310B981),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Column {
                                    Text(
                                        text = "Connected: ${connectionState.deviceName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    val batStr = connectionState.batteryMv?.let { "Battery: ${it / 1000.0}V" } ?: "Battery: --"
                                    Text(
                                        text = "${connectionState.address} • $batStr",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x333B82F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Connecting to ${connectionState.deviceName}...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    is ConnectionState.Reconnecting -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33F59E0B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFFBBF24)
                                )
                                Column {
                                    Text(
                                        text = "Auto-reconnecting to ${connectionState.deviceName}...",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Attempt ${connectionState.attempt} of ${connectionState.maxAttempts}",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    is ConnectionState.Scanning -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3338BDF8),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Scanning nearby devices (${connectionState.foundCount} found)...",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is ConnectionState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33EF4444),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = connectionState.message,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    ConnectionState.Disconnected -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DETECTED & PAIRED DEVICES",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { if (isScanning) onStopScan() else onStartScan() }) {
                        Text(if (isScanning) "Stop Scan" else "Scan Nearby", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                }

                if (devices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "No NXT devices detected.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = onStartScan,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scan for Nearby Devices", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(devices) { device ->
                            val isNxt = device.name.contains("NXT", ignoreCase = true) || device.name.contains("LEGO", ignoreCase = true)
                            val isCurrent = connectionState is ConnectionState.Connected && connectionState.address == device.address

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onConnect(device.address, device.name)
                                    }
                                    .testTag("device_${device.address}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(if (isNxt) Color(0xFF10B981) else Color(0xFF64748B))
                                        )
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = device.name,
                                                    color = Color.White,
                                                    fontWeight = if (isNxt) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                )
                                                if (isNxt) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0x3310B981)
                                                    ) {
                                                        Text("NXT BRICK", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            val bondedLabel = if (device.isBonded) "Paired" else "Discovered"
                                            Text(text = "${device.address} • $bondedLabel", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }

                                    if (isCurrent) {
                                        Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Pairing Hint Notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x1AFFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 NXT Pairing Instructions:",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "1. On NXT: Bluetooth > On > Visibility: Visible\n2. Standard LEGO NXT PIN is: 1234",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (connectionState is ConnectionState.Connected) {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Disconnect")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
            ) {
                Text("Bluetooth Settings", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}
