package com.boxproxy.app.manager

import android.content.Context
import android.net.Uri
import com.boxproxy.app.model.ConfigInfo
import com.boxproxy.app.model.CoreInfo
import com.boxproxy.app.util.Constants
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoreManager(private val context: Context) {

    private val coresDir: File
        get() = File(context.filesDir, Constants.CORES_DIR).also { it.mkdirs() }

    private val configsDir: File
        get() = File(context.filesDir, Constants.CONFIGS_DIR).also { it.mkdirs() }

    private val runDir: File
        get() = File(context.filesDir, Constants.RUN_DIR).also { it.mkdirs() }

    fun listCores(): List<CoreInfo> {
        return coresDir.listFiles()
            ?.filter { it.isFile }
            ?.map {
                CoreInfo(
                    fileName = it.name,
                    size = it.length(),
                    lastModified = it.lastModified(),
                    path = it.absolutePath
                )
            }
            ?.sortedBy { it.fileName }
            ?: emptyList()
    }

    suspend fun importCore(uri: Uri, fileName: String): Result<CoreInfo> = withContext(Dispatchers.IO) {
        try {
            val target = File(coresDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open file"))

            Result.success(
                CoreInfo(
                    fileName = target.name,
                    size = target.length(),
                    lastModified = target.lastModified(),
                    path = target.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteCore(fileName: String): Boolean {
        val file = File(coresDir, fileName)
        return file.exists() && file.delete()
    }

    fun getCoreFile(fileName: String): File? {
        val file = File(coresDir, fileName)
        return if (file.exists()) file else null
    }

    fun listConfigs(): List<ConfigInfo> {
        return configsDir.listFiles()
            ?.filter { it.isFile }
            ?.map {
                ConfigInfo(
                    fileName = it.name,
                    size = it.length(),
                    lastModified = it.lastModified(),
                    path = it.absolutePath
                )
            }
            ?.sortedBy { it.fileName }
            ?: emptyList()
    }

    suspend fun importConfig(uri: Uri, fileName: String): Result<ConfigInfo> = withContext(Dispatchers.IO) {
        try {
            val target = File(configsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open file"))

            Result.success(
                ConfigInfo(
                    fileName = target.name,
                    size = target.length(),
                    lastModified = target.lastModified(),
                    path = target.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteConfig(fileName: String): Boolean {
        val file = File(configsDir, fileName)
        return file.exists() && file.delete()
    }

    fun getConfigFile(fileName: String): File? {
        val file = File(configsDir, fileName)
        return if (file.exists()) file else null
    }

    fun readConfigContent(fileName: String): String? {
        val file = File(configsDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    fun prepareWorkDir(coreName: String, configName: String): Shell.Result {
        val coreSrc = File(coresDir, coreName).absolutePath
        val configSrc = File(configsDir, configName).absolutePath
        val workDir = Constants.WORK_DIR
        val commands = listOf(
            "rm -rf $workDir",
            "mkdir -p $workDir",
            "cp '$coreSrc' '$workDir/$coreName'",
            "cp '$configSrc' '$workDir/$configName'",
            "chmod 755 '$workDir/$coreName'",
            "chmod 644 '$workDir/$configName'"
        )
        return Shell.cmd(*commands.toTypedArray()).exec()
    }

    fun cleanWorkDir(): Shell.Result {
        return Shell.cmd("rm -rf ${Constants.WORK_DIR}").exec()
    }

    private fun isProbablyElf(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4) return false
                header[0] == 0x7F.toByte() &&
                        header[1] == 'E'.code.toByte() &&
                        header[2] == 'L'.code.toByte() &&
                        header[3] == 'F'.code.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getRunDir(): File = runDir
}
