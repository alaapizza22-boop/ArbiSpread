package com.arbispread.app.ui

import androidx.compose.runtime.mutableStateOf

enum class AppLanguage { AR, EN }

/**
 * نظام ترجمة بسيط (عربي/إنجليزي) من غير الحاجة لملفات strings.xml منفصلة لكل لغة،
 * عشان يبقى سهل إننا نبدّل اللغة لحظياً من زرار واحد في الواجهة من غير إعادة تشغيل.
 */
object Strings {
    val language = mutableStateOf(AppLanguage.AR)

    fun toggle() {
        language.value = if (language.value == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
    }

    private val ar = mapOf(
        "app_name" to "آربي سبريد",
        "subtitle" to "فروق الأسعار والفاندينج بين المنصات لحظياً",
        "loading" to "جاري السحب من المنصات…",
        "refresh" to "تحديث",
        "settings" to "الإعدادات",
        "capital_label" to "رأس المال ($)",
        "net_profit" to "صافي الربح",
        "spread" to "فرق السعر",
        "fees" to "العمولات",
        "unlock_cta" to "شاهد إعلان للفتح",
        "locked_label" to "عملة مقفولة",
        "filter_profitable" to "عرض الفرص الرابحة فقط",
        "tab_arbitrage" to "آربيتراج الأسعار",
        "tab_funding" to "الفاندينج",
        "funding_rate" to "معدل الفاندينج",
        "next_funding" to "السداد القادم",
        "save" to "حفظ",
        "withdrawal_fee" to "عمولة سحب الشبكة التقديرية ($)",
        "fee_settings_desc" to "عمولة التداول (%) لكل منصة — عدّلها حسب مستوى حسابك الحقيقي",
        "back" to "رجوع",
        "lang_toggle" to "EN",
        "buy_from" to "اشترِ من",
        "sell_on" to "بِع في",
        "long_on" to "لونج في",
        "short_on" to "شورت في",
        "est_daily_funding_profit" to "ربح الفاندينج المتوقع يومياً",
        "no_data" to "بيانات غير متاحة حالياً",
        "net_profit_usd" to "صافي الربح بالدولار",
    )

    private val en = mapOf(
        "app_name" to "ArbiSpread",
        "subtitle" to "Live price spread & funding rate across exchanges",
        "loading" to "Fetching from exchanges…",
        "refresh" to "Refresh",
        "settings" to "Settings",
        "capital_label" to "Capital ($)",
        "net_profit" to "Net Profit",
        "spread" to "Spread",
        "fees" to "Fees",
        "unlock_cta" to "Watch ad to unlock",
        "locked_label" to "Locked coin",
        "filter_profitable" to "Show profitable only",
        "tab_arbitrage" to "Price Arbitrage",
        "tab_funding" to "Funding Rate",
        "funding_rate" to "Funding Rate",
        "next_funding" to "Next Funding",
        "save" to "Save",
        "withdrawal_fee" to "Estimated Network Withdrawal Fee ($)",
        "fee_settings_desc" to "Trading fee (%) per exchange — adjust to your real tier",
        "back" to "Back",
        "lang_toggle" to "AR",
        "buy_from" to "Buy from",
        "sell_on" to "Sell on",
        "long_on" to "Long on",
        "short_on" to "Short on",
        "est_daily_funding_profit" to "Est. daily funding profit",
        "no_data" to "No data available right now",
        "net_profit_usd" to "Net Profit ($)",
    )

    fun t(key: String): String {
        val map = if (language.value == AppLanguage.EN) en else ar
        return map[key] ?: key
    }
}
