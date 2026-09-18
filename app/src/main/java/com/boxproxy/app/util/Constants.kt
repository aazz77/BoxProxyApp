package com.boxproxy.app.util

object Constants {
    const val WORK_DIR = "/data/local/tmp/box"
    const val CORES_DIR = "cores"
    const val CONFIGS_DIR = "configs"
    const val RUN_DIR = "run"
    const val PID_FILE = "pid"
    const val RUN_LOG = "run.log"
    const val ERROR_LOG = "run_error.log"
    const val SETTINGS_FILE = "app_settings.json"
    const val DEFAULT_REDIR_PORT = 7876
    const val DEFAULT_TPROXY_PORT = 7878
    const val DEFAULT_DNS_PORT = 5353
    const val DEFAULT_USER_GROUP = "root:net_admin"
    const val NOTIFICATION_CHANNEL_ID = "box_proxy_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_START = "com.boxproxy.app.ACTION_START"
    const val ACTION_STOP = "com.boxproxy.app.ACTION_STOP"
    const val ACTION_RESTART = "com.boxproxy.app.ACTION_RESTART"
    const val ACTION_EMERGENCY_STOP = "com.boxproxy.app.ACTION_EMERGENCY_STOP"
}
