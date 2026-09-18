package com.boxproxy.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.boxproxy.app.model.AppSettings
import com.boxproxy.app.model.ProxyMethod
import com.boxproxy.app.model.ProxyMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "box_proxy_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val SELECTED_CORE = stringPreferencesKey("selected_core")
        val SELECTED_CONFIG = stringPreferencesKey("selected_config")
        val START_COMMAND = stringPreferencesKey("start_command")
        val PROXY_METHOD = stringPreferencesKey("proxy_method")
        val PROXY_MODE = stringPreferencesKey("proxy_mode")
        val USER_PACKAGES = stringPreferencesKey("user_packages")
        val GID_LIST = stringPreferencesKey("gid_list")
        val AP_LIST = stringPreferencesKey("ap_list")
        val IGNORE_OUT_LIST = stringPreferencesKey("ignore_out_list")
        val IPV6_ENABLE = booleanPreferencesKey("ipv6_enable")
        val REDIR_PORT = intPreferencesKey("redir_port")
        val TPROXY_PORT = intPreferencesKey("tproxy_port")
        val DNS_PORT = intPreferencesKey("dns_port")
        val BOX_USER_GROUP = stringPreferencesKey("box_user_group")
        val INTRANET = stringPreferencesKey("intranet")
        val INTRANET6 = stringPreferencesKey("intranet6")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val WORK_DIR = stringPreferencesKey("work_dir")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs.toAppSettings()
    }

    suspend fun getSettings(): AppSettings {
        return context.dataStore.data.first().toAppSettings()
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_CORE] = settings.selectedCore
            prefs[Keys.SELECTED_CONFIG] = settings.selectedConfig
            prefs[Keys.START_COMMAND] = settings.startCommand
            prefs[Keys.PROXY_METHOD] = settings.proxyMethod.name
            prefs[Keys.PROXY_MODE] = settings.proxyMode.name
            prefs[Keys.USER_PACKAGES] = gson.toJson(settings.userPackages)
            prefs[Keys.GID_LIST] = gson.toJson(settings.gidList)
            prefs[Keys.AP_LIST] = gson.toJson(settings.apList)
            prefs[Keys.IGNORE_OUT_LIST] = gson.toJson(settings.ignoreOutList)
            prefs[Keys.IPV6_ENABLE] = settings.ipv6Enable
            prefs[Keys.REDIR_PORT] = settings.redirPort
            prefs[Keys.TPROXY_PORT] = settings.tproxyPort
            prefs[Keys.DNS_PORT] = settings.dnsPort
            prefs[Keys.BOX_USER_GROUP] = settings.boxUserGroup
            prefs[Keys.INTRANET] = gson.toJson(settings.intranet)
            prefs[Keys.INTRANET6] = gson.toJson(settings.intranet6)
            prefs[Keys.AUTO_START] = settings.autoStart
            prefs[Keys.WORK_DIR] = settings.workDir
        }
    }

    suspend fun updateSelectedCore(name: String) {
        context.dataStore.edit { it[Keys.SELECTED_CORE] = name }
    }

    suspend fun updateSelectedConfig(name: String) {
        context.dataStore.edit { it[Keys.SELECTED_CONFIG] = name }
    }

    suspend fun updateStartCommand(cmd: String) {
        context.dataStore.edit { it[Keys.START_COMMAND] = cmd }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val default = AppSettings()
        return AppSettings(
            selectedCore = this[Keys.SELECTED_CORE] ?: default.selectedCore,
            selectedConfig = this[Keys.SELECTED_CONFIG] ?: default.selectedConfig,
            startCommand = this[Keys.START_COMMAND] ?: default.startCommand,
            proxyMethod = try {
                ProxyMethod.valueOf(this[Keys.PROXY_METHOD] ?: default.proxyMethod.name)
            } catch (e: Exception) {
                default.proxyMethod
            },
            proxyMode = try {
                ProxyMode.valueOf(this[Keys.PROXY_MODE] ?: default.proxyMode.name)
            } catch (e: Exception) {
                default.proxyMode
            },
            userPackages = parseStringList(this[Keys.USER_PACKAGES], default.userPackages),
            gidList = parseIntList(this[Keys.GID_LIST], default.gidList),
            apList = parseStringList(this[Keys.AP_LIST], default.apList),
            ignoreOutList = parseStringList(this[Keys.IGNORE_OUT_LIST], default.ignoreOutList),
            ipv6Enable = this[Keys.IPV6_ENABLE] ?: default.ipv6Enable,
            redirPort = this[Keys.REDIR_PORT] ?: default.redirPort,
            tproxyPort = this[Keys.TPROXY_PORT] ?: default.tproxyPort,
            dnsPort = this[Keys.DNS_PORT] ?: default.dnsPort,
            boxUserGroup = this[Keys.BOX_USER_GROUP] ?: default.boxUserGroup,
            intranet = parseStringList(this[Keys.INTRANET], default.intranet),
            intranet6 = parseStringList(this[Keys.INTRANET6], default.intranet6),
            autoStart = this[Keys.AUTO_START] ?: default.autoStart,
            workDir = this[Keys.WORK_DIR] ?: default.workDir
        )
    }

    private fun parseStringList(json: String?, default: MutableList<String>): MutableList<String> {
        if (json.isNullOrBlank()) return default.toMutableList()
        return try {
            val type = object : TypeToken<MutableList<String>>() {}.type
            gson.fromJson(json, type) ?: default.toMutableList()
        } catch (e: Exception) {
            default.toMutableList()
        }
    }

    private fun parseIntList(json: String?, default: MutableList<Int>): MutableList<Int> {
        if (json.isNullOrBlank()) return default.toMutableList()
        return try {
            val type = object : TypeToken<MutableList<Int>>() {}.type
            gson.fromJson(json, type) ?: default.toMutableList()
        } catch (e: Exception) {
            default.toMutableList()
        }
    }
}
