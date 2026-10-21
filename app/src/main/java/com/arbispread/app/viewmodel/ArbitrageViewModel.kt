package com.arbispread.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arbispread.app.data.ApiKeyStore
import com.arbispread.app.data.FeeConfig
import com.arbispread.app.data.PriceRepository
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.DEFAULT_COINS
import com.arbispread.app.model.TriangularQuotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REFRESH_INTERVAL_MS = 20_000L

class ArbitrageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PriceRepository(DEFAULT_COINS)

    private var feeConfig: FeeConfig = FeeConfig.load(application)

    private val _uiState = MutableStateFlow(ArbitrageUiState(feeConfig = feeConfig))
    val uiState: StateFlow<ArbitrageUiState> = _uiState.asStateFlow()

    init {
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = refreshResult?.spreadResults ?: emptyList(),
                    triangularResults = refreshResult?.triangularResults ?: emptyList(),
                    lastUpdatedAtMillis = System.currentTimeMillis()
                )
            }
        }
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
}

data class ArbitrageUiState(
    val isLoading: Boolean = true,
    val results: List<CoinSpreadResult> = emptyList(),
    val triangularResults: List<TriangularQuotes> = emptyList(),
    val unlockedAll: Boolean = false,
    val lastUpdatedAtMillis: Long = 0L,
    val capitalUsd: Double = 1000.0,
    val filterProfitableOnly: Boolean = false,
    val feeConfig: FeeConfig = FeeConfig(),
)
