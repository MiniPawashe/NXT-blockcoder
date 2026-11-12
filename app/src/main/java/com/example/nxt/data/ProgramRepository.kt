package com.example.nxt.data

import com.example.nxt.model.BlockSerializer
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.model.NxtCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProgramRepository(private val dao: ProgramDao) {

    val allPrograms: Flow<List<SavedProgramEntity>> = dao.getAllPrograms()

    suspend fun saveProgram(
        name: String,
        description: String,
        blocks: List<NxtBlock>,
        existingId: Long = 0L,
        isTemplate: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val json = BlockSerializer.toJson(blocks)
        val count = countAllBlocks(blocks)
        val entity = SavedProgramEntity(
            id = existingId,
            name = name.ifBlank { "Untitled Project" },
            description = description,
            jsonContent = json,
            blockCount = count,
            updatedAt = System.currentTimeMillis(),
            isTemplate = isTemplate
        )
        dao.insertProgram(entity)
    }

    suspend fun loadProgram(id: Long): Pair<SavedProgramEntity, List<NxtBlock>>? = withContext(Dispatchers.IO) {
        val entity = dao.getProgramById(id) ?: return@withContext null
        val blocks = BlockSerializer.fromJson(entity.jsonContent)
        Pair(entity, blocks)
    }

    suspend fun deleteProgram(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteProgramById(id)
    }

    private fun countAllBlocks(list: List<NxtBlock>): Int {
        var count = list.size
        for (blk in list) {
            if (blk.isContainerBlock) {
                count += countAllBlocks(blk.children)
            }
        }
        return count
    }

    suspend fun seedTemplatesIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getProgramCount() == 0) {
            // Template 1: Obstacle Avoider (From user's macOS Python script)
            val obstacleAvoider = listOf(
                NxtBlock(
                    type = BlockType.FOREVER,
                    children = listOf(
                        NxtBlock(
                            type = BlockType.IF,
                            condition = NxtCondition(sensorKey = "t1", operator = "pressed", value = "20"),
                            children = listOf(
                                NxtBlock(type = BlockType.MOTOR_ON, motor = "A+C", power = 60)
                            )
                        ),
                        NxtBlock(
                            type = BlockType.IF,
                            condition = NxtCondition(sensorKey = "t1", operator = "not pressed", value = "20"),
                            children = listOf(
                                NxtBlock(type = BlockType.MOTOR_OFF, motor = "A+C")
                            )
                        )
                    )
                )
            )
            saveProgram(
                name = "Obstacle Avoider",
                description = "Drives forward with Motors A+C when Touch 1 is pressed; stops when released (from Python script).",
                blocks = obstacleAvoider,
                isTemplate = true
            )

            // Template 2: Color Line Follower
            val colorFollower = listOf(
                NxtBlock(type = BlockType.BEEP, frequencyHz = 523, durationMs = 200),
                NxtBlock(type = BlockType.MOTOR_ON, motor = "B", power = 40),
                NxtBlock(
                    type = BlockType.WAIT_UNTIL,
                    condition = NxtCondition(sensorKey = "color", operator = "is", value = "red")
                ),
                NxtBlock(type = BlockType.MOTOR_OFF, motor = "B"),
                NxtBlock(type = BlockType.BEEP, frequencyHz = 880, durationMs = 500),
                NxtBlock(type = BlockType.SHOW, sensorKey = "color")
            )
            saveProgram(
                name = "Color Sensor Stopper",
                description = "Runs motor B until Red color is detected on Port 3, then plays a tone and logs result.",
                blocks = colorFollower,
                isTemplate = true
            )

            // Template 3: Ultrasonic Distance Guard
            val distanceGuard = listOf(
                NxtBlock(
                    type = BlockType.REPEAT,
                    repeatCount = 5,
                    children = listOf(
                        NxtBlock(type = BlockType.SHOW, sensorKey = "dist"),
                        NxtBlock(
                            type = BlockType.WAIT_UNTIL,
                            condition = NxtCondition(sensorKey = "dist", operator = "<", value = "25")
                        ),
                        NxtBlock(type = BlockType.BEEP, frequencyHz = 1000, durationMs = 150),
                        NxtBlock(type = BlockType.WAIT, seconds = 0.2),
                        NxtBlock(type = BlockType.BEEP, frequencyHz = 1200, durationMs = 200)
                    )
                )
            )
            saveProgram(
                name = "Ultrasonic Guard",
                description = "Scans with Ultrasonic sensor on Port 4; beeps warning alarm when an object gets closer than 25cm.",
                blocks = distanceGuard,
                isTemplate = true
            )

            // Template 4: Musical Fanfare
            val musicalFanfare = listOf(
                NxtBlock(type = BlockType.BEEP, frequencyHz = 262, durationMs = 180),
                NxtBlock(type = BlockType.BEEP, frequencyHz = 330, durationMs = 180),
                NxtBlock(type = BlockType.BEEP, frequencyHz = 392, durationMs = 180),
                NxtBlock(type = BlockType.BEEP, frequencyHz = 523, durationMs = 350),
                NxtBlock(type = BlockType.MOTOR_FOR, motor = "A", power = 50, seconds = 0.5)
            )
            saveProgram(
                name = "Musical Fanfare",
                description = "Plays ascending harmonic arpeggios through the NXT internal speaker and spins motor A.",
                blocks = musicalFanfare,
                isTemplate = true
            )
        }
    }
}
