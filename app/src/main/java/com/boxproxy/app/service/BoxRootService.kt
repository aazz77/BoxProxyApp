package com.boxproxy.app.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService

class BoxRootService : RootService() {
    companion object {
        private const val TAG = "BoxRootService"
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i(TAG, "BoxRootService onBind")
        return object : android.os.Binder() {}
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i(TAG, "BoxRootService onUnbind")
        return false
    }
}
