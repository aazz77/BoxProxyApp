package com.boxproxy.app.manager

import android.content.Context
import com.boxproxy.app.model.AppSettings
import com.boxproxy.app.model.RuntimeStatus
import com.boxproxy.app.repository.SettingsRepository
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class ProxyController(private val context: Context) {

    private val settingsRepo = SettingsRepository(context)
    private val coreManager = CoreManager(context)
    private val processManager = ProcessManager(coreManager)
    private val iptablesManager = IptablesManager(context)

    private val networkMonitor = NetworkMonitor(context) {
        if (processManager.isRunning()) {
            renewProxyRules()
        }
    }

    val status: StateFlow<RuntimeStatus> = processManager.status
    val coreManagerPublic get() = coreManager
    val iptablesManagerPublic get() = iptablesManager
    val settingsRepository get() = settingsRepo

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (Shell.isAppGrantedRoot() != true) {
            return@withContext Result.failure(Exception("Need Root"))
        }
        val settings = settingsRepo.getSettings()
        val startResult = processManager.start(settings)
        if (startResult.isFailure) return@withContext startResult
        val proxyResult = iptablesManager.enable(settings)
        if (proxyResult.isFailure) {
            processManager.stop()
            return@withContext Result.failure(
                Exception("Proxy rules failed: ${proxyResult.exceptionOrNull()?.message}")
            )
        }
        networkMonitor.start()
        Result.success(Unit)
    }

    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        networkMonitor.stop()
        iptablesManager.disable()
        processManager.stop()
    }

    suspend fun emergencyStop(): Result<Unit> = withContext(Dispatchers.IO) {
        networkMonitor.stop()
        iptablesManager.disable()
        processManager.emergencyStop()
    }

    suspend fun restart(): Result<Unit> {
        stop()
        kotlinx.coroutines.delay(500)
        return start()
    }

    suspend fun renewProxyRules(): Result<Unit> {
        val settings = settingsRepo.getSettings()
        return iptablesManager.renew(settings)
    }

    fun refreshStatus() { processManager.refreshStatus() }
    fun isRunning(): Boolean = processManager.isRunning()
    fun getRunLog(maxLines: Int = 300): String = processManager.readRunLog(maxLines)
    fun getErrorLog(maxLines: Int = 100): String = processManager.readErrorLog(maxLines)
    suspend fun getSettings(): AppSettings = settingsRepo.getSettings()
    suspend fun saveSettings(settings: AppSettings) { settingsRepo.updateSettings(settings) }
}
