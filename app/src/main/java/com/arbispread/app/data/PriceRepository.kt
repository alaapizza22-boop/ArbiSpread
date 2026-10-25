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
     * بيسحب الأسعار من المنصات الثمانية (كلها أزواج USDT) بالتوازي مرة واحدة بس، وبعدين
     * يبني مقارنة السبريد بين المنصات لكل عملة.
     */
    suspend fun refreshAll(
        fees: FeeConfig,
        /** مفاتيح المستخدم الشخصية المفعّلة (لو موجودة) - بتتستخدم لجلب رسوم سحب وعمولة
         * تداول حقيقية بنداء موقّع واحد بس لكل منصة (مش لكل عملة)، لأي منصة من الثمانية
         * المستخدم مدخّل لها مفتاح مُفعّل */
        apiKeys: Map<String, ExchangeApiKey> = emptyMap(),
    ): PriceRefreshResult = coroutineScope {
        val binanceDeferred = async { ExchangeClients.fetchBinance() }
        val bybitDeferred = async { ExchangeClients.fetchBybit() }
        val okxDeferred = async { ExchangeClients.fetchOkx() }
        val kucoinDeferred = async { ExchangeClients.fetchKucoin() }
        val gateioDeferred = async { ExchangeClients.fetchGateio() }
        val bitgetDeferred = async { ExchangeClients.fetchBitget() }
        val mexcDeferred = async { ExchangeClients.fetchMexc() }
        val bingxDeferred = async { ExchangeClients.fetchBingx() }

        val binance = binanceDeferred.await()
        val bybit = bybitDeferred.await()
        val okx = okxDeferred.await()
        val kucoin = kucoinDeferred.await()
        val gateio = gateioDeferred.await()
        val bitget = bitgetDeferred.await()
        val mexc = mexcDeferred.await()
        val bingx = bingxDeferred.await()

        // رسوم سحب حقيقية - نداء موقّع واحد بس لكل منصة من الثلاثة دي لو المستخدم معاه
        // مفتاح شخصي مُفعّل لها (مش لكل عملة على حدة، عشان نحترم حدود الطلبات). رسوم
        // السحب الحية متاحة حالياً لـ Binance/Bybit/OKX بس (الوحيدين اللي معندهمش
        // Endpoint عام، فمحتاجين المفتاح أصلاً - KuCoin/Gate.io عندهم Endpoint عام بالفعل)
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

        // عمولة تداول (Taker Fee %) حية - نداء موقّع واحد بس لكل منصة من السبعة لو
        // المستخدم معاه مفتاح شخصي مُفعّل لها، عشان نجيب نسبة عمولة حسابه الفعلية بدل
        // القيمة الافتراضية الثابتة في الإعدادات
        val liveTradingFeeDeferred = ApiKeyStore.SUPPORTED_EXCHANGES.associateWith { exchangeName ->
            async {
                val key = apiKeys[exchangeName]
                if (key != null && key.isConfigured) {
                    ExchangeClients.fetchTradingFeeWithKey(exchangeName, key)
                } else {
                    null
                }
            }
        }
        val liveTradingFee = liveTradingFeeDeferred.mapValues { it.value.await() }

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

            /**
             * عمولة التداول لكل Quote بتتحسب بالأولوية التالية:
             * 1. نداء حي موقّع بمفتاح المستخدم الشخصي (لو مُدخل ومُفعّل لهذه المنصة) ->
             *    عمولة حسابه الفعلية -> "محقق حي" (isFeeVerified = true).
             * 2. القيمة الافتراضية من شاشة الإعدادات -> "غير محقق" (ممكن تكون مختلفة
             *    عن عمولة حسابه الحقيقية لو عنده خصم VIP أو توكن).
             */
            fun tradingFeeFor(exchangeName: String, default: Double): Pair<Double, Boolean> {
                val live = liveTradingFee[exchangeName]
                if (live != null) return live to true
                return default to false
            }

            val quotes = buildList {
                binance[coin.binanceSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("Binance", it)
                    val (fee, feeVerified) = tradingFeeFor("Binance", fees.binance)
                    add(ExchangeQuote("Binance", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                bybit[coin.bybitSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("Bybit", it)
                    val (fee, feeVerified) = tradingFeeFor("Bybit", fees.bybit)
                    add(ExchangeQuote("Bybit", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                okx[coin.okxInstId]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("OKX", it)
                    val (fee, feeVerified) = tradingFeeFor("OKX", fees.okx)
                    add(ExchangeQuote("OKX", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                kucoin[coin.kucoinSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("KuCoin", it)
                    val (fee, feeVerified) = tradingFeeFor("KuCoin", fees.kucoin)
                    add(ExchangeQuote("KuCoin", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                gateio[coin.gateioSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("Gate.io", it)
                    val (fee, feeVerified) = tradingFeeFor("Gate.io", fees.gateio)
                    add(ExchangeQuote("Gate.io", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                bitget[coin.bitgetSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("Bitget", it)
                    val (fee, feeVerified) = tradingFeeFor("Bitget", fees.bitget)
                    add(ExchangeQuote("Bitget", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                mexc[coin.mexcSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("MEXC", it)
                    val (fee, feeVerified) = tradingFeeFor("MEXC", fees.mexc)
                    add(ExchangeQuote("MEXC", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
                bingx[coin.bingxSymbol]?.let {
                    val (withdrawalFee, withdrawalVerified) = withdrawalFeeFor("BingX", it)
                    val (fee, feeVerified) = tradingFeeFor("BingX", fees.bingx)
                    add(ExchangeQuote("BingX", it, fee, withdrawalFee, withdrawalVerified, feeVerified))
                }
            }

            CoinSpreadResult(coin = coin, quotes = quotes)
        }

        PriceRefreshResult(spreadResults)
    }
}
