package com.arbispread.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arbispread.app.data.ApiKeyStore
import com.arbispread.app.data.FeeConfig
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BgDeep
import com.arbispread.app.ui.theme.BorderSubtle
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
    var bitget by remember { mutableStateOf(initial.bitget.toString()) }
    var mexc by remember { mutableStateOf(initial.mexc.toString()) }
    var bingx by remember { mutableStateOf(initial.bingx.toString()) }
    var withdrawalFee by remember { mutableStateOf(initial.withdrawalFeeUsd.toString()) }
    var binanceFuturesTaker by remember { mutableStateOf(initial.binanceFuturesTaker.toString()) }
    var bybitFuturesTaker by remember { mutableStateOf(initial.bybitFuturesTaker.toString()) }

    // مفاتيح API شخصية اختيارية - بتتحمّل من التخزين المشفّر لو محفوظة قبل كده
    val apiKeys = remember {
        mutableStateListOf(*ApiKeyStore.SUPPORTED_EXCHANGES.map { ApiKeyStore.load(context, it) }.toTypedArray())
    }
    var apiKeysSaved by remember { mutableStateOf(false) }

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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
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
            FeeField("Bitget", bitget) { bitget = it }
            FeeField("MEXC", mexc) { mexc = it }
            FeeField("BingX", bingx) { bingx = it }

            FeeField(Strings.t("withdrawal_fee"), withdrawalFee) { withdrawalFee = it }
            Text(
                Strings.t("withdrawal_fee_hint"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Button(
                onClick = {
                    val config = FeeConfig(
                        binance = binance.toDoubleOrNull() ?: initial.binance,
                        bybit = bybit.toDoubleOrNull() ?: initial.bybit,
                        okx = okx.toDoubleOrNull() ?: initial.okx,
                        kucoin = kucoin.toDoubleOrNull() ?: initial.kucoin,
                        gateio = gateio.toDoubleOrNull() ?: initial.gateio,
                        bitget = bitget.toDoubleOrNull() ?: initial.bitget,
                        mexc = mexc.toDoubleOrNull() ?: initial.mexc,
                        bingx = bingx.toDoubleOrNull() ?: initial.bingx,
                        withdrawalFeeUsd = withdrawalFee.toDoubleOrNull() ?: initial.withdrawalFeeUsd,
                        // عمولة فيوتشرز الفاندينج اتشالت من الواجهة بعد حذف تبويب الفاندينج،
                        // بنحافظ على القيمة المخزنة زي ما هي من غير ما نعرضها أو نعدّلها
                        binanceFuturesTaker = initial.binanceFuturesTaker,
                        bybitFuturesTaker = initial.bybitFuturesTaker,
                    )
                    FeeConfig.save(context, config)
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.t("save"))
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                Strings.t("api_keys_section_title"),
                style = MaterialTheme.typography.titleMedium,
                color = AccentCyan
            )
            Text(
                Strings.t("api_keys_section_hint"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                Strings.t("api_keys_not_active_yet"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            ApiKeyStore.SUPPORTED_EXCHANGES.forEachIndexed { index, exchangeName ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(exchangeName, style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                    val current = apiKeys[index]
                    ApiKeyField(Strings.t("api_key_label"), current.apiKey, masked = false) {
                        apiKeys[index] = current.copy(apiKey = it)
                    }
                    ApiKeyField(Strings.t("secret_key_label"), current.secretKey, masked = true) {
                        apiKeys[index] = current.copy(secretKey = it)
                    }
                    if (ApiKeyStore.requiresPassphrase(exchangeName)) {
                        ApiKeyField(Strings.t("passphrase_label"), current.passphrase, masked = true) {
                            apiKeys[index] = current.copy(passphrase = it)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    apiKeys.forEachIndexed { index, key ->
                        ApiKeyStore.save(context, ApiKeyStore.SUPPORTED_EXCHANGES[index], key)
                    }
                    apiKeysSaved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = BgCard, contentColor = AccentCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.t("save_api_keys"))
            }

            if (apiKeysSaved) {
                Text(
                    Strings.t("api_keys_saved_confirmation"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
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

@Composable
private fun ApiKeyField(label: String, value: String, masked: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (masked) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = androidx.compose.ui.graphics.Color.White,
            unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
        )
    )
}
