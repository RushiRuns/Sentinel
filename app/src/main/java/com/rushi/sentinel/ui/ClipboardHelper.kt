package com.rushi.sentinel.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Handler

class ClipboardHelper(
    private val clipboardManager: ClipboardManager,
    private val handler: Handler
) {
    private var clearRunnable: Runnable? = null

    fun copyToClipboard(value: String) {
        val clip = ClipData.newPlainText("Sentinel Password", value)
        clipboardManager.setPrimaryClip(clip)

        clearRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            val primaryClip = clipboardManager.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val text = primaryClip.getItemAt(0).text
                if (text == value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboardManager.clearPrimaryClip()
                    } else {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        }
        clearRunnable = runnable
        handler.postDelayed(runnable, 30000L) // 30 seconds
    }

    fun onDestroy() {
        clearRunnable?.let { handler.removeCallbacks(it) }
    }
}
