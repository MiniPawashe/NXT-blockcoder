package com.example.nxt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.model.NxtCondition

val NXT_COLORS = listOf("black", "blue", "green", "yellow", "red", "white")

val SENSOR_OPTIONS = listOf(
    "t1" to "Touch 1 (Port 1)",
    "t2" to "Touch 2 (Port 2)",
    "dist" to "Distance (Port 4)",
    "color" to "Color (Port 3)"
)

val OPERATOR_OPTIONS = mapOf(
    "t1" to listOf("pressed", "not pressed"),
    "t2" to listOf("pressed", "not pressed"),
    "dist" to listOf("<", ">"),
    "color" to listOf("is", "is not")
)

fun parseColorChip(name: String): Color {
    return when (name.lowercase()) {
        "black" -> Color(0xFF1E293B)
        "blue" -> Color(0xFF3B82F6)
        "green" -> Color(0xFF10B981)
        "yellow" -> Color(0xFFFBBF24)
        "red" -> Color(0xFFEF4444)
        "white" -> Color(0xFFF8FAFC)
        else -> Color.Gray
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionEditor(
    condition: NxtCondition,
    onConditionChanged: (NxtCondition) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x33000000),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Sensor Selector
            var sensorExpanded by remember { mutableStateOf(false) }
            Box {
                ChipButton(
                    text = SENSOR_OPTIONS.find { it.first == condition.sensorKey }?.second ?: condition.sensorKey,
                    onClick = { sensorExpanded = true }
                )
                DropdownMenu(
                    expanded = sensorExpanded,
                    onDismissRequest = { sensorExpanded = false }
                ) {
                    SENSOR_OPTIONS.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sensorExpanded = false
                                val defaultOp = OPERATOR_OPTIONS[key]?.firstOrNull() ?: "pressed"
                                val defaultVal = if (key == "color") "red" else "20"
                                onConditionChanged(condition.copy(sensorKey = key, operator = defaultOp, value = defaultVal))
                            }
                        )
                    }
                }
            }

            // Operator Selector
            var opExpanded by remember { mutableStateOf(false) }
            val currentOps = OPERATOR_OPTIONS[condition.sensorKey] ?: listOf("==")
            Box {
                ChipButton(
                    text = condition.operator,
                    onClick = { opExpanded = true }
                )
                DropdownMenu(
                    expanded = opExpanded,
                    onDismissRequest = { opExpanded = false }
                ) {
                    currentOps.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = {
                                opExpanded = false
                                onConditionChanged(condition.copy(operator = op))
                            }
                        )
                    }
                }
            }

            // Value Selector (Distance numeric or Color picker)
            if (condition.sensorKey == "dist") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var textValue by remember(condition.value) { mutableStateOf(condition.value) }
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            onConditionChanged(condition.copy(value = it))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.size(width = 64.dp, height = 40.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    Text("cm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else if (condition.sensorKey == "color") {
                var colorExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { colorExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseColorChip(condition.value))
                        )
                        Text(
                            text = condition.value,
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pick Color",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = colorExpanded,
                        onDismissRequest = { colorExpanded = false }
                    ) {
                        NXT_COLORS.forEach { cName ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(parseColorChip(cName))
                                    )
                                },
                                text = { Text(cName.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    colorExpanded = false
                                    onConditionChanged(condition.copy(value = cName))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
