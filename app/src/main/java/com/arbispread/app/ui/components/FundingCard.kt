package com.arbispread.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbispread.app.model.FundingOpportunity
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BorderSubtle
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FundingOpportunityCard(opportunity: FundingOpportunity, capitalUsd: Double, modifier: Modifier = Modifier) {
    val dailyPercent = opportunity.estimatedDailyProfitPercent()
    val dailyUsd = opportunity.estimatedDailyProfitUsd(capitalUsd)
    val isProfit = (dailyPercent ?: 0.0) > 0.0
    val accent = if (!opportunity.isAvailable) TextSecondary else if (isProfit) ProfitGreen else LossRed

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(Strings.t("long_on"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(long.exchangeName, style = MaterialTheme.typography.titleMedium, color = ProfitGreen, fontWeight = FontWeight.Bold)
                    Text(
                        String.format(Locale.US, "%.4f", long.fundingRatePercent) + "%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(formatNextFunding(long.nextFundingTimeMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Strings.t("short_on"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(short.exchangeName, style = MaterialTheme.typography.titleMedium, color = LossRed, fontWeight = FontWeight.Bold)
                    Text(
                        String.format(Locale.US, "%.4f", short.fundingRatePercent) + "%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(formatNextFunding(short.nextFundingTimeMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            if (dailyUsd != null && capitalUsd > 0.0) {
                Text(
                    text = Strings.t("est_daily_funding_profit") + ": " + (if (dailyUsd >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", dailyUsd),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(top = 10.dp)
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
