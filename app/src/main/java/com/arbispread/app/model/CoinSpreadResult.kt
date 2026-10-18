package com.arbispread.app.model

/** سعر عملة على منصة واحدة، مع عمولة التداول وعمولة سحب الشبكة التقديرية */
data class ExchangeQuote(
    val exchangeName: String,
    val price: Double,
    val takerFeePercent: Double,
    val withdrawalFeeUsd: Double,
)

/** نتيجة المقارنة النهائية لعملة واحدة عبر كل المنصات */
data class CoinSpreadResult(
    val coin: CoinDef,
    val quotes: List<ExchangeQuote>,   // كل الأسعار المتاحة من المنصات (كلها Spot بأزواج USDT)
) {
    val buy: ExchangeQuote? = if (quotes.size >= 2) quotes.minByOrNull { it.price } else null
    val sell: ExchangeQuote? = if (quotes.size >= 2) quotes.maxByOrNull { it.price } else null

    /** الفرق الخام بين أعلى وأقل سعر، كنسبة مئوية (ممكن يكون سالب أو موجب أو null لو مفيش بيانات كفاية) */
    val spreadPercent: Double?
        get() {
            val b = buy?.price ?: return null
            val s = sell?.price ?: return null
            if (b <= 0.0) return null
            return (s - b) / b * 100.0
        }

    /**
     * فحص سلامة البيانات: فرق سعر حقيقي بين منصتين سائلتين (Spot/Spot، نفس زوج USDT)
     * بيبقى عادةً أقل من 1% وناذراً ما يتعدى 2-3%. أي فرق أكبر من [MAX_PLAUSIBLE_SPREAD_PERCENT]
     * (10%) غالباً سببه سعر متأخر (Stale) أو عملة ضعيفة السيولة على منصة معينة أو خطأ لحظي
     * في استجابة الـ API - مش فرصة آربيتراج حقيقية، فبنلغيها فوراً بدل ما نعرضها كربح وهمي.
     */
    val isAvailable: Boolean
        get() {
            val b = buy ?: return false
            val s = sell ?: return false
            if (b.exchangeName == s.exchangeName) return false
            val spread = spreadPercent ?: return false
            return kotlin.math.abs(spread) <= MAX_PLAUSIBLE_SPREAD_PERCENT
        }

    /**
     * صافي الربح كنسبة مئوية (مش بالدولار)، بعد خصم:
     * - عمولة الشراء (Taker) على منصة الشراء
     * - عمولة البيع (Taker) على منصة البيع
     * - عمولة سحب الشبكة لتحويل العملة بين المنصتين، معبّر عنها كنسبة من رأس المال المدخل
     * معادلة صافي الربح بالدولار = رأس المال × (هذه النسبة ÷ 100) — شوف [netProfitUsd].
     * بترجع null لو البيانات مش متاحة أو الفرق غير منطقي (شوف [isAvailable]).
     */
    fun netProfitPercent(capitalUsd: Double): Double? {
        if (!isAvailable) return null
        val spread = spreadPercent ?: return null
        val b = buy ?: return null
        val s = sell ?: return null
        val tradingFees = b.takerFeePercent + s.takerFeePercent
        val withdrawalPercent = if (capitalUsd > 0.0) (b.withdrawalFeeUsd / capitalUsd) * 100.0 else 0.0
        return spread - tradingFees - withdrawalPercent
    }

    /**
     * صافي الربح بالدولار = رأس المال × صافي الربح% ÷ 100.
     * مثال للتأكيد: رأس مال $100 وصافي ربح 2% => 100 × 2 ÷ 100 = $2 بالظبط (مش آلاف الدولارات).
     * القيمة دي أصلاً بترجع null لو [netProfitPercent] رجعت null (بيانات غير متاحة/غير منطقية).
     */
    fun netProfitUsd(capitalUsd: Double): Double? {
        val netPercent = netProfitPercent(capitalUsd) ?: return null
        return capitalUsd * netPercent / 100.0
    }

    companion object {
        /** أقصى فرق سعر منطقي بين منصتين (Spot/Spot) بالنسبة المئوية - أي فرق أكبر منه يتم إلغاؤه كبيانات غير موثوقة */
        const val MAX_PLAUSIBLE_SPREAD_PERCENT = 10.0
    }
}
