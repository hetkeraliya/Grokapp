package com.example.miband5.sync

import android.content.Context
import android.util.Log
import com.example.miband5.ble.BleConnection
import com.example.miband5.ble.MiBand5Commands
import com.example.miband5.ble.MiBand5DataParser
import com.example.miband5.ble.MiBand5Gatt
import com.example.miband5.data.AppDatabase
import com.example.miband5.data.entity.DailyStats
import com.example.miband5.data.entity.HrSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class SyncCoordinator(
    private val context: Context,
    private val ble: BleConnection,
    private val onProgress: (String) -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getInstance(context)
    private val dailyDao = db.dailyStatsDao()
    private val hrDao = db.hrSampleDao()

    private val maxHeartRate = 190

    fun start() {
        ble.notifyListener = { _, data -> onHeartRateSample(data) }
        scope.launch {
            try {
                startHeartRate()
                while (isActive) {
                    syncStepsAndBattery()
                    pruneOldSamples()
                    delay(10_000)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "sync failed", t)
                onProgress("Sync error: ${t.message}")
            }
        }
    }

    private suspend fun syncStepsAndBattery() {
        val walkBytes = ble.readCharacteristicSuspend(MiBand5Gatt.UUID_CHARACTERISTIC_WALK)
        val walk = walkBytes?.let { MiBand5DataParser.parseWalk(it) }

        val huamiBat = ble.readCharacteristicSuspend(MiBand5Gatt.UUID_CHARACTERISTIC_BATTERY)
            ?.let { MiBand5DataParser.parseBattery(it) }
        val stdBat = if (huamiBat == null) {
            ble.readCharacteristicSuspend(
                MiBand5Gatt.UUID_SERVICE_BATTERY,
                MiBand5Gatt.UUID_CHARACTERISTIC_BATTERY_LEVEL
            )?.firstOrNull()?.toInt()?.and(0xFF)
        } else null
        val battery = huamiBat ?: stdBat

        val today = LocalDate.now().toString()
        val existing = dailyDao.getByDate(today) ?: DailyStats(date = today)
        dailyDao.upsert(
            existing.copy(
                steps = walk?.steps ?: existing.steps,
                distanceMeters = walk?.distanceMeters ?: existing.distanceMeters,
                calories = walk?.calories ?: existing.calories,
                batteryLast = battery ?: existing.batteryLast
            )
        )
        onProgress("Steps ${walk?.steps ?: "–"} · Battery ${battery ?: "–"}%")
    }

    private fun startHeartRate() {
        ble.enableNotifications(
            MiBand5Gatt.UUID_SERVICE_HEART_RATE,
            MiBand5Gatt.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT
        )
        ble.writeCharacteristic(
            MiBand5Gatt.UUID_SERVICE_HEART_RATE,
            MiBand5Gatt.UUID_CHARACTERISTIC_HEART_RATE_CONTROL,
            MiBand5Commands.HR_CONTROL_STOP
        )
        scope.launch {
            delay(120)
            ble.writeCharacteristic(
                MiBand5Gatt.UUID_SERVICE_HEART_RATE,
                MiBand5Gatt.UUID_CHARACTERISTIC_HEART_RATE_CONTROL,
                MiBand5Commands.HR_CONTROL_CONTINUOUS
            )
        }
        onProgress("Heart-rate streaming…")
    }

    fun onHeartRateSample(data: ByteArray) {
        val hr = MiBand5DataParser.parseHeartRate(data) ?: return
        scope.launch {
            val now = System.currentTimeMillis() / 1000
            hrDao.insert(HrSample(timestamp = now, heartRate = hr, rawIntensity = 0))

            val today = LocalDate.now().toString()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            val endOfDay = startOfDay + 24 * 3600 - 1
            val samples = hrDao.getRange(startOfDay, endOfDay)
            val avg = if (samples.isNotEmpty()) samples.map { it.heartRate }.average().toInt() else hr

            val existing = dailyDao.getByDate(today) ?: DailyStats(date = today)
            val zone = zoneFor(hr)
            dailyDao.upsert(
                existing.copy(
                    heartRateMax = maxOf(existing.heartRateMax ?: 0, hr),
                    heartRateMin = existing.heartRateMin?.let { minOf(it, hr) } ?: hr,
                    heartRateAvg = avg,
                    hrZoneLowMin = existing.hrZoneLowMin + if (zone == 0) 1 else 0,
                    hrZoneModerateMin = existing.hrZoneModerateMin + if (zone == 1) 1 else 0,
                    hrZoneHighMin = existing.hrZoneHighMin + if (zone == 2) 1 else 0
                )
            )
            onProgress("HR $hr bpm")
        }
    }

    private fun zoneFor(hr: Int): Int = when {
        hr >= (maxHeartRate * 0.7) -> 2
        hr >= (maxHeartRate * 0.5) -> 1
        else -> 0
    }

    private suspend fun pruneOldSamples() {
        val cutoff = System.currentTimeMillis() / 1000 - 90L * 24 * 3600
        hrDao.deleteOlderThan(cutoff)
    }

    fun stop() {
        ble.notifyListener = null
        scope.cancel()
    }

    companion object {
        private const val TAG = "SyncCoordinator"
    }
}
