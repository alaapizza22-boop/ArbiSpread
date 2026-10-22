package com.arbispread.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.arbispread.app.data.TriangularLiquidityRepository
import com.arbispread.app.model.TriangularExecutionResult
import com.arbispread.app.model.TriangularQuotes
import com.arbispread.app.ui.Strings
import com.arbispread.app.ui.theme.AccentCyan
import com.arbispread.app.ui.theme.LossRed
import com.arbispread.app.ui.theme.ProfitGreen
import com.arbispread.app.ui.theme.TextSecondary
import com.arbispread.app.ui.theme.WarnAmber
import java.util.Locale

/**
 * حوار "التحقق قبل التنفيذ" للتداول الثلاثي - بيتفتح عند الطلب بس (مش تلقائي)، وبيسحب
 * عمق دفتر الطلبات الفعلي للثلاث أزواج لحظة الفتح، ويحسب النتيجة الحقيقية بعد المشي
 * عبر العمق الكامل (VWAP) بدل أفضل سعر بس - عشان يمنع تنفيذ صفقة البطاقة بتعرضها
 * رابحة لكن فعلياً هتبقى خسارة بسبب الانزلاق السعري.
 */
@Composable
fun TriangularExecutionDialog(
    quote: TriangularQuotes,
    capitalUsd: Double,
    onDismiss: () -> Unit,
) {
    var result by remember { mutableStateOf<TriangularExecutionResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        result = TriangularLiquidityRepository.checkExecution(quote, capitalUsd.coerceAtLeast(1.0))
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.t("pre_execution_check_title") + " · ${quote.exchangeName}") },
        text = {
            Column {
                if (isLoading) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp), color = AccentCyan)
                        Text(Strings.t("pre_execution_checking"), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val r = result
                    if (r == null || r.errorMessage != null) {
                        Text(
                            Strings.t("pre_execution_error") + (r?.errorMessage?.let { ": $it" } ?: ""),
                            color = LossRed
                        )
                    } else {
                        Text(
                            Strings.t("pre_execution_top_of_book") + ": " +
                                (if (r.topOfBookNetProfitUsd >= 0) "+" else "") +
                                "$" + String.format(Locale.US, "%.2f", r.topOfBookNetProfitUsd),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )

                        Text(
                            Strings.t("pre_execution_depth_aware") + ":",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        val netProfit = r.depthAwareNetProfitUsd
                        val isProfit = (netProfit ?: 0.0) > 0.0
                        Text(
                            if (netProfit != null)
                                (if (netProfit >= 0) "+" else "") + "$" + String.format(Locale.US, "%.2f", netProfit)
                            else "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (netProfit == null) WarnAmber else if (isProfit) ProfitGreen else LossRed
                        )

                        if (r.wouldBeLossFromSlippage) {
                            Text(
                                Strings.t("pre_execution_loss_warning"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LossRed,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        if (r.depthExhausted) {
                            Text(
                                Strings.t("pre_execution_depth_exhausted"),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarnAmber,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
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
