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
import com.arbispread.app.ui.screens.HomeScreen
import com.arbispread.app.ui.screens.SettingsScreen
import com.arbispread.app.ui.theme.ArbiSpreadTheme
import com.arbispread.app.viewmodel.ArbitrageViewModel

private enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: ArbitrageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ArbiSpreadTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                val state by viewModel.uiState.collectAsState()

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        state = state,
                        // الإعلانات اتشالت مؤقتاً (هترجع بمعرّفات AdMob حقيقية قبل
                        // النشر على جوجل بلاي) - لحد ما ترجع، زرار الفتح بيفتح العملات
                        // مباشرة من غير شرط مشاهدة إعلان
                        onUnlockRequested = { viewModel.unlockAllForSession() },
                        onRefreshClick = { viewModel.refreshNow() },
                        onSettingsClick = { screen = Screen.SETTINGS },
                        onCapitalChange = { viewModel.setCapital(it) },
                        onFilterChange = { viewModel.setFilterProfitableOnly(it) },
                        onFavoritesOnlyChange = { viewModel.setFavoritesOnly(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
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
