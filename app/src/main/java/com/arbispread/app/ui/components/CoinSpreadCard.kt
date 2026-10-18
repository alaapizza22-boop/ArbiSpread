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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arbispread.app.model.CoinSpreadResult
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.BgCard
import com.arbispread.app.ui.theme.BorderSubtle
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs

@Composable
fun CoinSpreadCard(result: CoinSpreadResult, capitalUsd: Double, modifier: Modifier = Modifier) {
    val netPercent = result.netProfitPercent(capitalUsd)
    val netUsd = result.netProfitUsd(capitalUsd)
    val isProfit = (netPercent ?: 0.0) > 0.0
    val accent = if (!result.isAvailable) TextSecondary else if (isProfit) ProfitGreen else LossRed

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
                Column {
                    Text(result.coin.id, style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                    Text(result.coin.displayName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                    price = result.buy.price,
                    color = ProfitGreen
                )
                ExchangeColumn(
                    label = Strings.t("sell_on"),
                    exchangeName = result.sell!!.exchangeName,
                    price = result.sell.price,
                    color = LossRed,
                    alignEnd = true
                )
            }

            Text(
                text = Strings.t("spread") + ": " + String.format(Locale.US, "%.2f", result.spreadPercent ?: 0.0) + "%  ·  " +
                    Strings.t("fees") + ": " + String.format(Locale.US, "%.2f", abs((result.spreadPercent ?: 0.0) - (netPercent ?: 0.0))) + "%",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )

            if (netUsd != null && capitalUsd > 0.0) {
                Text(
                    text = Strings.t("net_profit_usd") + ": " + (if (netUsd >= 0) "+" else "") +
                        "$" + String.format(Locale.US, "%.2f", netUsd),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExchangeColumn(
    label: String,
    exchangeName: String,
    price: Double,
    color: Color,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(exchangeName, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text("$" + String.format(Locale.US, "%.4f", price), style = MaterialTheme.typography.bodyMedium)
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
