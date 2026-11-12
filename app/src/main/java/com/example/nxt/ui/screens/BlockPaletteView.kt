package com.example.nxt.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nxt.model.BlockCategory
import com.example.nxt.model.BlockType

data class PaletteBlockItem(
    val type: BlockType,
    val previewTitle: String,
    val previewSubtitle: String
)

val PALETTE_ITEMS = mapOf(
    BlockCategory.MOTORS to listOf(
        PaletteBlockItem(BlockType.MOTOR_ON, "Motor [A+C] on at power [60%]", "Starts selected motors indefinitely"),
        PaletteBlockItem(BlockType.MOTOR_FOR, "Run motor [A+C] power [60%] for [1s]", "Runs motors for duration, then brakes"),
        PaletteBlockItem(BlockType.MOTOR_OFF, "Stop motor [A+C]", "Applies electronic brake to motors")
    ),
    BlockCategory.SENSING to listOf(
        PaletteBlockItem(BlockType.WAIT_UNTIL, "Wait until <Condition>", "Pauses execution until sensor condition is met"),
        PaletteBlockItem(BlockType.SHOW, "Show [Sensor] in log", "Polls sensor and prints live value to the log")
    ),
    BlockCategory.CONTROL to listOf(
        PaletteBlockItem(BlockType.WAIT, "Wait [1] seconds", "Pauses execution for a duration"),
        PaletteBlockItem(BlockType.REPEAT, "Repeat [10] times", "Loops nested child blocks N times"),
        PaletteBlockItem(BlockType.FOREVER, "Forever", "Loops nested child blocks continuously"),
        PaletteBlockItem(BlockType.IF, "If <Condition> then", "Executes child blocks if condition is true"),
        PaletteBlockItem(BlockType.UNTIL, "Repeat until <Condition>", "Repeats child blocks until condition is true")
    ),
    BlockCategory.SOUND to listOf(
        PaletteBlockItem(BlockType.BEEP, "Beep [440] Hz for [300] ms", "Plays audio tone through NXT speaker")
    )
)

@Composable
fun BlockPaletteView(
    onAddBlock: (BlockType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(BlockCategory.MOTORS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(12.dp)
    ) {
        Text(
            text = "Block Library",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Category Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(BlockCategory.entries) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    onClick = { selectedCategory = category },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) category.color else Color(0xFF1E293B),
                    modifier = Modifier.testTag("cat_${category.title.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(category.color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = category.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Blocks in current category
        val blocks = PALETTE_ITEMS[selectedCategory] ?: emptyList()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(blocks) { item ->
                PaletteCard(
                    item = item,
                    category = selectedCategory,
                    onClick = { onAddBlock(item.type) }
                )
            }
        }
    }
}

@Composable
fun PaletteCard(
    item: PaletteBlockItem,
    category: BlockCategory,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("palette_block_${item.type.rawType}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored category marker
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
                        fontSize = 13.sp
                    )
                    Text(
                        text = item.previewSubtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(category.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Block",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
