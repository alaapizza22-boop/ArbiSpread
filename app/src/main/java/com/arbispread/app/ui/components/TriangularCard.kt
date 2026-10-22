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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    // ملحوظة: الفرص غير المنطقية (سبريد شاذ أو نسبة غير واقعية) بتتفلتر خالص قبل
    // ما توصل هنا من PriceRepository - فلو الكارت ده ظاهر، البيانات موثوقة بالفعل
    val (isForward, flow) = quote.bestFlow(capitalUsd.coerceAtLeast(1.0))
    val isProfit = flow.netProfitUsd > 0.0
    val accent = if (isProfit) ProfitGreen else LossRed
    var showExecutionDialog by remember { mutableStateOf(false) }

    val pathText = if (isForward) {
        "USDT → ${quote.bridgeCoin} → ${quote.targetCoin.id} → USDT"
    } else {
        "USDT → ${quote.targetCoin.id} → ${quote.bridgeCoin} → USDT"
    }

    // أسعار Ask/Bid المستخدمة فعلياً في كل خطوة - للتحقق التقني قبل أي تنفيذ يدوي
    val stepPricesText = if (isForward) {
        "${quote.bridgeCoin}: Ask ${fmt(quote.bridgeBook.askPrice)} · " +
            "${quote.targetCoin.id}/${quote.bridgeCoin}: Ask ${fmt(quote.crossBook.askPrice)} · " +
            "${quote.targetCoin.id}: Bid ${fmt(quote.targetBook.bidPrice)}"
    } else {
        "${quote.targetCoin.id}: Ask ${fmt(quote.targetBook.askPrice)} · " +
            "${quote.targetCoin.id}/${quote.bridgeCoin}: Bid ${fmt(quote.crossBook.bidPrice)} · " +
            "${quote.bridgeCoin}: Bid ${fmt(quote.bridgeBook.bidPrice)}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isProfit) ProfitGreen.copy(alpha = 0.5f) else BorderSubtle)
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

            Text(
                text = stepPricesText,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )

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

            // زرار "التحقق قبل التنفيذ" - بيسحب عمق دفتر الطلبات الحقيقي للثلاث خطوات
            // عند الطلب بس (مش تلقائي)، ويحسب النتيجة الفعلية بعد الانزلاق السعري
            Button(
                onClick = { showExecutionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BgCard, contentColor = AccentCyan),
                border = BorderStroke(1.dp, AccentCyan),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    Strings.t("pre_execution_check_button"),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (showExecutionDialog) {
                TriangularExecutionDialog(
                    quote = quote,
                    capitalUsd = capitalUsd,
                    onDismiss = { showExecutionDialog = false }
                )
            }
        }
    }
}

/** تنسيق سعر للعرض المختصر جوه الكارت - بدقة متغيرة حسب حجم الرقم (عملات صغيرة القيمة محتاجة خانات عشرية أكتر) */
private fun fmt(price: Double): String = when {
    price >= 1.0 -> String.format(Locale.US, "%.4f", price)
    price >= 0.001 -> String.format(Locale.US, "%.6f", price)
    else -> String.format(Locale.US, "%.8f", price)
}
