package com.example.nxt.ui.dragdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.model.BlockType
import kotlin.math.roundToInt

@Composable
fun DragGhostOverlay(
    dndState: DragAndDropState,
    modifier: Modifier = Modifier
) {
    if (!dndState.isDragging || dndState.payload == null) return

    val payload = dndState.payload ?: return
    val density = LocalDensity.current

    val title: String
    val color: Color

    when (payload) {
        is DragPayload.NewBlock -> {
            title = payload.type.rawType.replace("_", " ").replaceFirstChar { it.uppercase() }
            color = payload.type.category.color
        }
        is DragPayload.MoveBlock -> {
            title = payload.block.type.rawType.replace("_", " ").replaceFirstChar { it.uppercase() }
            color = payload.block.type.category.color
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.92f),
            shadowElevation = 12.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dndState.dragPosition.x - 60).roundToInt(),
                        (dndState.dragPosition.y - 40).roundToInt()
                    )
                }
                .rotate(3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
