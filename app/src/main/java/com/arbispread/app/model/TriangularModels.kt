package com.arbispread.app.model

/**
 * تدفق فلوس خطوة بخطوة لمسار تداول ثلاثي كامل - كل رقم فيه محسوب من اللي قبله مباشرة
 * (مش معادلة واحدة جامعة)، عشان يبقى قابل للتدقيق خطوة بخطوة زي حاسبة حقيقية.
 *
 * [grossFinalUsd] هو نفس المسار بالظبط من غير خصم أي عمولة - عشان نفصل "الفرصة النظرية"
 * (اختلاف الأسعار نفسه) عن "الصافي الفعلي بعد تكلفة التنفيذ الحقيقية" بوضوح.
 */
data class TriangularLegFlow(
    val capitalUsd: Double,
    val finalUsd: Double,
    val grossFinalUsd: Double,
) {
    val netProfitUsd: Double get() = finalUsd - capitalUsd
    val netProfitPercent: Double get() = if (capitalUsd > 0.0) netProfitUsd / capitalUsd * 100.0 else 0.0
    val grossProfitUsd: Double get() = grossFinalUsd - capitalUsd
    val grossProfitPercent: Double get() = if (capitalUsd > 0.0) grossProfitUsd / capitalUsd * 100.0 else 0.0

    /** الفرق بين النتيجة النظرية (من غير عمولات) والفعلية - يعني تكلفة الثلاث عمولات مجتمعة بالدولار */
    val totalFeesUsd: Double get() = grossFinalUsd - finalUsd
}

/**
 * فرصة تداول ثلاثي (Triangular Arbitrage) **داخل نفس المنصة** - من غير أي تحويل بين
 * منصتين ومن غير أي رسم سحب شبكة خالص. المسار دايماً عن طريق عملة جسر واحدة (BTC
 * حالياً، لأنها أكثر عملة متاحة كـ"زوج تقاطع" Cross Pair مع باقي العملات على الخمس
 * منصات) - لو زوج التقاطع (مثل ETHBTC) مش موجود فعلياً على المنصة، العملة دي ببساطة
 * مش بتظهر من المنصة دي بدل ما نخترع سعر.
 *
 * الاتجاهين المحسوبين:
 *   - Forward: USDT → [bridgeCoin] → [targetCoin] → USDT
 *   - Reverse: USDT → [targetCoin] → [bridgeCoin] → USDT
 * وبيتعرض الأفضل بينهم بس.
 */
data class TriangularQuotes(
    val exchangeName: String,
    val bridgeCoin: String,
    val targetCoin: CoinDef,
    val bridgeUsdtPrice: Double,   // سعر [bridgeCoin]/USDT
    val crossPrice: Double,        // سعر [targetCoin]/[bridgeCoin] - كام [bridgeCoin] للوحدة الواحدة من [targetCoin]
    val targetUsdtPrice: Double,   // سعر [targetCoin]/USDT
    val takerFeePercent: Double,   // عمولة Taker الموحدة لنفس المنصة - بتتخصم 3 مرات (خطوة لكل صفقة)
) {
    private fun flow(capitalUsd: Double, forward: Boolean): TriangularLegFlow {
        val feeMultiplier = 1.0 - takerFeePercent / 100.0
        return if (forward) {
            // الخطوة 1: USDT -> bridgeCoin
            val bridgeGross = capitalUsd / bridgeUsdtPrice
            val bridgeNet = bridgeGross * feeMultiplier
            // الخطوة 2: bridgeCoin -> targetCoin (crossPrice = bridgeCoin لكل وحدة targetCoin)
            val targetGross = bridgeGross / crossPrice
            val targetNet = bridgeNet / crossPrice * feeMultiplier
            // الخطوة 3: targetCoin -> USDT
            val finalGross = targetGross * targetUsdtPrice
            val finalNet = targetNet * targetUsdtPrice * feeMultiplier
            TriangularLegFlow(capitalUsd, finalNet, finalGross)
        } else {
            // الخطوة 1: USDT -> targetCoin
            val targetGross = capitalUsd / targetUsdtPrice
            val targetNet = targetGross * feeMultiplier
            // الخطوة 2: targetCoin -> bridgeCoin
            val bridgeGross = targetGross * crossPrice
            val bridgeNet = targetNet * crossPrice * feeMultiplier
            // الخطوة 3: bridgeCoin -> USDT
            val finalGross = bridgeGross * bridgeUsdtPrice
            val finalNet = bridgeNet * bridgeUsdtPrice * feeMultiplier
            TriangularLegFlow(capitalUsd, finalNet, finalGross)
        }
    }

    fun forwardFlow(capitalUsd: Double): TriangularLegFlow = flow(capitalUsd, forward = true)
    fun reverseFlow(capitalUsd: Double): TriangularLegFlow = flow(capitalUsd, forward = false)

    /** بيرجع true لو الاتجاه الأفضل Forward (USDT->Bridge->Target->USDT)، مع أفضل تدفق فلوس بين الاتجاهين */
    fun bestFlow(capitalUsd: Double): Pair<Boolean, TriangularLegFlow> {
        val f = forwardFlow(capitalUsd)
        val r = reverseFlow(capitalUsd)
        return if (f.netProfitUsd >= r.netProfitUsd) true to f else false to r
    }

    /**
     * فحص سلامة: فرص التداول الثلاثي الحقيقية نادراً ما تتعدى 1-2% حتى قبل الرسوم،
     * لأن السوق كفء جداً في أزواج التقاطع السائلة. أي رقم أكبر من [MAX_PLAUSIBLE_PERCENT]
     * غالباً سببه سعر متأخر أو خطأ لحظي في أحد الأسعار الثلاثة - مش فرصة حقيقية.
     */
    fun isPlausible(capitalUsd: Double): Boolean {
        val (_, best) = bestFlow(capitalUsd)
        return kotlin.math.abs(best.netProfitPercent) <= MAX_PLAUSIBLE_PERCENT
    }

    companion object {
        const val MAX_PLAUSIBLE_PERCENT = 5.0
    }
}
