package com.boxproxy.app.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.boxproxy.app.model.AppSettings
import com.boxproxy.app.model.ProxyMethod
import com.boxproxy.app.model.ProxyMode
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IptablesManager(private val context: Context) {

    companion object {
        private const val TABLE_ID = "2024"
        private const val MARK_ID = "0x1000000/0x1000000"
        private const val CHAIN_BOX = "BOX"
        private const val CHAIN_BOX_LOCAL = "BOX_LOCAL"
        private const val CHAIN_BOX_PRE = "BOX_PRE"
        private const val CHAIN_BOX_WHITELIST = "BOX_WL"
    }

    suspend fun enable(settings: AppSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disableInternal()
            when (settings.proxyMethod) {
                ProxyMethod.TPROXY, ProxyMethod.MIXED -> setupTproxy(settings)
                ProxyMethod.REDIRECT -> setupRedirect(settings)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disable(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disableInternal()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renew(settings: AppSettings): Result<Unit> = enable(settings)

    private fun disableInternal() {
        Shell.cmd(
            "iptables -t mangle -D PREROUTING -j $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t mangle -D OUTPUT -j $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t mangle -F $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t mangle -X $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t mangle -F $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t mangle -X $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t mangle -F $CHAIN_BOX_WHITELIST 2>/dev/null",
            "iptables -t mangle -X $CHAIN_BOX_WHITELIST 2>/dev/null",
            "iptables -t mangle -F $CHAIN_BOX 2>/dev/null",
            "iptables -t mangle -X $CHAIN_BOX 2>/dev/null",
            "iptables -t nat -D PREROUTING -j $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t nat -D OUTPUT -j $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t nat -F $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t nat -X $CHAIN_BOX_PRE 2>/dev/null",
            "iptables -t nat -F $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t nat -X $CHAIN_BOX_LOCAL 2>/dev/null",
            "iptables -t nat -F $CHAIN_BOX_WHITELIST 2>/dev/null",
            "iptables -t nat -X $CHAIN_BOX_WHITELIST 2>/dev/null",
            "iptables -t nat -F $CHAIN_BOX 2>/dev/null",
            "iptables -t nat -X $CHAIN_BOX 2>/dev/null",
            "ip rule del fwmark $MARK_ID lookup $TABLE_ID 2>/dev/null",
            "ip route del local default dev lo table $TABLE_ID 2>/dev/null"
        ).exec()
        Shell.cmd(
            "ip6tables -t mangle -D PREROUTING -j $CHAIN_BOX_PRE 2>/dev/null",
            "ip6tables -t mangle -D OUTPUT -j $CHAIN_BOX_LOCAL 2>/dev/null",
            "ip6tables -t mangle -F $CHAIN_BOX_PRE 2>/dev/null",
            "ip6tables -t mangle -X $CHAIN_BOX_PRE 2>/dev/null",
            "ip6tables -t mangle -F $CHAIN_BOX_LOCAL 2>/dev/null",
            "ip6tables -t mangle -X $CHAIN_BOX_LOCAL 2>/dev/null",
            "ip6tables -t mangle -F $CHAIN_BOX_WHITELIST 2>/dev/null",
            "ip6tables -t mangle -X $CHAIN_BOX_WHITELIST 2>/dev/null",
            "ip6tables -t mangle -F $CHAIN_BOX 2>/dev/null",
            "ip6tables -t mangle -X $CHAIN_BOX 2>/dev/null",
            "ip -6 rule del fwmark $MARK_ID lookup $TABLE_ID 2>/dev/null",
            "ip -6 route del local default dev lo table $TABLE_ID 2>/dev/null"
        ).exec()
    }

    private fun setupTproxy(settings: AppSettings) {
        val tproxyPort = settings.tproxyPort
        val mark = MARK_ID
        Shell.cmd(
            "iptables -t mangle -N $CHAIN_BOX 2>/dev/null || iptables -t mangle -F $CHAIN_BOX",
            "iptables -t mangle -N $CHAIN_BOX_LOCAL 2>/dev/null || iptables -t mangle -F $CHAIN_BOX_LOCAL",
            "iptables -t mangle -N $CHAIN_BOX_PRE 2>/dev/null || iptables -t mangle -F $CHAIN_BOX_PRE",
            "iptables -t mangle -N $CHAIN_BOX_WHITELIST 2>/dev/null || iptables -t mangle -F $CHAIN_BOX_WHITELIST"
        ).exec()
        Shell.cmd(
            "ip rule add fwmark $mark lookup $TABLE_ID 2>/dev/null",
            "ip route add local default dev lo table $TABLE_ID 2>/dev/null"
        ).exec()
        settings.intranet.forEach { cidr ->
            Shell.cmd("iptables -t mangle -A $CHAIN_BOX -d $cidr -j RETURN").exec()
        }
        Shell.cmd(
            "iptables -t mangle -A $CHAIN_BOX -p tcp -j TPROXY --on-port $tproxyPort --tproxy-mark $mark",
            "iptables -t mangle -A $CHAIN_BOX -p udp -j TPROXY --on-port $tproxyPort --tproxy-mark $mark"
        ).exec()
        settings.apList.forEach { iface ->
            Shell.cmd("iptables -t mangle -A $CHAIN_BOX_PRE -i $iface -j $CHAIN_BOX").exec()
        }
        settings.ignoreOutList.forEach { iface ->
            Shell.cmd("iptables -t mangle -A $CHAIN_BOX_LOCAL -o $iface -j RETURN").exec()
        }
        applyOwnerRules(settings, "iptables", "mangle", CHAIN_BOX_LOCAL)
        Shell.cmd(
            "iptables -t mangle -A PREROUTING -j $CHAIN_BOX_PRE",
            "iptables -t mangle -A OUTPUT -j $CHAIN_BOX_LOCAL"
        ).exec()
        if (settings.ipv6Enable) setupTproxy6(settings)
    }

    private fun setupRedirect(settings: AppSettings) {
        val redirPort = settings.redirPort
        Shell.cmd(
            "iptables -t nat -N $CHAIN_BOX 2>/dev/null || iptables -t nat -F $CHAIN_BOX",
            "iptables -t nat -N $CHAIN_BOX_LOCAL 2>/dev/null || iptables -t nat -F $CHAIN_BOX_LOCAL",
            "iptables -t nat -N $CHAIN_BOX_PRE 2>/dev/null || iptables -t nat -F $CHAIN_BOX_PRE",
            "iptables -t nat -N $CHAIN_BOX_WHITELIST 2>/dev/null || iptables -t nat -F $CHAIN_BOX_WHITELIST"
        ).exec()
        settings.intranet.forEach { cidr ->
            Shell.cmd("iptables -t nat -A $CHAIN_BOX -d $cidr -j RETURN").exec()
        }
        Shell.cmd("iptables -t nat -A $CHAIN_BOX -p tcp -j REDIRECT --to-ports $redirPort").exec()
        settings.apList.forEach { iface ->
            Shell.cmd("iptables -t nat -A $CHAIN_BOX_PRE -i $iface -j $CHAIN_BOX").exec()
        }
        settings.ignoreOutList.forEach { iface ->
            Shell.cmd("iptables -t nat -A $CHAIN_BOX_LOCAL -o $iface -j RETURN").exec()
        }
        applyOwnerRules(settings, "iptables", "nat", CHAIN_BOX_LOCAL)
        Shell.cmd(
            "iptables -t nat -A PREROUTING -j $CHAIN_BOX_PRE",
            "iptables -t nat -A OUTPUT -j $CHAIN_BOX_LOCAL"
        ).exec()
    }

    private fun setupTproxy6(settings: AppSettings) {
        val tproxyPort = settings.tproxyPort
        val mark = MARK_ID
        Shell.cmd(
            "ip6tables -t mangle -N $CHAIN_BOX 2>/dev/null || ip6tables -t mangle -F $CHAIN_BOX",
            "ip6tables -t mangle -N $CHAIN_BOX_LOCAL 2>/dev/null || ip6tables -t mangle -F $CHAIN_BOX_LOCAL",
            "ip6tables -t mangle -N $CHAIN_BOX_PRE 2>/dev/null || ip6tables -t mangle -F $CHAIN_BOX_PRE",
            "ip6tables -t mangle -N $CHAIN_BOX_WHITELIST 2>/dev/null || ip6tables -t mangle -F $CHAIN_BOX_WHITELIST"
        ).exec()
        Shell.cmd(
            "ip -6 rule add fwmark $mark lookup $TABLE_ID 2>/dev/null",
            "ip -6 route add local default dev lo table $TABLE_ID 2>/dev/null"
        ).exec()
        settings.intranet6.forEach { cidr ->
            Shell.cmd("ip6tables -t mangle -A $CHAIN_BOX -d $cidr -j RETURN").exec()
        }
        Shell.cmd(
            "ip6tables -t mangle -A $CHAIN_BOX -p tcp -j TPROXY --on-port $tproxyPort --tproxy-mark $mark",
            "ip6tables -t mangle -A $CHAIN_BOX -p udp -j TPROXY --on-port $tproxyPort --tproxy-mark $mark"
        ).exec()
        settings.apList.forEach { iface ->
            Shell.cmd("ip6tables -t mangle -A $CHAIN_BOX_PRE -i $iface -j $CHAIN_BOX").exec()
        }
        settings.ignoreOutList.forEach { iface ->
            Shell.cmd("ip6tables -t mangle -A $CHAIN_BOX_LOCAL -o $iface -j RETURN").exec()
        }
        applyOwnerRules(settings, "ip6tables", "mangle", CHAIN_BOX_LOCAL)
        Shell.cmd(
            "ip6tables -t mangle -A PREROUTING -j $CHAIN_BOX_PRE",
            "ip6tables -t mangle -A OUTPUT -j $CHAIN_BOX_LOCAL"
        ).exec()
    }

    private fun applyOwnerRules(
        settings: AppSettings,
        iptablesBin: String,
        table: String,
        chain: String
    ) {
        val uids = resolveUids(settings.userPackages)
        val gids = settings.gidList
        when (settings.proxyMode) {
            ProxyMode.BLACKLIST -> {
                uids.forEach { uid ->
                    Shell.cmd("$iptablesBin -t $table -A $chain -m owner --uid-owner $uid -j RETURN").exec()
                }
                gids.forEach { gid ->
                    Shell.cmd("$iptablesBin -t $table -A $chain -m owner --gid-owner $gid -j RETURN").exec()
                }
                Shell.cmd("$iptablesBin -t $table -A $chain -j $CHAIN_BOX").exec()
            }
            ProxyMode.WHITELIST -> {
                if (uids.isEmpty() && gids.isEmpty()) {
                    Shell.cmd("$iptablesBin -t $table -A $chain -j RETURN").exec()
                } else {
                    uids.forEach { uid ->
                        Shell.cmd("$iptablesBin -t $table -A $CHAIN_BOX_WHITELIST -m owner --uid-owner $uid -j $CHAIN_BOX").exec()
                    }
                    gids.forEach { gid ->
                        Shell.cmd("$iptablesBin -t $table -A $CHAIN_BOX_WHITELIST -m owner --gid-owner $gid -j $CHAIN_BOX").exec()
                    }
                    Shell.cmd("$iptablesBin -t $table -A $CHAIN_BOX_WHITELIST -j RETURN").exec()
                    Shell.cmd("$iptablesBin -t $table -A $chain -j $CHAIN_BOX_WHITELIST").exec()
                }
            }
            ProxyMode.CORE -> {
                Shell.cmd("$iptablesBin -t $table -A $chain -j RETURN").exec()
            }
        }
    }

    fun resolveUids(packages: List<String>): List<Int> {
        val pm = context.packageManager
        val uids = mutableListOf<Int>()
        packages.forEach { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                uids.add(info.uid)
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        return uids.distinct()
    }

    fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = it.loadLabel(pm).toString(),
                    uid = it.uid,
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val uid: Int,
        val isSystem: Boolean
    )
}
