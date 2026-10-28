package com.example.ads

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

enum class NativeAdStyle {
    FEED,
    COMPACT
}

@Composable
fun NativeAdCard(
    screen: AdScreen,
    style: NativeAdStyle = NativeAdStyle.FEED,
    preferVideo: Boolean = (style == NativeAdStyle.FEED && (screen == AdScreen.COVERS || screen == AdScreen.RECORDINGS)) || screen == AdScreen.PLAYBACK,
    modifier: Modifier = Modifier
) {
    if (!AdsConfig.isAdsEnabled()) return

    val context = LocalContext.current
    val adState by NativeAdManager.getAdFlow(screen).collectAsStateWithLifecycle()

    LaunchedEffect(screen, preferVideo) {
        NativeAdManager.loadAdForScreen(context, screen, preferVideo)
    }

    val ad = adState ?: return

    AndroidView(
        factory = { ctx ->
            createNativeAdLayout(ctx, style)
        },
        update = { nativeAdView ->
            bindNativeAd(nativeAdView, ad)
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    )
}

private fun createNativeAdLayout(context: Context, style: NativeAdStyle): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dpToPx(dp: Float): Int = (dp * density).toInt()

    val isCompact = style == NativeAdStyle.COMPACT
    val nativeAdView = NativeAdView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val cardLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val pad = if (isCompact) dpToPx(10f) else dpToPx(12f)
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#151515"))
            cornerRadius = dpToPx(12f).toFloat()
            setStroke(dpToPx(0.8f), AndroidColor.parseColor("#2A2A2E"))
        }
    }

    // Header row: "Ad" badge + Advertiser name
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = if (isCompact) dpToPx(6f) else dpToPx(8f)
        }
    }

    val adBadge = TextView(context).apply {
        text = "Ad"
        setTextColor(AndroidColor.parseColor("#E0E0E0"))
        textSize = if (isCompact) 9.5f else 10f
        typeface = Typeface.DEFAULT_BOLD
        val hPad = dpToPx(5f)
        val vPad = dpToPx(1.5f)
        setPadding(hPad, vPad, hPad, vPad)
        background = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#262626"))
            cornerRadius = dpToPx(4f).toFloat()
            setStroke(dpToPx(0.5f), AndroidColor.parseColor("#383838"))
        }
        contentDescription = "Sponsored advertisement"
    }
    headerRow.addView(adBadge)

    val advertiserView = TextView(context).apply {
        textSize = if (isCompact) 10.5f else 11f
        setTextColor(AndroidColor.parseColor("#A0A0A0"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dpToPx(8f)
        }
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    headerRow.addView(advertiserView)
    cardLayout.addView(headerRow)

    // MediaView (for Native Video / Image ads)
    val mediaHeight = dpToPx(if (isCompact) 120f else 140f)
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            mediaHeight
        ).apply {
            bottomMargin = if (isCompact) dpToPx(8f) else dpToPx(10f)
        }
        clipToOutline = true
        background = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#0B0B0B"))
            cornerRadius = dpToPx(8f).toFloat()
        }
    }
    cardLayout.addView(mediaView)

    // Details row: App icon + Headline/Body + CTA button
    val detailsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val iconSize = dpToPx(if (isCompact) 32f else 38f)
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            rightMargin = if (isCompact) dpToPx(8f) else dpToPx(10f)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        background = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#202020"))
            cornerRadius = dpToPx(6f).toFloat()
        }
    }
    detailsRow.addView(iconView)

    val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = if (isCompact) dpToPx(6f) else dpToPx(8f)
        }
    }

    val headlineView = TextView(context).apply {
        textSize = if (isCompact) 12f else 13f
        setTextColor(AndroidColor.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    textColumn.addView(headlineView)

    val bodyView = TextView(context).apply {
        textSize = if (isCompact) 10f else 11f
        setTextColor(AndroidColor.parseColor("#A0A0A0"))
        maxLines = if (isCompact) 1 else 2
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(2f)
        }
    }
    textColumn.addView(bodyView)
    detailsRow.addView(textColumn)

    val ctaButton = TextView(context).apply {
        textSize = if (isCompact) 11f else 12f
        setTextColor(AndroidColor.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        val hPad = dpToPx(if (isCompact) 10f else 12f)
        val vPad = dpToPx(if (isCompact) 6f else 8f)
        setPadding(hPad, vPad, hPad, vPad)
        background = GradientDrawable().apply {
            setColor(AndroidColor.parseColor("#E53935"))
            cornerRadius = dpToPx(6f).toFloat()
        }
    }
    detailsRow.addView(ctaButton)
    cardLayout.addView(detailsRow)

    nativeAdView.addView(cardLayout)
    nativeAdView.mediaView = mediaView
    nativeAdView.headlineView = headlineView
    nativeAdView.bodyView = bodyView
    nativeAdView.iconView = iconView
    nativeAdView.callToActionView = ctaButton
    nativeAdView.advertiserView = advertiserView

    return nativeAdView
}

private fun bindNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    if (nativeAdView.tag == nativeAd) return
    nativeAdView.tag = nativeAd

    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
    nativeAdView.mediaView?.setMediaContent(nativeAd.mediaContent)

    val bodyView = nativeAdView.bodyView as? TextView
    if (nativeAd.body != null) {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    } else {
        bodyView?.visibility = View.GONE
    }

    val ctaView = nativeAdView.callToActionView as? TextView
    if (nativeAd.callToAction != null) {
        ctaView?.text = nativeAd.callToAction
        ctaView?.visibility = View.VISIBLE
    } else {
        ctaView?.visibility = View.GONE
    }

    val iconView = nativeAdView.iconView as? ImageView
    if (nativeAd.icon?.drawable != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val advertiserView = nativeAdView.advertiserView as? TextView
    if (nativeAd.advertiser != null) {
        advertiserView?.text = nativeAd.advertiser
        advertiserView?.visibility = View.VISIBLE
    } else {
        advertiserView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}
