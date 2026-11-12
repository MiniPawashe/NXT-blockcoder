package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nxt.model.BlockSerializer
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.model.NxtCondition
import com.example.nxt.protocol.NxtCommand
import com.example.nxt.protocol.NxtResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NXT Coder", appName)
    }

    @Test
    fun `serialize and deserialize python format blocks`() {
        val pythonJson = """
            [
              {
                "t": "forever",
                "a": {},
                "c": [
                  {
                    "t": "if",
                    "a": {},
                    "cond": { "k": "t1", "op": "pressed", "v": 20 },
                    "c": [
                      { "t": "motor_on", "a": { "m": "A+C", "p": 60 } }
                    ]
                  },
                  {
                    "t": "if",
                    "a": {},
                    "cond": { "k": "t1", "op": "not pressed", "v": 20 },
                    "c": [
                      { "t": "motor_off", "a": { "m": "A+C" } }
                    ]
                  }
                ]
              }
            ]
        """.trimIndent()

        val blocks = BlockSerializer.fromJson(pythonJson)
        assertEquals(1, blocks.size)
        assertEquals(BlockType.FOREVER, blocks[0].type)
        assertEquals(2, blocks[0].children.size)

        val ifBlock = blocks[0].children[0]
        assertEquals(BlockType.IF, ifBlock.type)
        assertEquals("t1", ifBlock.condition.sensorKey)
        assertEquals("pressed", ifBlock.condition.operator)

        val motorOn = ifBlock.children[0]
        assertEquals(BlockType.MOTOR_ON, motorOn.type)
        assertEquals("A+C", motorOn.motor)
        assertEquals(60, motorOn.power)

        // Test roundtrip serialization
        val serialized = BlockSerializer.toJson(blocks)
        assertTrue(serialized.contains("motor_on"))
        assertTrue(serialized.contains("A+C"))
    }

    @Test
    fun `test nxt command frame packet header`() {
        val packet = NxtCommand.setOutputState(port = 0, power = 75)
        // Check 2-byte Little Endian length header
        val len = (packet[0].toInt() and 0xFF) or ((packet[1].toInt() and 0xFF) shl 8)
        assertEquals(packet.size - 2, len)
        // Command byte
        assertEquals(NxtCommand.CMD_SET_OUTPUT_STATE, packet[3])
    }

    @Test
    fun `test nxt response color name parser`() {
        assertEquals("red", NxtResponse.colorNameFromScaled(5))
        assertEquals("blue", NxtResponse.colorNameFromScaled(2))
        assertEquals("green", NxtResponse.colorNameFromScaled(3))
        assertEquals("white", NxtResponse.colorNameFromScaled(6))
        assertEquals("black", NxtResponse.colorNameFromScaled(1))
    }

    @Test
    fun `test room database save and load program`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.nxt.data.NxtAppDatabase::class.java).build()
        val repo = com.example.nxt.data.ProgramRepository(db.programDao())

        repo.seedTemplatesIfEmpty()
        val all = repo.allPrograms.first()
        assertEquals(4, all.size)

        // Save custom project
        val id = repo.saveProgram(
            name = "Test Maze Runner",
            description = "Custom test",
            blocks = listOf(NxtBlock(type = BlockType.MOTOR_ON, motor = "A+C", power = 80))
        )
        val loaded = repo.loadProgram(id)
        assertNotNull(loaded)
        assertEquals("Test Maze Runner", loaded!!.first.name)
        assertEquals(1, loaded.second.size)
        assertEquals(BlockType.MOTOR_ON, loaded.second[0].type)
        db.close()
    }
}
