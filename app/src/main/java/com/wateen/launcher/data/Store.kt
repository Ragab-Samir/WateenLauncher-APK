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
            val s = p.getString("gold", null) ?: return Config.DEFAULT_GOLD
            return try {
                val a = s.split(",").map { it.toInt() }
                GoldRates(a[0], a[1], a[2], a[3])
            } catch (e: Exception) {
                Config.DEFAULT_GOLD
            }
        }
        set(v) { p.edit().putString("gold", "${v.k24},${v.k21},${v.k18},${v.pound}").apply() }
}
