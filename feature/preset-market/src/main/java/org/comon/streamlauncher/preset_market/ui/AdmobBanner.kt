package org.comon.streamlauncher.preset_market.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.comon.streamlauncher.preset_market.BuildConfig

// TODO: 실제 배포 시 아래 TEST_BANNER_AD_UNIT_ID를 실제 AdUnit ID로 교체
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID,
    enabled: Boolean = BuildConfig.ENABLE_BANNER_ADS,
) {
    if (!enabled) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var adView by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(lifecycleOwner, adView) {
        val currentAdView = adView ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentAdView.resume()
                Lifecycle.Event.ON_PAUSE -> currentAdView.pause()
                Lifecycle.Event.ON_DESTROY -> releaseAdView(currentAdView)
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { createdAdView ->
            adView = createdAdView
        },
        onRelease = { releasedAdView ->
            if (adView === releasedAdView) {
                adView = null
            }
            releaseAdView(releasedAdView)
        },
    )
}

private fun releaseAdView(adView: AdView) {
    (adView.parent as? ViewGroup)?.removeView(adView)
    adView.pause()
    adView.destroy()
}
