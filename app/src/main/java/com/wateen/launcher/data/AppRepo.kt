package com.wateen.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepo {

    suspend fun load(ctx: Context): List<AppItem> = withContext(Dispatchers.Default) {
        val pm = ctx.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (r in pm.queryIntentActivities(intent, 0)) {
                val pkg = r.activityInfo.packageName
                if (pkg == ctx.packageName || !seen.add(pkg)) continue
                val ai = r.activityInfo.applicationInfo
                val label = try { r.loadLabel(pm).toString() } catch (e: Exception) { pkg }
                val icon = try {
                    r.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
                } catch (e: Throwable) {
                    null
                }
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                @Suppress("DEPRECATION")
                val isGame = if (Build.VERSION.SDK_INT >= 26) ai.category == ApplicationInfo.CATEGORY_GAME
                else (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                out.add(AppItem(pkg, label, icon, isSystem, isGame))
            }
        }
        out.sortBy { it.label.lowercase() }
        out
    }

    fun launch(ctx: Context, pkg: String) {
        val pm = ctx.packageManager
        val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (i == null) {
            Toast.makeText(ctx, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show()
            return
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(i) } catch (e: Exception) {
            Toast.makeText(ctx, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show()
        }
    }
}
