package com.arbispread.app.model

/**
 * تعريف كل عملة وأسماء الرموز (Symbols) الخاصة بها في كل منصة،
 * لأن كل منصة بتسمي نفس العملة بشكل مختلف شوية.
 */
data class CoinDef(
    val id: String,                 // رمز مختصر للعرض، مثل BTC
    val displayName: String,        // اسم عرض
    val binanceSymbol: String,      // مثل BTCUSDT
    val bybitSymbol: String,        // مثل BTCUSDT
    val okxInstId: String,          // مثل BTC-USDT
    val kucoinSymbol: String,       // مثل BTC-USDT
    val gateioSymbol: String,       // مثل BTC_USDT
    val coinbaseProductId: String?, // مثل BTC-USD، أو null لو العملة مش مدرجة على Coinbase
)

/**
 * كل المنصات (ماعدا Coinbase) بتستخدم نفس نمط التسمية تقريباً لمعظم العملات،
 * فبنولّد الرموز تلقائياً من الرمز الأساسي بدل كتابة كل منصة يدوياً لكل عملة.
 * لو عملة معينة ليها اسم مختلف فعلاً في منصة ما، هي ببساطة مش هتظهر من المنصة دي
 * (الكود بيتعامل مع الغياب برمجياً من غير ما يكسر التطبيق).
 */
private fun coin(base: String, name: String, hasCoinbase: Boolean = true): CoinDef = CoinDef(
    id = base,
    displayName = name,
    binanceSymbol = "${base}USDT",
    bybitSymbol = "${base}USDT",
    okxInstId = "$base-USDT",
    kucoinSymbol = "$base-USDT",
    gateioSymbol = "${base}_USDT",
    coinbaseProductId = if (hasCoinbase) "$base-USD" else null,
)

/**
 * قائمة أكبر بكتير (100 عملة) بدل الـ 10 الأصليين، تغطي أهم العملات اللي
 * بتتداول على معظم المنصات الست. العملات الكبرى (أول 30 تقريباً) متاحة على Coinbase،
 * والباقي غالباً لأ، فبنوفّر طلبات شبكة زيادة من غير داعي.
 */
val DEFAULT_COINS: List<CoinDef> = listOf(
    coin("BTC", "بيتكوين"),
    coin("ETH", "إيثريوم"),
    coin("BNB", "بينانس كوين"),
    coin("SOL", "سولانا"),
    coin("XRP", "ريبل"),
    coin("ADA", "كاردانو"),
    coin("DOGE", "دوجكوين"),
    coin("TON", "تون"),
    coin("DOT", "بولكادوت"),
    coin("LTC", "لايتكوين"),
    coin("TRX", "ترون"),
    coin("AVAX", "أفالانش"),
    coin("SHIB", "شيبا إينو"),
    coin("LINK", "تشين لينك"),
    coin("BCH", "بيتكوين كاش"),
    coin("NEAR", "نير بروتوكول"),
    coin("MATIC", "بوليجون"),
    coin("UNI", "يونيسواب"),
    coin("ICP", "إنترنت كمبيوتر"),
    coin("ETC", "إيثريوم كلاسيك"),
    coin("XLM", "ستيلر"),
    coin("ATOM", "كوزموس"),
    coin("FIL", "فايلكوين"),
    coin("APT", "أبتوس"),
    coin("HBAR", "هيدرا هاشجراف"),
    coin("ARB", "أربيتروم"),
    coin("VET", "فيتشين"),
    coin("OP", "أوبتيميزم"),
    coin("MKR", "ميكر"),
    coin("GRT", "ذا جراف"),
    coin("AAVE", "آيف", hasCoinbase = true),
    coin("ALGO", "ألجوراند", hasCoinbase = false),
    coin("QNT", "كوانت", hasCoinbase = false),
    coin("STX", "ستاكس", hasCoinbase = false),
    coin("EGLD", "إلروند", hasCoinbase = false),
    coin("SAND", "ذا ساندبوكس", hasCoinbase = false),
    coin("THETA", "ثيتا", hasCoinbase = false),
    coin("FLOW", "فلو", hasCoinbase = false),
    coin("AXS", "أكسي إنفينيتي", hasCoinbase = false),
    coin("XTZ", "تيزوس", hasCoinbase = false),
    coin("EOS", "إيوس", hasCoinbase = false),
    coin("IMX", "إيميوتابل إكس", hasCoinbase = false),
    coin("RUNE", "ثور تشين", hasCoinbase = false),
    coin("MANA", "ديسنترالاند", hasCoinbase = false),
    coin("KAVA", "كافا", hasCoinbase = false),
    coin("NEO", "نيو", hasCoinbase = false),
    coin("CHZ", "تشيليز", hasCoinbase = false),
    coin("XMR", "مونيرو", hasCoinbase = false),
    coin("CAKE", "بان كيك سواب", hasCoinbase = false),
    coin("ZEC", "زي كاش", hasCoinbase = false),
    coin("DASH", "داش", hasCoinbase = false),
    coin("COMP", "كومباوند", hasCoinbase = false),
    coin("CRV", "كيرف", hasCoinbase = false),
    coin("ENJ", "إنجين كوين", hasCoinbase = false),
    coin("SNX", "سينثتكس", hasCoinbase = false),
    coin("GALA", "جالا", hasCoinbase = false),
    coin("LRC", "لووبرينج", hasCoinbase = false),
    coin("BAT", "بيسيك أتنشن توكن", hasCoinbase = false),
    coin("ZIL", "زيليقا", hasCoinbase = false),
    coin("WAVES", "ويفز", hasCoinbase = false),
    coin("KSM", "كوساما", hasCoinbase = false),
    coin("QTUM", "كوانتم", hasCoinbase = false),
    coin("OMG", "أو إم جي نتوورك", hasCoinbase = false),
    coin("ICX", "إيكون", hasCoinbase = false),
    coin("ONT", "أونتولوجي", hasCoinbase = false),
    coin("IOST", "آي أو إس تي", hasCoinbase = false),
    coin("ANKR", "أنكر", hasCoinbase = false),
    coin("SKL", "سكيل", hasCoinbase = false),
    coin("STORJ", "ستورج", hasCoinbase = false),
    coin("CELO", "سيلو", hasCoinbase = false),
    coin("BAND", "باند بروتوكول", hasCoinbase = false),
    coin("RSR", "ريزيرف رايتس", hasCoinbase = false),
    coin("CTSI", "كارتيسي", hasCoinbase = false),
    coin("OCEAN", "أوشن بروتوكول", hasCoinbase = false),
    coin("RVN", "رافنكوين", hasCoinbase = false),
    coin("SC", "سياكوين", hasCoinbase = false),
    coin("ZEN", "هورايزن", hasCoinbase = false),
    coin("LSK", "ليسك", hasCoinbase = false),
    coin("NANO", "نانو", hasCoinbase = false),
    coin("HOT", "هولوتشين", hasCoinbase = false),
    coin("WAXP", "واكس", hasCoinbase = false),
    coin("DGB", "ديجي بايت", hasCoinbase = false),
    coin("ARDR", "أردور", hasCoinbase = false),
    coin("STEEM", "ستيم", hasCoinbase = false),
    coin("SYS", "سيسكوين", hasCoinbase = false),
    coin("KMD", "كومودو", hasCoinbase = false),
    coin("ARK", "آرك", hasCoinbase = false),
    coin("BTS", "بت شيرز", hasCoinbase = false),
    coin("PIVX", "بيفكس", hasCoinbase = false),
    coin("NMR", "نيومرير", hasCoinbase = false),
    coin("REN", "رين", hasCoinbase = false),
    coin("BNT", "بانكور", hasCoinbase = false),
    coin("KNC", "كايبر نتوورك", hasCoinbase = false),
    coin("GNO", "جنوسيس", hasCoinbase = false),
    coin("UMA", "يو إم إيه", hasCoinbase = false),
    coin("MLN", "ميلون", hasCoinbase = false),
    coin("ANT", "أراجون", hasCoinbase = false),
    coin("BAL", "بالانسر", hasCoinbase = false),
    coin("YFI", "يرن فاينانس", hasCoinbase = false),
    coin("SUSHI", "سوشي سواب", hasCoinbase = false),
)

/** عدد العملات المتاحة مجاناً بدون أي إعلان */
const val FREE_COIN_LIMIT = 5
