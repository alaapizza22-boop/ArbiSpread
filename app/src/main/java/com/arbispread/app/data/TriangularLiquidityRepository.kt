package com.arbispread.app.data

import com.arbispread.app.model.TriangularExecutionResult
import com.arbispread.app.model.TriangularQuotes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * "التحقق قبل التنفيذ" للتداول الثلاثي: بيسحب عمق دفتر الطلبات الحقيقي للثلاث أزواج
 * (Bridge/USDT, Target/Bridge, Target/USDT) وقت الطلب بس (مش كل تحديث تلقائي، عشان
 * نحترم حدود طلبات المنصات)، وبيحسب النتيجة الفعلية بعد "المشي" عبر العمق الحقيقي
 * (VWAP) لمبلغ رأس المال المحدد - بدل افتراض إنك هتاخد أفضل سعر بس للمبلغ كله.
 */
object TriangularLiquidityRepository {

    suspend fun checkExecution(quote: TriangularQuotes, capitalUsd: Double): TriangularExecutionResult = coroutineScope {
        val (forward, topFlow) = quote.bestFlow(capitalUsd)

        val bridgeDepthDeferred = async { ExchangeClients.fetchDepthForSymbol(quote.exchangeName, quote.bridgeSymbol) }
        val crossDepthDeferred = async { ExchangeClients.fetchDepthForSymbol(quote.exchangeName, quote.crossSymbol) }
        val targetDepthDeferred = async { ExchangeClients.fetchDepthForSymbol(quote.exchangeName, quote.targetSymbol) }

        val bridgeDepth = bridgeDepthDeferred.await()
        val crossDepth = crossDepthDeferred.await()
        val targetDepth = targetDepthDeferred.await()

        if (bridgeDepth == null || crossDepth == null || targetDepth == null) {
            return@coroutineScope TriangularExecutionResult(
                exchangeName = quote.exchangeName,
                targetCoinId = quote.targetCoin.id,
                forward = forward,
                capitalUsd = capitalUsd,
                topOfBookNetProfitUsd = topFlow.netProfitUsd,
                depthAwareFinalUsd = null,
                depthAwareNetProfitUsd = null,
                depthExhausted = false,
                wouldBeLossFromSlippage = false,
                errorMessage = "تعذر سحب عمق دفتر الطلبات لأحد الأزواج الثلاثة - حاول تاني",
                checkedAtMillis = System.currentTimeMillis(),
            )
        }

        val feeMultiplier = 1.0 - quote.takerFeePercent / 100.0
        var depthExhausted = false
        var finalUsd: Double? = null

        if (forward) {
            // الخطوة 1: USDT -> bridgeCoin (شراء من جانب Asks لزوج Bridge/USDT بكل رأس المال)
            val step1 = DepthMath.buyWithQuoteAmount(bridgeDepth.asks, capitalUsd)
            depthExhausted = depthExhausted || step1.depthExhausted
            val bridgeQty = step1.filledQuantity * feeMultiplier
            if (bridgeQty > 0.0) {
                // الخطوة 2: bridgeCoin -> targetCoin (شراء من جانب Asks لزوج Target/Bridge بكمية bridgeQty)
                val step2 = DepthMath.buyWithQuoteAmount(crossDepth.asks, bridgeQty)
                depthExhausted = depthExhausted || step2.depthExhausted
                val targetQty = step2.filledQuantity * feeMultiplier
                if (targetQty > 0.0) {
                    // الخطوة 3: targetCoin -> USDT (بيع من جانب Bids لزوج Target/USDT)
                    val step3 = DepthMath.sellBaseAmount(targetDepth.bids, targetQty)
                    depthExhausted = depthExhausted || step3.depthExhausted
                    if (step3.filledQuantity > 0.0) finalUsd = step3.filledQuantity * feeMultiplier
                }
            }
        } else {
            // الخطوة 1: USDT -> targetCoin (شراء من جانب Asks لزوج Target/USDT بكل رأس المال)
            val step1 = DepthMath.buyWithQuoteAmount(targetDepth.asks, capitalUsd)
            depthExhausted = depthExhausted || step1.depthExhausted
            val targetQty = step1.filledQuantity * feeMultiplier
            if (targetQty > 0.0) {
                // الخطوة 2: targetCoin -> bridgeCoin (بيع من جانب Bids لزوج Target/Bridge)
                val step2 = DepthMath.sellBaseAmount(crossDepth.bids, targetQty)
                depthExhausted = depthExhausted || step2.depthExhausted
                val bridgeQty = step2.filledQuantity * feeMultiplier
                if (bridgeQty > 0.0) {
                    // الخطوة 3: bridgeCoin -> USDT (بيع من جانب Bids لزوج Bridge/USDT)
                    val step3 = DepthMath.sellBaseAmount(bridgeDepth.bids, bridgeQty)
                    depthExhausted = depthExhausted || step3.depthExhausted
                    if (step3.filledQuantity > 0.0) finalUsd = step3.filledQuantity * feeMultiplier
                }
            }
        }

        val netProfitUsd = finalUsd?.minus(capitalUsd)
        // الصفقة كانت عارضة ربح على البطاقة (أفضل سعر فقط)، لكن الفحص الحقيقي بعمق
        // السوق كشف إنها هتبقى خسارة فعلياً - ده بالظبط السيناريو اللي لازم نحذّر منه بوضوح
        val wouldBeLoss = topFlow.netProfitUsd > 0.0 && netProfitUsd != null && netProfitUsd <= 0.0

        TriangularExecutionResult(
            exchangeName = quote.exchangeName,
            targetCoinId = quote.targetCoin.id,
            forward = forward,
            capitalUsd = capitalUsd,
            topOfBookNetProfitUsd = topFlow.netProfitUsd,
            depthAwareFinalUsd = finalUsd,
            depthAwareNetProfitUsd = netProfitUsd,
            depthExhausted = depthExhausted,
            wouldBeLossFromSlippage = wouldBeLoss,
            errorMessage = if (finalUsd == null) "تعذر حساب التنفيذ الفعلي - العمق المتاح غير كافٍ لرأس المال ده" else null,
            checkedAtMillis = System.currentTimeMillis(),
        )
    }
}
