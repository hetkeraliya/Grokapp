package com.example.miband5.ble

object MiBand5DataParser {

    data class WalkData(val steps: Int, val distanceMeters: Int?, val calories: Int?)

    /**
     * Walk / steps characteristic. Matches the web parser:
     *  - length >= 13: steps at byte 1 (u16 LE), distance at 5 (u32 LE), calories at 9 (u32 LE)
     *  - otherwise: steps at byte 0
     */
    fun parseWalk(data: ByteArray): WalkData? {
        if (data.size < 2) return null
        val steps: Int
        val distance: Int?
        val calories: Int?
        if (data.size >= 13) {
            steps = leU16(data, 1)
            distance = leU32(data, 5)
            calories = leU32(data, 9)
        } else if (data.size >= 12) {
            steps = leU32(data, 0)
            distance = leU32(data, 4)
            calories = leU32(data, 8)
        } else {
            steps = leU16(data, 0)
            distance = null
            calories = null
        }
        if (steps < 0 || steps > 150_000) return null
        return WalkData(steps, distance?.takeIf { it >= 0 }, calories?.takeIf { it >= 0 })
    }

    fun parseHeartRate(data: ByteArray): Int? {
        if (data.size < 2) return null
        val flags = data[0].toInt() and 0xFF
        val hr = if (flags and 0x01 != 0) {
            if (data.size < 3) return null
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else {
            data[1].toInt() and 0xFF
        }
        return hr.takeIf { it in 1..253 }
    }

    fun parseBattery(data: ByteArray): Int? {
        if (data.isEmpty()) return null
        val level = if (data.size >= 2) data[1].toInt() and 0xFF else data[0].toInt() and 0xFF
        return level.takeIf { it in 0..100 }
    }

    private fun leU16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun leU32(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
}
