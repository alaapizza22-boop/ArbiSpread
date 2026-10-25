package com.arbispread.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbispread.app.data.ApiKeyStore
import com.arbispread.app.data.FavoritesStore
import com.arbispread.app.data.FeeConfig
import com.arbispread.app.data.PersistenceHistoryStore
import com.arbispread.app.data.PriceRepository
import com.arbispread.app.model.CoinPersistenceInfo
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.DEFAULT_COINS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REFRESH_INTERVAL_MS = 20_000L

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
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                refreshNow()
                kotlinx.coroutines.delay(REFRESH_INTERVAL_MS)
            }
        }
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
        _uiState.update { it.copy(filterProfitableOnly = enabled) }
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
)
