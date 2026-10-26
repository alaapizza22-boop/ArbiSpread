package com.arbispread.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arbispread.app.data.ApiKeyStore
import com.arbispread.app.data.LiquidityRepository
import com.arbispread.app.model.CoinDef
import com.arbispread.app.model.LiquidityCheckResult
import com.arbispread.app.model.MinimumOrderSize
import com.arbispread.app.model.NetworkTransferTime
import com.arbispread.app.model.WalletNetworkStatus
import com.arbispread.app.model.WithdrawalFees
import com.arbispread.app.ui.AppLanguage
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.ui.theme.WarnAmber
import java.util.Locale

/**
 * حوار "فحص التنفيذ الحي" - بيتفتح عند الطلب بس (مش تلقائي) عشان يسحب دفتر الطلبات
 * الفعلي لحظة الفتح، ويحسب أقصى مبلغ قابل للتنفيذ + يفحص حالة الإيداع/السحب الحقيقية.
 */
@Composable
fun LiquidityCheckDialog(
    coin: CoinDef,
    buyExchange: String,
    sellExchange: String,
    capitalUsd: Double,
    /** عمولة Taker% لمنصة الشراء والبيع ورسم السحب بالدولار - نفس المصادر اللي بيتحسب
     * منها الربح الظاهر على الكارت أصلاً ([com.arbispread.app.model.FeeBreakdown])، عشان
     * نحسب "الربح الحقيقي" هنا بنفس العمولات بس بسعر تنفيذ حقيقي من عمق السجل بدل السعر
     * اللحظي السريع */
    buyFeePercent: Double,
    sellFeePercent: Double,
    withdrawalFeeUsd: Double,
    isWithdrawalFeeVerified: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<LiquidityCheckResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        // لو المستخدم مدخل مفتاحه الشخصي لمنصة الشراء/البيع (اختياري)، بيتستخدم بس
        // لو المنصة دي معندهاش Endpoint عام أصلاً (Binance/Bybit/OKX)
        val buyKey = ApiKeyStore.load(context, buyExchange)
        val sellKey = ApiKeyStore.load(context, sellExchange)
        result = LiquidityRepository.checkExecution(
            coin, buyExchange, sellExchange,
            requestedCapitalUsd = capitalUsd,
            buyApiKey = buyKey, sellApiKey = sellKey,
        )
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.t("liquidity_check_title") + " · ${coin.id}") },
        text = {
            Column {
                if (isLoading) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp), color = AccentCyan)
                        Text(Strings.t("liquidity_checking"), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val r = result
                    if (r == null || r.errorMessage != null) {
                        Text(
                            Strings.t("liquidity_error") + (r?.errorMessage?.let { ": $it" } ?: ""),
                            color = LossRed
                        )
                    } else {
                        if (r.isBlockedByWallet) {
                            Text(
                                Strings.t("wallet_blocked_banner"),
                                color = LossRed,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        Text(
                            Strings.t("max_executable") + " (" + Strings.t("max_executable_desc") + " " +
                                String.format(Locale.US, "%.1f", r.maxSlippagePercent) + "%):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            if (r.maxExecutableCapitalUsd != null)
                                "$" + String.format(Locale.US, "%.0f", r.maxExecutableCapitalUsd)
                            else "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if ((r.maxExecutableCapitalUsd ?: 0.0) > 0) ProfitGreen else LossRed
                        )
                        if (r.depthExhausted) {
                            Text(
                                Strings.t("depth_exhausted_warning"),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarnAmber,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // الربح الحقيقي المتوقع لو نفذت بنفس رأس المال ده بالظبط - محسوب
                        // من متوسط سعر التنفيذ الفعلي (VWAP) من عمق السجل، مش السعر
                        // اللحظي السريع الظاهر في القايمة الرئيسية
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Text(
                                Strings.t("real_profit_title") + " ($" +
                                    String.format(Locale.US, "%.0f", capitalUsd) + "):",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            val realBreakdown = r.realProfitBreakdown(buyFeePercent, sellFeePercent, withdrawalFeeUsd)
                            if (realBreakdown == null) {
                                Text(
                                    Strings.t("real_profit_unavailable"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarnAmber
                                )
                            } else {
                                Text(
                                    "$" + String.format(Locale.US, "%.2f", realBreakdown.netProfitUsd) +
                                        " (" + String.format(Locale.US, "%.2f", realBreakdown.netProfitPercent) + "%)",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (realBreakdown.netProfitUsd > 0) ProfitGreen else LossRed
                                )
                                Text(
                                    Strings.t("real_profit_buy_price") + " $" +
                                        String.format(Locale.US, "%.6f", realBreakdown.buyVwapPrice) +
                                        " · " + Strings.t("real_profit_sell_price") + " $" +
                                        String.format(Locale.US, "%.6f", realBreakdown.sellVwapPrice),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    Strings.t("real_profit_fees_note") + ": " +
                                        Strings.t("real_profit_gross") + " $" +
                                        String.format(Locale.US, "%.2f", realBreakdown.grossProfitUsd) +
                                        " − " + Strings.t("fees") + " $" +
                                        String.format(Locale.US, "%.2f", realBreakdown.buyFeeUsd + realBreakdown.sellFeeUsd) +
                                        " − " + Strings.t("real_profit_withdrawal") + " $" +
                                        String.format(Locale.US, "%.2f", realBreakdown.withdrawalFeeUsd),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                if (!isWithdrawalFeeVerified) {
                                    Text(
                                        Strings.t("fee_not_verified_warning"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarnAmber,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // الشبكة اللي هتتحول بيها العملة فعلياً لازم تكون شغالة على الجانبين
                        // مع بعض: سحب من منصة الشراء + إيداع في منصة البيع. لو عرفنا نطابق
                        // شبكة مشتركة مؤكدة، نستخدمها هي (مش أرخص شبكة سحب لوحدها بس)
                        val commonNetwork = r.commonNetwork
                        val transferNetworkName = commonNetwork?.networkName
                            ?: r.buyWalletStatus?.networkName
                            ?: WithdrawalFees.networkNameFor(coin.id)
                        val transferRange = NetworkTransferTime.estimateFor(transferNetworkName)
                        Text(
                            Strings.t("transfer_time_estimate_label") +
                                (transferNetworkName?.let { " ($it)" } ?: "") + ": " +
                                if (transferRange != null) {
                                    if (Strings.language.value == AppLanguage.AR) NetworkTransferTime.formatRange(transferRange)
                                    else NetworkTransferTime.formatRangeEn(transferRange)
                                } else {
                                    Strings.t("transfer_time_unknown")
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // تحذير لو رأس المال أقل من الحد الأدنى النموذجي لصفقة واحدة على
                        // إحدى المنصتين - الصفقة غالباً هترفض فعلياً حتى لو الأرقام رابحة
                        MinimumOrderSize.firstBelowMinimum(capitalUsd, buyExchange, sellExchange)?.let { (exchangeName, minUsd) ->
                            Text(
                                "⚠️ $exchangeName: " + Strings.t("min_order_warning_detail") +
                                    " (min ~$" + String.format(Locale.US, "%.1f", minUsd) + ") " +
                                    Strings.t("min_order_typical_note"),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarnAmber,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // ملخص الشبكة المشتركة المؤكدة (سحب من الشراء + إيداع في البيع بنفس
                        // الاسم) - ده أهم حاجة قبل أي تحويل فعلي، لأن التحويل على شبكة
                        // غلط بين منصتين بيضيع العملة فعلياً من غير رجعة
                        val bothSidesHaveNetworkData = r.buyWalletStatus?.availableNetworks != null &&
                            r.sellWalletStatus?.availableNetworks != null
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                Strings.t("common_network_title"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            when {
                                commonNetwork != null -> {
                                    Text(
                                        "${commonNetwork.networkName}: " +
                                            String.format(Locale.US, "%.8f", commonNetwork.withdrawFeeNative),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ProfitGreen
                                    )
                                    Text(
                                        Strings.t("common_network_found"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                bothSidesHaveNetworkData -> {
                                    Text(
                                        Strings.t("common_network_not_found"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LossRed
                                    )
                                }
                                else -> {
                                    Text(
                                        Strings.t("common_network_unavailable"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarnAmber
                                    )
                                }
                            }
                        }

                        Text(
                            Strings.t("wallet_status_title"),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        // منصة الشراء: العملة بعد الشراء لازم "تتسحب" منها عشان تتحول لمنصة
                        // البيع. منصة البيع: العملة القادمة لازم "تتودع" فيها عشان تتباع
                        WalletStatusRow(buyExchange, r.buyWalletStatus, isDepositSide = false)
                        WalletStatusRow(sellExchange, r.sellWalletStatus, isDepositSide = true)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { refreshKey++ }) { Text(Strings.t("retry")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.t("close")) }
        }
    )
}

@Composable
private fun WalletStatusRow(exchangeName: String, status: WalletNetworkStatus?, isDepositSide: Boolean) {
    val label = if (isDepositSide) Strings.t("wallet_deposit") else Strings.t("wallet_withdraw")
    val relevantFlag = if (isDepositSide) status?.depositEnabled else status?.withdrawEnabled

    val (text, color) = when {
        status == null || !status.isVerifiable -> Strings.t("wallet_unverifiable") to WarnAmber
        relevantFlag == true -> Strings.t("wallet_enabled") to ProfitGreen
        relevantFlag == false -> Strings.t("wallet_disabled") to LossRed
        else -> Strings.t("wallet_unverifiable") to WarnAmber
    }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$exchangeName ($label)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
        // لو عندنا رسم سحب حقيقي مسحوب حياً (من Endpoint عام أو مفتاح شخصي)، نوريه هنا -
        // وده أصلاً أرخص شبكة شغالة من بين كل الشبكات المتاحة، مش أول شبكة أو الافتراضية بس
        if (!isDepositSide && status?.withdrawFeeNative != null) {
            Text(
                Strings.t("live_withdraw_fee_label") + " " +
                    String.format(Locale.US, "%.8f", status.withdrawFeeNative) +
                    (status.networkName?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = ProfitGreen
            )
            if ((status.availableNetworks?.size ?: 0) > 1) {
                Text(
                    Strings.t("live_withdraw_fee_cheapest_note"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // لو العملة معاها أكتر من شبكة سحب متاحة، نوريهم كلهم مرتبين من الأرخص للأغلى -
        // عشان المستخدم يقدر يشوف ويقرر بنفسه لو حاب شبكة تانية غير اللي اخترناها تلقائياً
        if (!isDepositSide && (status?.availableNetworks?.size ?: 0) > 1) {
            Text(
                Strings.t("available_networks_title"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            status!!.availableNetworks!!.forEach { option ->
                val isCheapestEnabled = option.networkName == status.networkName &&
                    option.withdrawFeeNative == status.withdrawFeeNative
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        (if (isCheapestEnabled) "✓ " else "") + option.networkName +
                            (if (!option.withdrawEnabled) " (${Strings.t("network_disabled_badge")})" else ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!option.withdrawEnabled) TextSecondary else if (isCheapestEnabled) ProfitGreen else TextSecondary
                    )
                    Text(
                        String.format(Locale.US, "%.8f", option.withdrawFeeNative),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCheapestEnabled) ProfitGreen else TextSecondary
                    )
                }
            }
        }
    }
}
