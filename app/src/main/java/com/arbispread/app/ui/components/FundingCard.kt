package com.arbispread.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbispread.app.model.ExchangeLinks
import com.arbispread.app.model.FundingOpportunity
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BorderSubtle
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.ui.theme.WarnAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FundingOpportunityCard(
    opportunity: FundingOpportunity,
    capitalUsd: Double,
    longTakerFeePercent: Double,
    shortTakerFeePercent: Double,
    modifier: Modifier = Modifier
) {
    val dailyPercent = opportunity.estimatedDailyProfitPercent()
    val isProfit = (dailyPercent ?: 0.0) > 0.0
    val accent = if (!opportunity.isAvailable) TextSecondary else if (isProfit) ProfitGreen else LossRed

    // عداد تنازلي حي بيتحدث كل ثانية محلياً (من غير أي طلب شبكة) - بيستخدم فقط المواعيد
    // اللي اتجابت أصلاً من آخر تحديث بيانات من الـ API
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isProfit && opportunity.isAvailable) ProfitGreen.copy(alpha = 0.5f) else BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(opportunity.coin.id, style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                    Text(opportunity.coin.displayName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (opportunity.isAvailable) {
                    Text(
                        text = (if ((dailyPercent ?: 0.0) >= 0) "+" else "") +
                            String.format(Locale.US, "%.3f", dailyPercent ?: 0.0) + "%/" + "24h",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            if (!opportunity.isAvailable) {
                Text(
                    text = Strings.t("no_data"),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }

            val long = opportunity.longExchange!!
            val short = opportunity.shortExchange!!
            val uriHandler = LocalUriHandler.current
            val longUrl = ExchangeLinks.futuresTradeUrl(long.exchangeName, opportunity.coin.id)
            val shortUrl = ExchangeLinks.futuresTradeUrl(short.exchangeName, opportunity.coin.id)

            if (opportunity.isSynced) {
                Text(
                    text = Strings.t("funding_synced_badge"),
                    style = MaterialTheme.typography.labelSmall,
                    color = ProfitGreen,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(Strings.t("long_on"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(long.exchangeName, style = MaterialTheme.typography.titleMedium, color = ProfitGreen, fontWeight = FontWeight.Bold)
                        if (longUrl != null) {
                            IconButton(onClick = { uriHandler.openUri(longUrl) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.OpenInNew,
                                    contentDescription = Strings.t("open_exchange") + " " + long.exchangeName,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        String.format(Locale.US, "%.4f", long.fundingRatePercent) + "%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        Strings.t("funding_interval_label") + " " + formatInterval(long.fundingIntervalHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(formatNextFunding(long.nextFundingTimeMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        Strings.t("countdown_label") + " " + formatCountdown(long.nextFundingTimeMillis, nowMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Strings.t("short_on"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (shortUrl != null) {
                            IconButton(onClick = { uriHandler.openUri(shortUrl) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.OpenInNew,
                                    contentDescription = Strings.t("open_exchange") + " " + short.exchangeName,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(short.exchangeName, style = MaterialTheme.typography.titleMedium, color = LossRed, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        String.format(Locale.US, "%.4f", short.fundingRatePercent) + "%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        Strings.t("funding_interval_label") + " " + formatInterval(short.fundingIntervalHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(formatNextFunding(short.nextFundingTimeMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        Strings.t("countdown_label") + " " + formatCountdown(short.nextFundingTimeMillis, nowMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val breakdown = opportunity.feeBreakdown(capitalUsd, longTakerFeePercent, shortTakerFeePercent)
            if (breakdown != null) {
                Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    text = Strings.t("daily_gross_profit") + ": " + (if (breakdown.dailyGrossProfitUsd >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", breakdown.dailyGrossProfitUsd),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = Strings.t("roundtrip_fee") + ": -$" + String.format(Locale.US, "%.2f", breakdown.roundTripFeeUsd) +
                        " (" + Strings.t("long_on") + " " + String.format(Locale.US, "%.3f", breakdown.longTakerFeePercent) + "% + " +
                        Strings.t("short_on") + " " + String.format(Locale.US, "%.3f", breakdown.shortTakerFeePercent) + "%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = Strings.t("no_withdrawal_needed_note"),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarnAmber,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = Strings.t("first_cycle_net_profit") + ": " + (if (breakdown.firstCycleNetProfitUsd >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", breakdown.firstCycleNetProfitUsd),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (breakdown.firstCycleNetProfitUsd >= 0) ProfitGreen else LossRed,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = Strings.t("ongoing_daily_net_profit") + ": " + (if (breakdown.ongoingDailyNetProfitUsd >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", breakdown.ongoingDailyNetProfitUsd),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatNextFunding(millis: Long): String {
    if (millis <= 0L) return "--:--"
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.format(Date(millis))
    } catch (_: Exception) {
        "--:--"
    }
}

/** بيرجع فترة التسوية بصيغة مقروءة - مثلاً "كل 4 س" - بدل ما تفترض دايماً 8 ساعات */
private fun formatInterval(hours: Double): String {
    if (hours <= 0.0) return "--"
    return if (hours == hours.toLong().toDouble()) {
        "${hours.toLong()}h"
    } else {
        String.format(Locale.US, "%.1fh", hours)
    }
}

/** عداد تنازلي بالساعات:الدقايق:الثواني لحظة التسوية القادمة - محسوب محلياً من غير أي طلب API */
private fun formatCountdown(targetMillis: Long, nowMillis: Long): String {
    if (targetMillis <= 0L) return "--:--:--"
    val remaining = targetMillis - nowMillis
    if (remaining <= 0L) return "00:00:00"
    val totalSeconds = remaining / 1000L
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
