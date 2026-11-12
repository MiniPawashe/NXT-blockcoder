package com.example.nxt.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parsers and data structures for LEGO Mindstorms NXT 2.0 direct command responses.
 */
object NxtResponse {

    data class BatteryResult(
        val success: Boolean,
        val millivolts: Int
    )

    data class SensorValueResult(
        val success: Boolean,
        val port: Int,
        val valid: Boolean,
        val sensorType: Byte,
        val sensorMode: Byte,
        val rawValue: Int,
        val normalizedValue: Int,
        val scaledValue: Int,
        val calibratedValue: Int
    )

    data class LsStatusResult(
        val success: Boolean,
        val bytesReady: Int
    )

    data class UltrasonicResult(
        val success: Boolean,
        val distanceCm: Int
    )

    /**
     * Parse battery voltage response from NXT.
     * Response payload: [0x02, 0x0B, status, voltage_LSB, voltage_MSB]
     */
    fun parseBattery(payload: ByteArray): BatteryResult {
        if (payload.size < 5) return BatteryResult(false, 0)
        val status = payload[2].toInt()
        if (status != 0) return BatteryResult(false, 0)

        val buf = ByteBuffer.wrap(payload, 3, 2).order(ByteOrder.LITTLE_ENDIAN)
        val mv = buf.short.toInt() and 0xFFFF
        return BatteryResult(true, mv)
    }

    /**
     * Parse sensor reading response from CMD_GET_INPUT_VALUES (0x07).
     * Response payload:
     * Byte 0: 0x02
     * Byte 1: 0x07
     * Byte 2: Status (0 = success)
     * Byte 3: Input port
     * Byte 4: Valid? (boolean)
     * Byte 5: Calibrated?
     * Byte 6: Sensor type
     * Byte 7: Sensor mode
     * Byte 8-9: Raw A/D value (USHORT LE)
     * Byte 10-11: Normalized value (USHORT LE)
     * Byte 12-13: Scaled value (SHORT LE)
     * Byte 14-15: Calibrated value (SHORT LE)
     */
    fun parseSensorValues(payload: ByteArray): SensorValueResult {
        if (payload.size < 16) {
            return SensorValueResult(
                success = false, port = -1, valid = false,
                sensorType = 0, sensorMode = 0,
                rawValue = 0, normalizedValue = 0,
                scaledValue = 0, calibratedValue = 0
            )
        }

        val status = payload[2].toInt()
        val success = (status == 0)
        val port = payload[3].toInt() and 0xFF
        val valid = payload[4].toInt() != 0
        val sensorType = payload[6]
        val sensorMode = payload[7]

        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val raw = buf.getShort(8).toInt() and 0xFFFF
        val norm = buf.getShort(10).toInt() and 0xFFFF
        val scaled = buf.getShort(12).toInt()
        val calib = buf.getShort(14).toInt()

        return SensorValueResult(
            success = success,
            port = port,
            valid = valid,
            sensorType = sensorType,
            sensorMode = sensorMode,
            rawValue = raw,
            normalizedValue = norm,
            scaledValue = scaled,
            calibratedValue = calib
        )
    }

    /**
     * Parse color string name from scaled value in COLORFULL mode.
     * Matches standard NXT-Python color names:
     * 1 = black, 2 = blue, 3 = green, 4 = yellow, 5 = red, 6 = white.
     */
    fun colorNameFromScaled(scaled: Int): String {
        return when (scaled) {
            1 -> "black"
            2 -> "blue"
            3 -> "green"
            4 -> "yellow"
            5 -> "red"
            6 -> "white"
            else -> "unknown"
        }
    }

    /**
     * Parse LS_GET_STATUS response payload.
     * Payload: [0x02, 0x0E, status, bytesReady]
     */
    fun parseLsStatus(payload: ByteArray): LsStatusResult {
        if (payload.size < 4) return LsStatusResult(false, 0)
        val status = payload[2].toInt()
        val bytesReady = payload[3].toInt() and 0xFF
        return LsStatusResult(status == 0, bytesReady)
    }

    /**
     * Parse LS_READ response payload.
     * Payload: [0x02, 0x10, status, rxDataLength, rxData (up to 16 bytes)]
     */
    fun parseUltrasonic(payload: ByteArray): UltrasonicResult {
        if (payload.size < 5) return UltrasonicResult(false, -1)
        val status = payload[2].toInt()
        if (status != 0) return UltrasonicResult(false, -1)
        val rxLen = payload[3].toInt() and 0xFF
        if (rxLen < 1 || payload.size < 4 + rxLen) return UltrasonicResult(false, -1)
        val distance = payload[4].toInt() and 0xFF
        return UltrasonicResult(true, distance)
    }
}
