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
import androidx.compose.ui.text.style.TextOverflow
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
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = pageData.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = pageData.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3D Live Interactive Phone Mockup Showcase
                    PhoneMockup3DShowcase(
                        showcaseType = pageData.showcaseType,
                        accentColor = pageData.accentColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Streamlined Subtext - Crisp, non-heavy 2-line maximum
                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp)
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
 * Ultra-realistic 3D Smartphone Showcase with perspective projection,
 * extruded side bezels, physical hardware buttons, and dynamic micro-scenes.
 */
@Composable
private fun PhoneMockup3DShowcase(
    showcaseType: ShowcaseType,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "phone_3d_motion")

    // Dynamic 3D perspective floating & rotation angles (pronounced 3D perspective)
    val rotY by infiniteTransition.animateFloat(
        initialValue = -24f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot_y"
    )
    val rotX by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot_x"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    // Hardware buttons animation cycle on Slide 2
    val hwCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hw_cycle"
    )
    val isHwShake = showcaseType == ShowcaseType.HARDWARE_BUTTONS && hwCycle in 0.36f..0.52f
    val hwShakeAngle = if (isHwShake) {
        val progress = (hwCycle - 0.36f) / 0.16f
        kotlin.math.sin(progress * kotlin.math.PI * 4).toFloat() * 6f
    } else 0f
    val isVolumeActive = showcaseType == ShowcaseType.HARDWARE_BUTTONS && hwCycle in 0.35f..0.85f

    // Battery saver animation cycle on Slide 3
    val batteryCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battery_cycle"
    )
    val isBatteryHot = showcaseType == ShowcaseType.BATTERY_SAVER && batteryCycle < 0.45f

    // Vault animation cycle on Slide 4
    val vaultCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vault_cycle"
    )

    // Dynamic ambient aura color based on live state
    val dynamicAmbientColor = when (showcaseType) {
        ShowcaseType.SCREEN_OFF -> accentColor
        ShowcaseType.HARDWARE_BUTTONS -> if (isVolumeActive) Color(0xFF4CAF50) else accentColor
        ShowcaseType.BATTERY_SAVER -> if (isBatteryHot) Color(0xFFFF5722) else Color(0xFF00B0FF)
        ShowcaseType.BIOMETRIC_VAULT -> accentColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Perspective Ground Contact Shadow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-8).dp, y = 10.dp)
                .width(165.dp)
                .height(24.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            dynamicAmbientColor.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // 3D Floating Smartphone Chassis
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .graphicsLayer {
                    rotationY = rotY
                    rotationX = rotX
                    rotationZ = -4f + hwShakeAngle
                    cameraDistance = 12f * density
                    shadowElevation = 24.dp.toPx()
                    shape = RoundedCornerShape(28.dp)
                    clip = false
                }
                .width(172.dp)
                .height(262.dp)
        ) {
            // Extruded 3D Side Bezel Depth Layer (reveals thickness when rotated in 3D)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 6.dp, y = 5.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF26262E),
                                Color(0xFF141418),
                                Color(0xFF1C1C22)
                            )
                        )
                    )
            )

            // Front Outer Titanium Bezel Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF2C2C34),
                                Color(0xFF16161C),
                                Color(0xFF202028)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF565666),
                                Color(0xFF22222A),
                                Color(0xFF3E3E4C)
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
                        .border(0.5.dp, Color(0xFF1A1A22), RoundedCornerShape(23.dp))
                ) {
                    // Render Live Interactive Micro-Scene
                    when (showcaseType) {
                        ShowcaseType.SCREEN_OFF -> ScreenOff3DLiveScene()
                        ShowcaseType.HARDWARE_BUTTONS -> HardwareButtons3DLiveScene(isClicked = isVolumeActive)
                        ShowcaseType.BATTERY_SAVER -> BatterySaver3DLiveScene(cycle = batteryCycle)
                        ShowcaseType.BIOMETRIC_VAULT -> BiometricVault3DLiveScene(cycle = vaultCycle)
                    }

                    // Curved Glass Sheen / Specular Light Reflection
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.07f),
                                        Color.White.copy(alpha = 0.02f),
                                        Color.Transparent,
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(240f, 480f)
                                )
                            )
                    )

                    // Top Dynamic Island Punch-Hole Camera
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 5.dp)
                            .size(width = 18.dp, height = 4.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D0D10))
                            .border(0.4.dp, Color(0xFF24242C), CircleShape)
                    )
                }
            }

            // Left Side Physical Hardware Volume Rocker (Upper)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-3.5).dp, y = (-14).dp)
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFF4CAF50) else Color(0xFF3C3C46)
                    )
            )

            // Left Side Physical Hardware Volume Rocker (Lower)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-3.5).dp, y = 24.dp)
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFF81C784) else Color(0xFF32323A)
                    )
            )

            // Right Side Physical Hardware Power Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 3.5.dp, y = (-6).dp)
                    .width(3.5.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(Color(0xFF383842))
            )

            // Glowing Drop Aura for Volume Key Click
            if (isVolumeActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-12).dp, y = 5.dp)
                        .size(24.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Slide 1: Live Viewfinder / Active Screen -> Instant Pure Blackout Covert Simulation
 */
@Composable
private fun ScreenOff3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "screen_off_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle"
    )

    // Phase 1 (0f .. 0.40f): Bright colorful camera viewfinder recording
    // Phase 2 (0.40f .. 0.50f): Shutter wipes down from top to bottom
    // Phase 3 (0.50f .. 1f): 100% pitch black screen with covert stealth HUD
    val isBlackout = cycle >= 0.48f

    val recBlink by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_blink"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Base Layer: BRIGHT VIVID ACTIVE SCREEN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFF3D00),
                            Color(0xFFFF9100),
                            Color(0xFFFFD600)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("09:41", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Vibrant Center Content (Simulating live camera viewfinder / video capture)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Red.copy(alpha = recBlink), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("LIVE REC", fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Text("1080P", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "BRIGHT SCREEN ACTIVE",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Bottom Tap Action Prompt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (cycle < 0.40f) "TAP TO BLACKOUT" else "BLACKING OUT...",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Overlay Shutter Layer: Sweeps down to completely turn screen off!
        if (cycle >= 0.40f) {
            val sweepAlpha = ((cycle - 0.40f) / 0.08f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sweepAlpha }
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isBlackout) {
                    // 100% PITCH BLACK AMOLED COVERT STEALTH RECORDING
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Covert Pulsing Stealth Red Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentRed.copy(alpha = recBlink), CircleShape)
                                .border(0.8.dp, Color(0xFF660000), CircleShape)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "SCREEN OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFEEEEEE)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "● REC 00:04 · 1080P",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentRed
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141416))
                                .border(0.6.dp, Color(0xFF282830), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "0% BRIGHTNESS · COVERT",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAAAAAA)
                            )
                        }
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
private fun HardwareButtons3DLiveScene(isClicked: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isClicked) {
            // Screen is black, recording in stealth
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16161A))
                        .border(0.8.dp, Color(0xFF2E2E36), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RECORDING IN POCKET",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "● REC 00:18",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF81C784)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E281E))
                        .border(0.6.dp, Color(0xFF388E3C), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "PRESS VOLUME TO STOP",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5D6A7)
                    )
                }
            }
        } else {
            // STOPPED & SAVED CONFIRMATION
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF081208))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.25f))
                        .border(1.2.dp, Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "STOPPED & SAVED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "ZERO TOUCH NEEDED",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC8E6C9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1B3B1B))
                        .border(0.5.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SAVED TO VAULT",
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
            }
        }
    }
}

/**
 * Slide 3: True OLED Battery Efficiency Gauge Simulation
 */
@Composable
private fun BatterySaver3DLiveScene(cycle: Float) {
    val isBlackout = cycle >= 0.45f

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isBlackout) {
            // BRIGHT WHITE / HIGH DRAIN STATE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF6D00),
                                Color(0xFFFF9100),
                                Color(0xFFFFAB40)
                            )
                        )
                    )
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "4.8W POWER DRAIN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "SCREEN ON · DRAINING FAST",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE0B2)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HEATS PHONE & BATTERY",
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD180)
                    )
                }
            }
        } else {
            // TRUE BLACK / 0.0W DRAIN STATE
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B0FF).copy(alpha = 0.15f))
                        .border(1.2.dp, Color(0xFF00B0FF), CircleShape),
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
                    text = "ALL OLED PIXELS OFF",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF002238))
                        .border(0.5.dp, Color(0xFF00B0FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ANTI-BURN-IN ACTIVE",
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80D8FF)
                    )
                }
            }
        }
    }
}

/**
 * Slide 4: Biometric Fingerprint & Encrypted Vault Simulation
 */
@Composable
private fun BiometricVault3DLiveScene(cycle: Float) {
    val isHidden = cycle >= 0.46f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isHidden) {
            // Simulated Public Media Gallery
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16161A))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PUBLIC GALLERY", fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFAAAAAA))
                    Text("VISIBLE", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                }

                // 4 Simulated video cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A34))
                                .border(0.5.dp, Color(0xFF444450), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF888894), modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A34))
                                .border(0.5.dp, Color(0xFF444450), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF888894), modifier = Modifier.size(16.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A34))
                                .border(0.5.dp, Color(0xFF444450), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF888894), modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A34))
                                .border(0.5.dp, Color(0xFF444450), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF888894), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("HIDING INTO VAULT...", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                }
            }
        } else {
            // ENCRYPTED & HIDDEN FROM GALLERY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0E08))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFD54F).copy(alpha = 0.18f))
                        .border(1.2.dp, Color(0xFFFFD54F), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "VAULT ENCRYPTED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD54F)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "HIDDEN FROM GALLERY",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEEEEEE)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282208))
                        .border(0.5.dp, Color(0xFFFFD54F), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BIOMETRIC PIN ONLY",
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE082)
                    )
                }
            }
        }
    }
}
