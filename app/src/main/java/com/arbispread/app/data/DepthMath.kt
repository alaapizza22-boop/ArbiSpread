package com.arbispread.app.data

import com.arbispread.app.model.OrderBookLevel

/**
 * حساب VWAP (متوسط سعر التنفيذ المرجّح بالحجم) لمبلغ/كمية *ثابتة* بالمشي فعلياً
 * عبر مستويات دفتر الطلبات الحقيقية، بدل الاعتماد على "أفضل سعر" فقط (Top of Book).
 *
 * ده مختلف عن [LiquidityRepository] الموجود، اللي بيحسب "أقصى مبلغ قابل للتنفيذ" عند حد
 * انزلاق معين. الأداة دي بالعكس: بتاخد مبلغ/كمية ثابتة (مثلاً "عندي 1000$ فعلاً") وبترجع
 * "قد ايه هتاخد فعلياً لو نفذت المبلغ ده دلوقتي"، وهي اللي محتاجينها لسلسلة التداول الثلاثي
 * (3 خطوات متتالية، كل خطوة مبلغها بيطلع من نتيجة اللي قبلها).
 */
object DepthMath {

    /** نتيجة "مشي" عبر دفتر الطلبات لمبلغ/كمية ثابتة */
    data class WalkResult(
        /** الكمية الفعلية اللي هتتنفذ (ممكن تكون أقل من المطلوب لو العمق المتاح خلص) */
        val filledQuantity: Double,
        /** متوسط سعر التنفيذ الفعلي (VWAP) - null لو مفيش عمق خالص */
        val avgPrice: Double?,
        /** true لو العمق المتاح في اللقطة خلص قبل ما نكمل المبلغ/الكمية المطلوبة بالكامل */
        val depthExhausted: Boolean,
    )

    /**
     * بتشتري بمبلغ دولار ثابت (quoteAmountToSpend) من جانب الـ Asks (عروض البيع)،
     * بالمشي عبر المستويات من الأرخص للأغلى، وترجع إجمالي الكمية (Base) اللي هتتشترى فعلياً.
     */
    fun buyWithQuoteAmount(asks: List<OrderBookLevel>, quoteAmountToSpend: Double): WalkResult {
        if (quoteAmountToSpend <= 0.0) return WalkResult(0.0, null, false)
        val sortedAsks = asks.sortedBy { it.price }
        var remainingQuote = quoteAmountToSpend
        var totalBase = 0.0
        var totalQuoteSpent = 0.0

        for (level in sortedAsks) {
            if (remainingQuote <= 0.0) break
            val levelQuoteCapacity = level.price * level.quantity
            if (levelQuoteCapacity >= remainingQuote) {
                // المستوى ده كافي لإكمال المبلغ المطلوب
                val baseBought = remainingQuote / level.price
                totalBase += baseBought
                totalQuoteSpent += remainingQuote
                remainingQuote = 0.0
            } else {
                // بناخد المستوى كله ونكمل على اللي بعده
                totalBase += level.quantity
                totalQuoteSpent += levelQuoteCapacity
                remainingQuote -= levelQuoteCapacity
            }
        }

        val depthExhausted = remainingQuote > 0.0001 // باقي مبلغ متنفذش - العمق في اللقطة خلص
        if (totalBase <= 0.0) return WalkResult(0.0, null, depthExhausted)
        return WalkResult(filledQuantity = totalBase, avgPrice = totalQuoteSpent / totalBase, depthExhausted = depthExhausted)
    }

    /**
     * بتبيع كمية (Base) ثابتة من جانب الـ Bids (عروض الشراء)، بالمشي عبر المستويات من
     * الأغلى للأرخص، وترجع إجمالي المبلغ (Quote) اللي هتاخده فعلياً.
     */
    fun sellBaseAmount(bids: List<OrderBookLevel>, baseAmountToSell: Double): WalkResult {
        if (baseAmountToSell <= 0.0) return WalkResult(0.0, null, false)
        val sortedBids = bids.sortedByDescending { it.price }
        var remainingBase = baseAmountToSell
        var totalQuoteReceived = 0.0
        var totalBaseSold = 0.0

        for (level in sortedBids) {
            if (remainingBase <= 0.0) break
            if (level.quantity >= remainingBase) {
                totalQuoteReceived += remainingBase * level.price
                totalBaseSold += remainingBase
                remainingBase = 0.0
            } else {
                totalQuoteReceived += level.quantity * level.price
                totalBaseSold += level.quantity
                remainingBase -= level.quantity
            }
        }

        val depthExhausted = remainingBase > 0.0000001
        if (totalBaseSold <= 0.0) return WalkResult(0.0, null, depthExhausted)
        return WalkResult(filledQuantity = totalQuoteReceived, avgPrice = totalQuoteReceived / totalBaseSold, depthExhausted = depthExhausted)
    }
}
