package com.arbispread.app.data

import com.arbispread.app.model.BookQuote
import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.ExchangeQuote
import com.arbispread.app.model.TriangularQuotes
import com.arbispread.app.model.WithdrawalFees
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** نتيجة تحديث واحدة شاملة: مقارنة السبريد بين المنصات + فرص التداول الثلاثي داخل كل منصة - مبنيين من نفس نداء الأسعار (من غير ما نكرر طلبات الشبكة) */
data class PriceRefreshResult(
    val spreadResults: List<CoinSpreadResult>,
    val triangularResults: List<TriangularQuotes>,
)

class PriceRepository(private val coins: List<CoinDef>) {

    /**
     * بيسحب الأسعار من المنصات الخمس (كلها أزواج USDT) بالتوازي مرة واحدة بس، وبعدين
     * يبني منهم (أ) مقارنة السبريد بين المنصات، و(ب) فرص التداول الثلاثي داخل كل منصة -
     * من غير ما يكرر أي طلب شبكة إضافي، لأن كل منصة بترجّع أصلاً كل الأزواج (USDT
     * وCross زي ETH/BTC) في نفس استجابة الـ Ticker الواحدة.
     */
    suspend fun refreshAll(fees: FeeConfig): PriceRefreshResult = coroutineScope {
        val binanceDeferred = async { ExchangeClients.fetchBinance() }
        val bybitDeferred = async { ExchangeClients.fetchBybit() }
        val okxDeferred = async { ExchangeClients.fetchOkx() }
        val kucoinDeferred = async { ExchangeClients.fetchKucoin() }
        val gateioDeferred = async { ExchangeClients.fetchGateio() }

        // أفضل Bid/Ask لكل منصة - مخصص لحساب التداول الثلاثي على سعر تنفيذ حقيقي
        // (Bybit/OKX/KuCoin/Gate.io بيرجّعوا bid/ask في نفس استجابة التيكر العادي
        // فده نداء إضافي لنفس الـ Endpoint، أما Binance فمستقل تماماً عن /ticker/price
        // لأن bookTicker مفيهوش "آخر سعر" خالص - استخدامين مختلفين فعلاً)
        val binanceBookDeferred = async { ExchangeClients.fetchBinanceBookTicker() }
        val bybitBookDeferred = async { ExchangeClients.fetchBybitBookTicker() }
        val okxBookDeferred = async { ExchangeClients.fetchOkxBookTicker() }
        val kucoinBookDeferred = async { ExchangeClients.fetchKucoinBookTicker() }
        val gateioBookDeferred = async { ExchangeClients.fetchGateioBookTicker() }

        val binance = binanceDeferred.await()
        val bybit = bybitDeferred.await()
        val okx = okxDeferred.await()
        val kucoin = kucoinDeferred.await()
        val gateio = gateioDeferred.await()

        val binanceBook = binanceBookDeferred.await()
        val bybitBook = bybitBookDeferred.await()
        val okxBook = okxBookDeferred.await()
        val kucoinBook = kucoinBookDeferred.await()
        val gateioBook = gateioBookDeferred.await()

        val spreadResults = coins.map { coin ->
            /**
             * رسوم السحب لكل Quote بتتحسب كالتالي:
             * 1. لو العملة موجودة في جدول [WithdrawalFees] المرجعي -> بتتحسب بالدولار
             *    فعلياً من (الرسم بوحدة العملة × السعر الحي على المنصة دي بالظبط)، وتتعلّم
             *    "محقق" (isWithdrawalFeeVerified = true).
             * 2. لو مش موجودة -> بترجع null من estimateUsd، فنستخدم القيمة اليدوية
             *    العامة من الإعدادات (fees.withdrawalFeeUsd، افتراضياً صفر)، وتتعلّم
             *    "غير محقق" (isWithdrawalFeeVerified = false).
             */
            val isVerified = WithdrawalFees.hasReferenceFee(coin.id)
            fun withdrawalFeeFor(price: Double): Double =
                WithdrawalFees.estimateUsd(coin.id, price) ?: fees.withdrawalFeeUsd

            val quotes = buildList {
                binance[coin.binanceSymbol]?.let {
                    add(ExchangeQuote("Binance", it, fees.binance, withdrawalFeeFor(it), isVerified))
                }
                bybit[coin.bybitSymbol]?.let {
                    add(ExchangeQuote("Bybit", it, fees.bybit, withdrawalFeeFor(it), isVerified))
                }
                okx[coin.okxInstId]?.let {
                    add(ExchangeQuote("OKX", it, fees.okx, withdrawalFeeFor(it), isVerified))
                }
                kucoin[coin.kucoinSymbol]?.let {
                    add(ExchangeQuote("KuCoin", it, fees.kucoin, withdrawalFeeFor(it), isVerified))
                }
                gateio[coin.gateioSymbol]?.let {
                    add(ExchangeQuote("Gate.io", it, fees.gateio, withdrawalFeeFor(it), isVerified))
                }
            }

            CoinSpreadResult(coin = coin, quotes = quotes)
        }

        val triangularResults = buildList {
            addAll(buildTriangularForExchange("Binance", binanceBook, fees.binance, crossSymbol = { "${it}BTC" }, usdtSymbol = { "${it}USDT" }))
            addAll(buildTriangularForExchange("Bybit", bybitBook, fees.bybit, crossSymbol = { "${it}BTC" }, usdtSymbol = { "${it}USDT" }))
            addAll(buildTriangularForExchange("OKX", okxBook, fees.okx, crossSymbol = { "$it-BTC" }, usdtSymbol = { "$it-USDT" }))
            addAll(buildTriangularForExchange("KuCoin", kucoinBook, fees.kucoin, crossSymbol = { "$it-BTC" }, usdtSymbol = { "$it-USDT" }))
            addAll(buildTriangularForExchange("Gate.io", gateioBook, fees.gateio, crossSymbol = { "${it}_BTC" }, usdtSymbol = { "${it}_USDT" }))
        }

        PriceRefreshResult(spreadResults, triangularResults)
    }

    /**
     * بيبني فرص التداول الثلاثي لمنصة واحدة، عن طريق عملة جسر واحدة (BTC) - لأنها أكثر
     * عملة متاحة كزوج تقاطع (Cross Pair) مع باقي العملات على الخمس منصات. لو زوج
     * التقاطع (مثلاً ETHBTC) مش موجود فعلياً في رد الـ API لمنصة معينة، العملة دي ببساطة
     * مش هتظهر من المنصة دي - مفيش سعر بديل مختلق.
     *
     * الفلترة هنا صارمة ونهائية: لو أي زوج من التلاتة سبريده شاذ (سيولة شبه معدومة)، أو
     * الصافي النهائي غير منطقي (أكبر من MAX_PLAUSIBLE_PERCENT)، الفرصة دي **مش بتتبنى
     * خالص** - مش هتظهر في القايمة ولا كـ"بيانات غير متاحة"، عشان الواجهة متعرضش رقم
     * وهمي زي +564% ناتج من زوج شبه ميت.
     */
    private fun buildTriangularForExchange(
        exchangeName: String,
        books: Map<String, BookQuote>,
        takerFeePercent: Double,
        crossSymbol: (base: String) -> String,
        usdtSymbol: (base: String) -> String,
        bridgeCoin: String = "BTC",
    ): List<TriangularQuotes> {
        val bridgeBook = books[usdtSymbol(bridgeCoin)] ?: return emptyList()
        return coins.mapNotNull { coin ->
            if (coin.id == bridgeCoin) return@mapNotNull null
            val targetBook = books[usdtSymbol(coin.id)] ?: return@mapNotNull null
            val crossBook = books[crossSymbol(coin.id)] ?: return@mapNotNull null
            val quote = TriangularQuotes(
                exchangeName = exchangeName,
                bridgeCoin = bridgeCoin,
                targetCoin = coin,
                bridgeBook = bridgeBook,
                crossBook = crossBook,
                targetBook = targetBook,
                takerFeePercent = takerFeePercent,
            )
            // النسبة% ثابتة أيًا كان رأس المال (محسوبة نسبياً بالكامل)، فاستخدام رقم مرجعي
            // ثابت هنا لفحص المنطقية كافٍ ومطابق تمامًا لنتيجة أي رأس مال حقيقي
            if (quote.isPlausible(capitalUsd = 1000.0)) quote else null
        }
    }
}
