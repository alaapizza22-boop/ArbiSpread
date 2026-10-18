package com.arbispread.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arbispread.app.ads.AdsManager
import com.arbispread.app.ui.screens.HomeScreen
import com.arbispread.app.ui.screens.SettingsScreen
import com.arbispread.app.ui.theme.ArbiSpreadTheme
import com.arbispread.app.viewmodel.ArbitrageViewModel

private enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: ArbitrageViewModel by viewModels()
    private lateinit var adsManager: AdsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adsManager = AdsManager(applicationContext)
        adsManager.initialize()

        setContent {
            ArbiSpreadTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                val state by viewModel.uiState.collectAsState()

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        state = state,
                        adsManager = adsManager,
                        onUnlockRequested = {
                            adsManager.showRewardedAd(this) {
                                viewModel.unlockAllForSession()
                            }
                        },
                        onRefreshClick = { viewModel.refreshNow() },
                        onSettingsClick = { screen = Screen.SETTINGS },
                        onCapitalChange = { viewModel.setCapital(it) },
                        onFilterChange = { viewModel.setFilterProfitableOnly(it) },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { screen = Screen.HOME },
                        onSaved = {
                            viewModel.reloadFees()
                            screen = Screen.HOME
                        }
                    )
                }
            }
        }
    }
}
