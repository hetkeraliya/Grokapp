package com.example.miband5.data

import android.content.Context

class AuthKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences("miband5_auth", Context.MODE_PRIVATE)

    var authKey: ByteArray?
        get() = prefs.getString(KEY_AUTH, null)?.hexToBytes()
        set(value) {
            prefs.edit().putString(KEY_AUTH, value?.toHex()).apply()
        }

    var authKeyHex: String?
        get() = prefs.getString(KEY_AUTH, null)
        set(value) {
            val cleaned = value
                ?.trim()
                ?.removePrefix("0x")
                ?.removePrefix("0X")
                ?.replace("\\s+".toRegex(), "")
                ?.lowercase()
            prefs.edit().putString(KEY_AUTH, cleaned).apply()
        }

    var lastDeviceAddress: String?
        get() = prefs.getString(KEY_DEVICE, null)
        set(value) {
            prefs.edit().putString(KEY_DEVICE, value).apply()
        }

    var lastDeviceName: String?
        get() = prefs.getString(KEY_DEVICE_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_DEVICE_NAME, value).apply()
        }

    var stepGoal: Int
        get() = prefs.getInt(KEY_GOAL, 8000)
        set(value) {
            prefs.edit().putInt(KEY_GOAL, value.coerceIn(1000, 50_000)).apply()
        }

    fun isValidKey(raw: String): Boolean {
        val hex = raw.trim().removePrefix("0x").removePrefix("0X").replace("\\s+".toRegex(), "")
        return hex.matches(Regex("[0-9a-fA-F]{32}"))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val KEY_AUTH = "auth_key_hex"
        private const val KEY_DEVICE = "last_device_address"
        private const val KEY_DEVICE_NAME = "last_device_name"
        private const val KEY_GOAL = "step_goal"
    }
}
