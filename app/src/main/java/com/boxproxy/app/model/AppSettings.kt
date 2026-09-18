package com.boxproxy.app.model

import com.boxproxy.app.util.Constants

data class AppSettings(
    var selectedCore: String = "",
    var selectedConfig: String = "",
    var startCommand: String = "",
    var proxyMethod: ProxyMethod = ProxyMethod.TPROXY,
    var proxyMode: ProxyMode = ProxyMode.BLACKLIST,
    var userPackages: MutableList<String> = mutableListOf(),
    var gidList: MutableList<Int> = mutableListOf(),
    var apList: MutableList<String> = mutableListOf("wlan+", "ap+", "rndis+", "ncm+"),
    var ignoreOutList: MutableList<String> = mutableListOf(),
    var ipv6Enable: Boolean = false,
    var redirPort: Int = Constants.DEFAULT_REDIR_PORT,
    var tproxyPort: Int = Constants.DEFAULT_TPROXY_PORT,
    var dnsPort: Int = Constants.DEFAULT_DNS_PORT,
    var boxUserGroup: String = Constants.DEFAULT_USER_GROUP,
    var intranet: MutableList<String> = mutableListOf(
        "0.0.0.0/8", "10.0.0.0/8", "100.0.0.0/8", "127.0.0.0/8",
        "169.254.0.0/16", "192.0.0.0/24", "192.0.2.0/24", "192.88.99.0/24",
        "192.168.0.0/16", "198.51.100.0/24", "203.0.113.0/24",
        "224.0.0.0/4", "240.0.0.0/4", "255.255.255.255/32"
    ),
    var intranet6: MutableList<String> = mutableListOf(
        "::/128", "::1/128", "::ffff:0:0/96", "100::/64",
        "64:ff9b::/96", "2001::/32", "2001:10::/28", "2001:20::/28",
        "2001:db8::/32", "2002::/16", "fe80::/10", "ff00::/8"
    ),
    var autoStart: Boolean = false,
    var workDir: String = Constants.WORK_DIR
)

enum class ProxyMethod {
    TPROXY,
    REDIRECT,
    MIXED
}

enum class ProxyMode {
    BLACKLIST,
    WHITELIST,
    CORE
}

data class CoreInfo(
    val fileName: String,
    val size: Long,
    val lastModified: Long,
    val path: String
)

data class ConfigInfo(
    val fileName: String,
    val size: Long,
    val lastModified: Long,
    val path: String
)

enum class ProxyState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

data class RuntimeStatus(
    val state: ProxyState = ProxyState.STOPPED,
    val pid: Int = -1,
    val message: String = "",
    val startTime: Long = 0L
)
