package com.example.miband5.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.miband5.R
import com.example.miband5.ble.BleConnection
import com.example.miband5.ble.BleConnectionState
import com.example.miband5.data.AuthKeyStore
import com.example.miband5.sync.SyncCoordinator

/**
 * Foreground service that keeps the process alive while a background BLE
 * session connects, authenticates and syncs the band. WorkManager's
 * PeriodicSyncWorker starts this; the app also starts it after a foreground scan.
 */
class BleSyncService : Service() {

    private var ble: BleConnection? = null
    private var sync: SyncCoordinator? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address = intent?.getStringExtra(EXTRA_ADDRESS)
        val key = AuthKeyStore(this).authKey
        if (address == null || key == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification("Connecting to $address…"))

        ble?.disconnect()
        ble = BleConnection(this, key) { state -> handleState(state) }
        ble?.onAuthenticated = { startSync() }
        ble?.connectTo(address)
        return START_STICKY
    }

    private fun handleState(state: BleConnectionState) {
        when (state) {
            is BleConnectionState.Error -> {
                updateNotification("Sync error: ${state.message}")
                stopSelf()
            }
            is BleConnectionState.Disconnected -> {
                updateNotification("Disconnected — retrying…")
            }
            is BleConnectionState.Connected -> {
                updateNotification("Connected — syncing…")
            }
            else -> updateNotification(statusText(state))
        }
    }

    private fun startSync() {
        val b = ble ?: return
        sync?.stop()
        sync = SyncCoordinator(this, b) { msg -> updateNotification(msg) }
        sync?.start()
    }

    private fun statusText(state: BleConnectionState): String = when (state) {
        is BleConnectionState.Scanning -> "Scanning…"
        is BleConnectionState.Connecting -> "Connecting…"
        is BleConnectionState.DiscoveringServices -> "Discovering services…"
        is BleConnectionState.Authenticating -> "Authenticating…"
        is BleConnectionState.Found -> "Found ${state.name}"
        else -> "Mi Band 5 Companion"
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE sync",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Band Stats")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        sync?.stop()
        ble?.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_ADDRESS = "extra_address"
        private const val CHANNEL_ID = "ble_sync"
        private const val NOTIF_ID = 1
        private const val TAG = "BleSyncService"
    }
}
