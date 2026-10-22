package com.arbispread.app.model

/** مستوى سعر واحد في دفتر الطلبات (Order Book) - السعر والكمية المتاحة عنده */
data class OrderBookLevel(val price: Double, val quantity: Double)

/** لقطة حية من دفتر الطلبات لمنصة معينة وقت الفحص */
data class OrderBookSnapshot(
    val exchangeName: String,
    val asks: List<OrderBookLevel>,  // أوامر البيع، مرتبة من الأرخص للأغلى (أسعارك وقت الشراء)
    val bids: List<OrderBookLevel>,  // أوامر الشراء، مرتبة من الأغلى للأرخص (أسعارك وقت البيع)
    val fetchedAtMillis: Long,
)

/**
 * شبكة سحب واحدة متاحة لعملة معينة على منصة معينة، بسعرها وحالتها - عشان نقدر نعرض
 * ونختار "أرخص شبكة شغالة" تلقائياً بدل الاعتماد على أول شبكة أو الشبكة الافتراضية بس
 * (كتير من العملات زي USDT/USDC بتتسحب على أكتر من شبكة بفروق رسوم ضخمة بين بعضها).
 */
data class NetworkFeeOption(
    val networkName: String,
    /** رسم السحب بوحدة العملة نفسها على الشبكة دي تحديداً */
    val withdrawFeeNative: Double,
    val withdrawEnabled: Boolean,
)

/**
 * حالة الإيداع/السحب لعملة معينة على منصة معينة.
 * null في أي حقل = "مش معروف" (مش "متاح") - التطبيق بيفرّق بوضوح بين الاتنين
 * عشان ميديش انطباع كاذب بالأمان لو فعلياً معندوش طريقة يتأكد.
 */
data class WalletNetworkStatus(
    val exchangeName: String,
    val depositEnabled: Boolean?,
    val withdrawEnabled: Boolean?,
    /** true لو المنصة دي معاها Endpoint عام حقيقي بيرجّع الحالة فعلياً (KuCoin, Gate.io)، أو مفتاح شخصي شغال (Binance/Bybit/OKX) */
    val isVerifiable: Boolean,
    /** اسم أرخص شبكة شغالة (تم اختيارها تلقائياً من [availableNetworks]) - null لو مش متاح */
    val networkName: String? = null,
    /** رسم أرخص شبكة شغالة بوحدة العملة نفسها - null لو مش متاح (مش صفر، عشان الواجهة تفرّق بين "صفر فعلي" و"مش معروف") */
    val withdrawFeeNative: Double? = null,
    /** كل الشبكات المتاحة لنفس العملة على المنصة دي (لو موجودة أكتر من واحدة) مرتبة من الأرخص للأغلى - عشان المستخدم يقدر يشوف ويقرر بنفسه لو حاب شبكة تانية غير اللي اخترناها تلقائياً */
    val availableNetworks: List<NetworkFeeOption>? = null,
)

/**
 * نتيجة فحص التنفيذ الحي الكامل لفرصة آربيتراج معينة: عمق السوق على الجانبين
 * (الشراء والبيع) + حالة المحافظ على المنصتين. بيتحسب عند الطلب بس (مش لكل الـ100
 * عملة كل تحديث) لتفادي تجاوز حدود طلبات المنصات (Rate Limits).
 */
data class LiquidityCheckResult(
    val coinId: String,
    val buyExchange: String,
    val sellExchange: String,
    val maxSlippagePercent: Double,
    /** أقصى مبلغ بالدولار ممكن تنفيذه من غير ما ينزلق السعر أكتر من [maxSlippagePercent] - null لو تعذر الفحص */
    val maxExecutableCapitalUsd: Double?,
    /** true لو العمق المتاح في اللقطة (أول N مستوى من الـ API) خلص قبل ما نوصل لحد الانزلاق - يبقى الرقم "حد أدنى مؤكد" مش الأقصى الفعلي */
    val depthExhausted: Boolean,
    val buyWalletStatus: WalletNetworkStatus?,
    val sellWalletStatus: WalletNetworkStatus?,
    val errorMessage: String?,
    val checkedAtMillis: Long,
) {
    val isBlockedByWallet: Boolean
        get() {
            val buyBlocked = buyWalletStatus?.depositEnabled == false
            val sellBlocked = sellWalletStatus?.withdrawEnabled == false
            return buyBlocked || sellBlocked
        }
}
