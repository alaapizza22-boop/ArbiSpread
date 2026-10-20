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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbispread.app.model.TriangularQuotes
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BorderSubtle
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TriangularCard(quote: TriangularQuotes, capitalUsd: Double, modifier: Modifier = Modifier) {
    val (isForward, flow) = quote.bestFlow(capitalUsd.coerceAtLeast(1.0))
    val isPlausible = quote.isPlausible(capitalUsd.coerceAtLeast(1.0))
    val isProfit = flow.netProfitUsd > 0.0
    val accent = if (!isPlausible) TextSecondary else if (isProfit) ProfitGreen else LossRed

    val pathText = if (isForward) {
        "USDT → ${quote.bridgeCoin} → ${quote.targetCoin.id} → USDT"
    } else {
        "USDT → ${quote.targetCoin.id} → ${quote.bridgeCoin} → USDT"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isProfit && isPlausible) ProfitGreen.copy(alpha = 0.5f) else BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(quote.exchangeName, style = MaterialTheme.typography.titleMedium, color = AccentCyan, fontWeight = FontWeight.Bold)
                    Text(pathText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Text(
                    text = (if (flow.netProfitPercent >= 0) "+" else "") + String.format(Locale.US, "%.3f", flow.netProfitPercent) + "%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            if (!isPlausible) {
                Text(
                    text = Strings.t("no_data"),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }

            Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = Strings.t("triangular_theoretical") + ": " + (if (flow.grossProfitUsd >= 0) "+" else "") +
                    "$" + String.format(Locale.US, "%.2f", flow.grossProfitUsd) +
                    " (" + String.format(Locale.US, "%.3f", flow.grossProfitPercent) + "%)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = Strings.t("triangular_total_fees") + ": -$" + String.format(Locale.US, "%.2f", flow.totalFeesUsd) +
                    " (" + String.format(Locale.US, "%.2f", quote.takerFeePercent) + "% × 3)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = Strings.t("triangular_net") + ": " + (if (flow.netProfitUsd >= 0) "+" else "") +
                    "$" + String.format(Locale.US, "%.2f", flow.netProfitUsd),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (!isProfit) {
                Text(
                    text = Strings.t("triangular_no_opportunity"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text(
                text = Strings.t("triangular_capital_note"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
