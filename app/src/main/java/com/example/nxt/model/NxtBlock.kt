package com.example.nxt.model

import java.util.UUID

/**
 * Condition descriptor for sensor tests in `wait_until`, `if`, and `until` blocks.
 * Matches Python format: { "k": "t1"|"t2"|"dist"|"color", "op": "pressed"|"not pressed"|"<"|">"|"is"|"is not", "v": "20"|"red" }
 */
data class NxtCondition(
    val sensorKey: String = "t1", // "t1", "t2", "dist", "color"
    val operator: String = "pressed", // "pressed", "not pressed", "<", ">", "is", "is not"
    val value: String = "20"
) {
    val sensorDisplayName: String
        get() = when (sensorKey) {
            "t1" -> "Touch 1 (Port 1)"
            "t2" -> "Touch 2 (Port 2)"
            "dist" -> "Distance (Port 4)"
            "color" -> "Color (Port 3)"
            else -> sensorKey
        }
}

enum class BlockType(val rawType: String, val category: BlockCategory) {
    MOTOR_ON("motor_on", BlockCategory.MOTORS),
    MOTOR_FOR("motor_for", BlockCategory.MOTORS),
    MOTOR_OFF("motor_off", BlockCategory.MOTORS),
    WAIT_UNTIL("wait_until", BlockCategory.SENSING),
    SHOW("show", BlockCategory.SENSING),
    WAIT("wait", BlockCategory.CONTROL),
    REPEAT("repeat", BlockCategory.CONTROL),
    FOREVER("forever", BlockCategory.CONTROL),
    IF("if", BlockCategory.CONTROL),
    UNTIL("until", BlockCategory.CONTROL),
    BEEP("beep", BlockCategory.SOUND);

    companion object {
        fun fromRaw(raw: String): BlockType = entries.find { it.rawType == raw } ?: WAIT
    }
}

/**
 * Unified representation of a Scratch-like block in the program tree.
 */
data class NxtBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val motor: String = "A", // 'A', 'B', 'C', 'A+B', 'A+C', 'B+C', 'A+B+C'
    val power: Int = 60, // -100 to 100
    val seconds: Double = 1.0,
    val sensorKey: String = "dist", // 't1', 't2', 'dist', 'color'
    val condition: NxtCondition = NxtCondition(),
    val frequencyHz: Int = 440,
    val durationMs: Int = 300,
    val repeatCount: Int = 10,
    val children: List<NxtBlock> = emptyList()
) {
    val isContainerBlock: Boolean
        get() = type == BlockType.REPEAT || type == BlockType.FOREVER ||
                type == BlockType.IF || type == BlockType.UNTIL
}
