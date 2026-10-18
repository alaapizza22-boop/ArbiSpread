package com.arbispread.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * إدارة إعلان المكافأة (Rewarded) اللي بيفتح باقي العملات.
 * معرّف الإعلان هنا تجريبي (Test ID) من جوجل - استبدله بمعرّفك من AdMob قبل النشر
 * (القيمة موجودة في res/values/strings.xml -> admob_rewarded_id).
 */
class AdsManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun initialize() {
        MobileAds.initialize(context) {}
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        val adUnitId = context.getString(com.arbispread.app.R.string.admob_rewarded_id)
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /** بيعرض إعلان المكافأة، وبينادي onRewardEarned لو المستخدم خلّص المشاهدة فعلاً */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            // لسه مفيش إعلان جاهز، نحاول نحمّل واحد تاني ونسيب المستخدم يحاول تاني بعد شوية
            loadRewardedAd()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
            }
        }
        ad.show(activity) { onRewardEarned() }
    }
}
