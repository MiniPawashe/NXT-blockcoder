package com.example.nxt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogAndCodeScreen(
    logs: List<String>,
    programJson: String,
    onClearLogs: () -> Unit,
    onImportJson: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Execution Log, 1 = Python JSON
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .padding(12.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 12.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Execution Log (${logs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text("Python JSON Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            )
        }

        if (selectedTab == 0) {
            // Execution Log view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RUNNER CONSOLE OUTPUT",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFCA5A5))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF020617),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }

                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No log messages yet. Press Run to execute.", color = Color(0xFF475569), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { entry ->
                            val color = when {
                                entry.startsWith("Error:") -> Color(0xFFEF4444)
                                entry.startsWith("Connected:") -> Color(0xFF10B981)
                                entry.startsWith("Stopped.") -> Color(0xFFF59E0B)
                                entry.startsWith("Finished.") -> Color(0xFF38BDF8)
                                entry.contains(":") -> Color(0xFFA5B4FC)
                                else -> Color(0xFFE2E8F0)
                            }
                            Text(
                                text = entry,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

        } else {
            // Python JSON view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DESKTOP COMPATIBLE JSON",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NXT Blocks JSON", programJson))
                            Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Import", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF020617),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    item {
                        Text(
                            text = programJson,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Import Dialog
        if (showImportDialog) {
            var importInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Import Program JSON", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Paste the JSON array exported from your macOS Python script:",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = importInput,
                            onValueChange = { importInput = it },
                            placeholder = { Text("[{\"t\":\"motor_on\", ...}]", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = onImportJson(importInput)
                            if (success) {
                                showImportDialog = false
                                Toast.makeText(context, "Program imported successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}
