package com.boxproxy.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.boxproxy.app.manager.ProxyController
import com.boxproxy.app.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("BootReceiver", "Boot completed, checking autoStart")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = SettingsRepository(context)
                    val settings = repo.getSettings()
                    if (settings.autoStart) {
                        val controller = ProxyController(context)
                        controller.start()
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Auto start failed", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
