package com.arbispread.app.data

import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.NetworkFeeOption
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

    /** زي [get] بالظبط بس مع هيدرز إضافية - مستخدمة في النداءات الموقّعة بمفتاح المستخدم الشخصي فقط */
    private fun getWithHeaders(url: String, headers: Map<String, String>): String? {
        return try {
            val builder = Request.Builder().url(url).get()
            headers.forEach { (k, v) -> builder.addHeader(k, v) }
            client.newCall(builder.build()).execute().use { response ->
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

    /** Bitget: منصة سادسة انضافت عشان سيولتها الكويسة على الستيبل كوين والعملات المتوسطة - نفس صيغة الرمز بتاعة Binance (مثل BTCUSDT) */
    suspend fun fetchBitget(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.bitget.com/api/v2/spot/market/tickers") ?: return@withContext emptyMap()
        val result = HashMap<String, Double>()
        try {
            val data = JSONObject(body).optJSONArray("data") ?: return@withContext emptyMap()
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val symbol = obj.optString("symbol")
                val price = obj.optString("lastPr").toDoubleOrNull()
                if (symbol.isNotEmpty() && price != null) result[symbol] = price
            }
        } catch (_: Exception) { }
        result
    }

    /** MEXC: منصة سابعة انضافت لأنها بتدرج عملات جديدة بسرعة وأحياناً بتفارق في السعر عن باقي المنصات - API مطابق لـ Binance تقريباً (نفس صيغة الرمز مثل BTCUSDT) */
    suspend fun fetchMexc(): Map<String, Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.mexc.com/api/v3/ticker/price") ?: return@withContext emptyMap()
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

    suspend fun fetchBitgetDepth(symbol: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.bitget.com/api/v2/spot/market/orderbook?symbol=$symbol&type=step0&limit=50") ?: return@withContext null
        try {
            val data = JSONObject(body).getJSONObject("data")
            OrderBookSnapshot(
                exchangeName = "Bitget",
                asks = parseLevels(data.optJSONArray("asks")),
                bids = parseLevels(data.optJSONArray("bids")),
                fetchedAtMillis = System.currentTimeMillis(),
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchMexcDepth(symbol: String): OrderBookSnapshot? = withContext(Dispatchers.IO) {
        val body = get("https://api.mexc.com/api/v3/depth?symbol=$symbol&limit=100") ?: return@withContext null
        try {
            val root = JSONObject(body)
            OrderBookSnapshot(
                exchangeName = "MEXC",
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
        "Bitget" -> fetchBitgetDepth(coin.bitgetSymbol)
        "MEXC" -> fetchMexcDepth(coin.mexcSymbol)
        else -> null
    }

    /**
     * زي [fetchDepthFor] بالظبط لكن بياخد رمز الزوج الخام مباشرة بدل [CoinDef] - مستخدمة
     * في التداول الثلاثي لأن زوج التقاطع (مثل ETHBTC) مش زوج USDT عادي موجود في CoinDef.
     * الرمز لازم يكون بصيغة المنصة الصحيحة أصلاً (مثلاً "ETHBTC" لـ Binance أو "ETH-BTC" لـ OKX).
     */
    suspend fun fetchDepthForSymbol(exchangeName: String, symbol: String): OrderBookSnapshot? = when (exchangeName) {
        "Binance" -> fetchBinanceDepth(symbol)
        "Bybit" -> fetchBybitDepth(symbol)
        "OKX" -> fetchOkxDepth(symbol)
        "KuCoin" -> fetchKucoinDepth(symbol)
        "Gate.io" -> fetchGateioDepth(symbol)
        "Bitget" -> fetchBitgetDepth(symbol)
        "MEXC" -> fetchMexcDepth(symbol)
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

    /**
     * KuCoin: `/api/v1/currencies/{code}` عام بالكامل (من غير مفتاح) وبيرجّع كمان مصفوفة
     * "chains" فيها رسم السحب الحقيقي (withdrawalMinFee) لكل شبكة - بناخد أول شبكة متاحة
     * كممثل. لو الحقل مش موجود (رد قديم أو عملة من غير شبكات متعددة)، withdrawFeeNative
     * بترجع null بدل ما نخترع رقم.
     */
    /** بياخد كل الشبكات المتاحة ويختار أرخص واحدة شغالة فعلاً (withdrawEnabled) - لو مفيش شبكة شغالة، بيرجع أرخص واحدة برضو كـ"مرجعي" بس مع العلم إنها موقوفة */
    private fun cheapestEnabledNetwork(options: List<NetworkFeeOption>): NetworkFeeOption? =
        options.filter { it.withdrawEnabled }.minByOrNull { it.withdrawFeeNative }
            ?: options.minByOrNull { it.withdrawFeeNative }

    suspend fun fetchKucoinWalletStatus(currencyCode: String): WalletNetworkStatus? = withContext(Dispatchers.IO) {
        val body = get("https://api.kucoin.com/api/v1/currencies/$currencyCode") ?: return@withContext null
        try {
            val data = JSONObject(body).getJSONObject("data")
            val chains = data.optJSONArray("chains")
            val options = mutableListOf<NetworkFeeOption>()
            if (chains != null) {
                for (c in 0 until chains.length()) {
                    val chain = chains.getJSONObject(c)
                    val name = chain.optString("chainName").takeIf { it.isNotBlank() } ?: continue
                    val fee = chain.optString("withdrawalMinFee").toDoubleOrNull() ?: continue
                    options.add(NetworkFeeOption(name, fee, chain.optBoolean("isWithdrawEnabled", true)))
                }
            }
            val cheapest = cheapestEnabledNetwork(options)
            WalletNetworkStatus(
                exchangeName = "KuCoin",
                depositEnabled = data.optBoolean("isDepositEnabled", true),
                withdrawEnabled = data.optBoolean("isWithdrawEnabled", true),
                isVerifiable = true,
                networkName = cheapest?.networkName,
                withdrawFeeNative = cheapest?.withdrawFeeNative,
                availableNetworks = options.sortedBy { it.withdrawFeeNative }.ifEmpty { null },
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
            var anyWithdrawEnabled = false
            val options = mutableListOf<NetworkFeeOption>()
            for (i in 0 until arr.length()) {
                val chain = arr.getJSONObject(i)
                if (!chain.optBoolean("is_deposit_disabled", false)) depositEnabled = true
                val withdrawEnabledHere = !chain.optBoolean("is_withdraw_disabled", false)
                if (withdrawEnabledHere) anyWithdrawEnabled = true
                // الحقل أحياناً مسمى withdraw_fix_fee في رد هذا الـ Endpoint
                val fee = chain.optString("withdraw_fix_fee").toDoubleOrNull()
                val name = chain.optString("chain").takeIf { it.isNotBlank() }
                if (fee != null && name != null) options.add(NetworkFeeOption(name, fee, withdrawEnabledHere))
            }
            val cheapest = cheapestEnabledNetwork(options)
            WalletNetworkStatus(
                exchangeName = "Gate.io",
                depositEnabled = depositEnabled,
                withdrawEnabled = anyWithdrawEnabled,
                isVerifiable = true,
                networkName = cheapest?.networkName,
                withdrawFeeNative = cheapest?.withdrawFeeNative,
                availableNetworks = options.sortedBy { it.withdrawFeeNative }.ifEmpty { null },
            )
        } catch (_: Exception) { null }
    }

    /** بيرجّع دالة فحص حالة المحفظة الصحيحة حسب اسم المنصة - أو null (غير قابل للتحقق) للمنصات الثلاث التانية لو مفيش مفتاح شخصي مُفعّل */
    suspend fun fetchWalletStatusFor(exchangeName: String, coinId: String): WalletNetworkStatus? = when (exchangeName) {
        "KuCoin" -> fetchKucoinWalletStatus(coinId)
        "Gate.io" -> fetchGateioWalletStatus(coinId)
        else -> null // Binance/Bybit/OKX/Bitget/MEXC: مفيش Endpoint عام موثّق - استخدم fetchWalletStatusWithKey لو المستخدم معاه مفتاح شخصي (متاح حالياً لـ Binance/Bybit/OKX بس)
    }

    // ---------------------------------------------------------------------
    // نداءات موقّعة بمفتاح المستخدم الشخصي (Binance/Bybit/OKX) - دي المنصات الوحيدة
    // اللي معندهاش Endpoint عام لرسوم السحب/حالة المحفظة. التوقيع هنا بيحصل محلياً على
    // جهاز المستخدم بمفتاحه هو، ومفيش أي مفتاح أو سر بيتبعت لغير المنصة نفسها مباشرة.
    // أي فشل (مفتاح غلط، صلاحيات ناقصة، مشكلة شبكة) بيرجع null بهدوء - التطبيق يرجع
    // تلقائياً لنفس السلوك القديم (جدول مرجعي / "غير محقق") من غير كراش ومن غير بيانات مختلقة.
    //
    // ⚠️ ملحوظة صراحة: التوقيع هنا مبني على توثيق كل منصة وقت كتابة الكود، ومستحيل
    // نختبره فعلياً من هنا (بيئة التطوير مقطوعة عن إنترنت المنصات دي بالكامل) - أول
    // اختبار حقيقي لازم يحصل على جهاز المستخدم نفسه بمفتاحه الحقيقي.
    // ---------------------------------------------------------------------

    /** Binance: /sapi/v1/capital/config/getall - موقّع، بيرجع كل العملات في نداء واحد */
    suspend fun fetchBinanceWithdrawalInfo(apiKey: String, secretKey: String): Map<String, WalletNetworkStatus> = withContext(Dispatchers.IO) {
        val result = HashMap<String, WalletNetworkStatus>()
        try {
            val timestamp = System.currentTimeMillis()
            val query = "timestamp=$timestamp&recvWindow=10000"
            val signature = Signing.hmacSha256Hex(secretKey, query)
            val url = "https://api.binance.com/sapi/v1/capital/config/getall?$query&signature=$signature"
            val body = getWithHeaders(url, mapOf("X-MBX-APIKEY" to apiKey)) ?: return@withContext emptyMap()
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val coin = arr.getJSONObject(i)
                val coinId = coin.optString("coin")
                if (coinId.isEmpty()) continue
                val networks = coin.optJSONArray("networkList")
                var depositEnabled: Boolean? = coin.optBoolean("depositAllEnable", true)
                var withdrawEnabled: Boolean? = coin.optBoolean("withdrawAllEnable", true)
                // بنبني كل الشبكات المتاحة للعملة دي ونختار أرخص واحدة شغالة تلقائياً -
                // بدل الاعتماد على الشبكة الافتراضية بس، اللي ممكن متبقاش الأرخص دايماً
                val networkOptions = mutableListOf<NetworkFeeOption>()
                if (networks != null) {
                    for (n in 0 until networks.length()) {
                        val net = networks.getJSONObject(n)
                        val name = net.optString("network").takeIf { it.isNotBlank() } ?: continue
                        val fee = net.optString("withdrawFee").toDoubleOrNull() ?: continue
                        networkOptions.add(NetworkFeeOption(name, fee, net.optBoolean("withdrawEnable", true)))
                    }
                }
                val cheapest = cheapestEnabledNetwork(networkOptions)
                result[coinId] = WalletNetworkStatus(
                    exchangeName = "Binance",
                    depositEnabled = depositEnabled,
                    withdrawEnabled = withdrawEnabled,
                    isVerifiable = true,
                    networkName = cheapest?.networkName,
                    withdrawFeeNative = cheapest?.withdrawFeeNative,
                    availableNetworks = networkOptions.sortedBy { it.withdrawFeeNative }.ifEmpty { null },
                )
            }
        } catch (_: Exception) { }
        result
    }

    /** Bybit v5: /v5/asset/coin/query-info - موقّع، بيرجع كل العملات في نداء واحد */
    suspend fun fetchBybitWithdrawalInfo(apiKey: String, secretKey: String): Map<String, WalletNetworkStatus> = withContext(Dispatchers.IO) {
        val result = HashMap<String, WalletNetworkStatus>()
        try {
            val timestamp = System.currentTimeMillis().toString()
            val recvWindow = "10000"
            val queryString = ""
            val signaturePayload = timestamp + apiKey + recvWindow + queryString
            val signature = Signing.hmacSha256Hex(secretKey, signaturePayload)
            val url = "https://api.bybit.com/v5/asset/coin/query-info"
            val body = getWithHeaders(
                url,
                mapOf(
                    "X-BAPI-API-KEY" to apiKey,
                    "X-BAPI-TIMESTAMP" to timestamp,
                    "X-BAPI-RECV-WINDOW" to recvWindow,
                    "X-BAPI-SIGN" to signature,
                )
            ) ?: return@withContext emptyMap()
            val rows = JSONObject(body).getJSONObject("result").getJSONArray("rows")
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val coinId = row.optString("coin")
                if (coinId.isEmpty()) continue
                val chains = row.optJSONArray("chains")
                // حالة الإيداع/السحب العامة للعملة = لو أي شبكة من شبكاتها شغالة (مش بس أول شبكة)
                var depositEnabled = false
                var withdrawEnabled = false
                val options = mutableListOf<NetworkFeeOption>()
                if (chains != null) {
                    for (c in 0 until chains.length()) {
                        val chain = chains.getJSONObject(c)
                        val name = chain.optString("chain").takeIf { it.isNotBlank() } ?: continue
                        val fee = chain.optString("withdrawFee").toDoubleOrNull() ?: continue
                        val chainWithdrawEnabled = chain.optString("chainWithdraw") == "1"
                        if (chain.optString("chainDeposit") == "1") depositEnabled = true
                        if (chainWithdrawEnabled) withdrawEnabled = true
                        options.add(NetworkFeeOption(name, fee, chainWithdrawEnabled))
                    }
                }
                val cheapest = cheapestEnabledNetwork(options)
                result[coinId] = WalletNetworkStatus(
                    exchangeName = "Bybit",
                    depositEnabled = depositEnabled,
                    withdrawEnabled = withdrawEnabled,
                    isVerifiable = true,
                    networkName = cheapest?.networkName,
                    withdrawFeeNative = cheapest?.withdrawFeeNative,
                    availableNetworks = options.sortedBy { it.withdrawFeeNative }.ifEmpty { null },
                )
            }
        } catch (_: Exception) { }
        result
    }

    /** OKX: /api/v5/asset/currencies - موقّع (محتاج Passphrase كمان)، بيرجع كل العملات في نداء واحد */
    suspend fun fetchOkxWithdrawalInfo(apiKey: String, secretKey: String, passphrase: String): Map<String, WalletNetworkStatus> = withContext(Dispatchers.IO) {
        val result = HashMap<String, WalletNetworkStatus>()
        try {
            val requestPath = "/api/v5/asset/currencies"
            // تنسيق الوقت المطلوب من OKX: ISO 8601 بالميلي ثانية، مثل 2024-01-01T00:00:00.000Z
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timestamp = sdf.format(java.util.Date())
            val signaturePayload = timestamp + "GET" + requestPath
            val signature = Signing.hmacSha256Base64(secretKey, signaturePayload)
            val url = "https://www.okx.com$requestPath"
            val body = getWithHeaders(
                url,
                mapOf(
                    "OK-ACCESS-KEY" to apiKey,
                    "OK-ACCESS-SIGN" to signature,
                    "OK-ACCESS-TIMESTAMP" to timestamp,
                    "OK-ACCESS-PASSPHRASE" to passphrase,
                )
            ) ?: return@withContext emptyMap()
            val data = JSONObject(body).getJSONArray("data")
            // لو العملة معاها أكتر من شبكة، كل شبكة بتيجي كصف منفصل في الرد - بنجمّع كل
            // صفوف نفس العملة الأول عشان نقدر نختار أرخص شبكة شغالة من بينهم كلهم
            val optionsByCoin = HashMap<String, MutableList<NetworkFeeOption>>()
            val depositByCoin = HashMap<String, Boolean>()
            val withdrawByCoin = HashMap<String, Boolean>()
            for (i in 0 until data.length()) {
                val row = data.getJSONObject(i)
                val coinId = row.optString("ccy")
                if (coinId.isEmpty()) continue
                val chainName = row.optString("chain").takeIf { it.isNotBlank() }
                val fee = row.optString("minFee").toDoubleOrNull()
                val canWd = row.optBoolean("canWd", true)
                val canDep = row.optBoolean("canDep", true)
                if (chainName != null && fee != null) {
                    optionsByCoin.getOrPut(coinId) { mutableListOf() }.add(NetworkFeeOption(chainName, fee, canWd))
                }
                depositByCoin[coinId] = (depositByCoin[coinId] ?: false) || canDep
                withdrawByCoin[coinId] = (withdrawByCoin[coinId] ?: false) || canWd
            }
            for ((coinId, options) in optionsByCoin) {
                val cheapest = cheapestEnabledNetwork(options)
                result[coinId] = WalletNetworkStatus(
                    exchangeName = "OKX",
                    depositEnabled = depositByCoin[coinId],
                    withdrawEnabled = withdrawByCoin[coinId],
                    isVerifiable = true,
                    networkName = cheapest?.networkName,
                    withdrawFeeNative = cheapest?.withdrawFeeNative,
                    availableNetworks = options.sortedBy { it.withdrawFeeNative },
                )
            }
        } catch (_: Exception) { }
        result
    }

    /** دالة موحّدة: بتختار النداء الصح حسب المنصة لو المستخدم معاه مفتاح شخصي مُفعّل لها */
    suspend fun fetchWalletStatusWithKey(exchangeName: String, key: ExchangeApiKey): Map<String, WalletNetworkStatus> {
        if (!key.isConfigured) return emptyMap()
        return when (exchangeName) {
            "Binance" -> fetchBinanceWithdrawalInfo(key.apiKey, key.secretKey)
            "Bybit" -> fetchBybitWithdrawalInfo(key.apiKey, key.secretKey)
            "OKX" -> fetchOkxWithdrawalInfo(key.apiKey, key.secretKey, key.passphrase)
            else -> emptyMap()
        }
    }

    // ---------------------------------------------------------------------
    // عمولة التداول الفعلية (Live Taker Fee %) - نداءات موقّعة بمفتاح المستخدم الشخصي
    // تجيب نسبة عمولتك الحقيقية (بعد أي خصم VIP/توكن) بدل القيمة الافتراضية الثابتة
    // في الإعدادات. كل دالة بترجع null بهدوء لو أي خطوة فشلت (مفتاح غلط، صلاحيات
    // ناقصة، شكل رد مختلف عن المتوقع) - التطبيق يرجع تلقائياً للقيمة الافتراضية
    // من غير كراش ومن غير رقم مختلق.
    //
    // ⚠️ نفس التنبيه بتاع نداءات رسوم السحب فوق: التوقيع هنا مبني على توثيق كل منصة
    // وقت كتابة الكود، ومستحيل نختبره فعلياً من بيئة التطوير دي - أول اختبار حقيقي
    // لازم يحصل على جهاز المستخدم نفسه بمفتاحه الحقيقي.
    // ---------------------------------------------------------------------

    /** Binance: /api/v3/account (موقّع) - commissionRates.taker نسبة عشرية (مثلاً "0.00100000" = 0.1%) */
    suspend fun fetchBinanceTradingFee(apiKey: String, secretKey: String): Double? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val query = "timestamp=$timestamp&recvWindow=10000"
            val signature = Signing.hmacSha256Hex(secretKey, query)
            val url = "https://api.binance.com/api/v3/account?$query&signature=$signature"
            val body = getWithHeaders(url, mapOf("X-MBX-APIKEY" to apiKey)) ?: return@withContext null
            val root = JSONObject(body)
            val rates = root.optJSONObject("commissionRates")
            val taker = rates?.optString("taker")?.toDoubleOrNull()
            taker?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** Bybit v5: /v5/account/fee-rate?category=spot (موقّع) - takerFeeRate لأول صف في القائمة */
    suspend fun fetchBybitTradingFee(apiKey: String, secretKey: String): Double? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis().toString()
            val recvWindow = "10000"
            val queryString = "category=spot"
            val signaturePayload = timestamp + apiKey + recvWindow + queryString
            val signature = Signing.hmacSha256Hex(secretKey, signaturePayload)
            val url = "https://api.bybit.com/v5/account/fee-rate?$queryString"
            val body = getWithHeaders(
                url,
                mapOf(
                    "X-BAPI-API-KEY" to apiKey,
                    "X-BAPI-TIMESTAMP" to timestamp,
                    "X-BAPI-RECV-WINDOW" to recvWindow,
                    "X-BAPI-SIGN" to signature,
                )
            ) ?: return@withContext null
            val list = JSONObject(body).getJSONObject("result").getJSONArray("list")
            if (list.length() == 0) return@withContext null
            list.getJSONObject(0).optString("takerFeeRate").toDoubleOrNull()?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** OKX: /api/v5/account/trade-fee?instType=SPOT (موقّع) - taker بترجع بالسالب (تكلفة) فبناخد القيمة المطلقة */
    suspend fun fetchOkxTradingFee(apiKey: String, secretKey: String, passphrase: String): Double? = withContext(Dispatchers.IO) {
        try {
            val requestPath = "/api/v5/account/trade-fee?instType=SPOT"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timestamp = sdf.format(java.util.Date())
            val signaturePayload = timestamp + "GET" + requestPath
            val signature = Signing.hmacSha256Base64(secretKey, signaturePayload)
            val url = "https://www.okx.com$requestPath"
            val body = getWithHeaders(
                url,
                mapOf(
                    "OK-ACCESS-KEY" to apiKey,
                    "OK-ACCESS-SIGN" to signature,
                    "OK-ACCESS-TIMESTAMP" to timestamp,
                    "OK-ACCESS-PASSPHRASE" to passphrase,
                )
            ) ?: return@withContext null
            val data = JSONObject(body).getJSONArray("data")
            if (data.length() == 0) return@withContext null
            val taker = data.getJSONObject(0).optString("taker").toDoubleOrNull() ?: return@withContext null
            kotlin.math.abs(taker) * 100.0
        } catch (_: Exception) { null }
    }

    /** KuCoin: /api/v1/base-fee (موقّع، محتاج Passphrase مشفّر كمان - API-KEY-VERSION 2) - takerFeeRate */
    suspend fun fetchKucoinTradingFee(apiKey: String, secretKey: String, passphrase: String): Double? = withContext(Dispatchers.IO) {
        try {
            val requestPath = "/api/v1/base-fee"
            val timestamp = System.currentTimeMillis().toString()
            val signaturePayload = timestamp + "GET" + requestPath
            val signature = Signing.hmacSha256Base64(secretKey, signaturePayload)
            // KuCoin API-KEY-VERSION 2: الـ Passphrase نفسه لازم يتوقّع بنفس الـ Secret قبل ما يتبعت
            val signedPassphrase = Signing.hmacSha256Base64(secretKey, passphrase)
            val url = "https://api.kucoin.com$requestPath"
            val body = getWithHeaders(
                url,
                mapOf(
                    "KC-API-KEY" to apiKey,
                    "KC-API-SIGN" to signature,
                    "KC-API-TIMESTAMP" to timestamp,
                    "KC-API-PASSPHRASE" to signedPassphrase,
                    "KC-API-KEY-VERSION" to "2",
                )
            ) ?: return@withContext null
            val data = JSONObject(body).optJSONObject("data") ?: return@withContext null
            data.optString("takerFeeRate").toDoubleOrNull()?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** Gate.io: /api/v4/wallet/fee (موقّع بـ SHA512 - الوحيدة من السبعة بالتوقيع ده) - taker_fee */
    suspend fun fetchGateioTradingFee(apiKey: String, secretKey: String): Double? = withContext(Dispatchers.IO) {
        try {
            val method = "GET"
            val requestPath = "/api/v4/wallet/fee"
            val queryString = ""
            val bodyHash = Signing.sha512Hex("")
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signaturePayload = "$method\n$requestPath\n$queryString\n$bodyHash\n$timestamp"
            val signature = Signing.hmacSha512Hex(secretKey, signaturePayload)
            val url = "https://api.gateio.ws$requestPath"
            val body = getWithHeaders(
                url,
                mapOf(
                    "KEY" to apiKey,
                    "SIGN" to signature,
                    "Timestamp" to timestamp,
                )
            ) ?: return@withContext null
            JSONObject(body).optString("taker_fee").toDoubleOrNull()?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** Bitget: /api/v2/common/trade-rate?symbol=BTCUSDT&business=spot (موقّع، محتاج Passphrase زي OKX) - takerFeeRate */
    suspend fun fetchBitgetTradingFee(apiKey: String, secretKey: String, passphrase: String): Double? = withContext(Dispatchers.IO) {
        try {
            val requestPath = "/api/v2/common/trade-rate"
            val queryString = "?symbol=BTCUSDT&business=spot"
            val timestamp = System.currentTimeMillis().toString()
            val signaturePayload = timestamp + "GET" + requestPath + queryString
            val signature = Signing.hmacSha256Base64(secretKey, signaturePayload)
            val url = "https://api.bitget.com$requestPath$queryString"
            val body = getWithHeaders(
                url,
                mapOf(
                    "ACCESS-KEY" to apiKey,
                    "ACCESS-SIGN" to signature,
                    "ACCESS-TIMESTAMP" to timestamp,
                    "ACCESS-PASSPHRASE" to passphrase,
                )
            ) ?: return@withContext null
            val data = JSONObject(body).optJSONObject("data") ?: return@withContext null
            data.optString("takerFeeRate").toDoubleOrNull()?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** MEXC: /api/v3/tradeFee?symbol=BTCUSDT (موقّع، نفس أسلوب Binance) - takerCommission */
    suspend fun fetchMexcTradingFee(apiKey: String, secretKey: String): Double? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val query = "symbol=BTCUSDT&timestamp=$timestamp&recvWindow=10000"
            val signature = Signing.hmacSha256Hex(secretKey, query)
            val url = "https://api.mexc.com/api/v3/tradeFee?$query&signature=$signature"
            val body = getWithHeaders(url, mapOf("X-MEXC-APIKEY" to apiKey)) ?: return@withContext null
            // بعض ردود MEXC بترجع كائن واحد مباشرة، وبعضها مصفوفة فيها صف واحد - بنتعامل مع الحالتين
            val obj = try {
                JSONArray(body).optJSONObject(0)
            } catch (_: Exception) {
                JSONObject(body)
            } ?: return@withContext null
            obj.optString("takerCommission").toDoubleOrNull()?.times(100.0)
        } catch (_: Exception) { null }
    }

    /** دالة موحّدة: بتختار نداء عمولة التداول الحية الصح حسب المنصة لو المستخدم معاه مفتاح شخصي مُفعّل لها */
    suspend fun fetchTradingFeeWithKey(exchangeName: String, key: ExchangeApiKey): Double? {
        if (!key.isConfigured) return null
        return when (exchangeName) {
            "Binance" -> fetchBinanceTradingFee(key.apiKey, key.secretKey)
            "Bybit" -> fetchBybitTradingFee(key.apiKey, key.secretKey)
            "OKX" -> fetchOkxTradingFee(key.apiKey, key.secretKey, key.passphrase)
            "KuCoin" -> fetchKucoinTradingFee(key.apiKey, key.secretKey, key.passphrase)
            "Gate.io" -> fetchGateioTradingFee(key.apiKey, key.secretKey)
            "Bitget" -> fetchBitgetTradingFee(key.apiKey, key.secretKey, key.passphrase)
            "MEXC" -> fetchMexcTradingFee(key.apiKey, key.secretKey)
            else -> null
        }
    }

    private fun String.toDoubleOrNull(): Double? = try {
        if (this.isBlank()) null else this.toDouble()
    } catch (e: NumberFormatException) {
        null
    }

}
