package com.example.nxt.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * LEGO Mindstorms NXT 2.0 Direct and System Command packet builder.
 * All commands sent over Bluetooth RFCOMM are framed with a 2-byte Little Endian length header:
 * [Length LSB, Length MSB, Telegram Payload...]
 */
object NxtCommand {

    // Telegram types
    const val TYPE_DIRECT_REPLY: Byte = 0x00
    const val TYPE_SYSTEM_REPLY: Byte = 0x01
    const val TYPE_DIRECT_NO_REPLY: Byte = 0x80.toByte()
    const val TYPE_SYSTEM_NO_REPLY: Byte = 0x81.toByte()

    // Command opcodes
    const val CMD_SET_OUTPUT_STATE: Byte = 0x04
    const val CMD_SET_INPUT_MODE: Byte = 0x05
    const val CMD_GET_OUTPUT_STATE: Byte = 0x06
    const val CMD_GET_INPUT_VALUES: Byte = 0x07
    const val CMD_PLAY_TONE: Byte = 0x06 // Note: 0x06 direct command is PLAY_TONE
    const val CMD_KEEP_ALIVE: Byte = 0x0D
    const val CMD_GET_BATTERY_LEVEL: Byte = 0x0B
    const val CMD_STOP_PROGRAM: Byte = 0x01
    const val CMD_LS_WRITE: Byte = 0x0F
    const val CMD_LS_READ: Byte = 0x10
    const val CMD_LS_GET_STATUS: Byte = 0x0E

    // Motor output ports (0 = A, 1 = B, 2 = C, 0xFF = All)
    const val MOTOR_A: Byte = 0x00
    const val MOTOR_B: Byte = 0x01
    const val MOTOR_C: Byte = 0x02
    const val MOTOR_ALL: Byte = 0xFF.toByte()

    // Motor Modes (bitfield)
    const val MODE_MOTORON: Byte = 0x01
    const val MODE_BRAKE: Byte = 0x02
    const val MODE_REGULATED: Byte = 0x04

    // Regulation Modes
    const val REGULATION_IDLE: Byte = 0x00
    const val REGULATION_MOTOR_SPEED: Byte = 0x01
    const val REGULATION_MOTOR_SYNC: Byte = 0x02

    // Run States
    const val RUN_STATE_IDLE: Byte = 0x00
    const val RUN_STATE_RAMPUP: Byte = 0x10
    const val RUN_STATE_RUNNING: Byte = 0x20
    const val RUN_STATE_RAMPDOWN: Byte = 0x40

    // Sensor Types
    const val SENSOR_TYPE_NO_SENSOR: Byte = 0x00
    const val SENSOR_TYPE_SWITCH: Byte = 0x01 // Touch sensor
    const val SENSOR_TYPE_TEMPERATURE: Byte = 0x02
    const val SENSOR_TYPE_REFLECTION: Byte = 0x03
    const val SENSOR_TYPE_ANGLE: Byte = 0x04
    const val SENSOR_TYPE_LIGHT_ACTIVE: Byte = 0x05
    const val SENSOR_TYPE_LIGHT_INACTIVE: Byte = 0x06
    const val SENSOR_TYPE_SOUND_DB: Byte = 0x07
    const val SENSOR_TYPE_SOUND_DBA: Byte = 0x08
    const val SENSOR_TYPE_LOWSPEED_9V: Byte = 0x0B // Ultrasonic sensor
    const val SENSOR_TYPE_COLORFULL: Byte = 0x0D // NXT 2.0 Color sensor
    const val SENSOR_TYPE_COLORRED: Byte = 0x0E
    const val SENSOR_TYPE_COLORGREEN: Byte = 0x0F
    const val SENSOR_TYPE_COLORBLUE: Byte = 0x10
    const val SENSOR_TYPE_COLORNONE: Byte = 0x11

    // Sensor Modes
    const val SENSOR_MODE_RAW: Byte = 0x00
    const val SENSOR_MODE_BOOLEAN: Byte = 0x20
    const val SENSOR_MODE_TRANSITIONCNT: Byte = 0x40
    const val SENSOR_MODE_PERIODCOUNTER: Byte = 0x60
    const val SENSOR_MODE_PCTFULLSCALE: Byte = 0x80.toByte()
    const val SENSOR_MODE_CELSIUS: Byte = 0xA0.toByte()
    const val SENSOR_MODE_FAHRENHEIT: Byte = 0xC0.toByte()
    const val SENSOR_MODE_ANGLESTEPS: Byte = 0xE0.toByte()

    /**
     * Wrap payload in 2-byte Little Endian Bluetooth length header.
     */
    fun framePacket(payload: ByteArray): ByteArray {
        val len = payload.size
        val packet = ByteArray(len + 2)
        packet[0] = (len and 0xFF).toByte()
        packet[1] = ((len shr 8) and 0xFF).toByte()
        System.arraycopy(payload, 0, packet, 2, len)
        return packet
    }

    /**
     * Build SetOutputState packet.
     * @param port 0=A, 1=B, 2=C, 0xFF=All
     * @param power -100 to 100
     * @param mode bitfield (MOTORON | BRAKE | REGULATED)
     * @param regulationMode REGULATION_MOTOR_SPEED or REGULATION_IDLE
     * @param turnRatio -100 to 100
     * @param runState RUN_STATE_RUNNING or RUN_STATE_IDLE
     * @param tachoLimit 0 for unlimited run, or degree count
     */
    fun setOutputState(
        port: Byte,
        power: Int,
        mode: Byte = (MODE_MOTORON.toInt() or MODE_BRAKE.toInt() or MODE_REGULATED.toInt()).toByte(),
        regulationMode: Byte = REGULATION_MOTOR_SPEED,
        turnRatio: Byte = 0,
        runState: Byte = RUN_STATE_RUNNING,
        tachoLimit: Long = 0L,
        requireReply: Boolean = false
    ): ByteArray {
        val buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(if (requireReply) TYPE_DIRECT_REPLY else TYPE_DIRECT_NO_REPLY)
        buf.put(CMD_SET_OUTPUT_STATE)
        buf.put(port)
        buf.put(power.coerceIn(-100, 100).toByte())
        buf.put(mode)
        buf.put(regulationMode)
        buf.put(turnRatio)
        buf.put(runState)
        buf.putInt(tachoLimit.toInt())
        return framePacket(buf.array())
    }

    /**
     * Stop a motor and apply electronic brake.
     */
    fun brakeMotor(port: Byte, requireReply: Boolean = false): ByteArray {
        return setOutputState(
            port = port,
            power = 0,
            mode = MODE_BRAKE,
            regulationMode = REGULATION_IDLE,
            turnRatio = 0,
            runState = RUN_STATE_IDLE,
            tachoLimit = 0L,
            requireReply = requireReply
        )
    }

    /**
     * Float a motor (idle with no electronic braking).
     */
    fun idleMotor(port: Byte, requireReply: Boolean = false): ByteArray {
        return setOutputState(
            port = port,
            power = 0,
            mode = 0x00,
            regulationMode = REGULATION_IDLE,
            turnRatio = 0,
            runState = RUN_STATE_IDLE,
            tachoLimit = 0L,
            requireReply = requireReply
        )
    }

    /**
     * Set input sensor configuration for Port 0..3 (representing ports 1..4).
     */
    fun setInputMode(
        port: Byte,
        sensorType: Byte,
        sensorMode: Byte,
        requireReply: Boolean = false
    ): ByteArray {
        val buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(if (requireReply) TYPE_DIRECT_REPLY else TYPE_DIRECT_NO_REPLY)
        buf.put(CMD_SET_INPUT_MODE)
        buf.put(port)
        buf.put(sensorType)
        buf.put(sensorMode)
        return framePacket(buf.array())
    }

    /**
     * Query input sensor values (Port 0..3). Response required.
     */
    fun getInputValues(port: Byte): ByteArray {
        val buf = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_REPLY)
        buf.put(CMD_GET_INPUT_VALUES)
        buf.put(port)
        return framePacket(buf.array())
    }

    /**
     * Play tone at given frequency for duration ms.
     */
    fun playTone(hz: Int, durationMs: Int, requireReply: Boolean = false): ByteArray {
        val buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(if (requireReply) TYPE_DIRECT_REPLY else TYPE_DIRECT_NO_REPLY)
        buf.put(CMD_PLAY_TONE)
        buf.putShort(hz.coerceIn(100, 10000).toShort())
        buf.putShort(durationMs.coerceIn(10, 10000).toShort())
        return framePacket(buf.array())
    }

    /**
     * Query battery voltage in millivolts. Response required.
     */
    fun getBatteryLevel(): ByteArray {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_REPLY)
        buf.put(CMD_GET_BATTERY_LEVEL)
        return framePacket(buf.array())
    }

    /**
     * Reset sleep timer on NXT brick.
     */
    fun keepAlive(): ByteArray {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_NO_REPLY)
        buf.put(CMD_KEEP_ALIVE)
        return framePacket(buf.array())
    }

    /**
     * Ultrasonic I2C Write: Send command 0x42 to I2C address 0x02 to request distance reading.
     */
    fun ultrasonicInitRead(port: Byte = 3): ByteArray {
        // Port 3 = Sensor Port 4 (Ultrasonic)
        val buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_NO_REPLY)
        buf.put(CMD_LS_WRITE)
        buf.put(port)
        buf.put(2.toByte()) // TxDataLength (2 bytes)
        buf.put(1.toByte()) // RxDataLength (expect 1 byte distance back)
        buf.put(0x02.toByte()) // Ultrasonic I2C Device Address
        buf.put(0x42.toByte()) // Read measurement byte 0 (distance in cm)
        return framePacket(buf.array())
    }

    /**
     * Query I2C LowSpeed status to see if bytes are ready.
     */
    fun lsGetStatus(port: Byte = 3): ByteArray {
        val buf = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_REPLY)
        buf.put(CMD_LS_GET_STATUS)
        buf.put(port)
        return framePacket(buf.array())
    }

    /**
     * Read LowSpeed I2C data bytes.
     */
    fun lsRead(port: Byte = 3): ByteArray {
        val buf = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DIRECT_REPLY)
        buf.put(CMD_LS_READ)
        buf.put(port)
        return framePacket(buf.array())
    }
}
