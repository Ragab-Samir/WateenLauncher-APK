package com.wateen.launcher

import com.wateen.launcher.data.Gold
import com.wateen.launcher.data.Prayer

object Config {
    // النص الظاهر أعلى الثيم
    const val TOP_TEXT = "وتين حبيبة قلبي ❤️"

    const val CITY_AR = "بني سويف"
    const val LAT = 29.0661
    const val LON = 31.0994

    // لو الإنترنت مقطوع أو الـ API فشل، تُستخدم هذه القيم
    val DEFAULT_PRAYERS = listOf(
        Prayer("الفجر", "04:41"),
        Prayer("الشروق", "06:19"),
        Prayer("الظهر", "11:42"),
        Prayer("العصر", "14:29"),
        Prayer("المغرب", "17:00"),
        Prayer("العشاء", "18:19")
    )

    val DEFAULT_GOLD = Gold(6246, 5465, 4684, 43720)

    const val NEWS_DEFAULT =
        "شركة المياه في بني سويف تعلن قطع المياه عن بعض قرى ببا وناصر لأعمال الصيانة"

    // ضع رابط RSS للأخبار هنا (اختياري). فارغ = يعرض الخبر الافتراضي
    const val NEWS_RSS_URL = ""

    // ضع رابط JSON لأسعار الذهب هنا (اختياري) بالشكل:
    // {"k24":6246,"k21":5465,"k18":4684,"pound":43720}
    const val GOLD_URL = ""

    // تعديل التاريخ الهجري بالأيام لو كان مختلفاً عن المعتمد عندكم (مثلاً -1 أو 1)
    const val HIJRI_OFFSET_DAYS = 0L
}
