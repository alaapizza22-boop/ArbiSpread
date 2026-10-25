package com.arbispread.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.arbispread.app.model.FREE_COIN_LIMIT
import com.arbispread.app.model.STABLECOIN_IDS
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.components.CoinSpreadCard
import com.arbispread.app.ui.components.LockedCoinCard
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgDeep
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.viewmodel.ArbitrageUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ArbitrageUiState,
    onUnlockRequested: () -> Unit,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCapitalChange: (Double) -> Unit,
    onFilterChange: (Boolean) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var capitalText by remember { mutableStateOf(state.capitalUsd.toInt().toString()) }
    // تاب "الكل" بيعرض كل العملات زي ما هو دايماً، وتاب "الستيبل كوين" بيفلتر على
    // الـ4 عملات المستقرة بس (STABLECOIN_IDS) - عشان متتدفنش تحت عملات متقلبة زي BTC
    // اللي فروقها أكبر بكتير وبترتب فوق في الترتيب العادي
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.t("app_name"), style = MaterialTheme.typography.titleLarge)
                        Text(
                            Strings.t("subtitle"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { Strings.toggle() }) {
                        Icon(Icons.Filled.Translate, contentDescription = Strings.t("lang_toggle"), tint = AccentCyan)
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Filled.Refresh, contentDescription = Strings.t("refresh"), tint = AccentCyan)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = Strings.t("settings"), tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDeep)
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BgDeep,
                contentColor = AccentCyan,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.t("tab_arbitrage")) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.t("tab_stablecoins")) },
                )
            }

            if (selectedTab == 1) {
                Text(
                    Strings.t("stablecoins_section_hint"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // رأس المال + فلتر الفرص الرابحة فقط
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = capitalText,
                    onValueChange = {
                        capitalText = it
                        it.toDoubleOrNull()?.let { value -> onCapitalChange(value) }
                    },
                    label = { Text(Strings.t("capital_label")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.t("filter_profitable"), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Switch(
                        checked = state.filterProfitableOnly,
                        onCheckedChange = onFilterChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.t("favorites_only"), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Switch(
                        checked = state.favoritesOnly,
                        onCheckedChange = onFavoritesOnlyChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan)
                    )
                }

                // آخر وقت وصلت فيه بيانات فعلية من المنصات - بيتحدّث تلقائياً كل 20 ثانية
                // أو عند الضغط على زرار التحديث اليدوي، عشان يبان واضح إن الأسعار مش لحظة الفتح بس
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Strings.t("last_updated_label") + " " +
                            if (state.lastUpdatedAtMillis > 0) formatUpdateTime(state.lastUpdatedAtMillis) else Strings.t("last_updated_never"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (state.isLoading && state.lastUpdatedAtMillis > 0) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.results.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AccentCyan)
                        Text(
                            Strings.t("loading"),
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    return@Box
                }

                // تاب "الستيبل كوين" بيقصر النتايج على الـ4 عملات المستقرة بس قبل أي
                // ترتيب أو فلترة تانية - عشان العملات دي تفضل ظاهرة في قسمها الخاص
                // من غير ما تتدفن تحت عملات متقلبة فروقها أكبر بكتير
                val scopedResults = if (selectedTab == 1) {
                    state.results.filter { it.coin.id in STABLECOIN_IDS }
                } else {
                    state.results
                }

                // من غير ترتيب حسب الربح - العملات بتفضل بنفس ترتيب القايمة الأصلية
                // (زرار "اظهار العملات المربحه فقط" كفاية لفلترة الفرص الرابحة)
                val profitFiltered = if (state.filterProfitableOnly) {
                    scopedResults.filter { (it.netProfitPercent(state.capitalUsd) ?: -1.0) > 0.0 }
                } else {
                    scopedResults
                }
                val filteredResults = if (state.favoritesOnly) {
                    profitFiltered.filter { it.coin.id in state.favoriteCoinIds }
                } else {
                    profitFiltered
                }

                if (selectedTab == 1 && filteredResults.isEmpty() && !state.favoritesOnly) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            Strings.t("stablecoins_empty"),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    return@Box
                }

                if (state.favoritesOnly && filteredResults.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            Strings.t("favorites_empty"),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    return@Box
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredResults) { index, result ->
                        // حد الاشتراك المجاني (FREE_COIN_LIMIT) بيتطبّق لكل تاب على حدة -
                        // يعني لو المستخدم مش فاتح الاشتراك الكامل، أول FREE_COIN_LIMIT نتيجة
                        // في تاب الستيبل كوين بيبانوا برضه حتى لو كانوا مقفولين في تاب الكل
                        if (index < FREE_COIN_LIMIT || state.unlockedAll) {
                            CoinSpreadCard(
                                result,
                                capitalUsd = state.capitalUsd,
                                persistence = state.persistenceByCoinId[result.coin.id],
                                isFavorite = result.coin.id in state.favoriteCoinIds,
                                onToggleFavorite = { onToggleFavorite(result.coin.id) },
                                liveLiquidityUsd = state.liveLiquidityByCoinId[result.coin.id],
                                showLiveLiquidity = state.liveLiquidityByCoinId.containsKey(result.coin.id)
                            )
                        } else {
                            LockedCoinCard(onUnlockClick = onUnlockRequested)
                        }
                    }
                }
            }
        }
    }
}

/** بيصيغ وقت آخر تحديث بصيغة HH:mm:ss محلية بسيطة - مش محتاجين تاريخ كامل لأن التحديث بيحصل كل 20 ثانية */
private fun formatUpdateTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))
