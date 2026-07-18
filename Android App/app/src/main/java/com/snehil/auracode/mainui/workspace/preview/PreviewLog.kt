package com.snehil.auracode.mainui.workspace.preview

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared preview debug log — visible on screen and in Logcat (`adb logcat -s AuraPreview`).
 */
object PreviewLog {
    private const val TAG = "AuraPreview"
    private const val MAX = 80

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun clear() {
        _lines.value = emptyList()
        Log.i(TAG, "---- log cleared ----")
    }

    fun i(msg: String) = append("I", msg, Log.INFO)
    fun w(msg: String) = append("W", msg, Log.WARN)
    fun e(msg: String, t: Throwable? = null) {
        append("E", if (t != null) "$msg | ${t.message}" else msg, Log.ERROR)
        if (t != null) Log.e(TAG, msg, t)
    }

    private fun append(level: String, msg: String, priority: Int) {
        val line = "${timeFmt.format(Date())} $level $msg"
        when (priority) {
            Log.ERROR -> Log.e(TAG, msg)
            Log.WARN -> Log.w(TAG, msg)
            else -> Log.i(TAG, msg)
        }
        _lines.update { (it + line).takeLast(MAX) }
    }
}
