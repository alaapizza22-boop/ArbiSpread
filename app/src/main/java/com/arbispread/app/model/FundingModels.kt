package com.arbispread.app.model

/** معدل فاندينج عملة معينة على منصة فيوتشرز معينة */
data class FundingQuote(
    val exchangeName: String,
    val fundingRatePercent: Double,   // مثلاً 0.01 تعني 0.01% كل فترة سداد (عادة كل 8 ساعات)
    val nextFundingTimeMillis: Long,  // وقت السداد القادم (Unix millis)، أو 0 لو غير معروف
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

    /** الفرق بين معدلي الفاندينج (اللي بتكسبه كل فترة سداد من الصفقتين مع بعض) */
    val spreadPercent: Double?
        get() {
            val l = longExchange?.fundingRatePercent ?: return null
            val s = shortExchange?.fundingRatePercent ?: return null
            return s - l
        }

    /** تقدير الربح اليومي (3 فترات سداد تقريباً كل 8 ساعات) كنسبة من رأس المال المستخدم في الصفقتين */
    fun estimatedDailyProfitPercent(): Double? {
        val spread = spreadPercent ?: return null
        return spread * 3.0
    }

    fun estimatedDailyProfitUsd(capitalUsd: Double): Double? {
        val dailyPercent = estimatedDailyProfitPercent() ?: return null
        return capitalUsd * dailyPercent / 100.0
    }
}
