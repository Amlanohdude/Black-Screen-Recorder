package com.example.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted

enum class NativeAdStyle {
    FEED,
    COMPACT
}

@Composable
fun NativeAdCard(
    screen: AdScreen,
    style: NativeAdStyle = NativeAdStyle.FEED,
    modifier: Modifier = Modifier
) {
    if (!AdsConfig.isAdsEnabled()) return

    val context = LocalContext.current
    val adState by NativeAdManager.getAdFlow(screen).collectAsStateWithLifecycle()

    val cardHeight = if (style == NativeAdStyle.FEED) 180.dp else 90.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSecondary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Advertisement",
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}
