package com.arbispread.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** مفاتيح منصة واحدة - Secret وPassphrase ممكن يكونوا فاضيين لو المستخدم مدخلش قيمة */
data class ExchangeApiKey(
    val apiKey: String = "",
    val secretKey: String = "",
    /** Passphrase مطلوب لـ OKX وKuCoin بس - باقي المنصات بتتجاهله */
    val passphrase: String = "",
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && secretKey.isNotBlank()
}

/**
 * تخزين محلي مشفّر لمفاتيح API الشخصية - اختياري بالكامل، المستخدم مش مجبر يدخله.
 *
 * **ليه ده مختلف عن "مفتاح مدمج في التطبيق" اللي كان مرفوض قبل كده؟**
 * المفتاح هنا بيتشفّر بمفتاح تشفير (Master Key) مربوط بـ Android Keystore الخاص
 * بجهاز المستخدم نفسه (مش رقم ثابت مكتوب في الكود)، فكل مستخدم بيحفظ مفتاحه هو بس
 * على جهازه هو، مشفّر بمفتاح جهازه هو - مفيش مشاركة أو تسريب لمستخدمين تانيين حتى
 * لو حد فكّك ملف الـ APK نفسه.
 *
 * **تنبيه مهم:** التخزين هنا بس - التطبيق **لسه مش بيستخدم** المفاتيح دي في أي نداء
 * API فعلي (لا للقراءة ولا غيرها). ده هيتفعّل في خطوة قادمة منفصلة. حفظ المفتاح هنا
 * دلوقتي مش هيغيّر أي رقم ظاهر في التطبيق لحد ما يتفعّل الاستخدام الفعلي.
 */
object ApiKeyStore {
    private const val PREFS_NAME = "arbi_spread_secure_keys"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** أسماء المنصات المدعومة بالترتيب اللي بتتعرض بيه في الواجهة */
    val SUPPORTED_EXCHANGES = listOf("Binance", "Bybit", "OKX", "KuCoin", "Gate.io")

    /** المنصات اللي محتاجة Passphrase إضافي غير API Key وSecret */
    fun requiresPassphrase(exchangeName: String): Boolean = exchangeName == "OKX" || exchangeName == "KuCoin"

    fun load(context: Context, exchangeName: String): ExchangeApiKey {
        val p = prefs(context)
        val slug = exchangeName.lowercase().replace(".", "")
        return ExchangeApiKey(
            apiKey = p.getString("${slug}_api_key", "") ?: "",
            secretKey = p.getString("${slug}_secret_key", "") ?: "",
            passphrase = p.getString("${slug}_passphrase", "") ?: "",
        )
    }

    fun save(context: Context, exchangeName: String, key: ExchangeApiKey) {
        val p = prefs(context)
        val slug = exchangeName.lowercase().replace(".", "")
        p.edit()
            .putString("${slug}_api_key", key.apiKey)
            .putString("${slug}_secret_key", key.secretKey)
            .putString("${slug}_passphrase", key.passphrase)
            .apply()
    }

    /** مسح مفتاح منصة واحدة بالكامل */
    fun clear(context: Context, exchangeName: String) {
        save(context, exchangeName, ExchangeApiKey())
    }
}
