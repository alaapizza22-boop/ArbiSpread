package com.arbispread.app.data

import android.content.Context

/**
 * نسب عمولة التداول (Taker Fee تقريبية) لكل منصة + عمولة سحب الشبكة التقديرية بالدولار.
 * دي قيم افتراضية شائعة - المستخدم يقدر يعدّلها من شاشة الإعدادات
 * لأن العمولة الحقيقية بتختلف حسب مستوى حساب كل شخص (VIP tiers, خصومات توكن المنصة...)
 * وحسب الشبكة المستخدمة في السحب (TRC20 أرخص من ERC20 مثلاً).
 */
data class FeeConfig(
    var binance: Double = 0.10,
    var bybit: Double = 0.10,
    var okx: Double = 0.10,
    var kucoin: Double = 0.10,
    var gateio: Double = 0.20,
    var coinbase: Double = 0.60,
    var withdrawalFeeUsd: Double = 2.0,
) {
    fun feeFor(exchangeName: String): Double = when (exchangeName) {
        "Binance" -> binance
        "Bybit" -> bybit
        "OKX" -> okx
        "KuCoin" -> kucoin
        "Gate.io" -> gateio
        "Coinbase" -> coinbase
        else -> 0.1
    }

    companion object {
        private const val PREFS = "arbi_spread_prefs"

        fun load(context: Context): FeeConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return FeeConfig(
                binance = prefs.getFloat("fee_binance", 0.10f).toDouble(),
                bybit = prefs.getFloat("fee_bybit", 0.10f).toDouble(),
                okx = prefs.getFloat("fee_okx", 0.10f).toDouble(),
                kucoin = prefs.getFloat("fee_kucoin", 0.10f).toDouble(),
                gateio = prefs.getFloat("fee_gateio", 0.20f).toDouble(),
                coinbase = prefs.getFloat("fee_coinbase", 0.60f).toDouble(),
                withdrawalFeeUsd = prefs.getFloat("fee_withdrawal_usd", 2.0f).toDouble(),
            )
        }

        fun save(context: Context, config: FeeConfig) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("fee_binance", config.binance.toFloat())
                .putFloat("fee_bybit", config.bybit.toFloat())
                .putFloat("fee_okx", config.okx.toFloat())
                .putFloat("fee_kucoin", config.kucoin.toFloat())
                .putFloat("fee_gateio", config.gateio.toFloat())
                .putFloat("fee_coinbase", config.coinbase.toFloat())
                .putFloat("fee_withdrawal_usd", config.withdrawalFeeUsd.toFloat())
                .apply()
        }
    }
}
