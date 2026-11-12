package com.example.nxt.ui.dragdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock

sealed interface DragPayload {
    data class NewBlock(val type: BlockType) : DragPayload
    data class MoveBlock(val block: NxtBlock, val parentId: String?) : DragPayload
}

sealed interface DropTarget {
    data class RootSlot(val index: Int) : DropTarget
    data class ChildSlot(val parentBlockId: String, val index: Int) : DropTarget
    object TrashZone : DropTarget
}

class DragAndDropState {
    var isDragging by mutableStateOf(false)
        private set

    var payload by mutableStateOf<DragPayload?>(null)
        private set

    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    var currentDropTarget by mutableStateOf<DropTarget?>(null)
        private set

    fun startDrag(dragPayload: DragPayload, initialPosition: Offset) {
        payload = dragPayload
        dragPosition = initialPosition
        currentDropTarget = null
        isDragging = true
    }

    fun updateDragPosition(offset: Offset) {
        dragPosition = offset
    }

    fun setHoveredTarget(target: DropTarget?) {
        currentDropTarget = target
    }

    fun endDrag(onDrop: (DragPayload, DropTarget?) -> Unit) {
        val currentPayload = payload
        val currentTarget = currentDropTarget
        isDragging = false
        payload = null
        currentDropTarget = null
        if (currentPayload != null) {
            onDrop(currentPayload, currentTarget)
        }
    }

    fun cancelDrag() {
        isDragging = false
        payload = null
        currentDropTarget = null
    }
}

val LocalDragAndDropState = compositionLocalOf { DragAndDropState() }

@Composable
fun rememberDragAndDropState(): DragAndDropState {
    return remember { DragAndDropState() }
}
