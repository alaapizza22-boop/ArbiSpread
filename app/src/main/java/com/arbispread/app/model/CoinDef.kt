package com.arbispread.app.model

/**
 * تعريف كل عملة وأسماء الرموز (Symbols) الخاصة بها في كل منصة،
 * لأن كل منصة بتسمي نفس العملة بشكل مختلف شوية.
 *
 * ملاحظة: Coinbase اتشال من المقارنة لأنه بيسعّر بالـ USD مش USDT، والفرق
 * بين الاتنين مش ثابت (USDT ممكن يتداول عند premium أو discount بسيط عن الدولار
 * الحقيقي حسب العرض والطلب)، وده كان بيطلّع فروق سعر وهمية (زي 18%) مش حقيقية.
 * المنصات السبعة هنا (Binance, Bybit, OKX, KuCoin, Gate.io, Bitget, MEXC) بتسعّر
 * بالـ USDT فالمقارنة بينهم دلوقتي "تفاحة بتفاحة" فعلاً. Bitget وMEXC انضافوا بعدين
 * كمرشحين لسيولة/فرص إضافية مقارنة بالخمسة الأساسيين - اتأكد إن عمولاتهم في شاشة
 * الإعدادات مظبوطة على حسابك قبل أي قرار فعلي.
 */
data class CoinDef(
    val id: String,              // رمز مختصر للعرض، مثل BTC
    val displayName: String,     // اسم عرض
    val binanceSymbol: String,   // مثل BTCUSDT
    val bybitSymbol: String,     // مثل BTCUSDT
    val okxInstId: String,       // مثل BTC-USDT
    val kucoinSymbol: String,    // مثل BTC-USDT
    val gateioSymbol: String,    // مثل BTC_USDT
    val bitgetSymbol: String,    // مثل BTCUSDT
    val mexcSymbol: String,      // مثل BTCUSDT
)

/**
 * كل المنصات بتستخدم نفس نمط التسمية تقريباً لمعظم العملات (الرمز + USDT)،
 * فبنولّد الرموز تلقائياً من الرمز الأساسي بدل كتابة كل منصة يدوياً لكل عملة.
 * لو عملة معينة ليها اسم مختلف فعلاً في منصة ما، هي ببساطة مش هتظهر من المنصة دي
 * (الكود بيتعامل مع الغياب برمجياً من غير ما يكسر التطبيق).
 */
private fun coin(base: String, name: String): CoinDef = CoinDef(
    id = base,
    displayName = name,
    binanceSymbol = "${base}USDT",
    bybitSymbol = "${base}USDT",
    okxInstId = "$base-USDT",
    kucoinSymbol = "$base-USDT",
    gateioSymbol = "${base}_USDT",
    bitgetSymbol = "${base}USDT",
    mexcSymbol = "${base}USDT",
)

/** رموز العملات المستقرة (Stablecoins) المعروضة في قسمها الخاص بالواجهة - مش شاملة USDT نفسها لأنها عملة الأساس (Quote) اللي بيتقاس بيها كل حاجة تانية، فمفيش معنى لمقارنتها بنفسها */
val STABLECOIN_IDS: Set<String> = setOf("USDC", "DAI", "FDUSD", "TUSD")

/** قائمة 100 عملة تغطي أهم العملات اللي بتتداول على معظم المنصات الخمس (كلها أزواج USDT). */
val DEFAULT_COINS: List<CoinDef> = listOf(
    // عملات مستقرة (Stablecoins) - المفروض سعرها ~1$ على كل المنصات، فأي فرق سعر هنا
    // مصدره احتكاك سحب/إيداع حقيقي مش منافسة بوتات لحظية، فبيستمر لفترة أطول من عملات
    // التقلب العادية. الهامش عادةً صغير جداً، فلازم تتأكد المنصة عندها عمولة قليلة/صفر
    // على زوج الستيبل كوين بالذات وإلا العمولة هتاكل الربح الصغير ده بالكامل.
    coin("USDC", "يو إس دي كوين"),
    coin("DAI", "داي"),
    coin("FDUSD", "إف دي يو إس دي"),
    coin("TUSD", "ترو يو إس دي"),
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
    coin("AAVE", "آيف"),
    coin("ALGO", "ألجوراند"),
    coin("QNT", "كوانت"),
    coin("STX", "ستاكس"),
    coin("EGLD", "إلروند"),
    coin("SAND", "ذا ساندبوكس"),
    coin("THETA", "ثيتا"),
    coin("FLOW", "فلو"),
    coin("AXS", "أكسي إنفينيتي"),
    coin("XTZ", "تيزوس"),
    coin("EOS", "إيوس"),
    coin("IMX", "إيميوتابل إكس"),
    coin("RUNE", "ثور تشين"),
    coin("MANA", "ديسنترالاند"),
    coin("KAVA", "كافا"),
    coin("NEO", "نيو"),
    coin("CHZ", "تشيليز"),
    coin("XMR", "مونيرو"),
    coin("CAKE", "بان كيك سواب"),
    coin("ZEC", "زي كاش"),
    coin("DASH", "داش"),
    coin("COMP", "كومباوند"),
    coin("CRV", "كيرف"),
    coin("ENJ", "إنجين كوين"),
    coin("SNX", "سينثتكس"),
    coin("GALA", "جالا"),
    coin("LRC", "لووبرينج"),
    coin("BAT", "بيسيك أتنشن توكن"),
    coin("ZIL", "زيليقا"),
    coin("WAVES", "ويفز"),
    coin("KSM", "كوساما"),
    coin("QTUM", "كوانتم"),
    coin("OMG", "أو إم جي نتوورك"),
    coin("ICX", "إيكون"),
    coin("ONT", "أونتولوجي"),
    coin("IOST", "آي أو إس تي"),
    coin("ANKR", "أنكر"),
    coin("SKL", "سكيل"),
    coin("STORJ", "ستورج"),
    coin("CELO", "سيلو"),
    coin("BAND", "باند بروتوكول"),
    coin("RSR", "ريزيرف رايتس"),
    coin("CTSI", "كارتيسي"),
    coin("OCEAN", "أوشن بروتوكول"),
    coin("RVN", "رافنكوين"),
    coin("SC", "سياكوين"),
    coin("ZEN", "هورايزن"),
    coin("LSK", "ليسك"),
    coin("NANO", "نانو"),
    coin("HOT", "هولوتشين"),
    coin("WAXP", "واكس"),
    coin("DGB", "ديجي بايت"),
    coin("ARDR", "أردور"),
    coin("STEEM", "ستيم"),
    coin("SYS", "سيسكوين"),
    coin("KMD", "كومودو"),
    coin("ARK", "آرك"),
    coin("BTS", "بت شيرز"),
    coin("PIVX", "بيفكس"),
    coin("NMR", "نيومرير"),
    coin("REN", "رين"),
    coin("BNT", "بانكور"),
    coin("KNC", "كايبر نتوورك"),
    coin("GNO", "جنوسيس"),
    coin("UMA", "يو إم إيه"),
    coin("MLN", "ميلون"),
    coin("ANT", "أراجون"),
    coin("BAL", "بالانسر"),
    coin("YFI", "يرن فاينانس"),
    coin("SUSHI", "سوشي سواب"),
)

/** عدد العملات المتاحة مجاناً بدون أي إعلان */
const val FREE_COIN_LIMIT = 5
