package com.wateen.launcher.data

import com.wateen.launcher.Config
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar

private fun f2(n: Int) = if (n < 10) "0$n" else "$n"

private val DAYS = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
private val MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)
private val HIJRI_MONTHS = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

fun toMin(t: String): Int {
    val s = t.trim().take(5).split(":")
    return s[0].toInt() * 60 + s[1].toInt()
}

fun to12(t24: String): String {
    val m = toMin(t24)
    var h = (m / 60) % 12
    if (h == 0) h = 12
    return f2(h) + ":" + f2(m % 60)
}

fun clock12(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    var h = c.get(Calendar.HOUR)
    if (h == 0) h = 12
    val ap = if (c.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
    return f2(h) + ":" + f2(c.get(Calendar.MINUTE)) + " " + ap
}

fun gregorian(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return DAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + " " +
        MONTHS[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR) + " م"
}

fun hijriLocal(): String {
    return try {
        val h = HijrahDate.from(LocalDate.now().plusDays(Config.HIJRI_OFFSET_DAYS))
        h.get(ChronoField.DAY_OF_MONTH).toString() + " " +
            HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1] + " " +
            h.get(ChronoField.YEAR) + " هـ"
    } catch (e: Exception) {
        ""
    }
}

fun fmtRemain(min: Int): String = (min / 60).toString() + ":" + f2(min % 60) + " س"

// يرجع (رقم الصلاة القادمة, الدقائق المتبقية) مع تجاهل الشروق
fun nextPrayer(list: List<Prayer>, nowMs: Long): Pair<Int, Int> {
    val c = Calendar.getInstance().apply { timeInMillis = nowMs }
    val nowMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    var best = 0
    var bestDiff = Int.MAX_VALUE
    list.forEachIndexed { i, p ->
        if (p.name == "الشروق") return@forEachIndexed
        var d = toMin(p.time24) - nowMin
        if (d <= 0) d += 1440
        if (d < bestDiff) { bestDiff = d; best = i }
    }
    return best to bestDiff
}

fun weatherSymbol(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..67 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌦️"
    in 95..99 -> "⛈️"
    else -> "☁️"
}
