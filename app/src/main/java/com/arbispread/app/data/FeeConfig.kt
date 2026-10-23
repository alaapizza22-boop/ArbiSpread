package com.arbispread.app.data

import android.content.Context

/**
 * نسب عمولة التداول الفورية (Spot Taker Fee) الافتراضية (Standard/Regular tier - من غير خصم
 * توكن المنصة زي BNB على Binance أو مستوى VIP)، كما هي منشورة رسمياً في صفحة جدول العمولات
 * الخاصة بكل منصة وقت كتابة هذا الكود:
 *   - Binance : 0.10% (Spot, Regular user, من غير خصم BNB)
 *   - Bybit   : 0.10% (Spot, Regular user)
 *   - OKX     : 0.10% (Spot, Regular/Lv.1 user)
 *   - KuCoin  : 0.10% (Spot, Regular user)
 *   - Gate.io : 0.20% (Spot, Regular user)
 *   - Bitget  : 0.10% (Spot, Regular user)
 *   - MEXC    : 0.20% (Spot, Regular user - MEXC أحياناً بيطلع عروض 0% مؤقتة، اتأكد من حسابك)
 *
 * ملحوظتين مهمتين لازم تكونا واضحتين:
 * 1. العمولة الفعلية بتختلف حسب مستوى حسابك (VIP tiers حسب حجم تداولك الشهري) وخصومات
 *    توكن المنصة - الأرقام دي هي "القيمة الافتراضية للمستخدم العادي" مش رقم مختلق، لكنها
 *    برضه مش بالضرورة نفس رقمك أنت بالظبط. **لازم تتأكد من صفحة Fee Schedule الرسمية لحسابك
 *    على كل منصة وتعدّل الأرقام دي من شاشة الإعدادات لو مختلفة.**
 * 2. مفيش منصة بتسمح بسحب نسبة العمولة الشخصية لحسابك من API عام (Public API) من غير
 *    تسجيل دخول بمفتاح API حقيقي (API Key + Secret) - وده التطبيق متعمد إنه ميطلبوش
 *    عشان يفضل تطبيق معلوماتي بس (مش متصل بحسابك ولا بيقدر يلمس فلوسك). فالتطبيق مينفعش
 *    "يسحب" العمولة الحقيقية تلقائياً، وده سبب تقني حقيقي مش قصور في الكود.
 */
data class FeeConfig(
    var binance: Double = 0.10,
    var bybit: Double = 0.10,
    var okx: Double = 0.10,
    var kucoin: Double = 0.10,
    var gateio: Double = 0.20,
    var bitget: Double = 0.10,
    var mexc: Double = 0.20,

    /**
     * عمولة سحب شبكة التحويل (Network Withdrawal Fee) بالدولار - لازم تدخلها إنت يدوياً
     * قبل ما تعتمد على الحساب، لأنها بتختلف جذرياً حسب:
     *   - العملة نفسها (BTC مختلفة تماماً عن USDT عن Dogecoin...)
     *   - الشبكة اللي هتختارها للتحويل (TRC20 عادة أرخص بكتير من ERC20 لنفس العملة)
     *   - سعر العملة وقتها (لو العمولة ثابتة بعدد من العملة، قيمتها بالدولار بتتغير مع السعر)
     *   - ازدحام الشبكة وقت التحويل (رسوم غاز الإيثريوم مثلاً بتتضاعف في أوقات الزحمة)
     * القيمة دي مفيش API عام بيرجّعها بدقة لحظية بدون مفتاح حساب شخصي، فالافتراضي هنا **صفر**
     * عمداً (مش رقم مختلق زي $2) - التطبيق مش هيضيف أي خصم وهمي لحد ما تدخل الرقم الحقيقي
     * بنفسك من صفحة السحب (Withdraw) في المنصة اللي هتحوّل منها فعلياً.
     */
    var withdrawalFeeUsd: Double = 0.0,

    /**
     * عمولة فتح/قفل مركز فيوتشرز (Taker) على Binance وBybit - مستخدمة في حساب فاندينج
     * آربيتراج بس (مش آربيتراج السبوت). القيم الافتراضية هي السعر المعياري المنشور
     * رسمياً للمستخدم العادي (من غير خصم توكن أو مستوى VIP) وقت كتابة الكود:
     *   - Binance USDⓈ-M Futures : 0.05% (Taker, Regular user)
     *   - Bybit USDT Perpetual   : 0.055% (Taker, Regular user)
     * نفس ملاحظة عمولة السبوت: دي مش رقم مختلق، لكنها برضه مش بالضرورة رقمك بالظبط -
     * اتأكد من صفحة الرسوم الرسمية لحسابك وعدّل لو مختلف.
     */
    var binanceFuturesTaker: Double = 0.05,
    var bybitFuturesTaker: Double = 0.055,
) {
    fun feeFor(exchangeName: String): Double = when (exchangeName) {
        "Binance" -> binance
        "Bybit" -> bybit
        "OKX" -> okx
        "KuCoin" -> kucoin
        "Gate.io" -> gateio
        "Bitget" -> bitget
        "MEXC" -> mexc
        else -> 0.1
    }

    fun futuresFeeFor(exchangeName: String): Double = when (exchangeName) {
        "Binance" -> binanceFuturesTaker
        "Bybit" -> bybitFuturesTaker
        else -> 0.05
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
                bitget = prefs.getFloat("fee_bitget", 0.10f).toDouble(),
                mexc = prefs.getFloat("fee_mexc", 0.20f).toDouble(),
                withdrawalFeeUsd = prefs.getFloat("fee_withdrawal_usd", 0.0f).toDouble(),
                binanceFuturesTaker = prefs.getFloat("fee_binance_futures_taker", 0.05f).toDouble(),
                bybitFuturesTaker = prefs.getFloat("fee_bybit_futures_taker", 0.055f).toDouble(),
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
                .putFloat("fee_bitget", config.bitget.toFloat())
                .putFloat("fee_mexc", config.mexc.toFloat())
                .putFloat("fee_withdrawal_usd", config.withdrawalFeeUsd.toFloat())
                .putFloat("fee_binance_futures_taker", config.binanceFuturesTaker.toFloat())
                .putFloat("fee_bybit_futures_taker", config.bybitFuturesTaker.toFloat())
                .apply()
        }
    }
}
