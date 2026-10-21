package com.arbispread.app.model

/** أفضل سعر بيع (Bid) وأفضل سعر شراء (Ask) متاحين فعلياً للتنفيذ اللحظي - مش "آخر صفقة" (Last Price) اللي ممكن تبقى قديمة على عملة ضعيفة السيولة */
data class BookQuote(val bidPrice: Double, val askPrice: Double) {
    /** نسبة الفرق بين Bid وAsk - مؤشر سيولة الزوج؛ فرق كبير جداً يعني الزوج شبه ميت وأي حساب عليه مش واقعي */
    val spreadPercent: Double
        get() = if (bidPrice > 0.0) (askPrice - bidPrice) / bidPrice * 100.0 else Double.MAX_VALUE
}

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
 * **كل حساب هنا على أفضل سعر تنفيذ فعلي (Best Ask/Bid)، مش "آخر صفقة" (Last Price):**
 * الشراء دايماً بسعر Ask (أعلى شوية)، والبيع دايماً بسعر Bid (أقل شوية) - ده الفرق
 * الحقيقي اللي هتدفعه فعلاً لو نفذت الصفقة دلوقتي، مش سعر نظري متفائل.
 *
 * الاتجاهين المحسوبين:
 *   - Forward: USDT → [bridgeCoin] → [targetCoin] → USDT
 *   - Reverse: USDT → [targetCoin] → [bridgeCoin] → USDT
 * وبيتعرض الأفضل بينهم بس.
 *
 * ملحوظة مهمة عن حدود الدقة الحالية: الأرقام هنا مبنية على **أفضل سعر فقط** (Top of
 * Book)، من غير فحص عمق كامل لدفتر الطلبات (Full Depth / VWAP) - يعني لو رأس مالك
 * كبير نسبياً لسيولة الزوج، ممكن السعر الفعلي وقت التنفيذ يكون أسوأ من المعروض هنا.
 * فحص العمق الكامل والانزلاق السعري (Slippage) قبل أي تنفيذ حقيقي خطوة منفصلة قادمة.
 */
data class TriangularQuotes(
    val exchangeName: String,
    val bridgeCoin: String,
    val targetCoin: CoinDef,
    val bridgeBook: BookQuote,   // [bridgeCoin]/USDT
    val crossBook: BookQuote,    // [targetCoin]/[bridgeCoin] - كام [bridgeCoin] للوحدة الواحدة من [targetCoin]
    val targetBook: BookQuote,   // [targetCoin]/USDT
    val takerFeePercent: Double, // عمولة Taker الموحدة لنفس المنصة - بتتخصم 3 مرات (خطوة لكل صفقة)
) {
    private fun flow(capitalUsd: Double, forward: Boolean): TriangularLegFlow {
        val feeMultiplier = 1.0 - takerFeePercent / 100.0
        return if (forward) {
            // الخطوة 1: USDT -> bridgeCoin (شراء -> بسعر Ask)
            val bridgeGross = capitalUsd / bridgeBook.askPrice
            val bridgeNet = bridgeGross * feeMultiplier
            // الخطوة 2: bridgeCoin -> targetCoin (شراء targetCoin -> بسعر Ask لزوج التقاطع)
            val targetGross = bridgeGross / crossBook.askPrice
            val targetNet = bridgeNet / crossBook.askPrice * feeMultiplier
            // الخطوة 3: targetCoin -> USDT (بيع -> بسعر Bid)
            val finalGross = targetGross * targetBook.bidPrice
            val finalNet = targetNet * targetBook.bidPrice * feeMultiplier
            TriangularLegFlow(capitalUsd, finalNet, finalGross)
        } else {
            // الخطوة 1: USDT -> targetCoin (شراء -> بسعر Ask)
            val targetGross = capitalUsd / targetBook.askPrice
            val targetNet = targetGross * feeMultiplier
            // الخطوة 2: targetCoin -> bridgeCoin (بيع targetCoin -> بسعر Bid لزوج التقاطع)
            val bridgeGross = targetGross * crossBook.bidPrice
            val bridgeNet = targetNet * crossBook.bidPrice * feeMultiplier
            // الخطوة 3: bridgeCoin -> USDT (بيع -> بسعر Bid)
            val finalGross = bridgeGross * bridgeBook.bidPrice
            val finalNet = bridgeNet * bridgeBook.bidPrice * feeMultiplier
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
     * فحص سلامة البيانات الثلاثة قبل أي عرض - لو أي زوج من التلاتة سبريده (الفرق بين
     * Bid وAsk) أكبر من [MAX_LEG_SPREAD_PERCENT]، ده معناه الزوج شبه ميت (سيولة ضعيفة
     * جداً أو مش بيتداول فعلياً)، وأي حساب عليه مش واقعي خالص - فبنرفض الفرصة دي من
     * الأساس بدل ما نعرضها برقم مش حقيقي.
     */
    val hasReliableLegs: Boolean
        get() = bridgeBook.spreadPercent <= MAX_LEG_SPREAD_PERCENT &&
            crossBook.spreadPercent <= MAX_LEG_SPREAD_PERCENT &&
            targetBook.spreadPercent <= MAX_LEG_SPREAD_PERCENT

    /**
     * فحص سلامة النتيجة النهائية: حتى مع سبريد معقول لكل زوج على حدة، فرص التداول
     * الثلاثي الحقيقية نادراً ما تتعدى 1-2% حتى قبل الرسوم. أي صافي أكبر من
     * [MAX_PLAUSIBLE_PERCENT] غالباً خطأ لحظي في أحد الأسعار الثلاثة - مش فرصة حقيقية.
     */
    fun isPlausible(capitalUsd: Double): Boolean {
        if (!hasReliableLegs) return false
        val (_, best) = bestFlow(capitalUsd)
        return kotlin.math.abs(best.netProfitPercent) <= MAX_PLAUSIBLE_PERCENT
    }

    companion object {
        const val MAX_PLAUSIBLE_PERCENT = 5.0
        const val MAX_LEG_SPREAD_PERCENT = 5.0
    }
}
