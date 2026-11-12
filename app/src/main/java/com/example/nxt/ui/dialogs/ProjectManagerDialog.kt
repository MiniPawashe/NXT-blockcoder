package com.example.nxt.ui.dialogs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.data.SavedProgramEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectManagerDialog(
    currentProjectName: String,
    currentBlockCount: Int,
    savedProjects: List<SavedProgramEntity>,
    onSaveProject: (name: String, description: String) -> Unit,
    onLoadProject: (id: Long) -> Unit,
    onDeleteProject: (id: Long) -> Unit,
    onNewProject: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Project Library, 1: Save As
    var projectNameInput by remember { mutableStateOf(currentProjectName) }
    var projectDescInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("NXT Project Manager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Saved Projects (${savedProjects.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Save Current", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // Projects Library View
                    if (savedProjects.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text("No saved projects found.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedProjects, key = { it.id }) { project ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("project_item_${project.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = project.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (project.isTemplate) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0x3338BDF8)
                                                    ) {
                                                        Text("STARTER", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0x3364748B)
                                            ) {
                                                Text(
                                                    text = "${project.blockCount} blocks",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (project.description.isNotBlank()) {
                                            Text(
                                                text = project.description,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = dateFormat.format(Date(project.updatedAt)),
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (!project.isTemplate) {
                                                    IconButton(
                                                        onClick = { onDeleteProject(project.id) },
                                                        modifier = Modifier.size(28.dp).testTag("delete_proj_${project.id}")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        onLoadProject(project.id)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    modifier = Modifier.height(28.dp).testTag("load_proj_${project.id}")
                                                ) {
                                                    Text("Load", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Save Current Program View
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Save current script ($currentBlockCount blocks) into Room database:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = projectNameInput,
                            onValueChange = { projectNameInput = it },
                            label = { Text("Project Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = projectDescInput,
                            onValueChange = { projectDescInput = it },
                            label = { Text("Description / Robot Notes (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                onSaveProject(projectNameInput, projectDescInput)
                                selectedTab = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().testTag("btn_save_confirm")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save to Library", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    onNewProject()
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("New Blank Project", color = Color.White, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}
