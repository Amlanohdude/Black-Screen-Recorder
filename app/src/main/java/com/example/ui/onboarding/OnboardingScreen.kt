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
                description = "Record video and audio while screen is black",
                accentColor = AccentRed,
                secondaryColor = Color(0xFFFF5252),
                showcaseType = ShowcaseType.SCREEN_OFF
            ),
            OnboardingPageData(
                title = strings.onboarding.titleNonTouch,
                description = "Stop recording with volume buttons or a shake",
                accentColor = Color(0xFF388E3C),
                secondaryColor = Color(0xFF81C784),
                showcaseType = ShowcaseType.HARDWARE_BUTTONS
            ),
            OnboardingPageData(
                title = strings.onboarding.title2,
                description = "AMOLED true black uses 0% display power",
                accentColor = Color(0xFF00B0FF),
                secondaryColor = Color(0xFF40C4FF),
                showcaseType = ShowcaseType.BATTERY_SAVER
            ),
            OnboardingPageData(
                title = strings.onboarding.title3,
                description = "Encrypted and hidden from gallery",
                accentColor = Color(0xFFFFD54F),
                secondaryColor = Color(0xFFFFA000),
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
                    // 3D Live Interactive Phone Mockup Showcase (Sleek 19:9 Ratio)
                    PhoneMockup3DShowcase(
                        showcaseType = pageData.showcaseType,
                        accentColor = pageData.accentColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Streamlined Subtext - Crisp single line, zero text clutter
                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
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

    // Dynamic 3D perspective floating & rotation angles
    val rotY by infiniteTransition.animateFloat(
        initialValue = -22f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot_y"
    )
    val rotX by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot_x"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hw_cycle"
    )
    val isHwShake = showcaseType == ShowcaseType.HARDWARE_BUTTONS && hwCycle in 0.36f..0.52f
    val hwShakeAngle = if (isHwShake) {
        val progress = (hwCycle - 0.36f) / 0.16f
        kotlin.math.sin(progress * kotlin.math.PI * 4).toFloat() * 5f
    } else 0f
    val isVolumeActive = showcaseType == ShowcaseType.HARDWARE_BUTTONS && hwCycle in 0.35f..0.85f

    // Battery saver animation cycle on Slide 3
    val batteryCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
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
            animation = tween(4200, easing = LinearEasing),
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
            .height(295.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Perspective Ground Contact Shadow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-6).dp, y = 8.dp)
                .width(135.dp)
                .height(20.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            dynamicAmbientColor.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // 3D Floating Smartphone Chassis (Modern Flagship 19.5:9 Aspect Ratio)
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .graphicsLayer {
                    rotationY = rotY
                    rotationX = rotX
                    rotationZ = -3.5f + hwShakeAngle
                    cameraDistance = 14f * density
                    shadowElevation = 20.dp.toPx()
                    shape = RoundedCornerShape(30.dp)
                    clip = false
                }
                .width(138.dp)
                .height(286.dp)
        ) {
            // Extruded 3D Side Bezel Depth Layer (reveals metallic thickness in perspective)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 5.dp, y = 4.5.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF282830),
                                Color(0xFF141418),
                                Color(0xFF1A1A20)
                            )
                        )
                    )
            )

            // Front Outer Titanium Bezel Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
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
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF565666),
                                Color(0xFF22222A),
                                Color(0xFF3E3E4C)
                            )
                        ),
                        RoundedCornerShape(30.dp)
                    )
                    .padding(4.5.dp)
            ) {
                // Inner AMOLED Screen Display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.Black)
                        .border(0.5.dp, Color(0xFF181820), RoundedCornerShape(25.dp))
                ) {
                    // Render Live Interactive Micro-Scene
                    when (showcaseType) {
                        ShowcaseType.SCREEN_OFF -> ScreenOff3DLiveScene()
                        ShowcaseType.HARDWARE_BUTTONS -> HardwareButtons3DLiveScene()
                        ShowcaseType.BATTERY_SAVER -> BatterySaver3DLiveScene()
                        ShowcaseType.BIOMETRIC_VAULT -> BiometricVault3DLiveScene()
                    }

                    // Curved Glass Sheen / Specular Light Reflection
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.White.copy(alpha = 0.015f),
                                        Color.Transparent,
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(200f, 440f)
                                )
                            )
                    )

                    // Top Dynamic Island Punch-Hole Camera
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 5.dp)
                            .size(width = 16.dp, height = 4.dp)
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
                    .offset(x = (-3).dp, y = (-18).dp)
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFF4CAF50) else Color(0xFF3C3C46)
                    )
            )

            // Left Side Physical Hardware Volume Rocker (Lower)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-3).dp, y = 16.dp)
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFF81C784) else Color(0xFF32323A)
                    )
            )

            // Right Side Physical Hardware Power Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 3.dp, y = (-8).dp)
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(Color(0xFF383842))
            )

            // Glowing Indicator for Volume Key Click
            if (isVolumeActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-10).dp, y = 0.dp)
                        .size(20.dp)
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

    // Phase 1 (0f .. 0.42f): Bright camera viewfinder
    // Phase 2 (0.42f .. 0.50f): Shutter wipe down
    // Phase 3 (0.50f .. 1f): 100% pitch black screen
    val isBlackout = cycle >= 0.48f

    val recBlink by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_blink"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Base Layer: REAL CAMERA VIEWFINDER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1A24),
                            Color(0xFF2E2638),
                            Color(0xFF1A1A24)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Camera Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color.Red.copy(alpha = recBlink), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "00:03",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    "4K 60",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Viewfinder Center: Realistic Focus Reticle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFFFD54F).copy(alpha = 0.8f), CircleShape)
                )
            }

            // Camera Bottom Controls (Authentic Shutter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outer Ring + Red Recording Core Shutter Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Red)
                    )
                }
            }
        }

        // Overlay Shutter Layer: Sweeps down to completely turn screen off
        if (cycle >= 0.42f) {
            val sweepAlpha = ((cycle - 0.42f) / 0.08f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sweepAlpha }
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isBlackout) {
                    // 100% PURE AMOLED COVERT BLACKOUT
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Minimal stealth red dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(AccentRed.copy(alpha = recBlink), CircleShape)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "REC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentRed
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "00:04",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
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
private fun HardwareButtons3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "hw_buttons_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hw_cycle"
    )
    val isClicked = cycle in 0.40f..0.90f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isClicked) {
            // Black screen recording in pocket
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418)),
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
                    text = "Screen Off",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Press volume to stop",
                    fontSize = 8.5.sp,
                    color = Color(0xFF888888)
                )
            }
        } else {
            // Native system pill toast: Clean, authentic OS style
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B3820)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Saved",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Zero touch",
                    fontSize = 8.5.sp,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

/**
 * Slide 3: True OLED Battery Efficiency Simulation
 */
@Composable
private fun BatterySaver3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "battery_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battery_cycle"
    )
    val isBlackout = cycle >= 0.45f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (!isBlackout) Color(0xFF1E1410) else Color.Black)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isBlackout) {
            // Screen On: High Drain
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = Color(0xFFFF7043),
                    modifier = Modifier.size(30.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Screen On",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "High Power Drain",
                    fontSize = 8.sp,
                    color = Color(0xFFFF8A65)
                )
            }
        } else {
            // OLED Pixels Off: 0% Drain
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF003350)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = Color(0xFF00B0FF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "0% Display Drain",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00B0FF)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "OLED Pixels Off",
                    fontSize = 8.sp,
                    color = Color(0xFF80D8FF)
                )
            }
        }
    }
}

/**
 * Slide 4: Biometric & Encrypted Vault Simulation
 */
@Composable
private fun BiometricVault3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "vault_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vault_cycle"
    )
    val isHidden = cycle >= 0.46f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isHidden) {
            // Gallery View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gallery",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Clean 2x2 grid
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
                                .background(Color(0xFF22222A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF666677), modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF22222A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF666677), modifier = Modifier.size(16.dp))
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
                                .background(Color(0xFF22222A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF666677), modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF22222A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF666677), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text(
                    "Moving to vault...",
                    fontSize = 7.sp,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 2.dp)
                )
            }
        } else {
            // Encrypted Vault
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF332808)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Vault Locked",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Hidden from gallery",
                    fontSize = 8.sp,
                    color = Color(0xFFFFD54F)
                )
            }
        }
    }
}
