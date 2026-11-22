package com.wateen.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wateen.launcher.Config
import com.wateen.launcher.data.GoldRates
import com.wateen.launcher.data.PrayerInfo
import com.wateen.launcher.data.SysStats
import com.wateen.launcher.data.Weather
import com.wateen.launcher.data.clock12
import com.wateen.launcher.data.fmtRemain
import com.wateen.launcher.data.gregorian
import com.wateen.launcher.data.hijriLocal
import com.wateen.launcher.data.nextPrayer
import com.wateen.launcher.data.to12
import com.wateen.launcher.data.weatherSymbol

@Composable
fun PrayerPanel(info: PrayerInfo, modifier: Modifier = Modifier) {
    val now by rememberNow(20_000)
    val (nextIdx, diff) = nextPrayer(info.prayers, now)
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier.background(Panel, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        W("مواقيت الصلاة", 20, Gold, true)
        W(Config.CITY_AR, 14, Muted)
        Spacer(Modifier.height(4.dp))
        info.prayers.forEachIndexed { i, p ->
            val isNext = i == nextIdx
            var rm = Modifier.fillMaxWidth()
            if (isNext) rm = rm.background(Color(0x33FFC107), shape).border(2.dp, Gold, shape)
            Row(rm.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                W(p.name, 18, if (isNext) Gold else TextW, isNext)
                W(to12(p.time24), 18, if (isNext) Gold else TextW, isNext)
            }
        }
        Spacer(Modifier.weight(1f))
        W("الأذان القادم", 14, Muted)
        W(info.prayers[nextIdx].name + " بعد " + fmtRemain(diff), 16, Gold, true)
    }
}

@Composable
fun ClockWidget(hijri: String?, modifier: Modifier = Modifier) {
    val now by rememberNow(5_000)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        W(clock12(now), 56, TextW, true)
        W(gregorian(now) + " - " + (hijri ?: hijriLocal()), 18, Gold, align = TextAlign.Center)
    }
}

@Composable
fun GoldPanel(g: GoldRates, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Panel, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        W("أسعار الذهب", 18, Gold, true)
        W("عيار 24: " + g.k24 + " ج", 16)
        W("عيار 21: " + g.k21 + " ج", 16)
        W("عيار 18: " + g.k18 + " ج", 16)
        W("الجنيه الذهب: " + g.pound + " ج", 16, Gold, true)
    }
}

@Composable
fun StatusPanel(weather: Weather?, stats: SysStats, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Panel, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        W(Config.CITY_AR, 16, Muted)
        val t = if (weather != null) weather.temp.toString() + "°C " + weatherSymbol(weather.code) else "--°C"
        W(t, 32, TextW, true)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            W("● Wi-Fi", 14, if (stats.wifi) Green else Muted)
            W("● Ethernet", 14, if (stats.eth) Green else Muted)
        }
        val b = if (stats.battery != null) stats.battery.toString() + "%" else "--"
        W("بطارية الريموت: $b", 14, TextW)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsTicker(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(38.dp).background(Gold, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicText(
            text = "آخر الأخبار: $text",
            modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
            style = TextStyle(color = Bg, fontSize = 17.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false
        )
    }
}
