package com.arbispread.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbispread.app.data.ApiKeyStore
import com.arbispread.app.data.FavoritesStore
import com.arbispread.app.data.FeeConfig
import com.arbispread.app.data.LiquidityRepository
import com.arbispread.app.data.PersistenceHistoryStore
import com.arbispread.app.data.PriceRepository
import com.arbispread.app.model.CoinPersistenceInfo
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.DEFAULT_COINS
import com.arbispread.app.model.LiveNetworkStatus
import com.arbispread.app.model.LiveOpportunityCheck
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REFRESH_INTERVAL_MS = 20_000L

/** فاصل تحديث معاينة السيولة اللايف - أبطأ بكتير من تحديث الأسعار (20 ثانية) عشان
 * ده نداءات عمق دفتر طلبات إضافية لكل عملة، فمحتاجين نحترم حدود الطلبات أكتر */
private const val LIVE_LIQUIDITY_INTERVAL_MS = 45_000L

/** أقصى عدد عملات بنفحص سيولتها اللايف في نفس الدورة - عشان نفضل بعيد عن حدود الطلبات
 * حتى لو كان فيه فرص رابحة كتير ظاهرة في نفس اللحظة */
private const val LIVE_LIQUIDITY_CAP = 8

/** أقصى مدة بنحتفظ بتاريخ الاستمرارية خلالها - أبعد من كده مش محتاجينه، وبيتمسح تلقائياً */
private const val PERSISTENCE_WINDOW_MILLIS = 60 * 60 * 1000L

class ArbitrageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PriceRepository(DEFAULT_COINS)

    /**
     * سجل آخر حالة (رابحة/مش رابحة) لكل عملة عبر آخر تحديثات، على رأس مال مرجعي ثابت
     * (مش رأس مال المستخدم المتغيّر) - مستخدم لحساب "من قد ايه الفرصة دي مستمرة".
     * بيتحمّل من [PersistenceHistoryStore] وقت بدء التشغيل وبيتحفظ بعد كل تحديث، عشان
     * يفضل موجود بين فتحات التطبيق المختلفة بدل ما يتصفّر كل مرة التطبيق يتقفل.
     */
    private val persistenceHistory: HashMap<String, MutableList<Pair<Long, Boolean>>> =
        PersistenceHistoryStore.load(application)

    private var feeConfig: FeeConfig = FeeConfig.load(application)

    private val _uiState = MutableStateFlow(
        ArbitrageUiState(feeConfig = feeConfig, favoriteCoinIds = FavoritesStore.load(application))
    )
    val uiState: StateFlow<ArbitrageUiState> = _uiState.asStateFlow()

    init {
        // لو التطبيق كان مقفول فترة طويلة، نشيل أي سجل قديم أقدم من نافذة التتبع فور
        // التحميل - عشان مينفعش عملة تتحسب "مستمرة" من وقت قديم جداً قبل ما حتى نحدّث
        trimPersistenceHistory(System.currentTimeMillis())
        startAutoRefresh()
        startLiveLiquidityLoop()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                refreshNow()
                kotlinx.coroutines.delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    /**
     * معاينة سيولة + شبكة مشتركة تلقائية - شغالة بس وقت ما زرار "عرض الفرص الرابحة فقط"
     * مفعّل، ومحدودة بعدد قليل من العملات ([LIVE_LIQUIDITY_CAP]) وبفاصل زمني أبطأ بكتير
     * من تحديث الأسعار العادي ([LIVE_LIQUIDITY_INTERVAL_MS]) - عشان نديك فكرة سريعة عن
     * الفرص اللي سيولتها فعلاً كفاية وعندها شبكة سحب/إيداع رخيصة مشتركة، من غير ما نضرب
     * حدود الطلبات بتاعة المنصات. بتستخدم نفس فحص التنفيذ الكامل اللي في زرار "فحص
     * السيولة" اليدوي ([LiquidityRepository.checkExecution]) - بما فيه حالة المحافظ،
     * عشان نقدر نحسب رسم أرخص شبكة مشتركة فعلية بدل الرسم الثابت/المرجعي.
     */
    private fun startLiveLiquidityLoop() {
        viewModelScope.launch {
            while (isActive) {
                refreshLiveLiquidity()
                kotlinx.coroutines.delay(LIVE_LIQUIDITY_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshLiveLiquidity() {
        val state = _uiState.value
        if (!state.filterProfitableOnly) return

        val targets = state.results
            .filter { (it.netProfitPercent(state.capitalUsd) ?: -1.0) > 0.0 }
            .filter { it.buy != null && it.sell != null }
            .take(LIVE_LIQUIDITY_CAP)
        if (targets.isEmpty()) return

        // بنحمّل المفاتيح الشخصية (لو موجودة) زي ما بيحصل بالظبط في فحص السيولة اليدوي -
        // عشان نقدر نجيب شبكات المنصات اللي معندهاش Endpoint عام (Binance/Bybit/OKX/
        // MEXC/BingX). قراءة محلية خفيفة من غير أي شبكة
        val application = getApplication<Application>()
        val apiKeys = ApiKeyStore.SUPPORTED_EXCHANGES.associateWith { ApiKeyStore.load(application, it) }

        val updates = coroutineScope {
            targets.map { result ->
                async {
                    val buyExchange = result.buy!!.exchangeName
                    val sellExchange = result.sell!!.exchangeName
                    val check = try {
                        LiquidityRepository.checkExecution(
                            result.coin, buyExchange, sellExchange,
                            buyApiKey = apiKeys[buyExchange], sellApiKey = apiKeys[sellExchange],
                        )
                    } catch (e: Exception) {
                        null
                    }

                    val networkDataAvailable = check?.buyWalletStatus?.availableNetworks != null &&
                        check.sellWalletStatus?.availableNetworks != null
                    val commonNetwork = check?.commonNetwork
                    val breakdown = result.feeBreakdown(state.capitalUsd)
                    val commonNetworkFeeUsd = commonNetwork?.withdrawFeeNative?.times(result.buy!!.price)

                    val networkStatus = when {
                        check?.isBlockedByWallet == true -> LiveNetworkStatus.WALLET_BLOCKED
                        commonNetwork != null -> LiveNetworkStatus.CONFIRMED
                        networkDataAvailable -> LiveNetworkStatus.NO_COMMON_NETWORK
                        else -> LiveNetworkStatus.DATA_UNAVAILABLE
                    }

                    val netProfitAfterCommonNetwork = if (
                        networkStatus == LiveNetworkStatus.CONFIRMED && breakdown != null && commonNetworkFeeUsd != null
                    ) {
                        breakdown.grossProfitUsd - breakdown.buyFeeUsd - breakdown.sellFeeUsd - commonNetworkFeeUsd
                    } else {
                        null
                    }

                    result.coin.id to LiveOpportunityCheck(
                        maxExecutableUsd = check?.maxExecutableCapitalUsd,
                        networkStatus = networkStatus,
                        commonNetworkName = commonNetwork?.networkName,
                        commonNetworkFeeUsd = commonNetworkFeeUsd,
                        netProfitAfterCommonNetworkUsd = netProfitAfterCommonNetwork,
                    )
                }
            }.awaitAll()
        }.toMap()

        _uiState.update { it.copy(liveLiquidityByCoinId = it.liveLiquidityByCoinId + updates) }
    }

    fun refreshNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // بنحمّل المفاتيح المحفوظة (لو موجودة) من التخزين المشفّر في كل تحديث - قراءة
            // محلية خفيفة من غير أي شبكة، عشان أي تغيير يعمله المستخدم في الإعدادات ينعكس فوراً
            val application = getApplication<Application>()
            val apiKeys = ApiKeyStore.SUPPORTED_EXCHANGES.associateWith { ApiKeyStore.load(application, it) }

            val refreshResult = try {
                repository.refreshAll(feeConfig, apiKeys)
            } catch (e: Exception) {
                null
            }

            val now = System.currentTimeMillis()
            if (refreshResult != null) {
                updatePersistenceHistory(refreshResult.spreadResults, now)
                PersistenceHistoryStore.save(getApplication<Application>(), persistenceHistory)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = refreshResult?.spreadResults ?: emptyList(),
                    persistenceByCoinId = computePersistenceMap(now),
                    lastUpdatedAtMillis = now
                )
            }
        }
    }

    /**
     * بتسجّل حالة كل عملة (رابحة/مش رابحة على رأس المال المرجعي) في التحديث ده، وبتمسح
     * أي سجل أقدم من نافذة التتبع عشان الذاكرة متكبرش من غير داعي مع الوقت
     */
    private fun updatePersistenceHistory(results: List<CoinSpreadResult>, now: Long) {
        results.forEach { result ->
            val isProfitable = (result.netProfitPercent(CoinPersistenceInfo.REFERENCE_CAPITAL_USD) ?: -1.0) > 0.0
            val history = persistenceHistory.getOrPut(result.coin.id) { mutableListOf() }
            history.add(now to isProfitable)
        }
        trimPersistenceHistory(now)
    }

    /** بتشيل أي قيد أقدم من نافذة التتبع، ولو عملة بقت من غير أي قيد خالص بتشيلها بالكامل من الخريطة */
    private fun trimPersistenceHistory(now: Long) {
        val cutoff = now - PERSISTENCE_WINDOW_MILLIS
        val emptyCoinIds = mutableListOf<String>()
        persistenceHistory.forEach { (coinId, history) ->
            while (history.isNotEmpty() && history.first().first < cutoff) history.removeAt(0)
            if (history.isEmpty()) emptyCoinIds.add(coinId)
        }
        emptyCoinIds.forEach { persistenceHistory.remove(it) }
    }

    /** بتحسب لكل عملة عندها سجل: عدد التحديثات المتتالية الأخيرة اللي كانت فيها رابحة + من قد ايه */
    private fun computePersistenceMap(now: Long): Map<String, CoinPersistenceInfo> =
        persistenceHistory.mapValues { (_, history) ->
            if (history.isEmpty() || !history.last().second) {
                return@mapValues CoinPersistenceInfo(consecutiveProfitableCount = 0, persistenceMinutes = 0.0)
            }
            var count = 0
            var streakStartMillis = history.last().first
            for (i in history.indices.reversed()) {
                if (!history[i].second) break
                count++
                streakStartMillis = history[i].first
            }
            CoinPersistenceInfo(consecutiveProfitableCount = count, persistenceMinutes = (now - streakStartMillis) / 60000.0)
        }

    /** بيتنادى بعد ما المستخدم يخلّص مشاهدة إعلان المكافأة بنجاح */
    fun unlockAllForSession() {
        _uiState.update { it.copy(unlockedAll = true) }
    }

    fun reloadFees() {
        feeConfig = FeeConfig.load(getApplication<Application>())
        _uiState.update { it.copy(feeConfig = feeConfig) }
        refreshNow()
    }

    fun setCapital(capital: Double) {
        _uiState.update { it.copy(capitalUsd = capital.coerceAtLeast(0.0)) }
    }

    fun setFilterProfitableOnly(enabled: Boolean) {
        // لو اتقفل الفلتر، نمسح أي سيولة لايف محفوظة فوراً (مش محتاجينها دلوقتي) بدل ما
        // نستنى لحد الدورة الجاية. لو اتفتح، نبدأ فحص فوري بدل الانتظار لحد 45 ثانية
        _uiState.update { it.copy(filterProfitableOnly = enabled, liveLiquidityByCoinId = if (enabled) it.liveLiquidityByCoinId else emptyMap()) }
        if (enabled) {
            viewModelScope.launch { refreshLiveLiquidity() }
        }
    }

    fun setFavoritesOnly(enabled: Boolean) {
        _uiState.update { it.copy(favoritesOnly = enabled) }
    }

    /** بيقلب حالة عملة في المفضلة (يضيفها لو مش موجودة، يشيلها لو موجودة) وبيحفظ على طول */
    fun toggleFavorite(coinId: String) {
        val updated = FavoritesStore.toggle(getApplication<Application>(), coinId)
        _uiState.update { it.copy(favoriteCoinIds = updated) }
    }
}

data class ArbitrageUiState(
    val isLoading: Boolean = true,
    val results: List<CoinSpreadResult> = emptyList(),
    val persistenceByCoinId: Map<String, CoinPersistenceInfo> = emptyMap(),
    val unlockedAll: Boolean = false,
    val lastUpdatedAtMillis: Long = 0L,
    val capitalUsd: Double = 1000.0,
    val filterProfitableOnly: Boolean = false,
    val feeConfig: FeeConfig = FeeConfig(),
    val favoriteCoinIds: Set<String> = emptySet(),
    val favoritesOnly: Boolean = false,
    /** نتيجة المعاينة اللايف (سيولة + شبكة مشتركة) لكل عملة ظهرت في الفحص التلقائي -
     * مفتاح مش موجود خالص معناه "لسه متفحصتش" */
    val liveLiquidityByCoinId: Map<String, LiveOpportunityCheck> = emptyMap(),
)
