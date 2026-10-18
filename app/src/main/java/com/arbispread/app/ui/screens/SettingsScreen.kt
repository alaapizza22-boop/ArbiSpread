package com.arbispread.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arbispread.app.data.FeeConfig
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgDeep
import com.arbispread.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val initial = remember { FeeConfig.load(context) }

    var binance by remember { mutableStateOf(initial.binance.toString()) }
    var bybit by remember { mutableStateOf(initial.bybit.toString()) }
    var okx by remember { mutableStateOf(initial.okx.toString()) }
    var kucoin by remember { mutableStateOf(initial.kucoin.toString()) }
    var gateio by remember { mutableStateOf(initial.gateio.toString()) }
    var coinbase by remember { mutableStateOf(initial.coinbase.toString()) }
    var withdrawalFee by remember { mutableStateOf(initial.withdrawalFeeUsd.toString()) }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            TopAppBar(
                title = { Text(Strings.t("settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Strings.t("back"), tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDeep)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                Strings.t("fee_settings_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            FeeField("Binance", binance) { binance = it }
            FeeField("Bybit", bybit) { bybit = it }
            FeeField("OKX", okx) { okx = it }
            FeeField("KuCoin", kucoin) { kucoin = it }
            FeeField("Gate.io", gateio) { gateio = it }
            FeeField("Coinbase", coinbase) { coinbase = it }
            FeeField(Strings.t("withdrawal_fee"), withdrawalFee) { withdrawalFee = it }

            Button(
                onClick = {
                    val config = FeeConfig(
                        binance = binance.toDoubleOrNull() ?: initial.binance,
                        bybit = bybit.toDoubleOrNull() ?: initial.bybit,
                        okx = okx.toDoubleOrNull() ?: initial.okx,
                        kucoin = kucoin.toDoubleOrNull() ?: initial.kucoin,
                        gateio = gateio.toDoubleOrNull() ?: initial.gateio,
                        coinbase = coinbase.toDoubleOrNull() ?: initial.coinbase,
                        withdrawalFeeUsd = withdrawalFee.toDoubleOrNull() ?: initial.withdrawalFeeUsd,
                    )
                    FeeConfig.save(context, config)
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.t("save"))
            }
        }
    }
}

@Composable
private fun FeeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = androidx.compose.ui.graphics.Color.White,
            unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
        )
    )
}
