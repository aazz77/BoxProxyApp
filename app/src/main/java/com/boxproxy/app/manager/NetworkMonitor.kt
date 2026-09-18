package com.boxproxy.app.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val context: Context,
    private val onNetworkChanged: suspend () -> Unit
) {
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val DEBOUNCE_MS = 1500L
    }

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "Network available: $network")
            scheduleRenew()
        }
        override fun onLost(network: Network) {
            Log.i(TAG, "Network lost: $network")
            scheduleRenew()
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            Log.d(TAG, "Capabilities changed: $network")
            scheduleRenew()
        }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
            Log.d(TAG, "Link properties changed")
            scheduleRenew()
        }
    }

    fun start() {
        if (registered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            registered = true
            Log.i(TAG, "NetworkMonitor started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    fun stop() {
        if (!registered) return
        try {
            cm.unregisterNetworkCallback(callback)
            registered = false
            debounceJob?.cancel()
            Log.i(TAG, "NetworkMonitor stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister", e)
        }
    }

    private fun scheduleRenew() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            try {
                Log.i(TAG, "Triggering proxy rules renew")
                onNetworkChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Renew failed", e)
            }
        }
    }
}
