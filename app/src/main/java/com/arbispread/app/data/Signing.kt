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
    private const val HMAC_SHA512 = "HmacSHA512"

    /** توقيع Hex لوضع Binance وBybit وMEXC */
    fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256))
        val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    /** توقيع Base64 لوضع OKX وBitget وKuCoin */
    fun hmacSha256Base64(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256))
        val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    /** توقيع Hex بـ SHA512 لوضع Gate.io بس (التوقيع الوحيد من السبعة اللي بيستخدم SHA512 مش SHA256) */
    fun hmacSha512Hex(secret: String, message: String): String {
        val mac = Mac.getInstance(HMAC_SHA512)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA512))
        val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    /** هاش SHA512 Hex عادي (من غير مفتاح) - Gate.io محتاجه كجزء من الرسالة الموقّعة (هاش الـ body، فاضي للطلبات GET) */
    fun sha512Hex(message: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-512")
        val raw = digest.digest(message.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }
}
