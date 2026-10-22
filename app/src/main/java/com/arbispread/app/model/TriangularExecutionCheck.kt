package com.arbispread.app.model

/**
 * نتيجة فحص "التحقق قبل التنفيذ" الحقيقي لفرصة تداول ثلاثي - بيمشي على عمق دفتر
 * الطلبات الكامل (مش أفضل سعر بس) لكل الثلاث خطوات، ويحسب متوسط سعر التنفيذ
 * الترجيحي (VWAP) الفعلي لو رأس مالك أكبر من اللي متاح عند أفضل سعر.
 */
data class TriangularExecutionCheck(
    val capitalUsd: Double,
    /** الدولار الفعلي بعد الثلاث خطوات والعمولات - null لو تعذر الفحص (مثلاً فشل سحب العمق) */
    val finalUsd: Double?,
    /** true لو عمق الكتاب المتاح (أول N مستوى) غطى الصفقة بالكامل في الثلاث خطوات - لو false يبقى الرقم "أسوأ تقدير مؤكد" مش دقيق 100% */
    val depthSufficientForAllLegs: Boolean,
    val errorMessage: String?,
) {
    val netProfitUsd: Double? get() = finalUsd?.let { it - capitalUsd }
    val netProfitPercent: Double? get() = netProfitUsd?.let { if (capitalUsd > 0.0) it / capitalUsd * 100.0 else 0.0 }
    val isProfitable: Boolean get() = (netProfitUsd ?: -1.0) > 0.0
}
