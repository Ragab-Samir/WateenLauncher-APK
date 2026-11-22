package com.wateen.launcher

import com.wateen.launcher.data.GoldRates
import com.wateen.launcher.data.Prayer

object Config {
    // النص الظاهر أعلى الثيم
    const val TOP_TEXT = "وتين حبيبة قلبي ❤️"

    const val CITY_AR = "بني سويف"
    const val LAT = 29.0661
    const val LON = 31.0994

    // تُستخدم فقط لو الإنترنت مقطوع أو الـ API فشل
    val DEFAULT_PRAYERS = listOf(
        Prayer("الفجر", "04:41"),
        Prayer("الشروق", "06:19"),
        Prayer("الظهر", "11:42"),
        Prayer("العصر", "14:29"),
        Prayer("المغرب", "17:00"),
        Prayer("العشاء", "18:19")
    )

    val DEFAULT_GOLD = GoldRates(6246, 5465, 4684, 43720)

    const val NEWS_DEFAULT = "جارِ تحميل آخر الأخبار..."

    // مصادر الأخبار (RSS) - تُجلب معاً وتُدمج، وتتحدث كل 10 دقائق
    val NEWS_FEEDS = listOf(
        "اليوم السابع" to "https://www.youm7.com/rss/SectionRss?SectionID=65",
        "اليوم السابع - المحافظات" to "https://www.youm7.com/rss/SectionRss?SectionID=296",
        "بي بي سي عربي" to "https://feeds.bbci.co.uk/arabic/rss.xml"
    )

    // علاوة سوق الصاغة المحلي فوق السعر العالمي (نسبة مئوية).
    // تُستخدم فقط في الحساب التقديري الاحتياطي لو تعذر جلب أسعار آي صاغة.
    const val GOLD_PREMIUM_PERCENT = 2.5

    // تعديل التاريخ الهجري بالأيام لو كان مختلفاً (مثلاً -1 أو 1)
    const val HIJRI_OFFSET_DAYS = 0L
}
