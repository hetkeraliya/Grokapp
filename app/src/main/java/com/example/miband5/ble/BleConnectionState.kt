package com.example.miband5.ble

/** Connection state surfaced to the UI. */
sealed class BleConnectionState {
    object Idle : BleConnectionState()
    object Scanning : BleConnectionState()
    data class Found(val name: String, val address: String) : BleConnectionState()
    object Connecting : BleConnectionState()
    object DiscoveringServices : BleConnectionState()
    object Authenticating : BleConnectionState()
    object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
    data class Disconnected(val reason: String) : BleConnectionState()
}
