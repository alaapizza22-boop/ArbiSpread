package com.arbispread.app.data

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * أدوات توقيع HMAC-SHA256 للنداءات الموقّعة لحسابات المستخدمين (Binance/Bybit/OKX) -
 * بنستخدم مكتبة javax.crypto القياسية المدمجة في جافا/أندرويد، من غير أي مكتبة خارجية
 * جديدة. المفتاح السري (Secret Key) بييجي من [ApiKeyStore] (مخزّن مشفّر على جهاز
 * المستخدم) ومبيتسربش لأي مكان تاني - بيتستخدم هنا بس لحظة بناء التوقيع.
 */
object Signing {
    private const val HMAC_SHA256 = "HmacSHA256"

    /** توقيع Hex لوضع Binance وBybit */
    fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256))
        val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    /** توقيع Base64 لوضع OKX */
    fun hmacSha256Base64(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256))
        val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }
}
