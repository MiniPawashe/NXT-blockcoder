package com.example.nxt.model

import androidx.compose.ui.graphics.Color

enum class BlockCategory(
    val title: String,
    val color: Color,
    val darkColor: Color
) {
    MOTORS(
        title = "Motors",
        color = Color(0xFF4C97FF),
        darkColor = Color(0xFF2563EB)
    ),
    SENSING(
        title = "Sensing",
        color = Color(0xFF2FB4D8),
        darkColor = Color(0xFF0891B2)
    ),
    CONTROL(
        title = "Control",
        color = Color(0xFFFFAB19),
        darkColor = Color(0xFFD97706)
    ),
    SOUND(
        title = "Sound",
        color = Color(0xFFCF63CF),
        darkColor = Color(0xFF9333EA)
    )
}
