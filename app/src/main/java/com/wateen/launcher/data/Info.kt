package com.wateen.launcher.data

import com.wateen.launcher.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Info {

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

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

    suspend fun gold(): Gold? {
        if (Config.GOLD_URL.isBlank()) return null
        val body = get(Config.GOLD_URL) ?: return null
        return try {
            val j = JSONObject(body)
            Gold(j.getInt("k24"), j.getInt("k21"), j.getInt("k18"), j.getInt("pound"))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun news(): String {
        if (Config.NEWS_RSS_URL.isBlank()) return Config.NEWS_DEFAULT
        val body = get(Config.NEWS_RSS_URL) ?: return Config.NEWS_DEFAULT
        val rx = Regex("<item>.*?<title>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</title>", RegexOption.DOT_MATCHES_ALL)
        val titles = rx.findAll(body).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.take(8).toList()
        return if (titles.isEmpty()) Config.NEWS_DEFAULT else titles.joinToString("   ●   ")
    }
}
