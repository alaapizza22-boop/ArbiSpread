package com.arbispread.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbispread.app.model.CoinPersistenceInfo
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.model.ExchangeLinks
import com.arbispread.app.model.MinimumOrderSize
import com.arbispread.app.model.WithdrawalFees
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BorderSubtle
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.ui.theme.WarnAmber
import java.util.Locale
import kotlin.math.abs

@Composable
fun CoinSpreadCard(
    result: CoinSpreadResult,
    capitalUsd: Double,
    persistence: CoinPersistenceInfo? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val breakdown = result.feeBreakdown(capitalUsd)
    val netPercent = breakdown?.netProfitPercent
    val netUsd = breakdown?.netProfitUsd
    val isProfit = (netPercent ?: 0.0) > 0.0
    val accent = if (!result.isAvailable) TextSecondary else if (isProfit) ProfitGreen else LossRed
    var showLiquidityDialog by remember { mutableStateOf(false) }

    // أنيميشن بسيط للنسبة وقت ما تتحدّث، عشان الواجهة تحس إنها "حيّة"
    var displayedNet by remember { mutableFloatStateOf(netPercent?.toFloat() ?: 0f) }
    LaunchedEffect(netPercent) { displayedNet = netPercent?.toFloat() ?: 0f }
    val animatedNet by animateFloatAsState(
        targetValue = displayedNet,
        animationSpec = tween(400),
        label = "netProfitAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isProfit && result.isAvailable) ProfitGreen.copy(alpha = 0.5f) else BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = Strings.t("favorite_toggle"),
                            tint = if (isFavorite) WarnAmber else TextSecondary
                        )
                    }
                    Column {
                        Text(result.coin.id, style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                        Text(result.coin.displayName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }

                if (result.isAvailable) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
                        Text(
                            text = (if (animatedNet >= 0) "+" else "") + String.format(Locale.US, "%.2f", animatedNet) + "%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!result.isAvailable) {
                Text(
                    text = Strings.t("no_data"),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExchangeColumn(
                    label = Strings.t("buy_from"),
                    exchangeName = result.buy!!.exchangeName,
                    coinId = result.coin.id,
                    price = result.buy.price,
                    color = ProfitGreen
                )
                ExchangeColumn(
                    label = Strings.t("sell_on"),
                    exchangeName = result.sell!!.exchangeName,
                    coinId = result.coin.id,
                    price = result.sell.price,
                    color = LossRed,
                    alignEnd = true
                )
            }

            Text(
                text = Strings.t("spread") + ": " + String.format(Locale.US, "%.2f", result.spreadPercent ?: 0.0) + "%",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )

            // تفصيل كامل وشفاف بالدولار لكل خطوة في الحساب - مفيش رقم "من غير مصدر"
            if (breakdown != null) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    FeeLine(
                        Strings.t("gross_profit") + ": +$" + String.format(Locale.US, "%.2f", breakdown.grossProfitUsd)
                    )
                    FeeLine(
                        Strings.t("buy_fee") + " (" + String.format(Locale.US, "%.2f", breakdown.buyFeePercent) + "%): -$" +
                            String.format(Locale.US, "%.2f", breakdown.buyFeeUsd)
                    )
                    FeeLine(
                        Strings.t("sell_fee") + " (" + String.format(Locale.US, "%.2f", breakdown.sellFeePercent) + "%): -$" +
                            String.format(Locale.US, "%.2f", breakdown.sellFeeUsd)
                    )
                    val networkName = WithdrawalFees.networkNameFor(result.coin.id)
                    FeeLine(
                        Strings.t("withdrawal_fee_short") + ": -$" + String.format(Locale.US, "%.2f", breakdown.withdrawalFeeUsd) +
                            if (networkName != null) " (${Strings.t("network_label")}: $networkName)" else ""
                    )
                    // لو عمولة التداول جاية من مفتاح المستخدم الشخصي (عمولة حسابه الفعلية)
                    // بدل القيمة الافتراضية، نوضح ده صراحة عشان يثق في الرقم أكتر
                    if (breakdown.isFeeVerified) {
                        Text(
                            Strings.t("fee_verified_live_note"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Text(
                    text = Strings.t("net_profit_usd") + ": " + (if (netUsd!! >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", netUsd),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(top = 6.dp)
                )

                // شارة استمرارية الفرصة - بتفرّق بين فرصة ومضة لحظية (محتاج تكون حذر منها)
                // وفرصة مستمرة فعلاً من كذا دقيقة (أكتر ثقة إنها لسه موجودة وقت التنفيذ).
                // بتتحسب على رأس مال مرجعي ثابت مش رأس مال المستخدم - عشان متتصفرش كل
                // مرة يغيّر الرقم، وبتظهر بس لما الصفقة رابحة دلوقتي
                if (isProfit && persistence != null && persistence.consecutiveProfitableCount > 0) {
                    val isStrong = persistence.consecutiveProfitableCount >= CoinPersistenceInfo.STRONG_STREAK_THRESHOLD
                    val persistenceText = when {
                        persistence.consecutiveProfitableCount <= 1 -> "⚡ " + Strings.t("persistence_just_appeared")
                        persistence.persistenceMinutes < 1.0 -> "👀 " + Strings.t("persistence_seconds_label")
                        else -> (if (isStrong) "🔥 " else "👀 ") + Strings.t("persistence_label") + " ~" +
                            persistence.persistenceMinutes.toInt() + " " + Strings.t("persistence_minutes_suffix")
                    }
                    Text(
                        text = persistenceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStrong) ProfitGreen else if (persistence.consecutiveProfitableCount <= 1) WarnAmber else TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // تحذير لو رأس المال المدخل أقل من الحد الأدنى النموذجي لصفقة واحدة على
                // إحدى المنصتين - الصفقة غالباً هترفض فعلياً حتى لو النسبة% بتقول رابحة
                MinimumOrderSize.firstBelowMinimum(capitalUsd, result.buy!!.exchangeName, result.sell!!.exchangeName)?.let { (exchangeName, minUsd) ->
                    Text(
                        text = Strings.t("min_order_warning_card") + " $exchangeName (min ~$" +
                            String.format(Locale.US, "%.1f", minUsd) + ") " + Strings.t("min_order_typical_note"),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnAmber,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // زرار فحص السيولة الحية وحالة المحافظ - بيتسحب عند الطلب بس (مش تلقائي لكل عملة)
                Button(
                    onClick = { showLiquidityDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BgCard, contentColor = AccentCyan),
                    border = BorderStroke(1.dp, AccentCyan),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        Strings.t("liquidity_check_cta"),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                if (showLiquidityDialog) {
                    LiquidityCheckDialog(
                        coin = result.coin,
                        buyExchange = result.buy!!.exchangeName,
                        sellExchange = result.sell!!.exchangeName,
                        capitalUsd = capitalUsd,
                        onDismiss = { showLiquidityDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeeLine(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
}

@Composable
private fun ExchangeColumn(
    label: String,
    exchangeName: String,
    coinId: String,
    price: Double,
    color: Color,
    alignEnd: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val tradeUrl = ExchangeLinks.spotTradeUrl(exchangeName, coinId)

    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignEnd && tradeUrl != null) {
                OpenExchangeButton(tradeUrl, exchangeName, uriHandler)
            }
            Text(exchangeName, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            if (!alignEnd && tradeUrl != null) {
                OpenExchangeButton(tradeUrl, exchangeName, uriHandler)
            }
        }
        Text("$" + String.format(Locale.US, "%.4f", price), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OpenExchangeButton(
    url: String,
    exchangeName: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    IconButton(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            Icons.Filled.OpenInNew,
            contentDescription = Strings.t("open_exchange") + " " + exchangeName,
            tint = AccentCyan,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun LockedCoinCard(onUnlockClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUnlockClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary)
                Text(
                    Strings.t("locked_label"),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = AccentCyan)
                Text(
                    Strings.t("unlock_cta"),
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
