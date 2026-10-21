package com.arbispread.app.data

import com.arbispread.app.model.BookQuote
import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.OrderBookLevel
import com.arbispread.app.model.OrderBookSnapshot
import com.arbispread.app.model.WalletNetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * كل دوال سحب الأسعار (Spot) ومعدلات الفاندينج (Futures) من المنصات.
 * كل دالة بترجع Map من "رمز المنصة" إلى القيمة، وأي خطأ بيترجم لـ Map فاضية
 * (يعني المنصة دي بس مش هتظهر في المقارنة لحد التحديث الجاي، من غير ما يكسر التطبيق).
 */
object ExchangeClients {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String? {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------------
    // أسعار السوق الفوري (Spot)
    // ---------------------------------------------------------------------

    suspend fun fetchBinance(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.binance.com/api/v3/ticker/price") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val price = obj.optString("price").toDoubleOrNull()
                if (symbol.isNotEmpty() && price != null) result[symbol] = price
            }
        } catch (_: Exception) { }
        result
    }

    suspend fun fetchBybit(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.bybit.com/v5/market/tickers?category=spot") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val root = JSONObject(body)
            val list = root.getJSONObject("result").getJSONArray("list")
            for (i in 0 until list.length()) {
                val obj = list.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val price = obj.optString("lastPrice").toDoubleOrNull()
                if (symbol.isNotEmpty() && price != null) result[symbol] = price
            }
        } catch (_: Exception) { }
        result
    }

    suspend fun fetchOkx(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://www.okx.com/api/v5/market/tickers?instType=SPOT") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val root = JSONObject(body)
            val data = root.getJSONArray("data")
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val instId = obj.optString("instId")
                val price = obj.optString("last").toDoubleOrNull()
                if (instId.isNotEmpty() && price != null) result[instId] = price
            }
        } catch (_: Exception) { }
        result
    }

    suspend fun fetchKucoin(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.kucoin.com/api/v1/market/allTickers") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val root = JSONObject(body)
            val ticker = root.getJSONObject("data").getJSONArray("ticker")
            for (i in 0 until ticker.length()) {
                val obj = ticker.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val price = obj.optString("last").toDoubleOrNull()
                if (symbol.isNotEmpty() && price != null) result[symbol] = price
            }
        } catch (_: Exception) { }
        result
    }

    suspend fun fetchGateio(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.gateio.ws/api/v4/spot/tickers") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pair = obj.optString("currency_pair")
                val price = obj.optString("last").toDoubleOrNull()
                if (pair.isNotEmpty() && price != null) result[pair] = price
            }
        } catch (_: Exception) { }
        result
    }

    // ---------------------------------------------------------------------
    // أفضل سعر بيع/شراء فعلي (Best Bid/Ask) - مخصص لحساب التداول الثلاثي، عشان
    // الحساب يبقى على سعر قابل للتنفيذ فعلياً (Ask للشراء، Bid للبيع) مش "آخر صفقة"
    // (Last Price) اللي ممكن تبقى قديمة جداً على عملة ضعيفة السيولة وتطلّع أرقام وهمية.
    // ---------------------------------------------------------------------

    /** Binance: /ticker/bookTicker بدل /ticker/price أو /ticker/24hr - أخف وأدق لأفضل سعر تنفيذ لحظي */
    suspend fun fetchBinanceBookTicker(): Map<String, BookQuote> = withContext(Dispatchers.IO) {
        val body = get("https://api.binance.com/api/v3/ticker/bookTicker") ?: return@withContext emptyMap()
        val result = HashMap<String, BookQuote>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val bid = obj.optString("bidPrice").toDoubleOrNull()
                val ask = obj.optString("askPrice").toDoubleOrNull()
                if (symbol.isNotEmpty() && bid != null && ask != null && bid > 0.0 && ask > 0.0) {
                    result[symbol] = BookQuote(bid, ask)
                }
            }
        } catch (_: Exception) { }
        result
    }

    /** Bybit: نفس نداء /v5/market/tickers الحالي أصلاً بيرجّع bid1Price/ask1Price - مفيش طلب إضافي */
    suspend fun fetchBybitBookTicker(): Map<String, BookQuote> = withContext(Dispatchers.IO) {
        val body = get("https://api.bybit.com/v5/market/tickers?category=spot") ?: return@withContext emptyMap()
        val result = HashMap<String, BookQuote>()
        try {
            val root = JSONObject(body)
            val list = root.getJSONObject("result").getJSONArray("list")
            for (i in 0 until list.length()) {
                val obj = list.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val bid = obj.optString("bid1Price").toDoubleOrNull()
                val ask = obj.optString("ask1Price").toDoubleOrNull()
                if (symbol.isNotEmpty() && bid != null && ask != null && bid > 0.0 && ask > 0.0) {
                    result[symbol] = BookQuote(bid, ask)
                }
            }
        } catch (_: Exception) { }
        result
    }

    /** OKX: نفس نداء /market/tickers الحالي أصلاً بيرجّع bidPx/askPx - مفيش طلب إضافي */
    suspend fun fetchOkxBookTicker(): Map<String, BookQuote> = withContext(Dispatchers.IO) {
        val body = get("https://www.okx.com/api/v5/market/tickers?instType=SPOT") ?: return@withContext emptyMap()
        val result = HashMap<String, BookQuote>()
        try {
            val root = JSONObject(body)
            val data = root.getJSONArray("data")
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val instId = obj.optString("instId")
                val bid = obj.optString("bidPx").toDoubleOrNull()
                val ask = obj.optString("askPx").toDoubleOrNull()
                if (instId.isNotEmpty() && bid != null && ask != null && bid > 0.0 && ask > 0.0) {
                    result[instId] = BookQuote(bid, ask)
                }
            }
        } catch (_: Exception) { }
        result
    }

    /** KuCoin: نفس نداء /market/allTickers الحالي أصلاً بيرجّع buy (أفضل Bid) وsell (أفضل Ask) - مفيش طلب إضافي */
    suspend fun fetchKucoinBookTicker(): Map<String, BookQuote> = withContext(Dispatchers.IO) {
        val body = get("https://api.kucoin.com/api/v1/market/allTickers") ?: return@withContext emptyMap()
        val result = HashMap<String, BookQuote>()
        try {
            val root = JSONObject(body)
            val ticker = root.getJSONObject("data").getJSONArray("ticker")
            for (i in 0 until ticker.length()) {
                val obj = ticker.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val bid = obj.optString("buy").toDoubleOrNull()
                val ask = obj.optString("sell").toDoubleOrNull()
                if (symbol.isNotEmpty() && bid != null && ask != null && bid > 0.0 && ask > 0.0) {
                    result[symbol] = BookQuote(bid, ask)
                }
            }
        } catch (_: Exception) { }
        result
    }

    /** Gate.io: نفس نداء /spot/tickers الحالي أصلاً بيرجّع highest_bid/lowest_ask - مفيش طلب إضافي */
    suspend fun fetchGateioBookTicker(): Map<String, BookQuote> = withContext(Dispatchers.IO) {
        val body = get("https://api.gateio.ws/api/v4/spot/tickers") ?: return@withContext emptyMap()
        val result = HashMap<String, BookQuote>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pair = obj.optString("currency_pair")
                val bid = obj.optString("highest_bid").toDoubleOrNull()
                val ask = obj.optString("lowest_ask").toDoubleOrNull()
                if (pair.isNotEmpty() && bid != null && ask != null && bid > 0.0 && ask > 0.0) {
                    result[pair] = BookQuote(bid, ask)
                }
            }
        } catch (_: Exception) { }
        result
    }

    // ---------------------------------------------------------------------
    // عمق دفتر الطلبات (Order Book Depth) - بيتسحب عند الطلب بس (مش كل تحديث)
    // عشان ميتجاوزش حدود الطلبات (Rate Limits) بتاعة المنصات. كل دالة بترجع
    // لقطة حية فعلية وقت الاستدعاء، أو null لو فشل الطلب (من غير بيانات وهمية بديلة).
    // ---------------------------------------------------------------------

    private fun parseLevels(arr: JSONArray?): List<OrderBookLevel> {
        if (arr == null) return emptyList()
        val list = ArrayList<OrderBookLevel>(arr.length())
        for (i in 0 until arr.length()) {
            val level = arr.optJSONArray(i) ?: continue
            val price = level.optString(0).toDoubleOrNull() ?: continue
            val qty = level.optString(1).toDoubleOrNull() ?: continue
            list.add(OrderBookLevel(price, qty))
        }
        return list
    }

    suspend fun fetchBinanceDepth(symbol: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.binance.com/api/v3/depth?symbol=$symbol&limit=100") ?: return@withContext null
        try {
            val root = JSONObject(body)
            OrderBookSnapshot(
                exchangeName = "Binance",
                asks = parseLevels(root.optJSONArray("asks")),
                bids = parseLevels(root.optJSONArray("bids")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchBybitDepth(symbol: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.bybit.com/v5/market/orderbook?category=spot&symbol=$symbol&limit=50") ?: return@withContext null
        try {
            val result = JSONObject(body).getJSONObject("result")
            OrderBookSnapshot(
                exchangeName = "Bybit",
                asks = parseLevels(result.optJSONArray("a")),
                bids = parseLevels(result.optJSONArray("b")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchOkxDepth(instId: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://www.okx.com/api/v5/market/books?instId=$instId&sz=50") ?: return@withContext null
        try {
            val entry = JSONObject(body).getJSONArray("data").getJSONObject(0)
            OrderBookSnapshot(
                exchangeName = "OKX",
                asks = parseLevels(entry.optJSONArray("asks")),
                bids = parseLevels(entry.optJSONArray("bids")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchKucoinDepth(symbol: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.kucoin.com/api/v1/market/orderbook/level2_20?symbol=$symbol") ?: return@withContext null
        try {
            val data = JSONObject(body).getJSONObject("data")
            OrderBookSnapshot(
                exchangeName = "KuCoin",
                asks = parseLevels(data.optJSONArray("asks")),
                bids = parseLevels(data.optJSONArray("bids")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchGateioDepth(pair: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.gateio.ws/api/v4/spot/order_book?currency_pair=$pair&limit=50") ?: return@withContext null
        try {
            val root = JSONObject(body)
            OrderBookSnapshot(
                exchangeName = "Gate.io",
                asks = parseLevels(root.optJSONArray("asks")),
                bids = parseLevels(root.optJSONArray("bids")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    /** بيرجّع دالة فحص العمق الصحيحة حسب اسم المنصة - أداة مساعدة لتوحيد الاستدعاء في LiquidityRepository */
    suspend fun fetchDepthFor(exchangeName: String, coin: CoinDef): OrderBookSnapshot? = when (exchangeName) {
        "Binance" -> fetchBinanceDepth(coin.binanceSymbol)
        "Bybit" -> fetchBybitDepth(coin.bybitSymbol)
        "OKX" -> fetchOkxDepth(coin.okxInstId)
        "KuCoin" -> fetchKucoinDepth(coin.kucoinSymbol)
        "Gate.io" -> fetchGateioDepth(coin.gateioSymbol)
        else -> null
    }

    // ---------------------------------------------------------------------
    // حالة الإيداع/السحب (Wallet Network Status)
    // **تنبيه صريح**: Binance و Bybit و OKX ماعندهمش Endpoint عام (من غير مفتاح API
    // شخصي موقّع) بيرجّع حالة الإيداع/السحب الفعلية - دي بيانات محتاجة توثيق بحسابك،
    // والتطبيق متعمد إنه ميطلبش مفتاح منك عشان يفضل بعيد عن حسابك وفلوسك. فبالنسبالهم
    // التطبيق بيرجّع isVerifiable=false (مش "متاح" ولا "موقوف" - ببساطة "مش معروف").
    // KuCoin و Gate.io بس عندهم Endpoint عام حقيقي، فبيترجعلهم isVerifiable=true.
    // ---------------------------------------------------------------------

    suspend fun fetchKucoinWalletStatus(currencyCode: String): WalletNetworkStatus? = withContext(Dispatchers.IO) {
        val body = get("https://api.kucoin.com/api/v1/currencies/$currencyCode") ?: return@withContext null
        try {
            val data = JSONObject(body).getJSONObject("data")
            WalletNetworkStatus(
                exchangeName = "KuCoin",
                depositEnabled = data.optBoolean("isDepositEnabled", true),
                withdrawEnabled = data.optBoolean("isWithdrawEnabled", true),
                isVerifiable = true,
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchGateioWalletStatus(currencyCode: String): WalletNetworkStatus? = withContext(Dispatchers.IO) {
        val body = get("https://api.gateio.ws/api/v4/wallet/currency_chains?currency=$currencyCode") ?: return@withContext null
        try {
            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null
            // لو أي شبكة من شبكات العملة شغالة، نعتبر الإيداع/السحب متاح عموماً على المنصة
            var depositEnabled = false
            var withdrawEnabled = false
            for (i in 0 until arr.length()) {
                val chain = arr.getJSONObject(i)
                if (!chain.optBoolean("is_deposit_disabled", false)) depositEnabled = true
                if (!chain.optBoolean("is_withdraw_disabled", false)) withdrawEnabled = true
            }
            WalletNetworkStatus(
                exchangeName = "Gate.io",
                depositEnabled = depositEnabled,
                withdrawEnabled = withdrawEnabled,
                isVerifiable = true,
            )
        } catch (_: Exception) { null }
    }

    /** بيرجّع دالة فحص حالة المحفظة الصحيحة حسب اسم المنصة - أو null (غير قابل للتحقق) للمنصات الثلاث التانية */
    suspend fun fetchWalletStatusFor(exchangeName: String, coinId: String): WalletNetworkStatus? = when (exchangeName) {
        "KuCoin" -> fetchKucoinWalletStatus(coinId)
        "Gate.io" -> fetchGateioWalletStatus(coinId)
        else -> null // Binance/Bybit/OKX: مفيش Endpoint عام - التطبيق بيوضح كده صراحة في الواجهة بدل ما يفترض "متاح"
    }

    private fun String.toDoubleOrNull(): Double? = try {
        if (this.isBlank()) null else this.toDouble()
    } catch (e: NumberFormatException) {
        null
    }

}
