package com.example.nxt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.model.BlockCategory
import com.example.nxt.model.BlockType
import com.example.nxt.ui.dragdrop.DragPayload
import com.example.nxt.ui.dragdrop.DropTarget
import com.example.nxt.ui.dragdrop.LocalDragAndDropState

fun getCategoryIcon(cat: BlockCategory): ImageVector {
    return when (cat) {
        BlockCategory.MOTORS -> Icons.Default.Settings
        BlockCategory.SENSING -> Icons.Default.Sensors
        BlockCategory.CONTROL -> Icons.Default.Loop
        BlockCategory.SOUND -> Icons.Default.MusicNote
    }
}

@Composable
fun ScratchSidebar(
    onAddBlock: (BlockType) -> Unit,
    onDeleteDroppedBlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(BlockCategory.MOTORS) }
    val dndState = LocalDragAndDropState.current

    val isTrashHovered = dndState.isDragging &&
            dndState.payload is DragPayload.MoveBlock &&
            dndState.currentDropTarget == DropTarget.TrashZone

    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0F172A))
    ) {
        // Left Column: Category Navigation Buttons Strip (#cats)
        Column(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .background(Color(0xFF0B1120))
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlockCategory.entries.forEach { category ->
                val isSelected = category == selectedCategory
                Surface(
                    onClick = { selectedCategory = category },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nav_cat_${category.title.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(category.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = category.title,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = category.title,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Trash Drop Target (Visible when dragging an existing block)
            if (dndState.isDragging && dndState.payload is DragPayload.MoveBlock) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTrashHovered) Color(0xFFEF4444) else Color(0x33EF4444),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInWindow()
                            val pos = dndState.dragPosition
                            if (pos.x in bounds.left..bounds.right && pos.y in bounds.top..bounds.bottom) {
                                dndState.setHoveredTarget(DropTarget.TrashZone)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Trash",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Drop to\nDelete",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E293B))
        )

        // Right Column: Palette blocks for selected category (#blocks)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(selectedCategory.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${selectedCategory.title} Blocks",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Drag into script or tap to append",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val blocks = PALETTE_ITEMS[selectedCategory] ?: emptyList()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(blocks) { item ->
                    DraggablePaletteBlockCard(
                        item = item,
                        category = selectedCategory,
                        onTapAdd = { onAddBlock(item.type) }
                    )
                }
            }
        }
    }
}

@Composable
fun DraggablePaletteBlockCard(
    item: PaletteBlockItem,
    category: BlockCategory,
    onTapAdd: () -> Unit
) {
    val dndState = LocalDragAndDropState.current
    var itemWindowOffset by remember { mutableStateOf(Offset.Zero) }

    Surface(
        onClick = onTapAdd,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draggable_block_${item.type.rawType}")
            .onGloballyPositioned { coordinates ->
                itemWindowOffset = coordinates.boundsInWindow().topLeft
            }
            .pointerInput(item.type) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val globalStart = itemWindowOffset + localOffset
                        dndState.startDrag(DragPayload.NewBlock(item.type), globalStart)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dndState.updateDragPosition(dndState.dragPosition + dragAmount)
                    },
                    onDragEnd = {
                        dndState.endDrag { payload, target ->
                            // Handled by canvas drop callback
                        }
                    },
                    onDragCancel = {
                        dndState.cancelDrag()
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(category.color)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.previewTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.previewSubtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(category.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
