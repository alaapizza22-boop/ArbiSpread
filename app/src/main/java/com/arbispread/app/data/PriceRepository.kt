package com.arbispread.app.data

import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.ExchangeQuote
import com.arbispread.app.model.FundingOpportunity
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
            val quotes = buildList {
                binance[coin.binanceSymbol]?.let {
                    add(ExchangeQuote("Binance", it, fees.binance, fees.withdrawalFeeUsd))
                }
                bybit[coin.bybitSymbol]?.let {
                    add(ExchangeQuote("Bybit", it, fees.bybit, fees.withdrawalFeeUsd))
                }
                okx[coin.okxInstId]?.let {
                    add(ExchangeQuote("OKX", it, fees.okx, fees.withdrawalFeeUsd))
                }
                kucoin[coin.kucoinSymbol]?.let {
                    add(ExchangeQuote("KuCoin", it, fees.kucoin, fees.withdrawalFeeUsd))
                }
                gateio[coin.gateioSymbol]?.let {
                    add(ExchangeQuote("Gate.io", it, fees.gateio, fees.withdrawalFeeUsd))
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
