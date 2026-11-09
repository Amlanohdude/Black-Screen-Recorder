package com.example.ui.recorder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalAppStrings
import com.example.ui.theme.AccentRed
import kotlinx.coroutines.delay

/**
 * FadingRecCountdownOverlay provides a visual HUD on recording startup.
 * Displays a pulsing red REC symbol, a 3-second countdown ring, and a concise helper subtext,
 * completely unboxed directly on AMOLED true black, then smoothly fades out for energy conservation.
 */
@Composable
fun FadingRecCountdownOverlay(
    onCountdownFinished: () -> Unit,
    onFadeComplete: () -> Unit,
    modifier: Modifier = Modifier,
    subtext: String? = null
) {
    val displaySubtext = subtext ?: LocalAppStrings.current.tapToStop
    var countdownValue by remember { mutableIntStateOf(3) }
    var isFading by remember { mutableStateOf(false) }
    var hasTriggeredFinished by remember { mutableStateOf(false) }

    fun triggerCountdownFinished() {
        if (!hasTriggeredFinished) {
            hasTriggeredFinished = true
            onCountdownFinished()
        }
    }

    // Pulsing animation for the red recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val dotPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse_alpha"
    )

    // Smooth fade-out alpha for the entire overlay
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isFading) 0f else 1f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "overlay_fade_alpha",
        finishedListener = { alpha ->
            if (alpha <= 0.01f) {
                onFadeComplete()
            }
        }
    )

    // Progress target for circular countdown sweep
    val sweepProgress by animateFloatAsState(
        targetValue = when (countdownValue) {
            3 -> 1.0f
            2 -> 0.66f
            1 -> 0.33f
            else -> 0.0f
        },
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "countdown_sweep"
    )

    // 3-second countdown: 3 -> 2 -> 1 -> LIVE (triggering recording), then fading out
    LaunchedEffect(Unit) {
        delay(1000L)
        countdownValue = 2
        delay(1000L)
        countdownValue = 1
        delay(1000L)
        countdownValue = 0 // "LIVE"
        triggerCountdownFinished()
        delay(400L)
        isFading = true
        delay(1200L)
        onFadeComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(overlayAlpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Dismiss immediately if user taps, and trigger recording start immediately
                        triggerCountdownFinished()
                        isFading = true
                    }
                )
            }
            .testTag("fading_rec_countdown_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // REC Header with Pulsing Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AccentRed.copy(alpha = dotPulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Circular Countdown Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track background
                    drawCircle(
                        color = Color(0x22FFFFFF),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Animated countdown arc
                    drawArc(
                        color = AccentRed,
                        startAngle = -90f,
                        sweepAngle = 360f * sweepProgress,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = if (countdownValue > 0) countdownValue.toString() else "LIVE",
                    color = if (countdownValue > 0) Color.White else AccentRed,
                    fontSize = if (countdownValue > 0) 32.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Concise, enlarged helper subtext directly on AMOLED black without box
            Text(
                text = displaySubtext,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
