package com.example.nxt.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BlockSerializer {

    /**
     * Parse JSON string matching the desktop Python format into a list of NxtBlocks.
     */
    fun fromJson(jsonStr: String): List<NxtBlock> {
        return try {
            val array = JSONArray(jsonStr.trim())
            parseBlockArray(array)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBlockArray(array: JSONArray): List<NxtBlock> {
        val list = mutableListOf<NxtBlock>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val block = parseBlockObject(obj)
            list.add(block)
        }
        return list
    }

    private fun parseBlockObject(obj: JSONObject): NxtBlock {
        val rawType = obj.optString("t", "wait")
        val blockType = BlockType.fromRaw(rawType)
        val args = obj.optJSONObject("a") ?: JSONObject()
        val childrenArray = obj.optJSONArray("c")
        val children = if (childrenArray != null) parseBlockArray(childrenArray) else emptyList()

        val condObj = obj.optJSONObject("cond")
        val condition = if (condObj != null) {
            NxtCondition(
                sensorKey = condObj.optString("k", "t1"),
                operator = condObj.optString("op", "pressed"),
                value = condObj.opt("v")?.toString() ?: "20"
            )
        } else {
            NxtCondition()
        }

        return NxtBlock(
            id = UUID.randomUUID().toString(),
            type = blockType,
            motor = args.optString("m", "A"),
            power = args.optInt("p", 60),
            seconds = args.optDouble("s", 1.0),
            sensorKey = args.optString("k", "dist"),
            condition = condition,
            frequencyHz = args.optInt("hz", 440),
            durationMs = args.optInt("ms", 300),
            repeatCount = args.optInt("n", 10),
            children = children
        )
    }

    /**
     * Serialize a list of NxtBlocks into JSON matching the desktop Python format.
     */
    fun toJson(blocks: List<NxtBlock>): String {
        val array = serializeBlockList(blocks)
        return array.toString(2)
    }

    private fun serializeBlockList(blocks: List<NxtBlock>): JSONArray {
        val array = JSONArray()
        for (b in blocks) {
            val obj = JSONObject()
            obj.put("t", b.type.rawType)
            val args = JSONObject()

            when (b.type) {
                BlockType.MOTOR_ON -> {
                    args.put("m", b.motor)
                    args.put("p", b.power)
                }
                BlockType.MOTOR_FOR -> {
                    args.put("m", b.motor)
                    args.put("p", b.power)
                    args.put("s", b.seconds)
                }
                BlockType.MOTOR_OFF -> {
                    args.put("m", b.motor)
                }
                BlockType.WAIT -> {
                    args.put("s", b.seconds)
                }
                BlockType.WAIT_UNTIL -> {
                    val cond = JSONObject()
                    cond.put("k", b.condition.sensorKey)
                    cond.put("op", b.condition.operator)
                    cond.put("v", if (b.condition.sensorKey == "dist") b.condition.value.toDoubleOrNull() ?: 20.0 else b.condition.value)
                    obj.put("cond", cond)
                }
                BlockType.SHOW -> {
                    args.put("k", b.sensorKey)
                }
                BlockType.BEEP -> {
                    args.put("hz", b.frequencyHz)
                    args.put("ms", b.durationMs)
                }
                BlockType.REPEAT -> {
                    args.put("n", b.repeatCount)
                    obj.put("c", serializeBlockList(b.children))
                }
                BlockType.FOREVER -> {
                    obj.put("c", serializeBlockList(b.children))
                }
                BlockType.IF -> {
                    val cond = JSONObject()
                    cond.put("k", b.condition.sensorKey)
                    cond.put("op", b.condition.operator)
                    cond.put("v", if (b.condition.sensorKey == "dist") b.condition.value.toDoubleOrNull() ?: 20.0 else b.condition.value)
                    obj.put("cond", cond)
                    obj.put("c", serializeBlockList(b.children))
                }
                BlockType.UNTIL -> {
                    val cond = JSONObject()
                    cond.put("k", b.condition.sensorKey)
                    cond.put("op", b.condition.operator)
                    cond.put("v", if (b.condition.sensorKey == "dist") b.condition.value.toDoubleOrNull() ?: 20.0 else b.condition.value)
                    obj.put("cond", cond)
                    obj.put("c", serializeBlockList(b.children))
                }
            }
            obj.put("a", args)
            array.put(obj)
        }
        return array
    }
}
