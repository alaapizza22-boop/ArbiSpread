package com.arbispread.app.data

import com.arbispread.app.model.FundingQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    /**
     * Coinbase مفيهاش endpoint واحد بيرجع كل الأسعار دفعة واحدة بسهولة،
     * فبنعمل طلب منفصل لكل عملة مطلوبة بالتوازي (مرة كل فترة تحديث).
     */
    suspend fun fetchCoinbase(productIds: List<String>): Map<String, Double> = coroutineScope {
        val deferredList = productIds.map { productId ->
            async(Dispatchers.IO) {
                val body = get("https://api.exchange.coinbase.com/products/$productId/ticker")
                val price = body?.let {
                    try {
                        JSONObject(it).optString("price").toDoubleOrNull()
                    } catch (_: Exception) {
                        null
                    }
                }
                productId to price
            }
        }
        val result = HashMap<String, Double>()
        deferredList.forEach { deferred ->
            val (id, price) = deferred.await()
            if (price != null) result[id] = price
        }
        result
    }

    // ---------------------------------------------------------------------
    // معدلات الفاندينج (Futures Funding Rate)
    // مدعومة حالياً من Binance و Bybit (أكبر منصتي فيوتشرز من حيث السيولة)،
    // ممكن تتوسع لاحقاً لمنصات فيوتشرز إضافية بنفس الطريقة.
    // ---------------------------------------------------------------------

    /** بيرجع Map من الرمز (مثل BTCUSDT) إلى FundingQuote الخاص بـ Binance Futures */
    suspend fun fetchBinanceFunding(): Map<String, FundingQuote> = withContext(Dispatchers.IO) {
        val body = get("https://fapi.binance.com/fapi/v1/premiumIndex") ?: return@withContext emptyMap()
        val result = HashMap<String, FundingQuote>()
        try {
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val rate = obj.optString("lastFundingRate").toDoubleOrNull()
                val nextTime = obj.optLong("nextFundingTime", 0L)
                if (symbol.isNotEmpty() && rate != null) {
                    result[symbol] = FundingQuote("Binance", rate * 100.0, nextTime)
                }
            }
        } catch (_: Exception) { }
        result
    }

    /** بيرجع Map من الرمز (مثل BTCUSDT) إلى FundingQuote الخاص بـ Bybit Linear Futures */
    suspend fun fetchBybitFunding(): Map<String, FundingQuote> = withContext(Dispatchers.IO) {
        val body = get("https://api.bybit.com/v5/market/tickers?category=linear") ?: return@withContext emptyMap()
        val result = HashMap<String, FundingQuote>()
        try {
            val root = JSONObject(body)
            val list = root.getJSONObject("result").getJSONArray("list")
            for (i in 0 until list.length()) {
                val obj = list.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val rate = obj.optString("fundingRate").toDoubleOrNull()
                val nextTime = obj.optString("nextFundingTime").toLongOrNull() ?: 0L
                if (symbol.isNotEmpty() && rate != null) {
                    result[symbol] = FundingQuote("Bybit", rate * 100.0, nextTime)
                }
            }
        } catch (_: Exception) { }
        result
    }

    private fun String.toDoubleOrNull(): Double? = try {
        if (this.isBlank()) null else this.toDouble()
    } catch (e: NumberFormatException) {
        null
    }

    private fun String.toLongOrNull(): Long? = try {
        if (this.isBlank()) null else this.toLong()
    } catch (e: NumberFormatException) {
        null
    }
}
