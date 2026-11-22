package com.wateen.launcher.data

import androidx.compose.ui.graphics.ImageBitmap

data class Prayer(val name: String, val time24: String)
data class PrayerInfo(val prayers: List<Prayer>, val hijri: String?)
data class GoldRates(
    val k24: Int,
    val k21: Int,
    val k18: Int,
    val pound: Int,
    val updated: Long = 0L,
    val note: String = ""
)
data class Weather(val temp: Int, val code: Int)
data class SysStats(val wifi: Boolean, val eth: Boolean, val battery: Int?)

data class AppItem(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
    val isGameCat: Boolean
)
