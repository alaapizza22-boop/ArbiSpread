package com.arbispread.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * تخزين محلي بسيط (SharedPreferences عادي، مش مشفّر - البيانات هنا مجرد أرقام نسبة%
 * وتوقيتات، مش حساسة زي مفاتيح الـ API) لسجل استمرارية فرص الاسبريد، عشان يفضل
 * موجود بين فتحات التطبيق بدل ما يتصفّر كل مرة التطبيق يتقفل.
 *
 * **ليه ده مهم؟** المستخدم غالباً بيفتح التطبيق، يتأكد من فرصة، يقفله، ويرجع يفتحه
 * تاني بعد شوية - لو السجل اتصفّر كل مرة، ميزة "من قد ايه الفرصة دي مستمرة" مفيدتش
 * فعلياً إلا لو سايب التطبيق فاتح طول الوقت، وده مش واقعي لمعظم المستخدمين.
 */
object PersistenceHistoryStore {
    private const val PREFS_NAME = "arbi_spread_persistence_history"
    private const val KEY_HISTORY_JSON = "history_json"

    /** بتحمّل السجل المحفوظ - خريطة فاضية لو أول مرة، أو لو الملف اتلف لأي سبب (بدل ما نكراش) */
    fun load(context: Context): HashMap<String, MutableList<Pair<Long, Boolean>>> {
        val result = HashMap<String, MutableList<Pair<Long, Boolean>>>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_HISTORY_JSON, null) ?: return result
            val root = JSONObject(json)
            val coinIds = root.keys()
            while (coinIds.hasNext()) {
                val coinId = coinIds.next()
                val arr = root.getJSONArray(coinId)
                val list = mutableListOf<Pair<Long, Boolean>>()
                for (i in 0 until arr.length()) {
                    val entry = arr.getJSONArray(i)
                    list.add(entry.getLong(0) to (entry.getInt(1) == 1))
                }
                result[coinId] = list
            }
        } catch (_: Exception) {
            // ملف تالف أو تنسيق قديم - نرجع خريطة فاضية بدل ما نكراش، التتبع هيبدأ من جديد بس كده
        }
        return result
    }

    /** بتحفظ السجل الحالي كامل - بيتنادى بعد كل تحديث أسعار (البيانات صغيرة جداً، مفيش تأثير أداء محسوس) */
    fun save(context: Context, history: Map<String, List<Pair<Long, Boolean>>>) {
        try {
            val root = JSONObject()
            history.forEach { (coinId, list) ->
                val arr = JSONArray()
                list.forEach { (timestampMillis, isProfitable) ->
                    val entry = JSONArray()
                    entry.put(timestampMillis)
                    entry.put(if (isProfitable) 1 else 0)
                    arr.put(entry)
                }
                root.put(coinId, arr)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY_JSON, root.toString())
                .apply()
        } catch (_: Exception) {
            // فشل الحفظ (مساحة ممتلئة مثلاً) - التطبيق يكمل عادي، بس الاستمرارية مش هتتحفظ المرة دي
        }
    }
}
