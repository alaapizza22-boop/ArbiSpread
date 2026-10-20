package com.arbispread.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.arbispread.app.ads.AdsManager
import com.arbispread.app.model.FREE_COIN_LIMIT
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.components.BannerAdView
import com.arbispread.app.ui.components.CoinSpreadCard
import com.arbispread.app.ui.components.TriangularCard
import com.arbispread.app.ui.components.LockedCoinCard
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgDeep
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.viewmodel.ArbitrageUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ArbitrageUiState,
    adsManager: AdsManager,
    onUnlockRequested: () -> Unit,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCapitalChange: (Double) -> Unit,
    onFilterChange: (Boolean) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var capitalText by remember { mutableStateOf(state.capitalUsd.toInt().toString()) }

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
        bottomBar = { BannerAdView() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BgDeep,
                contentColor = AccentCyan
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.t("tab_arbitrage")) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.t("tab_triangular")) }
                )
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

                if (selectedTab == 0) {
                    val filteredResults = if (state.filterProfitableOnly) {
                        state.results.filter { (it.netProfitPercent(state.capitalUsd) ?: -1.0) > 0.0 }
                    } else {
                        state.results
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredResults) { index, result ->
                            if (index < FREE_COIN_LIMIT || state.unlockedAll) {
                                CoinSpreadCard(result, capitalUsd = state.capitalUsd)
                            } else {
                                LockedCoinCard(onUnlockClick = onUnlockRequested)
                            }
                        }
                    }
                } else {
                    val filteredTriangular = if (state.filterProfitableOnly) {
                        state.triangularResults.filter { it.bestFlow(state.capitalUsd.coerceAtLeast(1.0)).second.netProfitUsd > 0.0 }
                    } else {
                        state.triangularResults
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredTriangular) { index, quote ->
                            if (index < FREE_COIN_LIMIT || state.unlockedAll) {
                                TriangularCard(quote, capitalUsd = state.capitalUsd)
                            } else {
                                LockedCoinCard(onUnlockClick = onUnlockRequested)
                            }
                        }
                    }
                }
            }
        }
    }
}
