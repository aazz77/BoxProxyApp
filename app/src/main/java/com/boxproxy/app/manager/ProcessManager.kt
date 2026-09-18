package com.boxproxy.app.manager

import com.boxproxy.app.model.AppSettings
import com.boxproxy.app.model.ProxyState
import com.boxproxy.app.model.RuntimeStatus
import com.boxproxy.app.util.Constants
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ProcessManager(
    private val coreManager: CoreManager
) {
    private val _status = MutableStateFlow(RuntimeStatus())
    val status: StateFlow<RuntimeStatus> = _status.asStateFlow()

    suspend fun start(settings: AppSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _status.value = RuntimeStatus(state = ProxyState.STARTING, message = "Preparing...")
            if (settings.selectedCore.isBlank()) {
                return@withContext Result.failure(Exception("Select core first"))
            }
            if (settings.selectedConfig.isBlank()) {
                return@withContext Result.failure(Exception("Select config first"))
            }
            if (settings.startCommand.isBlank()) {
                return@withContext Result.failure(Exception("Set start command"))
            }
            val prepareResult = coreManager.prepareWorkDir(
                settings.selectedCore,
                settings.selectedConfig
            )
            if (!prepareResult.isSuccess) {
                val err = prepareResult.err.joinToString("\n")
                _status.value = RuntimeStatus(state = ProxyState.ERROR, message = "Prepare failed: $err")
                return@withContext Result.failure(Exception("Prepare failed: $err"))
            }
            val workDir = Constants.WORK_DIR
            val logFile = "$workDir/${Constants.RUN_LOG}"
            val errFile = "$workDir/${Constants.ERROR_LOG}"
            val pidFile = "$workDir/${Constants.PID_FILE}"
            Shell.cmd("rm -f $logFile $errFile $pidFile").exec()
            val fullCmd = """
                cd $workDir
                nohup ${settings.startCommand} > $logFile 2> $errFile &
                echo ${'$'}! > $pidFile
            """.trimIndent()
            Shell.cmd(fullCmd).exec()
            Thread.sleep(800)
            val pid = readPid()
            if (pid <= 0 || !isProcessAlive(pid)) {
                val errLog = readErrorLog()
                _status.value = RuntimeStatus(
                    state = ProxyState.ERROR,
                    message = "Start failed\n$errLog"
                )
                return@withContext Result.failure(Exception("Process start failed"))
            }
            _status.value = RuntimeStatus(
                state = ProxyState.RUNNING,
                pid = pid,
                message = "Running (PID: $pid)",
                startTime = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = RuntimeStatus(state = ProxyState.ERROR, message = e.message ?: "Unknown")
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _status.value = RuntimeStatus(state = ProxyState.STOPPING, message = "Stopping...")
            val pid = readPid()
            if (pid > 0) {
                Shell.cmd("kill $pid").exec()
                Thread.sleep(500)
                if (isProcessAlive(pid)) {
                    Shell.cmd("kill -9 $pid").exec()
                }
            }
            Shell.cmd("rm -f ${Constants.WORK_DIR}/${Constants.PID_FILE}").exec()
            _status.value = RuntimeStatus(state = ProxyState.STOPPED, message = "Stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = RuntimeStatus(state = ProxyState.ERROR, message = e.message ?: "Stop failed")
            Result.failure(e)
        }
    }

    suspend fun emergencyStop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pid = readPid()
            if (pid > 0) {
                Shell.cmd("kill -9 $pid").exec()
            }
            Shell.cmd("pkill -9 -f ${Constants.WORK_DIR}").exec()
            Shell.cmd("rm -f ${Constants.WORK_DIR}/${Constants.PID_FILE}").exec()
            _status.value = RuntimeStatus(state = ProxyState.STOPPED, message = "Emergency stop done")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restart(settings: AppSettings): Result<Unit> {
        stop()
        Thread.sleep(300)
        return start(settings)
    }

    fun refreshStatus() {
        val pid = readPid()
        if (pid > 0 && isProcessAlive(pid)) {
            if (_status.value.state != ProxyState.RUNNING) {
                _status.value = RuntimeStatus(
                    state = ProxyState.RUNNING,
                    pid = pid,
                    message = "Running (PID: $pid)",
                    startTime = _status.value.startTime
                )
            }
        } else {
            if (_status.value.state == ProxyState.RUNNING) {
                _status.value = RuntimeStatus(state = ProxyState.STOPPED, message = "Process exited")
            }
        }
    }

    private fun readPid(): Int {
        return try {
            val result = Shell.cmd("cat ${Constants.WORK_DIR}/${Constants.PID_FILE} 2>/dev/null").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.first().trim().toIntOrNull() ?: -1
            } else -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun isProcessAlive(pid: Int): Boolean {
        if (pid <= 0) return false
        val result = Shell.cmd("kill -0 $pid 2>/dev/null").exec()
        return result.isSuccess
    }

    fun readRunLog(maxLines: Int = 200): String {
        return try {
            val result = Shell.cmd("tail -n $maxLines ${Constants.WORK_DIR}/${Constants.RUN_LOG} 2>/dev/null").exec()
            result.out.joinToString("\n")
        } catch (e: Exception) {
            ""
        }
    }

    fun readErrorLog(maxLines: Int = 100): String {
        return try {
            val result = Shell.cmd("tail -n $maxLines ${Constants.WORK_DIR}/${Constants.ERROR_LOG} 2>/dev/null").exec()
            result.out.joinToString("\n")
        } catch (e: Exception) {
            ""
        }
    }

    fun isRunning(): Boolean {
        val pid = readPid()
        return pid > 0 && isProcessAlive(pid)
    }
}
