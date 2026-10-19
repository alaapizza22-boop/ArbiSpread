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
        "withdrawal_fee" to "عمولة سحب احتياطية للعملات غير المدرجة ($)",
        "withdrawal_fee_hint" to "بتتستخدم بس لو العملة مش موجودة في جدول الرسوم المرجعي (شوف WithdrawalFees.kt) — افتراضياً صفر، يعني مفيش خصم وهمي لحد ما تحدد رقم حقيقي بنفسك",
        "fee_settings_desc" to "عمولة التداول (%) الفعلية لكل منصة (القيم الافتراضية هي السعر المعياري المنشور رسمياً للمستخدم العادي) — عدّلها لو عندك مستوى VIP أو خصم مختلف",
        "back" to "رجوع",
        "lang_toggle" to "EN",
        "buy_from" to "اشترِ من",
        "sell_on" to "بِع في",
        "long_on" to "لونج في",
        "short_on" to "شورت في",
        "est_daily_funding_profit" to "ربح الفاندينج المتوقع يومياً",
        "no_data" to "بيانات غير متاحة حالياً",
        "net_profit_usd" to "صافي الربح بالدولار",
        "network_label" to "الشبكة",
        "open_exchange" to "افتح في",
        "gross_profit" to "الربح الخام",
        "buy_fee" to "عمولة الشراء",
        "sell_fee" to "عمولة البيع",
        "withdrawal_fee_short" to "عمولة السحب",
        "fee_not_verified_badge" to "⚠️ غير محقق",
        "fee_not_verified_warning" to "رسوم سحب هذه العملة مش موجودة في جدولنا المرجعي، فصافي الربح هنا بيعتمد على القيمة اليدوية في الإعدادات (افتراضياً صفر) — مش رقم حقيقي محقق. دوّر على رسوم السحب الفعلية في صفحة Withdraw بالمنصة قبل أي قرار.",
        "liquidity_check_title" to "فحص التنفيذ الحي",
        "liquidity_check_cta" to "فحص السيولة والمحافظ",
        "liquidity_checking" to "جاري سحب دفتر الطلبات وحالة المحافظ لحظياً…",
        "max_executable" to "أقصى مبلغ قابل للتنفيذ",
        "max_executable_desc" to "بدون انزلاق سعري (Slippage) يتجاوز",
        "depth_exhausted_warning" to "⚠️ العمق المعروض من الـ API محدود (أول 50-100 مستوى) — هذا حد أدنى مؤكد، المتاح فعلياً قد يكون أعلى",
        "liquidity_error" to "تعذر الفحص",
        "wallet_status_title" to "حالة الإيداع/السحب",
        "wallet_deposit" to "الإيداع",
        "wallet_withdraw" to "السحب",
        "wallet_enabled" to "✅ متاح",
        "wallet_disabled" to "🚫 موقوف",
        "wallet_unverifiable" to "⚠️ غير قابل للتحقق من API عام — تأكد يدوياً",
        "wallet_blocked_banner" to "⚠️ الصفقة غير قابلة للتنفيذ حالياً — إيداع أو سحب موقوف على إحدى المنصتين",
        "retry" to "إعادة المحاولة",
        "close" to "إغلاق",
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
        "withdrawal_fee" to "Fallback withdrawal fee for unlisted coins ($)",
        "withdrawal_fee_hint" to "Only used when the coin isn't in the reference fee table (see WithdrawalFees.kt) — defaults to zero, so no fake deduction until you set a real number",
        "fee_settings_desc" to "Real spot taker fee (%) per exchange (defaults are the officially published standard rate) — adjust if you have a VIP tier or token discount",
        "back" to "Back",
        "lang_toggle" to "AR",
        "buy_from" to "Buy from",
        "sell_on" to "Sell on",
        "long_on" to "Long on",
        "short_on" to "Short on",
        "est_daily_funding_profit" to "Est. daily funding profit",
        "no_data" to "No data available right now",
        "net_profit_usd" to "Net Profit ($)",
        "network_label" to "Network",
        "open_exchange" to "Open in",
        "gross_profit" to "Gross profit",
        "buy_fee" to "Buy fee",
        "sell_fee" to "Sell fee",
        "withdrawal_fee_short" to "Withdrawal fee",
        "fee_not_verified_badge" to "⚠️ Unverified",
        "fee_not_verified_warning" to "This coin's withdrawal fee isn't in our reference table, so net profit here relies on the manual fallback in Settings (zero by default) — not a verified real number. Check the exchange's actual Withdraw page before acting.",
        "liquidity_check_title" to "Live Execution Check",
        "liquidity_check_cta" to "Check liquidity & wallets",
        "liquidity_checking" to "Fetching live order book and wallet status…",
        "max_executable" to "Max executable capital",
        "max_executable_desc" to "without slippage exceeding",
        "depth_exhausted_warning" to "⚠️ API-returned depth is limited (top 50-100 levels) — this is a confirmed minimum, actual liquidity may be higher",
        "liquidity_error" to "Check failed",
        "wallet_status_title" to "Deposit/Withdrawal Status",
        "wallet_deposit" to "Deposit",
        "wallet_withdraw" to "Withdraw",
        "wallet_enabled" to "✅ Enabled",
        "wallet_disabled" to "🚫 Suspended",
        "wallet_unverifiable" to "⚠️ Not checkable via public API — verify manually",
        "wallet_blocked_banner" to "⚠️ Trade not executable right now — deposit or withdrawal suspended on one exchange",
        "retry" to "Retry",
        "close" to "Close",
    )

    fun t(key: String): String {
        val map = if (language.value == AppLanguage.EN) en else ar
        return map[key] ?: key
    }
}
