package com.wateen.launcher.data

import android.content.Context
import com.wateen.launcher.Config

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("wateen", Context.MODE_PRIVATE)

    private fun getSet(k: String): Set<String> = HashSet<String>(p.getStringSet(k, null) ?: emptySet<String>())
    private fun putSet(k: String, v: Set<String>) { p.edit().putStringSet(k, HashSet(v)).apply() }

    var favorites: Set<String>
        get() = getSet("fav")
        set(v) = putSet("fav", v)

    var games: Set<String>
        get() = getSet("games")
        set(v) = putSet("games", v)

    var nonGames: Set<String>
        get() = getSet("nongames")
        set(v) = putSet("nongames", v)

    var locked: Set<String>
        get() = getSet("locked")
        set(v) = putSet("locked", v)

    var pin: String?
        get() = p.getString("pin", null)
        set(v) { p.edit().putString("pin", v).apply() }

    var firstRun: Boolean
        get() = p.getBoolean("first", true)
        set(v) { p.edit().putBoolean("first", v).apply() }

    var gold: GoldRates
        get() {
            val raw = p.getString("gold", null) ?: return Config.DEFAULT_GOLD
            return try {
                val parts = raw.split("|", limit = 2)
                val a = parts[0].split(",")
                GoldRates(
                    a[0].toInt(), a[1].toInt(), a[2].toInt(), a[3].toInt(),
                    if (a.size > 4) a[4].toLong() else 0L,
                    if (parts.size > 1) parts[1] else ""
                )
            } catch (e: Exception) {
                Config.DEFAULT_GOLD
            }
        }
        set(v) {
            val note = v.note.replace("|", " ")
            p.edit().putString("gold", "${v.k24},${v.k21},${v.k18},${v.pound},${v.updated}|$note").apply()
        }

    var news: String?
        get() = p.getString("news", null)
        set(v) { p.edit().putString("news", v).apply() }
}
