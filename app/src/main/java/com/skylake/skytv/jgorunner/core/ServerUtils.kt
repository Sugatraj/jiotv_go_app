package com.skylake.skytv.jgorunner.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min
import kotlin.math.pow

enum class LocalServerProbeStatus {
    READY,
    AUTH_REQUIRED,
    UNREACHABLE
}

suspend fun checkServerStatus(
    port: Int = 5350,
    baseDelay: Long = 150L,
    maxDelay: Long = 2000L,
    maxAttempts: Int = 12
): LocalServerProbeStatus = withContext(Dispatchers.IO) {
    val url = URL("http://localhost:$port/live/143.m3u8")

    fun calculateDelay(attempt: Int): Long =
        min(baseDelay * (1.5.pow(attempt.toDouble())).toLong(), maxDelay)

    for (attempt in 0 until maxAttempts) {
        val responseCode = try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5000
                readTimeout = 5000
            }
            try {
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            Log.w("ServerLoginCheck", "Attempt #${attempt + 1}: local readiness probe failed", e)
            null
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.d("ServerLoginCheck", "Local endpoint is ready")
            return@withContext LocalServerProbeStatus.READY
        }
        Log.w("ServerLoginCheck", "Attempt #${attempt + 1}: readiness response=$responseCode")
        if (attempt < maxAttempts - 1) delay(calculateDelay(attempt))
    }

    LocalServerProbeStatus.UNREACHABLE
}
