package com.arbispread.app.model

/**
 * تقدير تقريبي (مش حي ولا مضمون) لمدة تأكيد التحويل على أشهر شبكات البلوكتشين - الفترة
 * من لحظة إرسال السحب لحد ما العملة توصل فعلياً وتبقى متاحة للبيع على منصة الوصول.
 *
 * **ليه الوقت ده مهم في الآربيتراج تحديداً؟** لأن الفرق في السعر بين المنصتين بيتحسب
 * لحظياً وقت الفحص، لكن التنفيذ الفعلي (سحب → تأكيد الشبكة → إيداع → بيع) بياخد وقت
 * حقيقي - وأثناء الوقت ده السعر في أي من المنصتين (أو الاتنين) ممكن يتحرك ويقفل الفرصة
 * أو يحولها لخسارة، حتى لو كل الأرقام وقت الفحص كانت صح 100%.
 *
 * **تنبيه صريح عن حدود الدقة:** الأرقام هنا تقديرات عامة مبنية على معدل تأكيد الشبكة
 * المعتاد (عدد الـ Confirmations المطلوب × متوسط وقت البلوك)، **مش** وقت حي من المنصة
 * نفسها (مفيش منصة بترجع "وقت التأكيد المتوقع دلوقتي" في Endpoint عام). الوقت الفعلي
 * بيزيد كتير وقت ازدحام الشبكة (غاز الإيثريوم مثلاً وقت الزحمة) أو حسب عدد التأكيدات
 * الإضافية اللي منصة الوصول بالذات بتطلبها قبل ما تفرج عن الإيداع.
 */
object NetworkTransferTime {

    /** نطاق تقريبي بالدقايق (أقل تقدير، أعلى تقدير) */
    data class MinuteRange(val minMinutes: Int, val maxMinutes: Int)

    /**
     * كل زوج هنا عبارة عن (كلمة مفتاحية للبحث داخل اسم الشبكة بغض النظر عن حالة الأحرف،
     * نطاق الوقت التقريبي) - بنبحث بالترتيب ده عشان الشبكات الأسرع (TRC20, BEP20...)
     * تتفحص قبل "Ethereum" العامة اللي ممكن تتداخل مع اسمها
     */
    private val keywedRanges: List<Pair<String, MinuteRange>> = listOf(
        "trc20" to MinuteRange(2, 5),
        "tron" to MinuteRange(2, 5),
        "bep20" to MinuteRange(1, 3),
        "bnb smart chain" to MinuteRange(1, 3),
        "bsc" to MinuteRange(1, 3),
        "solana" to MinuteRange(1, 2),
        "polygon" to MinuteRange(2, 5),
        "matic" to MinuteRange(2, 5),
        "arbitrum" to MinuteRange(3, 8),
        "avalanche" to MinuteRange(1, 3),
        "ton" to MinuteRange(1, 3),
        "near" to MinuteRange(1, 3),
        "xrp" to MinuteRange(1, 3),
        "ripple" to MinuteRange(1, 3),
        "stellar" to MinuteRange(1, 2),
        "cosmos" to MinuteRange(1, 3),
        "cardano" to MinuteRange(5, 15),
        "erc20" to MinuteRange(5, 15),
        "ethereum" to MinuteRange(5, 15),
        "litecoin" to MinuteRange(15, 30),
        "dogecoin" to MinuteRange(20, 45),
        "bitcoin cash" to MinuteRange(20, 45),
        "bitcoin" to MinuteRange(30, 60),
    )

    /**
     * بتدور على أقرب تطابق كلمة مفتاحية في اسم الشبكة وترجع نطاق الوقت التقريبي -
     * null لو الاسم مش معروف عندنا، بدل ما نخترع رقم لشبكة مجهولة.
     */
    fun estimateFor(networkName: String?): MinuteRange? {
        if (networkName.isNullOrBlank()) return null
        val lower = networkName.lowercase()
        return keywedRanges.firstOrNull { (keyword, _) -> lower.contains(keyword) }?.second
    }

    /** بيصيغ النطاق كنص جاهز للعرض، مثلاً "2–5 دقيقة" أو "30–60 دقيقة (~1 ساعة)" */
    fun formatRange(range: MinuteRange): String = when {
        range.maxMinutes < 60 -> "${range.minMinutes}–${range.maxMinutes} دقيقة"
        else -> "${range.minMinutes}–${range.maxMinutes} دقيقة (~${range.maxMinutes / 60} ساعة)"
    }

    fun formatRangeEn(range: MinuteRange): String = when {
        range.maxMinutes < 60 -> "${range.minMinutes}–${range.maxMinutes} min"
        else -> "${range.minMinutes}–${range.maxMinutes} min (~${range.maxMinutes / 60}h)"
    }
}
