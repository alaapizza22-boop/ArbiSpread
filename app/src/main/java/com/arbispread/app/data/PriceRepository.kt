package com.arbispread.app.data

import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.ExchangeQuote
import com.arbispread.app.model.FundingOpportunity
import com.arbispread.app.model.WithdrawalFees
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class PriceRepository(private val coins: List<CoinDef>) {

    /**
     * بيسحب الأسعار من المنصات الخمس (كلها بأزواج USDT) بالتوازي، وبعدين يبني قايمة النتائج
     * (أرخص سعر / أغلى سعر / الفرق / الربح الصافي) لكل عملة.
     */
    suspend fun refreshAll(fees: FeeConfig): List<CoinSpreadResult> = coroutineScope {
        val binanceDeferred = async { ExchangeClients.fetchBinance() }
        val bybitDeferred = async { ExchangeClients.fetchBybit() }
        val okxDeferred = async { ExchangeClients.fetchOkx() }
        val kucoinDeferred = async { ExchangeClients.fetchKucoin() }
        val gateioDeferred = async { ExchangeClients.fetchGateio() }

        val binance = binanceDeferred.await()
        val bybit = bybitDeferred.await()
        val okx = okxDeferred.await()
        val kucoin = kucoinDeferred.await()
        val gateio = gateioDeferred.await()

        coins.map { coin ->
            /**
             * رسوم السحب لكل Quote بتتحسب كالتالي:
             * 1. لو العملة موجودة في جدول [WithdrawalFees] المرجعي -> بتتحسب بالدولار
             *    فعلياً من (الرسم بوحدة العملة × السعر الحي على المنصة دي بالظبط).
             * 2. لو مش موجودة -> بترجع null من estimateUsd، فنستخدم القيمة اليدوية
             *    العامة من الإعدادات (fees.withdrawalFeeUsd، افتراضياً صفر - مفيش خصم
             *    وهمي لحد ما المستخدم يدخل رقم حقيقي بنفسه).
             */
            fun withdrawalFeeFor(price: Double): Double =
                WithdrawalFees.estimateUsd(coin.id, price) ?: fees.withdrawalFeeUsd

            val quotes = buildList {
                binance[coin.binanceSymbol]?.let {
                    add(ExchangeQuote("Binance", it, fees.binance, withdrawalFeeFor(it)))
                }
                bybit[coin.bybitSymbol]?.let {
                    add(ExchangeQuote("Bybit", it, fees.bybit, withdrawalFeeFor(it)))
                }
                okx[coin.okxInstId]?.let {
                    add(ExchangeQuote("OKX", it, fees.okx, withdrawalFeeFor(it)))
                }
                kucoin[coin.kucoinSymbol]?.let {
                    add(ExchangeQuote("KuCoin", it, fees.kucoin, withdrawalFeeFor(it)))
                }
                gateio[coin.gateioSymbol]?.let {
                    add(ExchangeQuote("Gate.io", it, fees.gateio, withdrawalFeeFor(it)))
                }
            }

            CoinSpreadResult(coin = coin, quotes = quotes)
        }
    }

    /**
     * بيسحب معدلات الفاندينج من منصتي Binance و Bybit (فيوتشرز) بالتوازي،
     * وبيبني فرص الفاندينج آربيتراج لكل عملة معاها بيانات من منصتين على الأقل.
     */
    suspend fun refreshFunding(): List<FundingOpportunity> = coroutineScope {
        val binanceDeferred = async { ExchangeClients.fetchBinanceFunding() }
        val bybitDeferred = async { ExchangeClients.fetchBybitFunding() }

        val binance = binanceDeferred.await()
        val bybit = bybitDeferred.await()

        coins.map { coin ->
            val quotes = buildList {
                binance[coin.binanceSymbol]?.let { add(it) }
                bybit[coin.bybitSymbol]?.let { add(it) }
            }
            FundingOpportunity(coin = coin, quotes = quotes)
        }
    }
}
