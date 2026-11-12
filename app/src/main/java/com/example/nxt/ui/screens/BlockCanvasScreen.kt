package com.example.nxt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.ui.components.BlockItemCard
import com.example.nxt.ui.components.DropSlot
import com.example.nxt.ui.dragdrop.DragGhostOverlay
import com.example.nxt.ui.dragdrop.DragPayload
import com.example.nxt.ui.dragdrop.DropTarget
import com.example.nxt.ui.dragdrop.LocalDragAndDropState
import com.example.nxt.ui.dragdrop.rememberDragAndDropState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockCanvasScreen(
    program: List<NxtBlock>,
    activeBlockId: String?,
    isRunning: Boolean,
    onAddBlock: (BlockType) -> Unit,
    onInsertBlockAtIndex: (BlockType, Int, String?) -> Unit,
    onMoveBlockToIndex: (String, Int, String?) -> Unit,
    onAddChildBlock: (String, BlockType) -> Unit,
    onUpdateBlock: (NxtBlock) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onMoveBlock: (String, Int) -> Unit,
    onLoadExampleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dndState = rememberDragAndDropState()
    var showPhoneSidebarSheet by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDragAndDropState provides dndState) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1D))
        ) {
            val isWideScreen = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Wide Screen permanent Sidebar
                if (isWideScreen) {
                    ScratchSidebar(
                        onAddBlock = onAddBlock,
                        onDeleteDroppedBlock = onDeleteBlock,
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF1E293B))
                    )
                }

                // Interactive Code Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Top toolbar on compact phones to open blocks drawer
                        if (!isWideScreen) {
                            item {
                                Surface(
                                    onClick = { showPhoneSidebarSheet = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .testTag("btn_open_block_palette")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                            Text("Open Blocks Drawer (Motors, Sensors...)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        // Scratch Hat Block: "▶ when Run clicked"
                        item {
                            Surface(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                color = Color(0xFFFFBF00),
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color(0x33000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "when Run clicked",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Drop Slot at top (index 0)
                        item {
                            DropSlot(
                                target = DropTarget.RootSlot(0),
                                onDropHere = {
                                    handleDrop(dndState, 0, null, onInsertBlockAtIndex, onMoveBlockToIndex)
                                }
                            )
                        }

                        // If empty, display friendly onboarding state
                        if (program.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Your script is empty",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Drag blocks from the sidebar into the script, or tap a block to append it.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        OutlinedButton(
                                            onClick = onLoadExampleClick
                                        ) {
                                            Text("Load Preset Program", color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(program, key = { _, block -> block.id }) { index, block ->
                                val isActive = isRunning && activeBlockId == block.id

                                BlockItemCard(
                                    block = block,
                                    isActiveStep = isActive,
                                    parentId = null,
                                    onUpdateBlock = onUpdateBlock,
                                    onDeleteBlock = onDeleteBlock,
                                    onMoveBlock = onMoveBlock,
                                    onAddChildBlock = onAddChildBlock,
                                    onDropAtChildIndex = { targetParentId, childIndex ->
                                        handleDrop(dndState, childIndex, targetParentId, onInsertBlockAtIndex, onMoveBlockToIndex)
                                    }
                                )

                                // Drop slot following this block
                                DropSlot(
                                    target = DropTarget.RootSlot(index + 1),
                                    onDropHere = {
                                        handleDrop(dndState, index + 1, null, onInsertBlockAtIndex, onMoveBlockToIndex)
                                    }
                                )
                            }
                        }

                        // Bottom spacer
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }

                    // Floating Action Button to quickly open block palette on compact phones
                    if (!isWideScreen) {
                        FloatingActionButton(
                            onClick = { showPhoneSidebarSheet = true },
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp)
                                .testTag("fab_open_sidebar")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Blocks")
                        }
                    }
                }
            }

            // Compact phone modal sheet for sidebar
            if (showPhoneSidebarSheet && !isWideScreen) {
                ModalBottomSheet(
                    onDismissRequest = { showPhoneSidebarSheet = false },
                    containerColor = Color(0xFF0F172A)
                ) {
                    ScratchSidebar(
                        onAddBlock = { type ->
                            onAddBlock(type)
                            showPhoneSidebarSheet = false
                        },
                        onDeleteDroppedBlock = { id ->
                            onDeleteBlock(id)
                            showPhoneSidebarSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(bottom = 24.dp)
                    )
                }
            }

            // Floating drag ghost preview following touch
            DragGhostOverlay(dndState = dndState)
        }
    }
}

private fun handleDrop(
    dndState: com.example.nxt.ui.dragdrop.DragAndDropState,
    targetIndex: Int,
    targetParentId: String?,
    onInsert: (BlockType, Int, String?) -> Unit,
    onMove: (String, Int, String?) -> Unit
) {
    val payload = dndState.payload ?: return
    when (payload) {
        is DragPayload.NewBlock -> {
            onInsert(payload.type, targetIndex, targetParentId)
        }
        is DragPayload.MoveBlock -> {
            onMove(payload.block.id, targetIndex, targetParentId)
        }
    }
    dndState.endDrag { _, _ -> }
}
