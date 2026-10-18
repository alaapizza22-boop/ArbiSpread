package com.arbispread.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.arbispread.app.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * بانر AdMob - Jetpack Compose مفيهوش Composable جاهز لـ AdView،
 * فبنستخدم AndroidView عشان نستضيف الـ View التقليدي بتاع جوجل جواه.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.admob_banner_id)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
