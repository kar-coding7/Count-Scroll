package com.kiru.countscroll.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kiru.countscroll.CountScrollApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Menjaga konsistensi state setelah perangkat restart (BOOT_COMPLETED)
 * atau setelah pembaruan paket aplikasi (MY_PACKAGE_REPLACED).
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? CountScrollApp
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app?.settingsDataStore?.recordBootTimestamp()
                    app?.settingsDataStore?.ensureTargetAnchorInitialized()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
