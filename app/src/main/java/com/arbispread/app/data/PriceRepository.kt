package com.arbispread.app.data

import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.ExchangeQuote
import com.arbispread.app.model.WithdrawalFees
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** نتيجة تحديث واحدة شاملة: مقارنة السبريد بين المنصات لكل العملات */
data class PriceRefreshResult(
    val spreadResults: List<CoinSpreadResult>,
)

class PriceRepository(private val coins: List<CoinDef>) {

    /**
     * بيسحب الأسعار من المنصات السبع (كلها أزواج USDT) بالتوازي مرة واحدة بس، وبعدين
     * يبني مقارنة السبريد بين المنصات لكل عملة.
     */
    suspend fun refreshAll(
        fees: FeeConfig,
        /** مفاتيح المستخدم الشخصية المفعّلة (لو موجودة) - بتتستخدم بس لـ Binance/Bybit/OKX
         * عشان تجيب رسوم سحب وحالة محفظة حقيقية بنداء موقّع واحد بس لكل منصة (مش لكل عملة) */
        apiKeys: Map<String, ExchangeApiKey> = emptyMap(),
    ): PriceRefreshResult = coroutineScope {
        val binanceDeferred = async { ExchangeClients.fetchBinance() }
        val bybitDeferred = async { ExchangeClients.fetchBybit() }
        val okxDeferred = async { ExchangeClients.fetchOkx() }
        val kucoinDeferred = async { ExchangeClients.fetchKucoin() }
        val gateioDeferred = async { ExchangeClients.fetchGateio() }
        val bitgetDeferred = async { ExchangeClients.fetchBitget() }
        val mexcDeferred = async { ExchangeClients.fetchMexc() }

        val binance = binanceDeferred.await()
        val bybit = bybitDeferred.await()
        val okx = okxDeferred.await()
        val kucoin = kucoinDeferred.await()
        val gateio = gateioDeferred.await()
        val bitget = bitgetDeferred.await()
        val mexc = mexcDeferred.await()

        // رسوم سحب وحالة محفظة حقيقية - نداء موقّع واحد بس لكل منصة من الثلاثة دي لو
        // المستخدم معاه مفتاح شخصي مُفعّل لها (مش لكل عملة على حدة، عشان نحترم حدود الطلبات)
        val liveWithdrawalDeferred = listOf("Binance", "Bybit", "OKX").associateWith { exchangeName ->
            async {
                val key = apiKeys[exchangeName]
                if (key != null && key.isConfigured) {
                    ExchangeClients.fetchWalletStatusWithKey(exchangeName, key)
                } else {
                    emptyMap()
                }
            }
        }
        val liveWithdrawal = liveWithdrawalDeferred.mapValues { it.value.await() }

        val spreadResults = coins.map { coin ->
            /**
             * رسوم السحب لكل Quote بتتحسب بالأولوية التالية لكل منصة على حدة:
             * 1. بيانات حية حقيقية من نداء موقّع بمفتاح المستخدم الشخصي (Binance/Bybit/OKX
             *    بس، ولو المستخدم مُدخل مفتاح مُفعّل) -> "محقق حي" (isWithdrawalFeeVerified = true).
             * 2. جدول [WithdrawalFees] المرجعي الثابت -> "محقق" (مش حي، لكن مش مختلق).
             * 3. القيمة اليدوية العامة من الإعدادات (افتراضياً صفر) -> "غير محقق".
             */
            fun withdrawalFeeFor(exchangeName: String, price: Double): Pair<Double, Boolean> {
                val live = liveWithdrawal[exchangeName]?.get(coin.id)?.withdrawFeeNative
                if (live != null) return (live * price) to true
                val reference = WithdrawalFees.estimateUsd(coin.id, price)
                if (reference != null) return reference to true
                return fees.withdrawalFeeUsd to false
            }

            val quotes = buildList {
                binance[coin.binanceSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("Binance", it)
                    add(ExchangeQuote("Binance", it, fees.binance, fee, verified))
                }
                bybit[coin.bybitSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("Bybit", it)
                    add(ExchangeQuote("Bybit", it, fees.bybit, fee, verified))
                }
                okx[coin.okxInstId]?.let {
                    val (fee, verified) = withdrawalFeeFor("OKX", it)
                    add(ExchangeQuote("OKX", it, fees.okx, fee, verified))
                }
                kucoin[coin.kucoinSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("KuCoin", it)
                    add(ExchangeQuote("KuCoin", it, fees.kucoin, fee, verified))
                }
                gateio[coin.gateioSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("Gate.io", it)
                    add(ExchangeQuote("Gate.io", it, fees.gateio, fee, verified))
                }
                bitget[coin.bitgetSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("Bitget", it)
                    add(ExchangeQuote("Bitget", it, fees.bitget, fee, verified))
                }
                mexc[coin.mexcSymbol]?.let {
                    val (fee, verified) = withdrawalFeeFor("MEXC", it)
                    add(ExchangeQuote("MEXC", it, fees.mexc, fee, verified))
                }
            }

            CoinSpreadResult(coin = coin, quotes = quotes)
        }

        PriceRefreshResult(spreadResults)
    }
}
