package com.example.nxt.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.nxt.ui.dialogs.BluetoothDialog
import com.example.nxt.ui.dialogs.ProjectManagerDialog
import com.example.nxt.ui.screens.BlockCanvasScreen
import com.example.nxt.ui.screens.DirectControlScreen
import com.example.nxt.ui.screens.LogAndCodeScreen
import com.example.nxt.viewmodel.ExampleType
import com.example.nxt.viewmodel.NxtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NxtAppScaffold(
    viewModel: NxtViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val program by viewModel.program.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val activeBlockId by viewModel.activeBlockId.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val batteryMv by viewModel.batteryMv.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val activeProjectName by viewModel.activeProjectName.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()

    val touch1 by viewModel.touch1.collectAsState()
    val touch2 by viewModel.touch2.collectAsState()
    val color by viewModel.color.collectAsState()
    val distanceCm by viewModel.distanceCm.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Blocks, 1: Direct Control, 2: Logs & Code
    var showBtDialog by remember { mutableStateOf(false) }
    var showExamplesMenu by remember { mutableStateOf(false) }
    var showProjectManagerDialog by remember { mutableStateOf(false) }

    // Bluetooth permission request launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    var hasBtPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasBtPermission = granted
        if (granted) {
            showBtDialog = true
        } else {
            Toast.makeText(context, "Bluetooth permissions are required to connect to NXT", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Title & active project name
                        Column {
                            Text(
                                text = "NXT Blocks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = activeProjectName,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Bluetooth Status Pill
                        Surface(
                            onClick = {
                                permissionLauncher.launch(permissionsToRequest)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = when (connectionState) {
                                is ConnectionState.Connected -> Color(0x3310B981)
                                is ConnectionState.Connecting -> Color(0x333B82F6)
                                is ConnectionState.Reconnecting -> Color(0x33F59E0B)
                                is ConnectionState.Scanning -> Color(0x3338BDF8)
                                else -> Color(0x3364748B)
                            },
                            modifier = Modifier.testTag("btn_bt_status")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (connectionState) {
                                                is ConnectionState.Connected -> Color(0xFF10B981)
                                                is ConnectionState.Connecting -> Color(0xFF38BDF8)
                                                is ConnectionState.Reconnecting -> Color(0xFFFBBF24)
                                                is ConnectionState.Scanning -> Color(0xFF38BDF8)
                                                else -> Color(0xFF94A3B8)
                                            }
                                        )
                                )
                                Text(
                                    text = when (val s = connectionState) {
                                        is ConnectionState.Connected -> {
                                            val bat = batteryMv?.let { " (${it / 1000.0}V)" } ?: ""
                                            "${s.deviceName}$bat"
                                        }
                                        is ConnectionState.Connecting -> "Connecting..."
                                        is ConnectionState.Reconnecting -> "Reconnecting (${s.attempt}/${s.maxAttempts})..."
                                        is ConnectionState.Scanning -> "Scanning..."
                                        else -> "Connect NXT"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Project Manager / Saved Programs button
                    IconButton(
                        onClick = { showProjectManagerDialog = true },
                        modifier = Modifier.testTag("btn_project_manager")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Projects", tint = Color(0xFF38BDF8))
                    }

                    // Presets / Example button
                    Box {
                        IconButton(
                            onClick = { showExamplesMenu = true },
                            modifier = Modifier.testTag("btn_presets")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Presets", tint = Color(0xFF38BDF8))
                        }
                        DropdownMenu(
                            expanded = showExamplesMenu,
                            onDismissRequest = { showExamplesMenu = false }
                        ) {
                            ExampleType.entries.forEach { example ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(example.title, fontWeight = FontWeight.Bold)
                                            Text(example.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        showExamplesMenu = false
                                        viewModel.loadExample(example)
                                    }
                                )
                            }
                        }
                    }

                    // Clear button
                    IconButton(
                        onClick = { viewModel.clearProgram() },
                        modifier = Modifier.testTag("btn_clear")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                    }

                    // Run Button (Emerald Green)
                    Button(
                        onClick = { viewModel.runProgram() },
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF064E3B)
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 6.dp)
                            .testTag("btn_run")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Stop Button (Crimson Red)
                    Button(
                        onClick = { viewModel.stopProgram() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 8.dp)
                            .testTag("btn_stop")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Blocks") },
                    label = { Text("Blocks Workspace") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Direct Remote") },
                    label = { Text("Direct Remote") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Log & JSON") },
                    label = { Text("Log & JSON") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BlockCanvasScreen(
                    program = program,
                    activeBlockId = activeBlockId,
                    isRunning = isRunning,
                    onAddBlock = { type -> viewModel.addBlock(type) },
                    onInsertBlockAtIndex = { type, index, parentId -> viewModel.insertBlockAtIndex(type, index, parentId) },
                    onMoveBlockToIndex = { blockId, index, parentId -> viewModel.moveBlockToIndex(blockId, index, parentId) },
                    onAddChildBlock = { parentId, type -> viewModel.addBlock(type, parentId) },
                    onUpdateBlock = { updated -> viewModel.updateBlock(updated) },
                    onDeleteBlock = { id -> viewModel.removeBlock(id) },
                    onMoveBlock = { id, delta -> viewModel.moveBlock(id, delta) },
                    onLoadExampleClick = { showExamplesMenu = true }
                )
                1 -> DirectControlScreen(
                    isConnected = viewModel.btManager.isConnected(),
                    batteryMv = batteryMv,
                    touch1 = touch1,
                    touch2 = touch2,
                    colorName = color,
                    distanceCm = distanceCm,
                    onRunMotor = { m, p -> viewModel.directRunMotor(m, p) },
                    onBrakeMotor = { m -> viewModel.directBrakeMotor(m) },
                    onPlayTone = { hz, ms -> viewModel.directPlayBeep(hz, ms) },
                    onEnableTelemetry = { enabled -> viewModel.setTelemetryEnabled(enabled) }
                )
                2 -> LogAndCodeScreen(
                    logs = logs,
                    programJson = viewModel.getProgramJson(),
                    onClearLogs = { viewModel.clearLogs() },
                    onImportJson = { jsonStr -> viewModel.loadFromJson(jsonStr) }
                )
            }
        }
    }

    // Bluetooth Connection Dialog
    if (showBtDialog) {
        BluetoothDialog(
            connectionState = connectionState,
            devices = discoveredDevices,
            onAutoConnect = {
                viewModel.autoConnectToNxt()
            },
            onStartScan = {
                viewModel.startDiscovery()
            },
            onStopScan = {
                viewModel.stopDiscovery()
            },
            onConnect = { address, name ->
                viewModel.connectDevice(address, name)
            },
            onDisconnect = {
                viewModel.disconnectDevice()
            },
            onDismiss = {
                viewModel.stopDiscovery()
                showBtDialog = false
            }
        )
    }

    // Project Manager Dialog
    if (showProjectManagerDialog) {
        ProjectManagerDialog(
            currentProjectName = activeProjectName,
            currentBlockCount = program.size,
            savedProjects = savedProjects,
            onSaveProject = { name, desc ->
                viewModel.saveCurrentProject(name, desc)
            },
            onLoadProject = { id ->
                viewModel.loadProject(id)
            },
            onDeleteProject = { id ->
                viewModel.deleteProject(id)
            },
            onNewProject = {
                viewModel.createNewProject()
            },
            onDismiss = { showProjectManagerDialog = false }
        )
    }
}
