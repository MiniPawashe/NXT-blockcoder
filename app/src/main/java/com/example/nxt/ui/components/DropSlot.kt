package com.example.nxt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.ui.dragdrop.DropTarget
import com.example.nxt.ui.dragdrop.LocalDragAndDropState

@Composable
fun DropSlot(
    target: DropTarget,
    onDropHere: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dndState = LocalDragAndDropState.current
    val isTargetHovered = dndState.isDragging && dndState.currentDropTarget == target

    val height by animateDpAsState(
        targetValue = if (isTargetHovered) 28.dp else if (dndState.isDragging) 10.dp else 4.dp,
        animationSpec = tween(150),
        label = "slotHeight"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isTargetHovered) Color(0x6638BDF8) else if (dndState.isDragging) Color(0x2238BDF8) else Color.Transparent,
        animationSpec = tween(150),
        label = "slotBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(
                if (isTargetHovered) {
                    Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                } else Modifier
            )
            .onGloballyPositioned { coordinates ->
                if (dndState.isDragging) {
                    val bounds = coordinates.boundsInWindow()
                    // Check if current drag position falls inside this slot's expanded bounds
                    val pos = dndState.dragPosition
                    val expandedBounds = bounds.inflate(14f)
                    if (pos.x in expandedBounds.left..expandedBounds.right &&
                        pos.y in expandedBounds.top..expandedBounds.bottom
                    ) {
                        dndState.setHoveredTarget(target)
                    } else if (dndState.currentDropTarget == target) {
                        // Pointer moved away
                        dndState.setHoveredTarget(null)
                    }
                }
            }
            .clickable(enabled = dndState.isDragging) {
                onDropHere()
            }
            .testTag("drop_slot"),
        contentAlignment = Alignment.Center
    ) {
        if (isTargetHovered) {
            Text(
                text = "Drop block here",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

private fun androidx.compose.ui.geometry.Rect.inflate(amount: Float): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        left = left - amount,
        top = top - amount,
        right = right + amount,
        bottom = bottom + amount
    )
}
