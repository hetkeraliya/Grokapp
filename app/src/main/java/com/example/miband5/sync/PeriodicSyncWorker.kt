package com.example.miband5.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.miband5.data.AuthKeyStore
import com.example.miband5.service.BleSyncService
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync (WorkManager). Starts the foreground BLE sync
 * service, which keeps the process alive while connecting + syncing.
 */
class PeriodicSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = AuthKeyStore(applicationContext)
        val device = store.lastDeviceAddress
        val key = store.authKey
        if (device == null || key == null) {
            Log.d(TAG, "No saved device/key — skipping background sync")
            return Result.success()
        }
        val intent = Intent(applicationContext, BleSyncService::class.java).apply {
            putExtra(BleSyncService.EXTRA_ADDRESS, device)
        }
        ContextCompat.startForegroundService(applicationContext, intent)
        return Result.success()
    }

    companion object {
        private const val TAG = "PeriodicSyncWorker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "miband5-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
