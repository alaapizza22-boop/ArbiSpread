package com.arbispread.app.model

/**
 * توحيد أسماء شبكات السحب/الإيداع للمقارنة التقنية الحقيقية بين المنصات، بدل المقارنة
 * النصية الحرفية اللي بتفوّت تطابقات حقيقية بسبب اختلاف طريقة كتابة نفس الشبكة من منصة
 * للتانية - مثلاً TRC20 ممكن تيجي "TRC20" أو "TRON" أو "TRX" أو "TRON(TRC20)" حسب المنصة،
 * وكلهم فعلياً نفس شبكة Tron التقنية بالظبط.
 *
 * **تنبيه صريح عن حدود الدقة**: القايمة هنا مبنية على التسميات الشائعة المعروفة وقت كتابة
 * الكود، ومش شاملة كل شبكة ممكنة على كل منصة من الثمانية - لو لقيت شبكتين نفس المعنى
 * التقني بس مش بيتطابقوا هنا (يعني التطبيق قالك "مفيش شبكة مشتركة" بس إنت شايف إن في
 * شبكة مشتركة فعلياً بأسماء مختلفة)، ضيف الاسمين لنفس المجموعة تحت. مفيش طريقة نتأكد
 * من كل التسميات من غير اختبار حي على حساب حقيقي لكل منصة.
 */
object NetworkNormalization {

    /** كل مجموعة هنا بتمثل نفس الشبكة التقنية فعلياً - أول عنصر في كل مجموعة هو الاسم
     * المعياري (Canonical) اللي هيتعرض للمستخدم */
    private val aliasGroups: List<List<String>> = listOf(
        listOf("TRC20", "TRON", "TRX", "TRC-20"),
        listOf("ERC20", "ETH", "ETHEREUM", "ERC-20"),
        listOf("BEP20", "BSC", "BNBSMARTCHAIN", "BEP-20", "BNBSMARTCHAINBEP20", "BSCBEP20"),
        listOf("BEP2", "BNBBEACONCHAIN", "BNB", "BEP-2", "BNBBEACONCHAINBEP2"),
        listOf("MATIC", "POLYGON", "POLYGONMATIC", "MATICPOLYGON"),
        listOf("ARBITRUM", "ARBITRUMONE", "ARBI", "ARB", "ARBITRUMONEARB"),
        listOf("OPTIMISM", "OP", "OPETH", "OPTIMISMOP"),
        listOf("SOL", "SOLANA"),
        listOf("AVAXC", "AVAXCCHAIN", "AVALANCHECCHAIN", "AVALANCHE", "AVAX"),
        listOf("TON", "THEOPENNETWORK", "TONCOIN"),
        listOf("BASE", "BASEERC20LAYER2"),
        listOf("ZKSYNC", "ZKSYNCERA"),
        listOf("XRP", "RIPPLE"),
        listOf("APTOS", "APT"),
        listOf("NEAR", "NEARPROTOCOL"),
        listOf("CELO"),
        listOf("KCC", "KUCOINCOMMUNITYCHAIN"),
        listOf("HECO", "HRC20", "HUOBIECOCHAIN"),
        listOf("FTM", "FANTOM", "FANTOMOPERA"),
        listOf("CRONOS", "CRO"),
        listOf("LTC", "LITECOIN"),
        listOf("DOGE", "DOGECOIN"),
        listOf("BTC", "BITCOIN"),
        listOf("LIGHTNING", "LN", "BTCLN", "LIGHTNINGNETWORK"),
        listOf("EOS"),
        listOf("ALGO", "ALGORAND"),
        listOf("XLM", "STELLAR"),
        listOf("ADA", "CARDANO"),
        listOf("DOT", "POLKADOT"),
        listOf("ATOM", "COSMOS"),
        listOf("XTZ", "TEZOS"),
        listOf("ICP", "INTERNETCOMPUTER", "DFINITY"),
        listOf("FIL", "FILECOIN"),
        listOf("VET", "VECHAIN"),
        listOf("THETA"),
        listOf("ZIL", "ZILLIQA"),
        listOf("WAVES"),
        listOf("QTUM"),
        listOf("NEO"),
        listOf("ONT", "ONTOLOGY"),
    )

    /** كل اسم خام (بعد التنظيف) بيشاور على الاسم المعياري لمجموعته */
    private val canonicalByVariant: Map<String, String> = aliasGroups
        .flatMap { group -> group.map { variant -> clean(variant) to group.first() } }
        .toMap()

    /** بيشيل كل حاجة غير حروف/أرقام ويحوّل لحروف كبيرة - عشان "BEP-20" و"bep 20" و"Bep20" كلهم يبقوا نفس الحاجة */
    private fun clean(s: String): String = s.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")

    /**
     * بيرجّع الاسم المعياري (Canonical) لاسم شبكة خام جاي من أي منصة من الثمانية.
     * بيجرّب بالترتيب: (1) الاسم كامل بعد التنظيف، (2) لو فيه شرطة وبعدها اسم معروف
     * (زي نمط OKX "USDT-TRC20")، بيجرّب الجزء اللي بعد آخر شرطة، (3) لو فيه قوس
     * (زي "BNB Smart Chain(BEP20)")، بيجرّب اللي جوه القوس. لو مفيش تطابق معروف خالص،
     * بيرجّع النص المنظّف زي ما هو - عشان لسه يقدر يتطابق مع نفس الاسم الخام من منصة
     * تانية حتى لو مش موجود في القايمة أصلاً.
     */
    fun canonicalize(rawNetworkName: String): String {
        val direct = clean(rawNetworkName)
        canonicalByVariant[direct]?.let { return it }

        if (rawNetworkName.contains('-')) {
            val afterHyphen = clean(rawNetworkName.substringAfterLast('-'))
            canonicalByVariant[afterHyphen]?.let { return it }
        }

        Regex("\\(([^)]*)\\)").find(rawNetworkName)?.groupValues?.get(1)?.let { inner ->
            val cleanedInner = clean(inner)
            canonicalByVariant[cleanedInner]?.let { return it }
        }

        return direct
    }

    /** بيقارن شبكتين من منصتين مختلفتين - true لو نفس الشبكة التقنية فعلياً حتى لو الاسم الخام مكتوب بشكل مختلف */
    fun isSameNetwork(a: String, b: String): Boolean = canonicalize(a) == canonicalize(b)
}
