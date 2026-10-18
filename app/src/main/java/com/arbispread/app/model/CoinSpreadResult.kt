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
    val quotes: List<ExchangeQuote>,   // كل الأسعار المتاحة من المنصات
) {
    val buy: ExchangeQuote? = if (quotes.size >= 2) quotes.minByOrNull { it.price } else null
    val sell: ExchangeQuote? = if (quotes.size >= 2) quotes.maxByOrNull { it.price } else null

    val isAvailable: Boolean
        get() = buy != null && sell != null && buy.exchangeName != sell.exchangeName

    /** الفرق الخام بين أعلى وأقل سعر، كنسبة مئوية */
    val spreadPercent: Double?
        get() {
            val b = buy?.price ?: return null
            val s = sell?.price ?: return null
            if (b <= 0.0) return null
            return (s - b) / b * 100.0
        }

    /**
     * صافي الربح كنسبة مئوية من رأس المال، بعد خصم:
     * - عمولة الشراء (Taker) على منصة الشراء
     * - عمولة البيع (Taker) على منصة البيع
     * - عمولة سحب الشبكة لتحويل العملة بين المنصتين (كنسبة من رأس المال المدخل)
     */
    fun netProfitPercent(capitalUsd: Double): Double? {
        val spread = spreadPercent ?: return null
        val b = buy ?: return null
        val s = sell ?: return null
        val tradingFees = b.takerFeePercent + s.takerFeePercent
        val withdrawalPercent = if (capitalUsd > 0.0) (b.withdrawalFeeUsd / capitalUsd) * 100.0 else 0.0
        return spread - tradingFees - withdrawalPercent
    }

    /** صافي الربح بالدولار، بناءً على رأس المال اللي المستخدم أدخله */
    fun netProfitUsd(capitalUsd: Double): Double? {
        val netPercent = netProfitPercent(capitalUsd) ?: return null
        return capitalUsd * netPercent / 100.0
    }
}
