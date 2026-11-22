package com.wateen.launcher.system

import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.wateen.launcher.data.SysStats
import kotlinx.coroutines.delay

object SystemTools {

    private fun toast(c: Context, m: String) = Toast.makeText(c, m, Toast.LENGTH_SHORT).show()

    fun isDefaultHome(ctx: Context): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val r = ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        return r?.activityInfo?.packageName == ctx.packageName
    }

    fun requestDefault(act: Activity, roleLauncher: ActivityResultLauncher<Intent>) {
        if (isDefaultHome(act)) {
            toast(act, "وتين هو اللانشر الافتراضي بالفعل")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = act.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                }
            }
        } catch (e: Exception) { }
        try {
            act.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            return
        } catch (e: Exception) { }
        try {
            // عند وجود أكثر من لانشر يظهر اختيار "دائماً / مرة واحدة"
            act.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            toast(act, "اختر لانشر وتين ثم \"دائماً\"")
            return
        } catch (e: Exception) { }
        try {
            act.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            toast(act, "تعذر فتح إعدادات اللانشر")
        }
    }

    fun openFileManager(ctx: Context) {
        val pm = ctx.packageManager
        val pkgs = listOf(
            "com.google.android.documentsui", "com.android.documentsui",
            "com.mi.android.globalFileexplorer", "com.estrongs.android.pop",
            "com.lonelycatgames.Xplore"
        )
        for (p in pkgs) {
            val i = pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p)
            if (i != null) {
                try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (e: Exception) { }
            }
        }
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast(ctx, "لا يوجد مدير ملفات مثبت")
        }
    }

    fun openBrowser(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast(ctx, "لا يوجد متصفح مثبت")
        }
    }

    fun openSettings(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast(ctx, "تعذر فتح الإعدادات")
        }
    }

    fun uninstall(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast(ctx, "تعذر إلغاء التثبيت")
        }
    }

    // تنظيف الذاكرة: يوقف العمليات الخلفية للتطبيقات غير النظامية
    suspend fun clean(ctx: Context): Pair<Long, Long> {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val before = mi.availMem
        for (a in ctx.packageManager.getInstalledApplications(0)) {
            if (a.packageName == ctx.packageName) continue
            if ((a.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            try { am.killBackgroundProcesses(a.packageName) } catch (e: Exception) { }
        }
        delay(700)
        am.getMemoryInfo(mi)
        val mb = 1024L * 1024L
        return Pair(((mi.availMem - before) / mb).coerceAtLeast(0), mi.availMem / mb)
    }

    @Suppress("DEPRECATION")
    fun stats(ctx: Context): SysStats {
        var wifi = false
        var eth = false
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (n in cm.allNetworks) {
                val c = cm.getNetworkCapabilities(n) ?: continue
                if (!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) eth = true
            }
        } catch (e: Exception) { }
        return SysStats(wifi, eth, remoteBattery(ctx))
    }

    private fun remoteBattery(ctx: Context): Int? {
        try {
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val devices = bm.adapter?.bondedDevices ?: return null
            for (d in devices) {
                val m = d.javaClass.getMethod("getBatteryLevel")
                val lvl = m.invoke(d) as Int
                if (lvl in 0..100) return lvl
            }
        } catch (e: Throwable) { }
        return null
    }
}
