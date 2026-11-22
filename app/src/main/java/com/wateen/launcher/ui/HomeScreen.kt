package com.wateen.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wateen.launcher.Config
import com.wateen.launcher.data.AppItem
import com.wateen.launcher.data.AppRepo
import com.wateen.launcher.data.Info
import com.wateen.launcher.data.PrayerInfo
import com.wateen.launcher.data.Store
import com.wateen.launcher.data.SysStats
import com.wateen.launcher.data.Weather
import com.wateen.launcher.system.SystemTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PinKind { OPEN, UNLOCK, SET_AND_LOCK, VERIFY_CHANGE, SET }
private data class PinReq(val title: String, val kind: PinKind, val pkg: String?)
private class Act(val label: String, val symbol: String, val sub: String?, val run: () -> Unit)

@Composable
fun HomeScreen(activity: ComponentActivity, roleLauncher: ActivityResultLauncher<Intent>) {
    val ctx: Context = activity
    val store = remember { Store(ctx) }
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var favs by remember { mutableStateOf(store.favorites) }
    var games by remember { mutableStateOf(store.games) }
    var nonGames by remember { mutableStateOf(store.nonGames) }
    var locked by remember { mutableStateOf(store.locked) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var pinReq by remember { mutableStateOf<PinReq?>(null) }
    var showSide by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    var prayer by remember { mutableStateOf(PrayerInfo(Config.DEFAULT_PRAYERS, null)) }
    var gold by remember { mutableStateOf(store.gold) }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var news by remember { mutableStateOf(store.news ?: Config.NEWS_DEFAULT) }
    var stats by remember { mutableStateOf(SysStats(false, false, null)) }
    var isDefault by remember { mutableStateOf(false) }
    val firstFocus = remember { FocusRequester() }

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

    // ---- تحميل التطبيقات + متابعة التثبيت/الإزالة ----
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { reload++ }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(r) }
    }

    LaunchedEffect(reload) {
        apps = AppRepo.load(ctx)
        if (store.firstRun && apps.isNotEmpty()) {
            val rx = Regex("(?i)(iptv|youtube|netflix|shahid|شاهد|quran|قرآن|القرآن|القران)")
            val d = apps.filter { rx.containsMatchIn(it.label) || rx.containsMatchIn(it.pkg) }.map { it.pkg }.toSet()
            favs = d
            store.favorites = d
            store.firstRun = false
        }
    }

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(400)
            runCatching { firstFocus.requestFocus() }
        }
    }

    // ---- الخدمات: الصلاة / الطقس / الذهب / الأخبار / حالة النظام ----
    LaunchedEffect(Unit) {
        while (true) {
            val r = Info.prayers()
            if (r != null) { prayer = r; delay(6 * 3600_000L) } else delay(5 * 60_000L)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val w = Info.weather()
            if (w != null) { weather = w; delay(30 * 60_000L) } else delay(5 * 60_000L)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val g = Info.gold()
            if (g != null) {
                gold = g
                store.gold = g
                delay(15 * 60_000L)   // تحديث كل 15 دقيقة
            } else {
                delay(2 * 60_000L)    // فشل: إعادة المحاولة بعد دقيقتين
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val n = Info.news()
            if (n != null) {
                news = n
                store.news = n
                delay(10 * 60_000L)   // تحديث الأخبار كل 10 دقائق
            } else {
                delay(2 * 60_000L)
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            stats = withContext(Dispatchers.IO) { SystemTools.stats(ctx) }
            isDefault = SystemTools.isDefaultHome(ctx)
            delay(10_000L)
        }
    }

    // ---- منطق الفتح والقفل ----
    fun openApp(a: AppItem) {
        if (a.pkg in locked) pinReq = PinReq("أدخل الرقم السري", PinKind.OPEN, a.pkg)
        else AppRepo.launch(ctx, a.pkg)
    }

    fun onPin(req: PinReq, entered: String) {
        pinReq = null
        when (req.kind) {
            PinKind.OPEN -> {
                if (entered == store.pin) AppRepo.launch(ctx, req.pkg!!) else toast("الرقم السري غير صحيح")
            }
            PinKind.UNLOCK -> {
                if (entered == store.pin) {
                    locked = locked - req.pkg!!
                    store.locked = locked
                    toast("تم إزالة القفل")
                } else toast("الرقم السري غير صحيح")
            }
            PinKind.SET_AND_LOCK -> {
                store.pin = entered
                locked = locked + req.pkg!!
                store.locked = locked
                toast("تم حفظ الرقم وقفل التطبيق")
            }
            PinKind.VERIFY_CHANGE -> {
                if (entered == store.pin) pinReq = PinReq("اختر رقماً سرياً جديداً", PinKind.SET, null)
                else toast("الرقم السري غير صحيح")
            }
            PinKind.SET -> {
                store.pin = entered
                toast("تم حفظ الرقم السري")
            }
        }
    }

    fun menuItems(a: AppItem): List<Pair<String, () -> Unit>> {
        val isFav = a.pkg in favs
        val isLocked = a.pkg in locked
        val isGame = a.pkg in games || (a.isGameCat && a.pkg !in nonGames)
        val l = mutableListOf<Pair<String, () -> Unit>>()
        fun item(label: String, act: () -> Unit) { l.add(Pair(label, act)) }

        item("فتح") { menuApp = null; openApp(a) }
        item(if (isFav) "إلغاء التثبيت من المفضلة" else "تثبيت في المفضلة") {
            favs = if (isFav) favs - a.pkg else favs + a.pkg
            store.favorites = favs
            menuApp = null
        }
        item(if (isGame) "إخراج من الألعاب والترفيه" else "نقل إلى الألعاب والترفيه") {
            if (isGame) { games = games - a.pkg; nonGames = nonGames + a.pkg }
            else { games = games + a.pkg; nonGames = nonGames - a.pkg }
            store.games = games
            store.nonGames = nonGames
            menuApp = null
        }
        if (isLocked) {
            item("إزالة القفل") {
                menuApp = null
                pinReq = PinReq("أدخل الرقم السري", PinKind.UNLOCK, a.pkg)
            }
        } else {
            item("قفل بالرقم السري") {
                menuApp = null
                if (store.pin == null) {
                    pinReq = PinReq("اختر رقماً سرياً من 4 أرقام", PinKind.SET_AND_LOCK, a.pkg)
                } else {
                    locked = locked + a.pkg
                    store.locked = locked
                    toast("تم قفل التطبيق")
                }
            }
        }
        if (!a.isSystem) {
            item("إلغاء تثبيت التطبيق") { menuApp = null; SystemTools.uninstall(ctx, a.pkg) }
        }
        return l
    }

    // ---- بيانات الصفوف ----
    val favApps = remember(apps, favs) { apps.filter { it.pkg in favs } }
    val gameApps = remember(apps, games, nonGames) {
        apps.filter { it.pkg in games || (it.isGameCat && it.pkg !in nonGames) }
    }
    val sideApps = remember(apps) { apps.filter { !it.isSystem } }

    val actions = listOf(
        Act("تجاوز اللانشر الافتراضي", "🏠", if (isDefault) "مفعّل ✓" else null) {
            SystemTools.requestDefault(activity, roleLauncher)
        },
        Act("مدير الملفات", "📁", null) { SystemTools.openFileManager(ctx) },
        Act("تطبيقات الهاتف (Sideloaded)", "📱", if (showSide) "ظاهر ✓" else null) { showSide = !showSide },
        Act("تنظيف الذاكرة", "🧹", null) {
            scope.launch {
                toast("جارِ التنظيف...")
                val (freed, avail) = SystemTools.clean(ctx)
                toast("تم تحرير $freed MB - المتاح الآن $avail MB")
            }
        },
        Act("المتصفح", "🌐", null) { SystemTools.openBrowser(ctx) },
        Act("الإعدادات", "⚙️", null) { SystemTools.openSettings(ctx) },
        Act("الرقم السري", "🔒", if (store.pin != null) "مضبوط ✓" else null) {
            pinReq = if (store.pin == null) PinReq("اختر رقماً سرياً من 4 أرقام", PinKind.SET, null)
            else PinReq("أدخل الرقم الحالي", PinKind.VERIFY_CHANGE, null)
        }
    )

    // ---- الواجهة ----
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier.fillMaxSize().background(Bg).padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                W(Config.TOP_TEXT, 30, Gold, true)
                Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvIconButton(IconType.SETTINGS) { SystemTools.openSettings(ctx) }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrayerPanel(prayer, Modifier.width(210.dp).fillMaxHeight())

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(
                        Modifier.fillMaxWidth().height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatusPanel(weather, stats, Modifier.width(200.dp).fillMaxHeight())
                        ClockWidget(prayer.hijri, Modifier.weight(1f).fillMaxHeight())
                        GoldPanel(gold, Modifier.width(230.dp).fillMaxHeight())
                    }

                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        item {
                            AppsRow(
                                "المفضلة", favApps, "اضغط مطولاً على OK فوق أي تطبيق لتثبيته هنا",
                                favs, locked, favApps.firstOrNull()?.pkg, firstFocus,
                                { openApp(it) }, { menuApp = it }
                            )
                        }
                        item {
                            AppsRow(
                                "الألعاب والترفيه", gameApps, "لا توجد ألعاب بعد",
                                favs, locked, null, firstFocus,
                                { openApp(it) }, { menuApp = it }
                            )
                        }
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                W("أدوات وتخصيص", 20, Gold, true, modifier = Modifier.padding(bottom = 8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                                ) {
                                    items(actions) { a -> ActionCard(a.label, a.symbol, a.sub, a.run) }
                                }
                            }
                        }
                        if (showSide) {
                            item {
                                AppsRow(
                                    "تطبيقات الهاتف (Sideloaded)", sideApps, "لا توجد تطبيقات مثبتة يدوياً",
                                    favs, locked, null, firstFocus,
                                    { openApp(it) }, { menuApp = it }
                                )
                            }
                        }
                        item {
                            AppsRow(
                                "كل التطبيقات", apps, "جارِ التحميل...",
                                favs, locked, null, firstFocus,
                                { openApp(it) }, { menuApp = it }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            NewsTicker(news)
        }

        val ma = menuApp
        if (ma != null) {
            MenuDialog(ma.label, menuItems(ma)) { menuApp = null }
        }

        val pr = pinReq
        if (pr != null) {
            androidx.compose.runtime.key(pr) {
                PinDialog(pr.title, { onPin(pr, it) }) { pinReq = null }
            }
        }
    }
}

@Composable
private fun AppsRow(
    title: String,
    list: List<AppItem>,
    empty: String,
    favs: Set<String>,
    locked: Set<String>,
    focusPkg: String?,
    focus: FocusRequester,
    onOpen: (AppItem) -> Unit,
    onMenu: (AppItem) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        W(title, 20, Gold, true, modifier = Modifier.padding(bottom = 8.dp))
        if (list.isEmpty()) {
            W(empty, 14, Muted, modifier = Modifier.padding(8.dp))
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
            ) {
                items(list, key = { it.pkg }) { a ->
                    AppCard(
                        a, a.pkg in favs, a.pkg in locked,
                        if (a.pkg == focusPkg) focus else null,
                        { onOpen(a) }, { onMenu(a) }
                    )
                }
            }
        }
    }
}
