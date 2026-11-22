package com.syedhunain.hisaabi.util

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.syedhunain.hisaabi.BuildConfig
import com.syedhunain.hisaabi.data.Prefs
import com.syedhunain.hisaabi.ui.LocalExt
import java.lang.ref.WeakReference

/**
 * AdMob in one place. Debug builds use Google's test ad units (see app/build.gradle.kts).
 * Rules we keep: no ads next to Diya/Liya buttons or inside forms; full-screen ads only at
 * natural breaks (app open/return, new khata, every 4th entry), never two within 20 seconds;
 * and the user can hide ads for 12h with a rewarded video.
 */
object Ads {
    var ready by mutableStateOf(false)
        private set

    private const val MIN_GAP_MS = 20 * 1000L
    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShownAt = 0L
    private val counters = mutableMapOf<String, Int>()

    val isRobolectric: Boolean get() = Build.FINGERPRINT == "robolectric"

    fun allowed(prefs: Prefs): Boolean = ready && System.currentTimeMillis() > prefs.adsOffUntil

    /** Ask for consent where the law needs it (Google UMP), then start the SDK. */
    private var prefsRef: Prefs? = null

    fun start(activity: Activity, prefs: Prefs) {
        prefsRef = prefs
        if (isRobolectric || ready) return
        runCatching {
            val consent = UserMessagingPlatform.getConsentInformation(activity)
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        if (consent.canRequestAds()) initSdk(activity)
                    }
                },
                { if (consent.canRequestAds()) initSdk(activity) },
            )
            if (consent.canRequestAds()) initSdk(activity)
        }
    }

    private var initStarted = false

    private fun initSdk(activity: Activity) {
        if (initStarted) return
        initStarted = true
        runCatching {
            MobileAds.initialize(activity.applicationContext) {
                ready = true
                loadInterstitial(activity.applicationContext)
                loadAppOpen(activity.applicationContext)
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (loadingInterstitial || interstitial != null) return
        loadingInterstitial = true
        InterstitialAd.load(
            context, BuildConfig.AD_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad; loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null; loadingInterstitial = false
                }
            },
        )
    }

    /**
     * Call at a natural break (e.g. after saving). Shows an interstitial on every [every]-th
     * event of this kind, if one is loaded and none was shown in the last 3 minutes.
     */
    fun onBreak(activity: Activity?, prefs: Prefs, event: String, every: Int) {
        if (activity == null || !allowed(prefs)) return
        val n = (counters[event] ?: 0) + 1
        counters[event] = n
        if (n % every != 0) return
        val now = System.currentTimeMillis()
        if (now - lastShownAt < MIN_GAP_MS) return
        val ad = interstitial
        if (ad == null) {
            loadInterstitial(activity.applicationContext); return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                interstitial = null
                loadInterstitial(activity.applicationContext)
            }
        }
        lastShownAt = now
        suppressNextReturn()
        ad.show(activity)
    }

    // ---------- App open ads (app launch and coming back to the app) ----------

    private const val APP_OPEN_MAX_AGE_MS = 4 * 60 * 60 * 1000L
    private const val LAUNCH_WAIT_MS = 8_000L
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingAppOpen = false
    private var launchActivity: WeakReference<Activity>? = null
    private var launchDeadline = 0L
    private var suppressReturn = false

    /** Call before opening another app/screen of ours (share, dial, pickers) so coming back shows no ad. */
    fun suppressNextReturn() {
        suppressReturn = true
    }

    private fun appOpenFresh(): Boolean =
        appOpen != null && System.currentTimeMillis() - appOpenLoadedAt < APP_OPEN_MAX_AGE_MS

    private fun loadAppOpen(context: Context) {
        if (BuildConfig.AD_APP_OPEN.isEmpty() || loadingAppOpen || appOpenFresh()) return
        loadingAppOpen = true
        AppOpenAd.load(
            context, BuildConfig.AD_APP_OPEN, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpen = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    loadingAppOpen = false
                    // Cold start: show it if it arrived while the app is still opening.
                    val act = launchActivity?.get()
                    if (act != null && System.currentTimeMillis() < launchDeadline && !act.isFinishing) {
                        launchDeadline = 0L
                        showAppOpen(act)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                }
            },
        )
    }

    /** Called from the activity's onStart: app launch (coldStart) or coming back from the background. */
    fun onAppStart(activity: Activity, coldStart: Boolean) {
        if (isRobolectric) return
        if (suppressReturn) {
            suppressReturn = false
            return
        }
        if (coldStart) {
            launchActivity = WeakReference(activity)
            launchDeadline = System.currentTimeMillis() + LAUNCH_WAIT_MS
            if (appOpenFresh()) {
                launchDeadline = 0L
                showAppOpen(activity)
            }
            return
        }
        showAppOpen(activity)
    }

    private fun showAppOpen(activity: Activity) {
        val prefs = prefsRef ?: return
        if (!allowed(prefs)) return
        val ad = appOpen?.takeIf { appOpenFresh() }
        if (ad == null) {
            loadAppOpen(activity.applicationContext); return
        }
        val now = System.currentTimeMillis()
        if (now - lastShownAt < MIN_GAP_MS) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpen = null
                loadAppOpen(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                appOpen = null
                loadAppOpen(activity.applicationContext)
            }
        }
        appOpen = null
        lastShownAt = now
        suppressNextReturn()
        ad.show(activity)
    }

    /** Rewarded video. [onReward] runs only if the user watched it. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onFailed: () -> Unit) {
        if (!ready) {
            onFailed(); return
        }
        RewardedAd.load(
            activity, BuildConfig.AD_REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    suppressNextReturn()
                    ad.show(activity) { onReward() }
                }

                override fun onAdFailedToLoad(error: LoadAdError) = onFailed()
            },
        )
    }
}

/** Adaptive banner that fits the screen width. Draws nothing when ads are off. */
@Composable
fun BannerAd(prefs: Prefs, modifier: Modifier = Modifier) {
    if (!Ads.allowed(prefs)) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    adUnitId = BuildConfig.AD_BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

/** Native ad styled like a Hisaabi list row, with a clear "Ad" label. */
@Composable
fun NativeAdRow(prefs: Prefs, modifier: Modifier = Modifier) {
    if (!Ads.allowed(prefs)) return
    val ctx = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var loaded: NativeAd? = null
        val loader = AdLoader.Builder(ctx, BuildConfig.AD_NATIVE)
            .forNativeAd { loaded = it; ad = it }
            .withAdListener(object : AdListener() {})
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { loaded?.destroy() }
    }
    val current = ad ?: return
    val colors = NativeColors(
        card = MaterialTheme.colorScheme.surface.toArgb(),
        text = MaterialTheme.colorScheme.onSurface.toArgb(),
        muted = LocalExt.current.muted.toArgb(),
        button = MaterialTheme.colorScheme.primary.toArgb(),
        onButton = MaterialTheme.colorScheme.onPrimary.toArgb(),
        badge = 0xFFE8C977.toInt(),
        onBadge = 0xFF0B3F35.toInt(),
    )
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { c -> buildNativeView(c, colors) },
        update = { v -> bindNative(v, current) },
    )
}

private data class NativeColors(
    val card: Int, val text: Int, val muted: Int, val button: Int, val onButton: Int, val badge: Int, val onBadge: Int,
)

private fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

private fun buildNativeView(c: Context, colors: NativeColors): NativeAdView {
    val root = NativeAdView(c)
    val row = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(c.dp(16), c.dp(14), c.dp(12), c.dp(14))
        background = GradientDrawable().apply {
            setColor(colors.card); cornerRadius = c.dp(16).toFloat()
        }
    }
    val icon = ImageView(c).apply {
        layoutParams = LinearLayout.LayoutParams(c.dp(44), c.dp(44)).apply { marginEnd = c.dp(12) }
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        background = GradientDrawable().apply { cornerRadius = c.dp(10).toFloat(); setColor(0x11000000) }
    }
    val texts = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    val titleRow = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = TextView(c).apply {
        text = "Ad"
        setTextColor(colors.onBadge)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(c.dp(6), c.dp(1), c.dp(6), c.dp(1))
        background = GradientDrawable().apply { setColor(colors.badge); cornerRadius = c.dp(6).toFloat() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = c.dp(6) }
    }
    val headline = TextView(c).apply {
        setTextColor(colors.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val body = TextView(c).apply {
        setTextColor(colors.muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val cta = TextView(c).apply {
        setTextColor(colors.onButton)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        setPadding(c.dp(12), c.dp(8), c.dp(12), c.dp(8))
        background = GradientDrawable().apply { setColor(colors.button); cornerRadius = c.dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = c.dp(8) }
    }
    titleRow.addView(badge)
    titleRow.addView(headline)
    texts.addView(titleRow)
    texts.addView(body)
    row.addView(icon)
    row.addView(texts)
    row.addView(cta)
    root.addView(row)
    root.iconView = icon
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = cta
    return root
}

private fun bindNative(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).apply {
        text = ad.body
        visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.callToActionView as TextView).apply {
        text = ad.callToAction
        visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    (view.iconView as ImageView).apply {
        val d = ad.icon?.drawable
        setImageDrawable(d)
        visibility = if (d == null) View.GONE else View.VISIBLE
    }
    view.setNativeAd(ad)
}
