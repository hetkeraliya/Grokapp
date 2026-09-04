package com.example.miband5.ble

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Mi Band auth handshake (same sequence as the web app):
 *
 *  1. { 0x01, FLAG, key[16] }     → { 0x10, 0x01, 0x01 }
 *  2. { 0x02, FLAG }              → { 0x10, 0x02, 0x01, random[16] }
 *  3. { 0x03, FLAG, AES-ECB(key, random) } → { 0x10, 0x03, 0x01 }
 *
 * FLAG 0x00 is what the web app uses on Band 5. FLAG 0x08 is older firmware.
 */
object MiBand5Auth {

    private const val CMD_SEND_KEY = 0x01.toByte()
    private const val CMD_REQUEST_RANDOM = 0x02.toByte()
    private const val CMD_SEND_ENCRYPTED_RANDOM = 0x03.toByte()

    const val FLAG_BAND5 = 0x00.toByte()
    const val FLAG_LEGACY = 0x08.toByte()

    private const val RESP_OK = 0x01.toByte()

    fun sendKeyFrame(authKey: ByteArray, flag: Byte = FLAG_BAND5): ByteArray {
        require(authKey.size == 16) { "Auth key must be exactly 16 bytes" }
        return byteArrayOf(CMD_SEND_KEY, flag) + authKey
    }

    fun requestRandomFrame(flag: Byte = FLAG_BAND5): ByteArray =
        byteArrayOf(CMD_REQUEST_RANDOM, flag)

    fun sendEncryptedRandomFrame(
        authKey: ByteArray,
        random: ByteArray,
        flag: Byte = FLAG_BAND5
    ): ByteArray {
        val encrypted = aesEncryptEcbNoPadding(authKey, random)
        return byteArrayOf(CMD_SEND_ENCRYPTED_RANDOM, flag) + encrypted
    }

    fun parseAuthResponse(data: ByteArray): AuthResponse {
        if (data.size < 3 || data[0] != 0x10.toByte()) return AuthResponse.Unknown
        return when (data[1]) {
            CMD_SEND_KEY ->
                if (data[2] == RESP_OK) AuthResponse.KeyAccepted else AuthResponse.KeyRejected
            CMD_REQUEST_RANDOM -> {
                if (data.size >= 19 && data[2] == RESP_OK) {
                    AuthResponse.RandomReceived(data.copyOfRange(3, 19))
                } else {
                    AuthResponse.Unknown
                }
            }
            CMD_SEND_ENCRYPTED_RANDOM ->
                if (data[2] == RESP_OK) AuthResponse.Authenticated else AuthResponse.AuthFailed
            else -> AuthResponse.Unknown
        }
    }

    private fun aesEncryptEcbNoPadding(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    sealed class AuthResponse {
        object KeyAccepted : AuthResponse()
        object KeyRejected : AuthResponse()
        data class RandomReceived(val random: ByteArray) : AuthResponse()
        object Authenticated : AuthResponse()
        object AuthFailed : AuthResponse()
        object Unknown : AuthResponse()
    }
}
