package com.example.nxt.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.nxt.ui.components.parseColorChip

@Composable
fun DirectControlScreen(
    isConnected: Boolean,
    batteryMv: Int?,
    touch1: Boolean?,
    touch2: Boolean?,
    colorName: String?,
    distanceCm: Int?,
    onRunMotor: (String, Int) -> Unit,
    onBrakeMotor: (String) -> Unit,
    onPlayTone: (Int, Int) -> Unit,
    onEnableTelemetry: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Start telemetry when screen is active, pause when exited
    DisposableEffect(Unit) {
        onEnableTelemetry(true)
        onDispose {
            onEnableTelemetry(false)
        }
    }

    var powerSlider by remember { mutableFloatStateOf(60f) }
    val currentPower = powerSlider.toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning if not connected
        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x33EF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ NXT brick is disconnected. Connect via Bluetooth in the top right to enable live control and sensor readings.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Live Sensor Telemetry Section
        Text(
            text = "LIVE SENSOR TELEMETRY (FIXED WIRING)",
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Touch 1 (Port 1)
            SensorTelemetryCard(
                title = "Touch 1 (Port 1)",
                modifier = Modifier.weight(1f)
            ) {
                val isPressed = touch1 == true
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isPressed) Color(0xFF10B981) else Color(0xFF475569))
                    )
                    Text(
                        text = if (isPressed) "PRESSED" else "RELEASED",
                        color = if (isPressed) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Touch 2 (Port 2)
            SensorTelemetryCard(
                title = "Touch 2 (Port 2)",
                modifier = Modifier.weight(1f)
            ) {
                val isPressed = touch2 == true
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isPressed) Color(0xFF10B981) else Color(0xFF475569))
                    )
                    Text(
                        text = if (isPressed) "PRESSED" else "RELEASED",
                        color = if (isPressed) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color Sensor (Port 3)
            SensorTelemetryCard(
                title = "Color (Port 3)",
                modifier = Modifier.weight(1f)
            ) {
                val colorStr = colorName ?: "unknown"
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(parseColorChip(colorStr))
                    )
                    Text(
                        text = colorStr.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Ultrasonic (Port 4)
            SensorTelemetryCard(
                title = "Distance (Port 4)",
                modifier = Modifier.weight(1f)
            ) {
                val dist = distanceCm ?: 0
                Column {
                    Text(
                        text = if (distanceCm != null) "$dist cm" else "-- cm",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (dist / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }

        // Direct Drive D-Pad (Motors A+C standard drivetrain)
        Text(
            text = "DIRECT ROBOT DRIVE (MOTORS A + C)",
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Power slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Motor Power / Speed:", color = Color.White, fontSize = 13.sp)
                    Text("$currentPower%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Slider(
                    value = powerSlider,
                    onValueChange = { powerSlider = it },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("power_slider")
                )

                // Drive directional buttons
                // Forward
                FilledIconButton(
                    onClick = { onRunMotor("A+C", currentPower) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.size(54.dp).testTag("btn_drive_forward")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Forward", tint = Color.White)
                }

                // Left - Stop - Right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Turn Left: Motor A reverse, Motor C forward
                    FilledIconButton(
                        onClick = {
                            onRunMotor("A", -currentPower)
                            onRunMotor("C", currentPower)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.size(54.dp).testTag("btn_drive_left")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Turn Left", tint = Color.White)
                    }

                    // Brake / Stop
                    FilledIconButton(
                        onClick = { onBrakeMotor("A+C") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.size(54.dp).testTag("btn_drive_stop")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Brake", tint = Color.White)
                    }

                    // Turn Right: Motor A forward, Motor C reverse
                    FilledIconButton(
                        onClick = {
                            onRunMotor("A", currentPower)
                            onRunMotor("C", -currentPower)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.size(54.dp).testTag("btn_drive_right")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Turn Right", tint = Color.White)
                    }
                }

                // Backward
                FilledIconButton(
                    onClick = { onRunMotor("A+C", -currentPower) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.size(54.dp).testTag("btn_drive_backward")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Reverse", tint = Color.White)
                }
            }
        }

        // Individual Motor Testing (A, B, C)
        Text(
            text = "INDIVIDUAL MOTORS & SOUND",
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("A", "B", "C").forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Motor $m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onRunMotor(m, currentPower) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Forward", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onRunMotor(m, -currentPower) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Reverse", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onBrakeMotor(m) },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Brake", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Sound Beep Test
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NXT Speaker Tone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onPlayTone(440, 250) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("440Hz", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onPlayTone(880, 250) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("880Hz", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorTelemetryCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    }
}
