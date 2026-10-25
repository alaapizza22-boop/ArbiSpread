package com.arbispread.app.data

import android.content.Context

/**
 * تخزين محلي بسيط (SharedPreferences عادي، مش مشفّر - مجرد قايمة رموز عملات، مش
 * بيانات حساسة) لقايمة العملات المفضلة اللي المستخدم اختار يتابعها، عشان تفضل
 * محفوظة بين فتحات التطبيق المختلفة.
 */
object FavoritesStore {
    private const val PREFS_NAME = "arbi_spread_favorites"
    private const val KEY_FAVORITE_IDS = "favorite_coin_ids"

    /** بيحمّل قايمة رموز العملات المفضلة - مجموعة فاضية لو أول مرة */
    fun load(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
    }

    fun save(context: Context, favoriteIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, favoriteIds).apply()
    }

    /** بيقلب حالة عملة واحدة (يضيفها لو مش موجودة، يشيلها لو موجودة) وبيحفظ على طول، وبيرجّع القايمة الجديدة */
    fun toggle(context: Context, coinId: String): Set<String> {
        val current = load(context).toMutableSet()
        if (!current.add(coinId)) current.remove(coinId)
        save(context, current)
        return current
    }
}
