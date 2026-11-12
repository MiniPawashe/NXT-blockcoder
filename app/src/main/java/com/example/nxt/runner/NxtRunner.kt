package com.example.nxt.runner

import com.example.nxt.bluetooth.NxtBluetoothManager
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.model.NxtCondition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

class ExecutionStoppedException : Exception("Program stopped by user")

/**
 * Executes a Scratch block program on the LEGO Mindstorms NXT brick.
 * Faithfully mirrors the Python NXT Blocks v2 interpreter logic.
 */
class NxtRunner(
    private val btManager: NxtBluetoothManager,
    private val onLog: (String) -> Unit,
    private val onActiveBlockChanged: (String?) -> Unit
) {

    private val stopFlag = AtomicBoolean(false)

    fun stop() {
        stopFlag.set(true)
    }

    private suspend fun checkStopped() {
        if (stopFlag.get() || !currentCoroutineContext().isActive) {
            throw ExecutionStoppedException()
        }
    }

    /**
     * Sliced sleep that responds immediately to stop requests.
     */
    private suspend fun nap(seconds: Double) {
        val totalMs = (seconds * 1000).toLong().coerceAtLeast(0)
        val end = System.currentTimeMillis() + totalMs
        while (true) {
            checkStopped()
            val left = end - System.currentTimeMillis()
            if (left <= 0) return
            delay(minOf(30L, left))
        }
    }

    /**
     * Evaluate condition `c` against current live sensor readings.
     * Fixed wiring matching user's Python code:
     * t1: Port 1 (index 0)
     * t2: Port 2 (index 1)
     * color: Port 3 (index 2)
     * dist: Port 4 (index 3)
     */
    private suspend fun testCondition(cond: NxtCondition): Boolean {
        checkStopped()
        return when (cond.sensorKey) {
            "t1" -> {
                val pressed = btManager.readTouch(0) ?: false
                (pressed) == (cond.operator == "pressed")
            }
            "t2" -> {
                val pressed = btManager.readTouch(1) ?: false
                (pressed) == (cond.operator == "pressed")
            }
            "dist" -> {
                val distance = btManager.readUltrasonic(3) ?: 255
                val threshold = cond.value.toDoubleOrNull() ?: 20.0
                if (cond.operator == "<") distance < threshold else distance > threshold
            }
            "color" -> {
                val currentColor = btManager.readColor(2) ?: "unknown"
                val match = currentColor.equals(cond.value, ignoreCase = true)
                if (cond.operator == "is") match else !match
            }
            else -> false
        }
    }

    /**
     * Run a sequence of blocks.
     */
    suspend fun run(blocks: List<NxtBlock>) {
        try {
            executeBlockList(blocks)
        } finally {
            onActiveBlockChanged(null)
            try {
                btManager.stopAllMotors()
            } catch (_: Exception) {}
        }
    }

    private suspend fun executeBlockList(blocks: List<NxtBlock>) {
        for (blk in blocks) {
            checkStopped()
            onActiveBlockChanged(blk.id)

            when (blk.type) {
                BlockType.MOTOR_ON -> {
                    val p = blk.power.coerceIn(-100, 100)
                    btManager.runMotors(blk.motor, p)
                }

                BlockType.MOTOR_FOR -> {
                    val p = blk.power.coerceIn(-100, 100)
                    btManager.runMotors(blk.motor, p)
                    nap(blk.seconds.coerceAtLeast(0.0))
                    btManager.brakeMotors(blk.motor)
                }

                BlockType.MOTOR_OFF -> {
                    btManager.brakeMotors(blk.motor)
                }

                BlockType.WAIT -> {
                    nap(blk.seconds.coerceAtLeast(0.0))
                }

                BlockType.WAIT_UNTIL -> {
                    while (!testCondition(blk.condition)) {
                        nap(0.03)
                    }
                }

                BlockType.SHOW -> {
                    val reading = when (blk.sensorKey) {
                        "t1" -> "Touch 1: " + (btManager.readTouch(0) ?: "disconnected")
                        "t2" -> "Touch 2: " + (btManager.readTouch(1) ?: "disconnected")
                        "dist" -> "Distance: " + (btManager.readUltrasonic(3)?.let { "$it cm" } ?: "timeout")
                        "color" -> "Color: " + (btManager.readColor(2) ?: "unknown")
                        else -> "${blk.sensorKey}: ?"
                    }
                    onLog(reading)
                }

                BlockType.BEEP -> {
                    val hz = blk.frequencyHz.coerceIn(100, 10000)
                    val ms = blk.durationMs.coerceAtLeast(10)
                    btManager.playTone(hz, ms)
                    nap(ms / 1000.0)
                }

                BlockType.REPEAT -> {
                    val times = blk.repeatCount.coerceAtLeast(0)
                    for (i in 0 until times) {
                        checkStopped()
                        executeBlockList(blk.children)
                        nap(0.02)
                    }
                }

                BlockType.FOREVER -> {
                    while (true) {
                        checkStopped()
                        executeBlockList(blk.children)
                        nap(0.03)
                    }
                }

                BlockType.IF -> {
                    if (testCondition(blk.condition)) {
                        executeBlockList(blk.children)
                    }
                }

                BlockType.UNTIL -> {
                    while (!testCondition(blk.condition)) {
                        checkStopped()
                        executeBlockList(blk.children)
                        nap(0.03)
                    }
                }
            }
        }
    }
}
