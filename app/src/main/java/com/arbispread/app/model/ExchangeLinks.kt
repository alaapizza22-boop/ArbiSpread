package com.arbispread.app.model

/**
 * روابط صفحة التداول المباشرة لكل منصة، عشان لما المستخدم يدوس على زرار/سهم
 * المنصة جنب السعر، يتوجّه على طول لصفحة تداول العملة دي بالذات (مش الصفحة الرئيسية).
 *
 * الروابط دي روابط ويب عادية (https) بتتفتح بمتصفح الموبايل أو بتطبيق المنصة نفسه
 * تلقائياً لو متثبت وعامل Universal/App Links. التطبيق مش بيسجّل دخول ولا بيلمس
 * حساب المستخدم ولا بينفذ أي عملية تلقائية - مجرد رابط ملاحة (Navigation) بس
 * عشان يسهّل عليه يقارن أو ينفذ بنفسه وهو مرتاح.
 */
object ExchangeLinks {

    /** رابط صفحة تداول Spot لعملة معينة على منصة معينة - null لو المنصة مش مدعومة */
    fun spotTradeUrl(exchangeName: String, coinId: String): String? = when (exchangeName) {
        "Binance" -> "https://www.binance.com/en/trade/${coinId}_USDT"
        "Bybit" -> "https://www.bybit.com/en/trade/spot/$coinId/USDT"
        "OKX" -> "https://www.okx.com/trade-spot/${coinId.lowercase()}-usdt"
        "KuCoin" -> "https://www.kucoin.com/trade/${coinId}-USDT"
        "Gate.io" -> "https://www.gate.io/trade/${coinId}_USDT"
        "Bitget" -> "https://www.bitget.com/spot/${coinId}USDT"
        "MEXC" -> "https://www.mexc.com/exchange/${coinId}_USDT"
        else -> null
    }

    /** رابط صفحة تداول الفيوتشرز (Perpetual) لعملة معينة - مدعوم حالياً لـ Binance و Bybit بس */
    fun futuresTradeUrl(exchangeName: String, coinId: String): String? = when (exchangeName) {
        "Binance" -> "https://www.binance.com/en/futures/${coinId}USDT"
        "Bybit" -> "https://www.bybit.com/trade/usdt/${coinId}USDT"
        else -> null
    }
}
