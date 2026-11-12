package com.example.nxt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.ui.dragdrop.DragPayload
import com.example.nxt.ui.dragdrop.DropTarget
import com.example.nxt.ui.dragdrop.LocalDragAndDropState

val MOTOR_OPTIONS = listOf("A", "B", "C", "A+B", "A+C", "B+C", "A+B+C")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockItemCard(
    block: NxtBlock,
    isActiveStep: Boolean,
    parentId: String? = null,
    onUpdateBlock: (NxtBlock) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onMoveBlock: (String, Int) -> Unit,
    onAddChildBlock: (String, BlockType) -> Unit,
    onDropAtChildIndex: (targetParentId: String, index: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val dndState = LocalDragAndDropState.current
    var blockWindowOffset by remember { mutableStateOf(Offset.Zero) }

    val blockColor = block.type.category.color
    val darkBlockColor = block.type.category.darkColor

    val borderStroke = if (isActiveStep) {
        BorderStroke(3.dp, Color(0xFFFBBF24)) // Glowing amber highlight when running
    } else {
        BorderStroke(1.dp, Color(0x33FFFFFF))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("block_${block.id}")
            .onGloballyPositioned { coordinates ->
                blockWindowOffset = coordinates.boundsInWindow().topLeft
            }
            .padding(vertical = 2.dp)
    ) {
        // Main block banner
        Surface(
            shape = if (block.isContainerBlock) RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    else RoundedCornerShape(10.dp),
            color = blockColor,
            border = borderStroke,
            shadowElevation = if (isActiveStep) 8.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // Header row: Drag handle + Block parameters + controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Drag Handle Icon
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .pointerInput(block.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        val global = blockWindowOffset + localOffset
                                        dndState.startDrag(DragPayload.MoveBlock(block, parentId), global)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dndState.updateDragPosition(dndState.dragPosition + dragAmount)
                                    },
                                    onDragEnd = {
                                        dndState.endDrag { _, _ -> }
                                    },
                                    onDragCancel = {
                                        dndState.cancelDrag()
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag Block",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FlowRow(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RenderBlockControls(block = block, onUpdateBlock = onUpdateBlock)
                    }

                    // Action buttons (reorder, delete)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveBlock(block.id, -1) },
                            modifier = Modifier.size(30.dp).testTag("move_up_${block.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { onMoveBlock(block.id, 1) },
                            modifier = Modifier.size(30.dp).testTag("move_down_${block.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { onDeleteBlock(block.id) },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33000000))
                                .testTag("delete_${block.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // If container block (repeat, forever, if, until), render nested kids slot
        if (block.isContainerBlock) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Left vertical connector spine
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .background(blockColor)
                )

                // Indented children container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x1A000000))
                        .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                ) {
                    if (block.children.isEmpty()) {
                        DropSlot(
                            target = DropTarget.ChildSlot(block.id, 0),
                            onDropHere = { onDropAtChildIndex(block.id, 0) }
                        )
                        Text(
                            text = "Drop blocks here or tap + Add below",
                            color = Color(0x99FFFFFF),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    } else {
                        block.children.forEachIndexed { childIndex, childBlock ->
                            DropSlot(
                                target = DropTarget.ChildSlot(block.id, childIndex),
                                onDropHere = { onDropAtChildIndex(block.id, childIndex) }
                            )

                            BlockItemCard(
                                block = childBlock,
                                isActiveStep = isActiveStep,
                                parentId = block.id,
                                onUpdateBlock = onUpdateBlock,
                                onDeleteBlock = onDeleteBlock,
                                onMoveBlock = onMoveBlock,
                                onAddChildBlock = onAddChildBlock,
                                onDropAtChildIndex = onDropAtChildIndex
                            )
                        }

                        // Bottom child drop slot
                        DropSlot(
                            target = DropTarget.ChildSlot(block.id, block.children.size),
                            onDropHere = { onDropAtChildIndex(block.id, block.children.size) }
                        )
                    }

                    // Button to add child block inside
                    var addExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(
                            onClick = { addExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("add_inside_${block.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add block inside", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = addExpanded,
                            onDismissRequest = { addExpanded = false }
                        ) {
                            BlockType.entries.forEach { type ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(type.category.color)
                                        )
                                    },
                                    text = { Text(type.rawType.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        addExpanded = false
                                        onAddChildBlock(block.id, type)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom bar closure of container block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(blockColor)
            )
        }
    }
}

@Composable
fun RenderBlockControls(
    block: NxtBlock,
    onUpdateBlock: (NxtBlock) -> Unit
) {
    when (block.type) {
        BlockType.MOTOR_ON -> {
            BlockLabel("Motor")
            MotorDropdown(selected = block.motor, onSelected = { onUpdateBlock(block.copy(motor = it)) })
            BlockLabel("on at power")
            NumberParamInput(
                value = block.power.toString(),
                onChanged = { onUpdateBlock(block.copy(power = it.toIntOrNull()?.coerceIn(-100, 100) ?: 60)) },
                suffix = "%",
                width = 54.dp
            )
        }

        BlockType.MOTOR_FOR -> {
            BlockLabel("Run motor")
            MotorDropdown(selected = block.motor, onSelected = { onUpdateBlock(block.copy(motor = it)) })
            BlockLabel("at power")
            NumberParamInput(
                value = block.power.toString(),
                onChanged = { onUpdateBlock(block.copy(power = it.toIntOrNull()?.coerceIn(-100, 100) ?: 60)) },
                suffix = "%",
                width = 54.dp
            )
            BlockLabel("for")
            NumberParamInput(
                value = block.seconds.toString(),
                onChanged = { onUpdateBlock(block.copy(seconds = it.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0)) },
                suffix = "sec",
                width = 54.dp
            )
        }

        BlockType.MOTOR_OFF -> {
            BlockLabel("Stop motor")
            MotorDropdown(selected = block.motor, onSelected = { onUpdateBlock(block.copy(motor = it)) })
        }

        BlockType.WAIT -> {
            BlockLabel("Wait")
            NumberParamInput(
                value = block.seconds.toString(),
                onChanged = { onUpdateBlock(block.copy(seconds = it.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0)) },
                suffix = "seconds",
                width = 54.dp
            )
        }

        BlockType.WAIT_UNTIL -> {
            BlockLabel("Wait until")
            ConditionEditor(
                condition = block.condition,
                onConditionChanged = { onUpdateBlock(block.copy(condition = it)) }
            )
        }

        BlockType.SHOW -> {
            BlockLabel("Show")
            var sensorExpanded by remember { mutableStateOf(false) }
            Box {
                ChipButton(
                    text = SENSOR_OPTIONS.find { it.first == block.sensorKey }?.second ?: block.sensorKey,
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
                                onUpdateBlock(block.copy(sensorKey = key))
                            }
                        )
                    }
                }
            }
            BlockLabel("in log")
        }

        BlockType.BEEP -> {
            BlockLabel("Beep")
            NumberParamInput(
                value = block.frequencyHz.toString(),
                onChanged = { onUpdateBlock(block.copy(frequencyHz = it.toIntOrNull()?.coerceIn(100, 10000) ?: 440)) },
                suffix = "Hz",
                width = 62.dp
            )
            BlockLabel("for")
            NumberParamInput(
                value = block.durationMs.toString(),
                onChanged = { onUpdateBlock(block.copy(durationMs = it.toIntOrNull()?.coerceAtLeast(10) ?: 300)) },
                suffix = "ms",
                width = 58.dp
            )
        }

        BlockType.REPEAT -> {
            BlockLabel("Repeat")
            NumberParamInput(
                value = block.repeatCount.toString(),
                onChanged = { onUpdateBlock(block.copy(repeatCount = it.toIntOrNull()?.coerceAtLeast(0) ?: 10)) },
                suffix = "times",
                width = 50.dp
            )
        }

        BlockType.FOREVER -> {
            BlockLabel("Forever")
        }

        BlockType.IF -> {
            BlockLabel("If")
            ConditionEditor(
                condition = block.condition,
                onConditionChanged = { onUpdateBlock(block.copy(condition = it)) }
            )
            BlockLabel("then")
        }

        BlockType.UNTIL -> {
            BlockLabel("Repeat until")
            ConditionEditor(
                condition = block.condition,
                onConditionChanged = { onUpdateBlock(block.copy(condition = it)) }
            )
        }
    }
}

@Composable
fun BlockLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MotorDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ChipButton(text = selected, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MOTOR_OPTIONS.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = {
                        expanded = false
                        onSelected(m)
                    }
                )
            }
        }
    }
}

@Composable
fun NumberParamInput(
    value: String,
    onChanged: (String) -> Unit,
    suffix: String? = null,
    width: androidx.compose.ui.unit.Dp = 58.dp
) {
    var text by remember(value) { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChanged(it)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.size(width = width, height = 36.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        if (suffix != null) {
            Text(
                text = suffix,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
