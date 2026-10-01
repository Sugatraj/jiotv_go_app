package com.skylake.skytv.jgorunner.core.execution

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import com.skylake.skytv.jgorunner.data.SkySharedPref
import com.skylake.skytv.jgorunner.services.BinaryService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

suspend fun runBinary(
    activity: ComponentActivity,
    arguments: Array<String>,
    onOutput: (String) -> Unit,
    forceStart: Boolean = false
): Boolean {
    if (BinaryService.isRunning && !forceStart) {
        BinaryService.instance?.binaryOutput?.observe(activity) { onOutput(it) }
        return true
    }

    val preferenceManager = SkySharedPref.getInstance(activity)
    val intent = Intent(activity, BinaryService::class.java).apply {
        putExtra(
            "binaryFileLocation",
            preferenceManager.myPrefs.jtvGoBinaryName?.let {
                File(activity.filesDir, it).absolutePath
            })
        putExtra("arguments", arguments)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activity.startForegroundService(intent)
    } else {
        activity.startService(intent)
    }

    val started = withTimeoutOrNull(10_000L) {
        while (!BinaryService.isRunning) delay(100)
        true
    } ?: false

    if (started) {
        BinaryService.instance?.binaryOutput?.observe(activity) { onOutput(it) }
    }
    return started
}

fun stopBinary(
    context: Context,
    onBinaryStopped: () -> Unit
) {
    val intent = Intent(context, BinaryService::class.java).apply {
        action = BinaryService.ACTION_STOP_BINARY
    }

    context.startService(intent)
    onBinaryStopped()
}
