package com.wateen.launcher.data

import android.text.Html
import android.util.Xml
import com.wateen.launcher.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

object Info {

    private const val OUNCE_GRAMS = 31.1034768

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 10000
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; TV) WateenLauncher/1.0")
            if (c.responseCode !in 200..299) {
                c.disconnect()
                null
            } else {
                c.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- الصلاة ----------------
    suspend fun prayers(): PrayerInfo? {
        val body = get("https://api.aladhan.com/v1/timingsByCity?city=Beni%20Suef&country=Egypt&method=5")
            ?: return null
        return try {
            val d = JSONObject(body).getJSONObject("data")
            val t = d.getJSONObject("timings")
            val list = listOf(
                Prayer("الفجر", t.getString("Fajr").take(5)),
                Prayer("الشروق", t.getString("Sunrise").take(5)),
                Prayer("الظهر", t.getString("Dhuhr").take(5)),
                Prayer("العصر", t.getString("Asr").take(5)),
                Prayer("المغرب", t.getString("Maghrib").take(5)),
                Prayer("العشاء", t.getString("Isha").take(5))
            )
            val h = d.getJSONObject("date").getJSONObject("hijri")
            val hijri = h.getString("day").toInt().toString() + " " +
                h.getJSONObject("month").getString("ar") + " " + h.getString("year") + " هـ"
            PrayerInfo(list, hijri)
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- الطقس ----------------
    suspend fun weather(): Weather? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${Config.LAT}&longitude=${Config.LON}" +
            "&current=temperature_2m,weather_code"
        val body = get(url) ?: return null
        return try {
            val c = JSONObject(body).getJSONObject("current")
            Weather(Math.round(c.getDouble("temperature_2m")).toInt(), c.getInt("weather_code"))
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- الذهب ----------------
    // المصدر الأساسي: أسعار الصاغة المنشورة على منصة آي صاغة (سعر البيع).
    // الاحتياطي: حساب تقديري = سعر الأوقية العالمي × الدولار × (1 + علاوة السوق المحلي).
    suspend fun gold(): GoldRates? = isagha() ?: estimated()

    private suspend fun isagha(): GoldRates? {
        for (u in listOf("https://market.isagha.com/prices", "https://market.isagha.com/prices/eg")) {
            val html = get(u) ?: continue
            val r = parseIsagha(html)
            if (r != null) return r
        }
        return null
    }

    private fun digits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            val c: Char = when (ch) {
                in '٠'..'٩' -> '0' + (ch - '٠')
                '٫' -> '.'
                '٬' -> ','
                else -> ch
            }
            sb.append(c)
        }
        return sb.toString()
    }

    private fun htmlToText(html: String): String {
        var t = html.replace(Regex("(?s)<(script|style)[^>]*>.*?</\\1>"), " ")
        t = t.replace(Regex("<[^>]+>"), " ")
        t = t.replace("&nbsp;", " ")
        t = t.replace(Regex("[\\u200e\\u200f\\u00a0]"), " ")
        t = digits(t)
        return t.replace(Regex("\\s+"), " ")
    }

    private fun toNum(s: String): Double? = s.replace(",", "").toDoubleOrNull()

    // صف الجدول: الاسم ثم (بيع) ثم (فجوة البيع) ثم (شراء)
    private fun sellOf(text: String, label: String): Double? {
        val n = "([0-9]+(?:[.,][0-9]+)*)"
        val rx = Regex(label + "\\s*" + n + "\\s*ج\\.م\\s*" + n + "\\s*ج\\.م\\s*" + n + "\\s*ج\\.م")
        val m = rx.find(text) ?: return null
        return toNum(m.groupValues[1])
    }

    private fun parseIsagha(html: String): GoldRates? {
        val text = htmlToText(html)
        val k21 = sellOf(text, "عيار 21") ?: return null
        if (k21 !in 500.0..200000.0) return null
        val k24 = sellOf(text, "عيار 24") ?: (k21 * 24.0 / 21.0)
        val k18 = sellOf(text, "عيار 18") ?: (k21 * 18.0 / 21.0)
        val pound = sellOf(text, "جنيه ذهب") ?: (k21 * 8.0)

        val updRx = Regex("آخر تحديث:\\s*([0-9]{1,2}\\s+\\S+\\s+[0-9]{4}\\s*-\\s*[0-9]{1,2}:[0-9]{2}\\s*[صم])")
        val upd = updRx.find(text)?.groupValues?.get(1)
        val now = System.currentTimeMillis()
        val note = "آي صاغة • " + (upd?.replace(Regex("\\s*[0-9]{4}"), "") ?: clock12(now))
        return GoldRates(
            Math.round(k24).toInt(), Math.round(k21).toInt(), Math.round(k18).toInt(),
            Math.round(pound).toInt(), now, note
        )
    }

    private suspend fun estimated(): GoldRates? {
        val usd = ounceUsd()
        val fx = usdEgp()
        val primary: Double? = if (usd != null && fx != null) usd / OUNCE_GRAMS * fx else null
        val gram24: Double = primary ?: ounceEgpBackup()?.let { it / OUNCE_GRAMS } ?: return null

        val g24 = gram24 * (1.0 + Config.GOLD_PREMIUM_PERCENT / 100.0)
        val k24 = Math.round(g24).toInt()
        val k21 = Math.round(g24 * 21.0 / 24.0).toInt()
        val k18 = Math.round(g24 * 18.0 / 24.0).toInt()
        val now = System.currentTimeMillis()
        return GoldRates(k24, k21, k18, k21 * 8, now, "تقديري (سعر عالمي) • " + clock12(now))
    }

    private suspend fun ounceUsd(): Double? {
        val b = get("https://api.gold-api.com/price/XAU") ?: return null
        return try {
            val p = JSONObject(b).getDouble("price")
            if (p in 500.0..30000.0) p else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun usdEgp(): Double? {
        val a = get("https://open.er-api.com/v6/latest/USD")
        if (a != null) {
            try {
                val j = JSONObject(a)
                if (j.optString("result") == "success") {
                    val r = j.getJSONObject("rates").getDouble("EGP")
                    if (r in 10.0..500.0) return r
                }
            } catch (e: Exception) { }
        }
        val urls = listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json",
            "https://latest.currency-api.pages.dev/v1/currencies/usd.json"
        )
        for (u in urls) {
            val b = get(u) ?: continue
            try {
                val r = JSONObject(b).getJSONObject("usd").getDouble("egp")
                if (r in 10.0..500.0) return r
            } catch (e: Exception) { }
        }
        return null
    }

    private suspend fun ounceEgpBackup(): Double? {
        val urls = listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/xau.json",
            "https://latest.currency-api.pages.dev/v1/currencies/xau.json"
        )
        for (u in urls) {
            val b = get(u) ?: continue
            try {
                val r = JSONObject(b).getJSONObject("xau").getDouble("egp")
                if (r in 10000.0..5000000.0) return r
            } catch (e: Exception) { }
        }
        return null
    }

    // ---------------- الأخبار ----------------
    // يجلب كل المصادر معاً، يدمجها بالتناوب بين المصادر، ويحذف المكرر
    suspend fun news(): String? = coroutineScope {
        val results = Config.NEWS_FEEDS.map { f -> async { fetchFeed(f.first, f.second) } }.awaitAll()
        val out = ArrayList<String>()
        val seen = HashSet<String>()
        var i = 0
        while (out.size < 18 && results.any { i < it.size }) {
            for (r in results) {
                if (i < r.size) {
                    val t = r[i]
                    if (seen.add(t)) out.add(t)
                }
            }
            i++
        }
        if (out.isEmpty()) null else out.joinToString("   ●   ")
    }

    private suspend fun fetchFeed(label: String, url: String): List<String> {
        val body = get(url) ?: return emptyList()
        return parseTitles(body).take(8).map { "$label: $it" }
    }

    private fun clean(s: String): String =
        Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()

    private fun parseTitles(xml: String): List<String> {
        val out = ArrayList<String>()
        try {
            val p = Xml.newPullParser()
            p.setInput(StringReader(xml))
            var inItem = false
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    val n = p.name
                    if (n == "item" || n == "entry") {
                        inItem = true
                    } else if (inItem && n == "title") {
                        val t = clean(p.nextText())
                        if (t.isNotEmpty()) out.add(t)
                    }
                } else if (ev == XmlPullParser.END_TAG && (p.name == "item" || p.name == "entry")) {
                    inItem = false
                }
                ev = p.next()
            }
        } catch (e: Exception) { }

        if (out.isEmpty()) {
            val rx = Regex("<item>.*?<title>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</title>", RegexOption.DOT_MATCHES_ALL)
            for (m in rx.findAll(xml)) {
                val t = clean(m.groupValues[1])
                if (t.isNotEmpty()) out.add(t)
            }
        }
        return out
    }
}
