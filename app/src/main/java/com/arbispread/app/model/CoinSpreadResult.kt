package com.arbispread.app.model

/** سعر عملة على منصة واحدة، مع عمولة التداول وعمولة سحب الشبكة اللي المستخدم أدخلها فعلياً */
data class ExchangeQuote(
    val exchangeName: String,
    val price: Double,
    val takerFeePercent: Double,
    val withdrawalFeeUsd: Double,
    /**
     * true لو withdrawalFeeUsd جاي من جدول [WithdrawalFees] المرجعي (رسم حقيقي بوحدة
     * العملة × السعر الحي). false لو جاي من القيمة اليدوية العامة في الإعدادات (غير
     * محقق لهذه العملة بالذات) - الواجهة لازم تعرض تحذير صريح في الحالة دي، مش تعامله
     * كأنه رقم مؤكد.
     */
    val isWithdrawalFeeVerified: Boolean,
)

/**
 * تفصيل الحساب بالدولار خطوة بخطوة - كل رقم فيه قابل للتدقيق يدوياً بالآلة الحاسبة،
 * عشان ميبقاش فيه أي "رقم من عندنا" غير واضح من فين جه.
 *
 * المعادلة بالكامل:
 *   grossProfitUsd  = رأس المال × فرق السعر% ÷ 100   (الربح النظري - قبل أي رسوم)
 *   buyFeeUsd       = رأس المال × عمولة الشراء (فتح المركز)% ÷ 100  (مرة واحدة فقط)
 *   sellFeeUsd      = رأس المال × عمولة البيع (إغلاق المركز)% ÷ 100  (مرة واحدة فقط)
 *   withdrawalFeeUsd = رسم سحب الشبكة من منصة الشراء فقط (مش المنصتين) - لأن التحويل
 *                      الفعلي بيحصل مرة واحدة من منصة الشراء الأرخص لمنصة البيع الأغلى
 *   netProfitUsd    = grossProfitUsd − buyFeeUsd − sellFeeUsd − withdrawalFeeUsd  (الصافي المتوقع)
 *
 * ملاحظات مهمة عن حدود الحساب:
 * - العمولة المستخدمة هي سعر Taker (أوامر فورية) لأن آربيتراج محتاج تنفيذ مضمون في
 *   نفس اللحظة؛ معظم المنصات الخمس عندها Maker = Taker في المستوى العادي أصلاً،
 *   فمفيش داعي لحقل Maker منفصل لحد ما يبقى فيه فرق حقيقي موثق.
 * - الصفقة سبوت فورية بس (مفيش فاندينج هنا خالص - ده موجود في تبويب الفاندينج لوحده).
 * - الحساب بالقيمة الدولارية (Notional) مباشرة، مش بتحويل لكمية عملة، فمفيش تأثير
 *   لاختلاف دقة الكسور (Lot Size / Step Size) بين المنصات.
 * - مفيش احتساب مزدوج: رسم الفتح مرة على منصة الشراء، ورسم الإغلاق مرة على منصة
 *   البيع، ورسم سحب واحد بس (منصة الشراء) - مش اتنين.
 */
data class FeeBreakdown(
    val capitalUsd: Double,
    val spreadPercent: Double,
    val grossProfitUsd: Double,
    val buyFeePercent: Double,
    val buyFeeUsd: Double,
    val sellFeePercent: Double,
    val sellFeeUsd: Double,
    val withdrawalFeeUsd: Double,
    val isWithdrawalFeeVerified: Boolean,
    val netProfitUsd: Double,
) {
    /** صافي الربح كنسبة مئوية من رأس المال - محسوبة من الدولار الصافي، مش العكس */
    val netProfitPercent: Double
        get() = if (capitalUsd > 0.0) (netProfitUsd / capitalUsd) * 100.0 else 0.0
}

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
     * تفصيل الحساب بالدولار كامل وشفاف - دي المصدر الوحيد للحقيقة (Single Source of Truth)
     * لأي رقم ربح بيظهر في الواجهة. راجع توثيق [FeeBreakdown] فوق للمعادلة بالظبط.
     * بترجع null لو البيانات مش متاحة، الفرق غير منطقي، أو رأس المال صفر/سالب.
     */
    fun feeBreakdown(capitalUsd: Double): FeeBreakdown? {
        if (!isAvailable) return null
        if (capitalUsd <= 0.0) return null
        val spread = spreadPercent ?: return null
        val b = buy ?: return null
        val s = sell ?: return null

        val grossProfitUsd = capitalUsd * spread / 100.0
        val buyFeeUsd = capitalUsd * b.takerFeePercent / 100.0
        val sellFeeUsd = capitalUsd * s.takerFeePercent / 100.0
        val withdrawalFeeUsd = b.withdrawalFeeUsd
        val netProfitUsd = grossProfitUsd - buyFeeUsd - sellFeeUsd - withdrawalFeeUsd

        return FeeBreakdown(
            capitalUsd = capitalUsd,
            spreadPercent = spread,
            grossProfitUsd = grossProfitUsd,
            buyFeePercent = b.takerFeePercent,
            buyFeeUsd = buyFeeUsd,
            sellFeePercent = s.takerFeePercent,
            sellFeeUsd = sellFeeUsd,
            withdrawalFeeUsd = withdrawalFeeUsd,
            isWithdrawalFeeVerified = b.isWithdrawalFeeVerified,
            netProfitUsd = netProfitUsd,
        )
    }

    /** صافي الربح بالدولار = [feeBreakdown] . netProfitUsd */
    fun netProfitUsd(capitalUsd: Double): Double? = feeBreakdown(capitalUsd)?.netProfitUsd

    /** صافي الربح كنسبة مئوية من رأس المال = [feeBreakdown] . netProfitPercent */
    fun netProfitPercent(capitalUsd: Double): Double? = feeBreakdown(capitalUsd)?.netProfitPercent

    companion object {
        /** أقصى فرق سعر منطقي بين منصتين (Spot/Spot) بالنسبة المئوية - أي فرق أكبر منه يتم إلغاؤه كبيانات غير موثوقة */
        const val MAX_PLAUSIBLE_SPREAD_PERCENT = 10.0
    }
}
