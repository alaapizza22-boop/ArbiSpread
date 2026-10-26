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
    /** هل الإيداع شغال على نفس الشبكة دي تحديداً على نفس المنصة - null لو المنصة مرجعتش
     * حالة الإيداع لكل شبكة لوحدها (بترجع حالة واحدة عامة للعملة كلها بس) */
    val depositEnabled: Boolean? = null,
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
    /** رأس المال اللي اتطلب حساب متوسط سعر تنفيذه الحقيقي - null لو محدش طلب ذلك */
    val requestedCapitalUsd: Double? = null,
    /** متوسط سعر الشراء الحقيقي (VWAP) لو نفذت [requestedCapitalUsd] فعلياً من عمق سجل
     * الطلبات (مش السعر اللحظي السريع) - null لو العمق المتاح مش كفاية أو مفيش طلب */
    val buyVwapPrice: Double? = null,
    /** نفس الفكرة لسعر البيع الحقيقي */
    val sellVwapPrice: Double? = null,
) {
    /**
     * الربح الحقيقي المتوقع لو نفذت [requestedCapitalUsd] فعلياً - محسوب من متوسط سعر
     * التنفيذ الحقيقي (VWAP) من عمق السجل، مش من السعر اللحظي السريع (Ticker) اللي
     * بيتجاهل الانزلاق. null لو العمق مش كفاية يغطي المبلغ أو مفيش بيانات عمولة.
     */
    fun realProfitBreakdown(
        buyFeePercent: Double,
        sellFeePercent: Double,
        withdrawalFeeUsd: Double,
    ): RealProfitBreakdown? {
        val capital = requestedCapitalUsd ?: return null
        val buyPrice = buyVwapPrice ?: return null
        val sellPrice = sellVwapPrice ?: return null
        if (buyPrice <= 0.0) return null

        val realSpreadPercent = (sellPrice - buyPrice) / buyPrice * 100.0
        val grossProfitUsd = capital * realSpreadPercent / 100.0
        val buyFeeUsd = capital * buyFeePercent / 100.0
        val sellFeeUsd = capital * sellFeePercent / 100.0
        val netProfitUsd = grossProfitUsd - buyFeeUsd - sellFeeUsd - withdrawalFeeUsd

        return RealProfitBreakdown(
            capitalUsd = capital,
            buyVwapPrice = buyPrice,
            sellVwapPrice = sellPrice,
            realSpreadPercent = realSpreadPercent,
            grossProfitUsd = grossProfitUsd,
            buyFeeUsd = buyFeeUsd,
            sellFeeUsd = sellFeeUsd,
            withdrawalFeeUsd = withdrawalFeeUsd,
            netProfitUsd = netProfitUsd,
        )
    }

    /**
     * بتفحص حسب اتجاه التحويل الفعلي: السحب من منصة الشراء (عشان تحوّل العملة بعد
     * الشراء)، والإيداع في منصة البيع (عشان العملة توصل وتتباع). لو أي خطوة من الاتنين
     * موقوفة فعلياً، الصفقة مش قابلة للتنفيذ حتى لو الأرقام رابحة على الورق.
     */
    val isBlockedByWallet: Boolean
        get() {
            val buyBlocked = buyWalletStatus?.withdrawEnabled == false
            val sellBlocked = sellWalletStatus?.depositEnabled == false
            return buyBlocked || sellBlocked
        }

    /**
     * بتدوّر على أرخص شبكة شغالة (سحب شغال من منصة الشراء + إيداع شغال في منصة البيع
     * بنفس الاسم) - دي الشبكة الوحيدة الصح فعلياً تستخدمها في التحويل. لو مفيش تطابق
     * واضح (بيانات الإيداع لكل شبكة مش متاحة من المنصة)، بترجع null والواجهة لازم
     * توضح إنه لازم تتأكد يدوياً قبل التحويل.
     */
    val commonNetwork: NetworkFeeOption?
        get() {
            val buyOptions = buyWalletStatus?.availableNetworks ?: return null
            val sellOptions = sellWalletStatus?.availableNetworks ?: return null
            // المطابقة بالمعيار التقني (Canonical) مش بالاسم النصي الحرفي - عشان "TRC20"
            // من منصة و"TRON" أو "TRX" من منصة تانية يتطابقوا صح، مع إنهم نفس الشبكة
            // فعلياً. راجع توثيق [NetworkNormalization] لحدود دقة المطابقة دي.
            val sellByCanonical = sellOptions.associateBy { NetworkNormalization.canonicalize(it.networkName) }
            return buyOptions
                .filter { it.withdrawEnabled }
                .filter { buyOpt ->
                    val sellOpt = sellByCanonical[NetworkNormalization.canonicalize(buyOpt.networkName)] ?: return@filter false
                    sellOpt.depositEnabled != false
                }
                .minByOrNull { it.withdrawFeeNative }
        }
}

/**
 * تفصيل الربح الحقيقي المتوقع، محسوب من متوسط سعر التنفيذ الفعلي (VWAP) من عمق سجل
 * الطلبات نفسه اللي بيحسب منه زرار "فحص السيولة" أقصى مبلغ قابل للتنفيذ - مش من السعر
 * اللحظي السريع (Ticker) اللي بيظهر في الشاشة الرئيسية وبيتجاهل الانزلاق تماماً.
 */
data class RealProfitBreakdown(
    val capitalUsd: Double,
    val buyVwapPrice: Double,
    val sellVwapPrice: Double,
    /** فرق السعر الحقيقي % محسوب من متوسطي التنفيذ الفعليين، مش من أفضل سعر بس */
    val realSpreadPercent: Double,
    val grossProfitUsd: Double,
    val buyFeeUsd: Double,
    val sellFeeUsd: Double,
    val withdrawalFeeUsd: Double,
    val netProfitUsd: Double,
) {
    val netProfitPercent: Double
        get() = if (capitalUsd > 0.0) (netProfitUsd / capitalUsd) * 100.0 else 0.0
}

/** حالة التحقق من الشبكة المشتركة لفرصة واحدة في المعاينة اللايف التلقائية */
enum class LiveNetworkStatus {
    /** لقينا شبكة سحب/إيداع مشتركة مؤكدة شغالة على الجانبين، والربح محسوب على رسمها الحقيقي */
    CONFIRMED,
    /** بيانات الشبكات متاحة من المنصتين بس مفيش شبكة واحدة شغالة عليهم مع بعض - الفرصة غير قابلة للتنفيذ فعلياً */
    NO_COMMON_NETWORK,
    /** الإيداع أو السحب موقوف بالكامل على إحدى المنصتين */
    WALLET_BLOCKED,
    /** بيانات الشبكة مش متاحة بالكامل من المنصتين (منصة محتاجة مفتاح شخصي مش مُدخل، أو فشل الفحص) - متعرفش نحكم */
    DATA_UNAVAILABLE,
}

/**
 * نتيجة المعاينة التلقائية اللايف لفرصة واحدة - بتجمع أقصى مبلغ قابل للتنفيذ من السيولة
 * + حالة الشبكة المشتركة + تأثيرها الفعلي على صافي الربح، بدل الاعتماد على رسوم السحب
 * الثابتة/المرجعية بس زي الحساب الأساسي المعروض على كل كارت.
 */
data class LiveOpportunityCheck(
    val maxExecutableUsd: Double?,
    val networkStatus: LiveNetworkStatus,
    val commonNetworkName: String?,
    val commonNetworkFeeUsd: Double?,
    /** صافي الربح بعد تطبيق رسم أرخص شبكة مشتركة فعلية (بدل الرسم الثابت/المرجعي) -
     * null إلا لو [networkStatus] كانت CONFIRMED وأمكن حساب تفصيل الربح */
    val netProfitAfterCommonNetworkUsd: Double?,
)
