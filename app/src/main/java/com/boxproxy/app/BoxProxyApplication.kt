package com.boxproxy.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.boxproxy.app.util.Constants
import com.topjohnwu.superuser.Shell

class BoxProxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
        )
        createNotificationChannel()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Box Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Proxy core running status"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    companion object {
        lateinit var instance: BoxProxyApplication
            private set
    }
}
