# ===================================================================
# ProGuard / R8 rules - ArbiSpread (release)
# ===================================================================

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Our models (kept for reflection-free JSON parsing safety)
-keep class com.arbispread.app.model.** { *; }
