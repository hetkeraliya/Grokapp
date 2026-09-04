package com.example.miband5.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.miband5.data.AppDatabase
import com.example.miband5.data.entity.DailyStats
import com.example.miband5.data.entity.HrSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipFile

/**
 * One-time migration path: imports a Gadgetbridge SQLite export
 * (or its backup .zip) into Room.
 *
 * Gadgetbridge schema (documented in its source / wiki):
 *   DEVICE_ACTIVITY_SAMPLE(_id, TIMESTAMP, DEVICE_ID, USER_ID,
 *                          RAW_KIND, STEPS, RAW_INTENSITY, HEART_RATE)
 * RAW_KIND (ActivityKind): 1=activity, 2=light sleep, 3=deep sleep, 4=not worn.
 * Sleep-stage data from Huami bands is a best-effort heuristic — stored as-is.
 */
class GadgetbridgeImporter(private val context: Context) {

    suspend fun import(sourcePath: String): ImportResult = withContext(Dispatchers.IO) {
        val dbPath = extractIfZip(sourcePath)
        val db = AppDatabase.getInstance(context)
        val dest = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val samples = mutableListOf<HrSample>()
            val daily = mutableMapOf<String, DailyStats>()
            var rows = 0

            dest.rawQuery(
                "SELECT TIMESTAMP, RAW_KIND, STEPS, RAW_INTENSITY, HEART_RATE " +
                    "FROM DEVICE_ACTIVITY_SAMPLE ORDER BY TIMESTAMP ASC",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    rows++
                    val ts = c.getLong(0)
                    val kind = c.getInt(1)
                    val steps = c.getInt(2)
                    val intensity = c.getInt(3)
                    val hr = c.getInt(4)

                    if (hr > 0) {
                        samples.add(HrSample(timestamp = ts, heartRate = hr, rawIntensity = intensity))
                    }

                    val date = Instant.ofEpochSecond(ts)
                        .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    val stats = daily.getOrPut(date) { DailyStats(date = date) }
                    daily[date] = when (kind) {
                        2 -> stats.copy(
                            sleepMinutes = stats.sleepMinutes + 1,
                            lightSleepMinutes = stats.lightSleepMinutes + 1
                        )
                        3 -> stats.copy(
                            sleepMinutes = stats.sleepMinutes + 1,
                            deepSleepMinutes = stats.deepSleepMinutes + 1
                        )
                        else -> stats.copy(steps = stats.steps + steps)
                    }
                }
            }

            db.hrSampleDao().insertAll(samples)
            daily.values.forEach { db.dailyStatsDao().upsert(it) }

            ImportResult(rowsRead = rows, hrSamples = samples.size, days = daily.size)
        } finally {
            dest.close()
        }
    }

    /** If the source is a Gadgetbridge backup .zip, extract the .db inside. */
    private fun extractIfZip(path: String): String {
        if (!path.endsWith(".zip", ignoreCase = true)) return path
        val dir = File(context.cacheDir, "gb_import").apply {
            deleteRecursively()
            mkdirs()
        }
        ZipFile(path).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.isDirectory && entry.name.endsWith(".db", ignoreCase = true)) {
                    val out = File(dir, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
                    return out.absolutePath
                }
            }
        }
        return path
    }

    data class ImportResult(val rowsRead: Int, val hrSamples: Int, val days: Int)
}
