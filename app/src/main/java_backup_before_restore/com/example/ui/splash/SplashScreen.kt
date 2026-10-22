package com.example.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Cinematic Pulsing Stealth Glow Splash Screen
 *
 * Implements a high-end, discreet recording aesthetic designed specifically for OLED screens.
 * Features:
 * - Pure OLED black backdrop (#050303)
 * - Elastic spring entrance for the vector app logo
 * - Concentric radar pulse ripples emanating from the center red recording dot
 * - Dynamic typography reveal with animated letter spacing
 * - Guaranteed fail-safe exit transition into the main application
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startEntrance by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // Trigger entrance sequences
    LaunchedEffect(Unit) {
        startEntrance = true
        delay(300)
        showText = true
        delay(1500)
        isExiting = true
        delay(350)
        onSplashFinished()
    }

    // Logo entrance & exit spring transitions
    val logoScale by animateFloatAsState(
        targetValue = when {
            isExiting -> 1.08f
            startEntrance -> 1f
            else -> 0.72f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startEntrance -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = if (isExiting) 320 else 650, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    // Text reveal transitions
    val textAlpha by animateFloatAsState(
        targetValue = if (showText && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = if (isExiting) 250 else 550),
        label = "text_alpha"
    )

    val textSpacing by animateFloatAsState(
        targetValue = if (showText) 3.5f else 1.2f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "text_spacing"
    )

    // Infinite radar pulse waves for the red recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )
    val pulseProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.Center
    ) {
        // Ambient stealth background glow behind center logo
        Canvas(modifier = Modifier.size(360.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentRed.copy(alpha = 0.12f * logoAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 2f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Main Logo Container with pulsing recording radar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
            ) {
                // Expanding radar pulse ripples centered on the red recording dot
                // (In the 367x366 SVG, red dot is horizontally centered and at ~43.8% vertical)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val dotCenter = Offset(
                        x = size.width * 0.5f,
                        y = size.height * 0.438f
                    )

                    // Ripple 1
                    val radius1 = size.width * 0.16f + (pulseProgress1 * size.width * 0.45f)
                    val alpha1 = (1f - pulseProgress1).coerceIn(0f, 1f) * 0.5f * logoAlpha
                    drawCircle(
                        color = Color(0xFFFE1B1E).copy(alpha = alpha1),
                        radius = radius1,
                        center = dotCenter,
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Ripple 2
                    val radius2 = size.width * 0.16f + (pulseProgress2 * size.width * 0.45f)
                    val alpha2 = (1f - pulseProgress2).coerceIn(0f, 1f) * 0.5f * logoAlpha
                    drawCircle(
                        color = Color(0xFFFE1B1E).copy(alpha = alpha2),
                        radius = radius2,
                        center = dotCenter,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                // Vector App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Black Screen Recorder Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(36.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Branding Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            ) {
                Text(
                    text = "BLACK SCREEN RECORDER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = textSpacing.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DISCREET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(AccentRed, CircleShape)
                    )
                    Text(
                        text = "PRIVATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(AccentRed, CircleShape)
                    )
                    Text(
                        text = "POWERFUL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
