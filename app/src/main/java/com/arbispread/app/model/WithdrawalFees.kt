package com.arbispread.app.model

/**
 * رسوم سحب شبكة التحويل (Network Withdrawal Fee) + اسم الشبكة الأساسية/الأرخص عادة
 * لأشهر العملات، مكتوبة بوحدة العملة نفسها (مش بالدولار) زي ما بتظهر فعلياً في صفحة
 * "Withdraw" بأي منصة (مثال: "0.0002 BTC" مش "$9").
 *
 * **ليه القيمة دي بوحدة العملة مش بالدولار مباشرة؟**
 * عشان نحوّلها لدولار بضربها في السعر الحي اللي التطبيق أصلاً سحبه من المنصة لحظة
 * الحساب - فالتقدير بالدولار بيتحرك تلقائياً مع السعر الحقيقي، بدل رقم دولار ثابت.
 *
 * **تنبيه مهم وصريح عن حدود الدقة:**
 * مفيش ولا منصة من الخمسة (Binance, Bybit, OKX, KuCoin, Gate.io) بتوفّر Endpoint عام
 * (Public API من غير تسجيل دخول بمفتاح API شخصي) بيرجّع رسوم السحب أو الشبكات المتاحة
 * فعلياً لحظياً - دي بيانات مرتبطة بحساب المستخدم ومحتاجة توثيق (Authentication)،
 * والتطبيق ده معلوماتي بس وما بيطلبش منك مفتاح API عشان يفضل بعيد تماماً عن حسابك
 * وفلوسك. فالقيم هنا:
 * - **مرجعية (Reference/Typical)** مبنية على الشبكة الأساسية/الأرخص المعتادة والرسوم
 *   المعتادة المنشورة publicly من المنصات وقت كتابة الكود - مش أرقام أو أسماء مختلقة.
 * - **مش ضمان** للرقم أو الشبكة بالظبط وقت تحويلك إنت، لأن الشبكات المتاحة لنفس العملة
 *   بتختلف من منصة لمنصة، والرسوم بتتغير فعلياً حسب ازدحام الشبكة (غاز الإيثريوم مثلاً).
 * - **لازم تتأكد بنفسك** من صفحة Withdraw الفعلية في المنصة قبل أي قرار حقيقي - التطبيق
 *   بيوريك اسم الشبكة الأساسية/المتوقعة بس كدليل استرشادي، مش تأكيد 100% إنها متاحة.
 *
 * العملات اللي مش موجودة في الجدول ده (أغلب الـ 100 عملة، خصوصاً الأقل سيولة) بترجع
 * null من [estimateUsd] و[networkNameFor]، وعندها التطبيق بيستخدم القيمة اليدوية العامة
 * القابلة للتعديل من شاشة الإعدادات (افتراضياً صفر، مش رقم وهمي).
 */
object WithdrawalFees {

    /** رسم الشبكة بوحدة العملة نفسها + اسم الشبكة الأساسية/المتوقعة لنفس العملة */
    private data class NetworkFee(val networkName: String, val nativeFee: Double)

    private val reference: Map<String, NetworkFee> = mapOf(
        "BTC" to NetworkFee("Bitcoin", 0.0002),
        "ETH" to NetworkFee("ERC20 (Ethereum)", 0.002),
        "BNB" to NetworkFee("BEP20 (BNB Smart Chain)", 0.0005),
        "SOL" to NetworkFee("Solana", 0.01),
        "XRP" to NetworkFee("XRP Ledger", 0.15),
        "ADA" to NetworkFee("Cardano", 0.6),
        "DOGE" to NetworkFee("Dogecoin", 5.0),
        "TON" to NetworkFee("TON", 0.05),
        "DOT" to NetworkFee("Polkadot", 0.05),
        "LTC" to NetworkFee("Litecoin", 0.001),
        "TRX" to NetworkFee("TRC20 (Tron)", 1.0),
        "AVAX" to NetworkFee("Avalanche C-Chain", 0.01),
        "LINK" to NetworkFee("ERC20 (Ethereum) — دوّر على شبكة أرخص (BEP20/Polygon) لو متاحة", 0.3),
        "BCH" to NetworkFee("Bitcoin Cash", 0.0001),
        "NEAR" to NetworkFee("NEAR Protocol", 0.01),
        "MATIC" to NetworkFee("Polygon", 0.1),
        "UNI" to NetworkFee("ERC20 (Ethereum) — دوّر على شبكة أرخص لو متاحة", 0.3),
        "ICP" to NetworkFee("Internet Computer", 0.001),
        "ETC" to NetworkFee("Ethereum Classic", 0.01),
        "XLM" to NetworkFee("Stellar", 0.01),
        "ATOM" to NetworkFee("Cosmos", 0.01),
        "FIL" to NetworkFee("Filecoin", 0.01),
        "APT" to NetworkFee("Aptos", 0.01),
        "HBAR" to NetworkFee("Hedera", 1.0),
        "ARB" to NetworkFee("Arbitrum One", 0.1),
        "VET" to NetworkFee("VeChain", 10.0),
        "OP" to NetworkFee("Optimism", 0.1),
        "MKR" to NetworkFee("ERC20 (Ethereum) — دوّر على شبكة أرخص لو متاحة", 0.002),
        "GRT" to NetworkFee("ERC20 (Ethereum) — دوّر على شبكة أرخص لو متاحة", 5.0),
        "AAVE" to NetworkFee("ERC20 (Ethereum) — دوّر على شبكة أرخص لو متاحة", 0.01),
    )

    /** بيرجع true لو العملة دي معاها بيانات مرجعية حقيقية في الجدول (مش تقدير عام) */
    fun hasReferenceFee(coinId: String): Boolean = reference.containsKey(coinId)

    /**
     * بيحسب رسوم السحب التقديرية بالدولار = الرسم بوحدة العملة × سعرها الحي الحالي.
     * بيرجع null لو العملة مش في الجدول المرجعي (ساعتها التطبيق بيستخدم القيمة اليدوية
     * العامة من الإعدادات بدل ما يختلق رقم).
     */
    fun estimateUsd(coinId: String, livePriceUsd: Double): Double? {
        if (livePriceUsd <= 0.0) return null
        val fee = reference[coinId] ?: return null
        return fee.nativeFee * livePriceUsd
    }

    /** اسم الشبكة الأساسية/الأرخص عادة لنقل العملة دي - دليل استرشادي، تأكد منه قبل السحب فعلياً */
    fun networkNameFor(coinId: String): String? = reference[coinId]?.networkName
}
