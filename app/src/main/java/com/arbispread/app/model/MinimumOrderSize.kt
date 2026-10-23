package com.arbispread.app.model

/**
 * الحد الأدنى النموذجي (Typical Minimum Notional) لحجم أمر Spot واحد بالدولار على كل
 * منصة - لو رأس المال أقل منه، المنصة بترفض الأمر فعلياً حتى لو الحساب هنا بيقول إن
 * الصفقة رابحة، لأن الفحص هنا مبني على نسبة% مش على قدرة المنصة تنفذ المبلغ أصلاً.
 *
 * **تنبيه صريح عن حدود الدقة - بالظبط زي [WithdrawalFees]:**
 * - دي قيم **نموذجية/مرجعية (Typical)** مبنية على الحد الأدنى المعتاد لمعظم أزواج USDT
 *   على كل منصة وقت كتابة الكود - **مش** نداء حي لكل عملة على حدة (كان محتاج نداء
 *   إضافي لكل عملة من كل منصة، وده مكلف جداً على حدود الطلبات/Rate Limits).
 * - الحد الفعلي بيختلف شوية من عملة لعملة على نفس المنصة (عملات قليلة السيولة أحياناً
 *   حدها الأدنى أعلى شوية من المعتاد)، فالرقم هنا **تحذير استرشادي** مش ضمان دقيق 100%.
 * - **لازم تتأكد بنفسك** من صفحة التداول الفعلية في المنصة قبل أي قرار حقيقي، خصوصاً
 *   لو رأس مالك قريب من الحد المعروض هنا.
 */
object MinimumOrderSize {

    /** الحد الأدنى النموذجي بالدولار لمعظم أزواج USDT Spot على كل منصة */
    private val typicalMinUsd: Map<String, Double> = mapOf(
        "Binance" to 5.0,
        "Bybit" to 5.0,
        "OKX" to 1.0,
        "KuCoin" to 0.1,
        "Gate.io" to 1.0,
        "Bitget" to 5.0,
        "MEXC" to 1.0,
    )

    fun minUsdFor(exchangeName: String): Double? = typicalMinUsd[exchangeName]

    /** بيرجع اسم أول منصة (من بين الاتنين) رأس المال المحدد أقل من حدها الأدنى النموذجي - null لو مفيش مشكلة */
    fun firstBelowMinimum(capitalUsd: Double, buyExchange: String, sellExchange: String): Pair<String, Double>? {
        minUsdFor(buyExchange)?.let { min -> if (capitalUsd < min) return buyExchange to min }
        minUsdFor(sellExchange)?.let { min -> if (capitalUsd < min) return sellExchange to min }
        return null
    }
}
