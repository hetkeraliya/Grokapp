package com.example.miband5.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.miband5.ble.BleConnection
import com.example.miband5.ble.BleConnectionState
import com.example.miband5.data.AuthKeyStore
import com.example.miband5.service.BleSyncService
import com.example.miband5.sync.PeriodicSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionViewModel(app: Application) : AndroidViewModel(app) {

    private val authStore = AuthKeyStore(app)
    private var ble: BleConnection? = null

    private val _state = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val state: StateFlow<BleConnectionState> = _state.asStateFlow()

    val savedKeyHex: String get() = authStore.authKeyHex.orEmpty()
    val savedDeviceName: String get() = authStore.lastDeviceName.orEmpty()

    fun saveKey(raw: String): Boolean {
        if (!authStore.isValidKey(raw)) return false
        authStore.authKeyHex = raw
        return true
    }

    fun startScan() {
        val key = authStore.authKey
        if (key == null) {
            _state.value = BleConnectionState.Error("Auth key missing — paste it first")
            return
        }
        ble?.disconnect()
        ble = BleConnection(getApplication(), key) { s ->
            _state.value = s
            if (s is BleConnectionState.Found) {
                authStore.lastDeviceAddress = s.address
                authStore.lastDeviceName = s.name
                startSyncService(s.address)
                PeriodicSyncWorker.schedule(getApplication())
            }
        }
        ble?.startScan()
    }

    fun reconnectSaved() {
        val address = authStore.lastDeviceAddress ?: return startScan()
        val key = authStore.authKey
        if (key == null) {
            _state.value = BleConnectionState.Error("Auth key missing — paste it first")
            return
        }
        startSyncService(address)
    }

    fun disconnect() {
        getApplication<Application>().stopService(
            Intent(getApplication(), BleSyncService::class.java)
        )
        ble?.disconnect()
        _state.value = BleConnectionState.Idle
    }

    private fun startSyncService(address: String) {
        val intent = Intent(getApplication(), BleSyncService::class.java).apply {
            putExtra(BleSyncService.EXTRA_ADDRESS, address)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }
}
