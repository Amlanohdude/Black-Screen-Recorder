package com.example.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.AppLinks
import com.example.localization.LocalAppStrings
import com.example.localization.descNonTouch
import com.example.localization.skip
import com.example.localization.titleNonTouch
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val badgeLabel: String,
    val showcaseType: ShowcaseType
)

private enum class ShowcaseType {
    SCREEN_OFF,
    HARDWARE_BUTTONS,
    BATTERY_SAVER,
    BIOMETRIC_VAULT
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val pages = remember(strings) {
        listOf(
            OnboardingPageData(
                title = strings.onboarding.title1,
                description = strings.onboarding.desc1,
                accentColor = AccentRed,
                secondaryColor = Color(0xFFFF5252),
                badgeLabel = "DISCREET RECORDING",
                showcaseType = ShowcaseType.SCREEN_OFF
            ),
            OnboardingPageData(
                title = strings.onboarding.titleNonTouch,
                description = strings.onboarding.descNonTouch,
                accentColor = Color(0xFF388E3C),
                secondaryColor = Color(0xFF81C784),
                badgeLabel = "HARDWARE GESTURES",
                showcaseType = ShowcaseType.HARDWARE_BUTTONS
            ),
            OnboardingPageData(
                title = strings.onboarding.title2,
                description = strings.onboarding.desc2,
                accentColor = Color(0xFF00B0FF),
                secondaryColor = Color(0xFF40C4FF),
                badgeLabel = "OLED EFFICIENCY",
                showcaseType = ShowcaseType.BATTERY_SAVER
            ),
            OnboardingPageData(
                title = strings.onboarding.title3,
                description = strings.onboarding.desc3,
                accentColor = Color(0xFFFFD54F),
                secondaryColor = Color(0xFFFFA000),
                badgeLabel = "PRIVATE VAULT",
                showcaseType = ShowcaseType.BIOMETRIC_VAULT
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_motion")
    val ctaBeamOffset by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboarding_cta_beam"
    )

    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSecondary)
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BLACK SCREEN RECORDER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = TextSecondary
                    )
                }

                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = strings.onboarding.skip,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Swipeable Pager with Live Animated Phone Mockups
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("onboarding_pager")
            ) { pageIndex ->
                val pageData = pages[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(pageData.accentColor.copy(alpha = 0.12f))
                            .border(0.8.dp, pageData.accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pageData.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = pageData.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Interactive Phone Mockup Showcase
                    PhoneMockupShowcase(
                        showcaseType = pageData.showcaseType,
                        accentColor = pageData.accentColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Animated Indicator Capsules
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in pages.indices) {
                    val isActive = i == currentPage
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isActive) 26.dp else 8.dp,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "indicator_width_$i"
                    )
                    val indicatorColor = if (isActive) pages[currentPage].accentColor else BorderSubtle

                    Box(
                        modifier = Modifier
                            .size(indicatorWidth, 7.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Dynamic CTA Button
            val activeColor = pages[currentPage].accentColor
            val ctaBrush = Brush.linearGradient(
                colors = listOf(
                    activeColor,
                    pages[currentPage].secondaryColor,
                    activeColor
                ),
                start = Offset(ctaBeamOffset, 0f),
                end = Offset(ctaBeamOffset + 260f, 160f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ctaBrush)
                    .clickable {
                        if (currentPage < pages.lastIndex) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    }
                    .testTag("onboarding_next_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "cta_text"
                    ) { targetIsLast ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (targetIsLast) strings.onboarding.getStarted else strings.onboarding.next,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (targetIsLast) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Legal Transparency & Compliance Links
            AnimatedVisibility(
                visible = isLastPage,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val annotatedText = remember(strings) {
                        buildAnnotatedString {
                            append(strings.onboarding.disclaimerPrefix)
                            pushStringAnnotation(tag = "TERMS", annotation = AppLinks.TERMS_AND_CONDITIONS_URL)
                            withStyle(
                                style = SpanStyle(
                                    color = AccentRed,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(strings.onboarding.termsOfService)
                            }
                            pop()
                            append(strings.onboarding.andConnector)
                            pushStringAnnotation(tag = "PRIVACY", annotation = AppLinks.PRIVACY_POLICY_URL)
                            withStyle(
                                style = SpanStyle(
                                    color = AccentRed,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(strings.privacyPolicy)
                            }
                            pop()
                            append(strings.onboarding.disclaimerSuffix)
                        }
                    }

                    @Suppress("DEPRECATION")
                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedText,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            annotatedText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        }
                    )
                }
            }

            if (!isLastPage) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/**
 * High-end smartphone chassis mockup hosting animated micro-scenes.
 */
@Composable
private fun PhoneMockupShowcase(
    showcaseType: ShowcaseType,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "phone_mockup_ambient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = modifier
            .width(148.dp)
            .height(216.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ambient Breathing Glow
        Box(
            modifier = Modifier
                .size(width = 144.dp, height = 212.dp)
                .graphicsLayer {
                    scaleX = ambientPulse
                    scaleY = ambientPulse
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        )

        // Outer Metallic Smartphone Chassis
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF28282E),
                            Color(0xFF141418),
                            Color(0xFF24242A)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF464650),
                            Color(0xFF202026),
                            Color(0xFF363640)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(5.dp)
        ) {
            // Inner AMOLED Screen Display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.Black)
                    .border(0.5.dp, Color(0xFF1E1E22), RoundedCornerShape(23.dp))
            ) {
                // Top Notch Camera Punch-Hole
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFF16161A), CircleShape)
                            .border(0.5.dp, Color(0xFF2E2E36), CircleShape)
                    )
                }

                // Render Live Showcase Micro-Scene
                when (showcaseType) {
                    ShowcaseType.SCREEN_OFF -> ScreenOffLiveScene()
                    ShowcaseType.HARDWARE_BUTTONS -> HardwareButtonsLiveScene()
                    ShowcaseType.BATTERY_SAVER -> BatterySaverLiveScene()
                    ShowcaseType.BIOMETRIC_VAULT -> BiometricVaultLiveScene()
                }
            }
        }
    }
}

/**
 * Slide 1: Live Viewfinder -> Instant Blackout Covert Simulation
 */
@Composable
private fun ScreenOffLiveScene() {
    val transition = rememberInfiniteTransition(label = "screen_off_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val isBlackout = phase >= 0.45f

    val recDotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_dot_blink"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isBlackout) {
            // Viewfinder Camera Preview Phase
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AccentRed.copy(alpha = recDotAlpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "REC",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )
                    }
                    Text(
                        text = "00:08",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Camera Crosshair Frame
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(0.8.dp, Color(0xFF44444C), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF888894),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "DISPLAY ACTIVE",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA)
                )
            }
        } else {
            // 100% True Blackout Screen-Off Covert Phase
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Discreet Micro Stealth LED
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AccentRed.copy(alpha = recDotAlpha), CircleShape)
                            .border(0.5.dp, Color(0xFF550000), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SCREEN OFF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFDDDDDD)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "RECORDING SILENTLY",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = AccentRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141416))
                            .border(0.5.dp, Color(0xFF2A2A30), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "0% BRIGHTNESS",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 2: Hardware Physical Buttons & Motion Shake Simulation
 */
@Composable
private fun HardwareButtonsLiveScene() {
    val transition = rememberInfiniteTransition(label = "hardware_scene_motion")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle"
    )

    val isPressed = cycle in 0.35f..0.85f
    val tiltOffset = if (cycle in 0.30f..0.48f) (-2.5f) else 0f

    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = tiltOffset },
        contentAlignment = Alignment.Center
    ) {
        // Simulated Side Hardware Volume Rocker on the left chassis edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 1.dp)
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPressed) Color(0xFF4CAF50) else Color(0xFF44444A))
        )

        // Screen Interior
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            if (!isPressed) {
                // Pre-Press Stealth Recording State
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181C))
                        .border(0.8.dp, Color(0xFF2E2E36), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RECORDING...",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "CLICK VOLUME / SHAKE",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF777777)
                )
            } else {
                // Post-Gesture Saved Confirmation
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer {
                            scaleX = pulseScale.coerceAtMost(1.15f)
                            scaleY = pulseScale.coerceAtMost(1.15f)
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF1B5E20).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SAVED SAFELY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "ZERO TOUCH NEEDED",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBDBDBD)
                )
            }
        }
    }
}

/**
 * Slide 3: True OLED Battery Efficiency Gauge Simulation
 */
@Composable
private fun BatterySaverLiveScene() {
    val transition = rememberInfiniteTransition(label = "battery_motion")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Battery Gauge with Cyan Aura
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(Color(0xFF00B0FF).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFF00B0FF).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                tint = Color(0xFF00B0FF),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "0.0W DRAIN",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF00B0FF)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "AMOLED PIXELS OFF",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCCCCCC)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00B0FF).copy(alpha = 0.12f))
                .border(0.5.dp, Color(0xFF00B0FF).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "ANTI-BURN-IN ACTIVE",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF40C4FF)
            )
        }
    }
}

/**
 * Slide 4: Biometric Fingerprint & Encrypted Vault Simulation
 */
@Composable
private fun BiometricVaultLiveScene() {
    val transition = rememberInfiniteTransition(label = "vault_scan_motion")
    val scanCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_cycle"
    )

    val isUnlocked = scanCycle >= 0.48f
    val laserOffset = (scanCycle * 44f) - 22f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            // Fingerprint / Lock Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Scanning Laser Line
            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .offset(y = laserOffset.dp)
                        .width(42.dp)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFFFD54F),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isUnlocked) "VAULT UNLOCKED" else "BIOMETRIC AUTH",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD54F)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isUnlocked) "HIDDEN FROM GALLERY" else "TOUCH FINGERPRINT",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFDDDDDD)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222226))
                .border(0.5.dp, Color(0xFF3A3A42), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = "ENCRYPTED STORAGE",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAAAAA)
            )
        }
    }
}
