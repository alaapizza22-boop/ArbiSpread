package com.arbispread.app.data

import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.LiquidityCheckResult
import com.arbispread.app.model.OrderBookSnapshot
import com.arbispread.app.model.WalletNetworkStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * فحص التنفيذ الحي لفرصة آربيتراج معينة: بيسحب دفتر الطلبات الفعلي من منصتي الشراء
 * والبيع وقت الطلب (مش كل تحديث تلقائي، عشان نحترم حدود طلبات المنصات)، وبيحسب
 * "أقصى مبلغ قابل للتنفيذ" + يفحص حالة المحافظ.
 */
object LiquidityRepository {

    /** الحد الأقصى الافتراضي للانزلاق السعري المقبول قبل ما نوقف حساب "المبلغ القابل للتنفيذ" */
    const val DEFAULT_MAX_SLIPPAGE_PERCENT = 0.5

    suspend fun checkExecution(
        coin: CoinDef,
        buyExchange: String,
        sellExchange: String,
        maxSlippagePercent: Double = DEFAULT_MAX_SLIPPAGE_PERCENT,
        /** مفتاح المستخدم الشخصي (لو مُدخل ومُفعّل) لمنصة الشراء/البيع - بيتستخدم بس لو
         * المنصة دي معندهاش Endpoint عام (Binance/Bybit/OKX)، عشان نجيب حالة محفظة
         * ورسم سحب حقيقيين بدل "غير محقق" */
        buyApiKey: ExchangeApiKey? = null,
        sellApiKey: ExchangeApiKey? = null,
    ): LiquidityCheckResult = coroutineScope {
        val buyDepthDeferred = async { ExchangeClients.fetchDepthFor(buyExchange, coin) }
        val sellDepthDeferred = async { ExchangeClients.fetchDepthFor(sellExchange, coin) }
        val buyWalletDeferred = async { fetchWalletStatus(buyExchange, coin.id, buyApiKey) }
        val sellWalletDeferred = async { fetchWalletStatus(sellExchange, coin.id, sellApiKey) }

        val buyDepth = buyDepthDeferred.await()
        val sellDepth = sellDepthDeferred.await()
        val buyWallet = buyWalletDeferred.await()
        val sellWallet = sellWalletDeferred.await()

        if (buyDepth == null || sellDepth == null) {
            return@coroutineScope LiquidityCheckResult(
                coinId = coin.id,
                buyExchange = buyExchange,
                sellExchange = sellExchange,
                maxSlippagePercent = maxSlippagePercent,
                maxExecutableCapitalUsd = null,
                depthExhausted = false,
                buyWalletStatus = buyWallet,
                sellWalletStatus = sellWallet,
                errorMessage = "تعذر سحب دفتر الطلبات من إحدى المنصتين - حاول تاني",
                checkedAtMillis = System.currentTimeMillis(),
            )
        }

        val (buyExecutableUsd, buyExhausted) = executableUsdOnAskSide(buyDepth, maxSlippagePercent)
        val (sellExecutableUsd, sellExhausted) = executableUsdOnBidSide(sellDepth, maxSlippagePercent)

        val maxExecutable = if (buyExecutableUsd != null && sellExecutableUsd != null) {
            minOf(buyExecutableUsd, sellExecutableUsd)
        } else null

        LiquidityCheckResult(
            coinId = coin.id,
            buyExchange = buyExchange,
            sellExchange = sellExchange,
            maxSlippagePercent = maxSlippagePercent,
            maxExecutableCapitalUsd = maxExecutable,
            depthExhausted = buyExhausted || sellExhausted,
            buyWalletStatus = buyWallet,
            sellWalletStatus = sellWallet,
            errorMessage = null,
            checkedAtMillis = System.currentTimeMillis(),
        )
    }

    /**
     * بيجيب حالة المحفظة: بيجرّب الـ Endpoint العام الأول (KuCoin/Gate.io) - لو موجود
     * بيرجّعه فوراً. لو مش موجود (Binance/Bybit/OKX) وفيه مفتاح شخصي مُفعّل للمنصة دي،
     * بيعمل نداء موقّع بمفتاح المستخدم. غير كده بترجع null ("غير قابل للتحقق").
     */
    private suspend fun fetchWalletStatus(exchangeName: String, coinId: String, key: ExchangeApiKey?): WalletNetworkStatus? {
        ExchangeClients.fetchWalletStatusFor(exchangeName, coinId)?.let { return it }
        if (key != null && key.isConfigured) {
            return ExchangeClients.fetchWalletStatusWithKey(exchangeName, key)[coinId]
        }
        return null
    }

    /**
     * بيحسب أقصى مبلغ بالدولار تقدر تشتري بيه من جانب الـ Asks (عروض البيع) من غير
     * ما يتجاوز متوسط سعر التنفيذ (VWAP) حد الانزلاق المسموح به فوق أفضل سعر (Best Ask).
     * بيرجع (المبلغ, هل العمق المتاح في اللقطة خلص قبل ما نوصل لحد الانزلاق).
     */
    private fun executableUsdOnAskSide(depth: OrderBookSnapshot, maxSlippagePercent: Double): Pair<Double?, Boolean> {
        val asks = depth.asks.sortedBy { it.price }
        val bestAsk = asks.firstOrNull()?.price ?: return null to false
        val maxAcceptablePrice = bestAsk * (1.0 + maxSlippagePercent / 100.0)

        var totalUsd = 0.0
        for (level in asks) {
            if (level.price > maxAcceptablePrice) return totalUsd to false // وصلنا لحد الانزلاق بنجاح، العمق كفى
            totalUsd += level.price * level.quantity
        }
        // العمق المتاح في اللقطة (آخر مستوى) خلص قبل ما نتخطى حد الانزلاق - الرقم ده "حد أدنى مؤكد"
        return totalUsd to true
    }

    /** نفس الفكرة بس من جانب الـ Bids (عروض الشراء) عشان نحسب أقصى مبلغ تقدر تبيعه */
    private fun executableUsdOnBidSide(depth: OrderBookSnapshot, maxSlippagePercent: Double): Pair<Double?, Boolean> {
        val bids = depth.bids.sortedByDescending { it.price }
        val bestBid = bids.firstOrNull()?.price ?: return null to false
        val minAcceptablePrice = bestBid * (1.0 - maxSlippagePercent / 100.0)

        var totalUsd = 0.0
        for (level in bids) {
            if (level.price < minAcceptablePrice) return totalUsd to false
            totalUsd += level.price * level.quantity
        }
        return totalUsd to true
    }
}
