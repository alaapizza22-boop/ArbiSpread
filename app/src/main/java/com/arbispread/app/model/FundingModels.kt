package com.arbispread.app.model

/**
 * معدل فاندينج عملة معينة على منصة فيوتشرز معينة.
 *
 * [fundingIntervalHours]: فترة السداد الحقيقية لهذا الرمز بالذات، مسحوبة من الـ API
 * (`/fapi/v1/fundingInfo` على Binance، `/v5/market/instruments-info` على Bybit) - مش
 * افتراض ثابت "كل العملات كل 8 ساعات". بعض العملات (خصوصاً الأكثر تقلباً) فترتها 4
 * ساعات أو حتى ساعة واحدة على بعض المنصات، وده بيغيّر عدد مرات التسوية يومياً فعلياً.
 * لو الرمز مش موجود في رد الـ API الخاص بالفترات غير المعيارية، الافتراضي القياسي
 * المعلن من المنصة نفسها هو 8 ساعات (مش رقم من عندنا).
 */
data class FundingQuote(
    val exchangeName: String,
    val fundingRatePercent: Double,   // النسبة لكل فترة سداد واحدة (مش يومية)
    val nextFundingTimeMillis: Long,  // وقت السداد القادم (Unix millis)، أو 0 لو غير معروف
    val fundingIntervalHours: Double, // فترة السداد الحقيقية لهذا الرمز، من الـ API
) {
    /** عدد مرات السداد المتوقعة في اليوم بناءً على الفترة الحقيقية (مش رقم ثابت 3) */
    val periodsPerDay: Double
        get() = if (fundingIntervalHours > 0.0) 24.0 / fundingIntervalHours else 0.0
}

/**
 * تفصيل ربح/تكلفة فاندينج آربيتراج كامل وشفاف بالدولار - كل رقم قابل للتدقيق يدوياً.
 *
 * **ملاحظة مهمة عن طبيعة الصفقة**: على عكس آربيتراج الأسعار الفوري، فاندينج آربيتراج
 * مش محتاج تحويل العملة فعلياً بين المنصتين (مفيش سحب/إيداع) - إنت بتفتح مركز Long
 * بهامش على منصة وShort بهامش على التانية وبتسيبهم مفتوحين، فمفيش "عمولة سحب شبكة"
 * هنا خالص، العمولة الوحيدة هي عمولة فتح/قفل المركزين (Taker Fee على الفيوتشرز).
 *
 * **تحذير الرافعة المالية (Leverage)**: الحساب هنا مبني على قيمة الصفقة الاسمية
 * (Notional) = رأس المال اللي أدخلته، من غير مضاعفة برافعة مالية، لأن التطبيق معندوش
 * طريقة يعرف إعداد الرافعة بتاعك. لو بتستخدم رافعة، احسب هامشك الفعلي = رأس المال ÷ الرافعة.
 */
data class FundingFeeBreakdown(
    val capitalUsd: Double,
    val dailyGrossProfitPercent: Double,
    val dailyGrossProfitUsd: Double,
    val longTakerFeePercent: Double,
    val shortTakerFeePercent: Double,
    /** عمولة فتح + قفل المركزين مجتمعين (مرة واحدة فقط، مش يومية) */
    val roundTripFeeUsd: Double,
    /** صافي ربح أول دورة (دخول وخروج في نفس اليوم) = الربح اليومي الخام − عمولة الدخول والخروج كاملة */
    val firstCycleNetProfitUsd: Double,
    /** صافي الربح اليومي لو فاضل مركزك مفتوح من غير ما تدخل/تخرج تاني (من غير عمولات متكررة) */
    val ongoingDailyNetProfitUsd: Double,
)

/** فرصة فاندينج آربيتراج لعملة واحدة عبر منصتين فيوتشرز أو أكثر */
data class FundingOpportunity(
    val coin: CoinDef,
    val quotes: List<FundingQuote>,
) {
    /** المنصة اللي معدل الفاندينج فيها الأقل (أو الأكثر سلبية) -> تفتح عليها لونج */
    val longExchange: FundingQuote? = if (quotes.size >= 2) quotes.minByOrNull { it.fundingRatePercent } else null

    /** المنصة اللي معدل الفاندينج فيها الأعلى -> تفتح عليها شورت */
    val shortExchange: FundingQuote? = if (quotes.size >= 2) quotes.maxByOrNull { it.fundingRatePercent } else null

    val isAvailable: Boolean
        get() = longExchange != null && shortExchange != null && longExchange.exchangeName != shortExchange.exchangeName

    /** الفارق الزمني بين موعد تسوية الساقين (ميلي ثانية) - كل ما يكون أصغر، كل ما الدخول/الخروج أسهل يتزامن */
    val syncGapMillis: Long?
        get() {
            val l = longExchange?.nextFundingTimeMillis?.takeIf { it > 0 } ?: return null
            val s = shortExchange?.nextFundingTimeMillis?.takeIf { it > 0 } ?: return null
            return kotlin.math.abs(l - s)
        }

    /** true لو الفارق بين موعدي التسوية أقل من 5 دقايق - يعني الساقين عملياً بيتقفلوا مع بعض */
    val isSynced: Boolean
        get() = (syncGapMillis ?: Long.MAX_VALUE) <= SYNC_THRESHOLD_MILLIS

    /**
     * صافي الربح/الخسارة لكل ساق لكل فترة سداد واحدة بتاعتها:
     * - اللي إنت Long فيه: بتدفع لو المعدل موجب، وتاخد لو سالب -> المساهمة = −المعدل
     * - اللي إنت Short فيه: بتاخد لو المعدل موجب، وتدفع لو سالب -> المساهمة = +المعدل
     * وبعدين كل ساق بتتضرب في عدد مرات السداد الحقيقي بتاعها هي (مش رقم موحّد لكل العملات).
     */
    fun estimatedDailyProfitPercent(): Double? {
        val l = longExchange ?: return null
        val s = shortExchange ?: return null
        val longContribution = -l.fundingRatePercent * l.periodsPerDay
        val shortContribution = s.fundingRatePercent * s.periodsPerDay
        return longContribution + shortContribution
    }

    fun estimatedDailyProfitUsd(capitalUsd: Double): Double? {
        val dailyPercent = estimatedDailyProfitPercent() ?: return null
        return capitalUsd * dailyPercent / 100.0
    }

    /**
     * التفصيل الكامل الشفاف بالدولار، شامل عمولات فتح/قفل المركزين الفعلية (مش رقم مفروض).
     * [longTakerFeePercent]/[shortTakerFeePercent] بييجوا من إعدادات المستخدم (FeeConfig)،
     * القيم الافتراضية فيها هي السعر المعياري المنشور رسمياً، قابلة للتعديل لمستوى حسابك.
     */
    fun feeBreakdown(capitalUsd: Double, longTakerFeePercent: Double, shortTakerFeePercent: Double): FundingFeeBreakdown? {
        if (!isAvailable || capitalUsd <= 0.0) return null
        val dailyPercent = estimatedDailyProfitPercent() ?: return null
        val dailyGrossUsd = capitalUsd * dailyPercent / 100.0

        // عمولة فتح المركزين + عمولة قفلهم (مرتين: دخول وخروج) على نفس القيمة الاسمية لكل ساق
        val openFeeUsd = capitalUsd * (longTakerFeePercent + shortTakerFeePercent) / 100.0
        val closeFeeUsd = openFeeUsd
        val roundTripFeeUsd = openFeeUsd + closeFeeUsd

        return FundingFeeBreakdown(
            capitalUsd = capitalUsd,
            dailyGrossProfitPercent = dailyPercent,
            dailyGrossProfitUsd = dailyGrossUsd,
            longTakerFeePercent = longTakerFeePercent,
            shortTakerFeePercent = shortTakerFeePercent,
            roundTripFeeUsd = roundTripFeeUsd,
            firstCycleNetProfitUsd = dailyGrossUsd - roundTripFeeUsd,
            ongoingDailyNetProfitUsd = dailyGrossUsd,
        )
    }

    companion object {
        const val SYNC_THRESHOLD_MILLIS = 5 * 60 * 1000L // 5 دقايق
        const val DEFAULT_FUNDING_INTERVAL_HOURS = 8.0 // القياسي المعلن من المنصات لو الرمز مش في رد الفترات الخاصة
    }
}
