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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.localization.descBgWidgets
import com.example.localization.descCustomCovers
import com.example.localization.descNonTouch
import com.example.localization.skip
import com.example.localization.titleBgWidgets
import com.example.localization.titleCustomCovers
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
    BACKGROUND_WIDGETS,
    CUSTOM_COVERS,
    HARDWARE_BUTTONS
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
                title = strings.onboarding.titleBgWidgets,
                description = strings.onboarding.descBgWidgets,
                accentColor = Color(0xFF00C853),
                secondaryColor = Color(0xFF69F0AE),
                showcaseType = ShowcaseType.BACKGROUND_WIDGETS
            ),
            OnboardingPageData(
                title = strings.onboarding.titleCustomCovers,
                description = strings.onboarding.descCustomCovers,
                accentColor = Color(0xFF7C4DFF),
                secondaryColor = Color(0xFFB388FF),
                showcaseType = ShowcaseType.CUSTOM_COVERS
            ),
            OnboardingPageData(
                title = strings.onboarding.titleNonTouch,
                description = "Stop recording with volume buttons or a shake",
                accentColor = Color(0xFFFFD54F),
                secondaryColor = Color(0xFFFFA000),
                showcaseType = ShowcaseType.HARDWARE_BUTTONS
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
            // Header Bar with Centered Header Badge and Top-Right Skip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center
            ) {
                // Top-Center Brand Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSecondary)
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
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

                // Top-Right Skip Button
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("onboarding_skip_button")
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
        initialValue = -20f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot_y"
    )
    val rotX by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 16f,
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

    // Hardware buttons animation cycle on Slide 4
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp),
        contentAlignment = Alignment.Center
    ) {
        // 3D Perspective Ground Contact Shadow (Hero Sized)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-8).dp, y = 8.dp)
                .width(185.dp)
                .height(26.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // 3D Floating Smartphone Chassis (Enlarged Hero 19.5:9 Aspect Ratio)
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .graphicsLayer {
                    rotationY = rotY
                    rotationX = rotX
                    rotationZ = -3.5f + hwShakeAngle
                    cameraDistance = 14f * density
                    shadowElevation = 24.dp.toPx()
                    shape = RoundedCornerShape(38.dp)
                    clip = false
                }
                .width(186.dp)
                .height(385.dp)
        ) {
            // Extruded 3D Side Bezel Depth Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 6.dp, y = 5.dp)
                    .clip(RoundedCornerShape(38.dp))
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
                    .clip(RoundedCornerShape(38.dp))
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
                        RoundedCornerShape(38.dp)
                    )
                    .padding(5.dp)
            ) {
                // Inner AMOLED Screen Display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(33.dp))
                        .background(Color.Black)
                        .border(0.5.dp, Color(0xFF181820), RoundedCornerShape(33.dp))
                ) {
                    // Render Live Interactive Micro-Scene
                    when (showcaseType) {
                        ShowcaseType.SCREEN_OFF -> ScreenOff3DLiveScene()
                        ShowcaseType.BACKGROUND_WIDGETS -> BackgroundWidgets3DLiveScene()
                        ShowcaseType.CUSTOM_COVERS -> CustomCovers3DLiveScene()
                        ShowcaseType.HARDWARE_BUTTONS -> HardwareButtons3DLiveScene()
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
                                    end = Offset(240f, 520f)
                                )
                            )
                    )

                    // Top Dynamic Island Punch-Hole Camera
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 5.5.dp)
                            .size(width = 24.dp, height = 5.dp)
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
                    .offset(x = (-3.5).dp, y = (-26).dp)
                    .width(3.5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFFFFD54F) else Color(0xFF3C3C46)
                    )
            )

            // Left Side Physical Hardware Volume Rocker (Lower)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-3.5).dp, y = 22.dp)
                    .width(3.5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                    .background(
                        if (isVolumeActive) Color(0xFFFFE082) else Color(0xFF32323A)
                    )
            )

            // Right Side Physical Hardware Power Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 3.5.dp, y = (-10).dp)
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(Color(0xFF383842))
            )

            // Glowing Indicator for Volume Key Click
            if (isVolumeActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-12).dp, y = (-2).dp)
                        .size(24.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.6f),
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

    // Phase 1 (0f .. 0.44f): Bright, vivid daytime camera viewfinder
    // Phase 2 (0.44f .. 0.50f): Shutter sweep blackout
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
        // Base Layer: BRIGHT VIVID DAYLIGHT CAMERA VIEWFINDER
        Box(modifier = Modifier.fillMaxSize()) {
            // Bright daylight outdoor scene rendered with Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Radiant daylight sky gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF29B6F6), // Bright Azure
                            Color(0xFF81D4FA), // Vibrant Sky blue
                            Color(0xFFE1F5FE)  // Sunny horizon
                        ),
                        startY = 0f,
                        endY = h * 0.65f
                    ),
                    size = Size(w, h * 0.65f)
                )

                // Golden radiant sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF9C4),
                            Color(0xFFFFEE58).copy(alpha = 0.85f),
                            Color(0xFFFFCA28).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.20f),
                        radius = w * 0.35f
                    ),
                    center = Offset(w * 0.82f, h * 0.20f),
                    radius = w * 0.35f
                )

                // Rolling green landscape hills
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF81C784), Color(0xFF4CAF50))
                    ),
                    topLeft = Offset(-w * 0.25f, h * 0.46f),
                    size = Size(w * 0.95f, h * 0.35f)
                )
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF66BB6A), Color(0xFF388E3C))
                    ),
                    topLeft = Offset(w * 0.3f, h * 0.50f),
                    size = Size(w * 0.95f, h * 0.32f)
                )

                // Foreground terrace lawn
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF43A047), Color(0xFF2E7D32))
                    ),
                    topLeft = Offset(0f, h * 0.66f),
                    size = Size(w, h * 0.34f)
                )

                // Photography 3x3 Rule-of-Thirds Grid
                val gridColor = Color.White.copy(alpha = 0.42f)
                val strokeWidth = 0.75.dp.toPx()
                drawLine(gridColor, Offset(0f, h * 0.33f), Offset(w, h * 0.33f), strokeWidth = strokeWidth)
                drawLine(gridColor, Offset(0f, h * 0.66f), Offset(w, h * 0.66f), strokeWidth = strokeWidth)
                drawLine(gridColor, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeWidth = strokeWidth)
                drawLine(gridColor, Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), strokeWidth = strokeWidth)
            }

            // Foreground Camera Viewfinder HUD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Camera Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
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
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 4K 60 Quality Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "4K 60",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Center Focus Reticle
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(1.2.dp, Color(0xFFFFEB3B).copy(alpha = 0.9f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFFFEB3B), CircleShape)
                    )
                }

                // Bottom Camera Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0.6x", fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f))
                        Text("1x", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFEB3B))
                        Text("2x", fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f))
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    // Shutter button (White ring + Red record button)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.Red)
                        )
                    }
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Screen Off",
                            fontSize = 7.sp,
                            color = Color(0xFF444444)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 2: Background Recording & 1-Tap Home Screen Widgets Simulation
 * Features a bright, authentic Android Material You wallpaper with patterns,
 * Android status bar with privacy indicator, 1-tap widget, and bottom dock.
 */
@Composable
private fun BackgroundWidgets3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "bg_widgets_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_cycle"
    )

    // Phase 1 (0 .. 0.38): Standby on Home Screen
    // Phase 2 (0.38 .. 0.44): User finger tap on widget trigger
    // Phase 3 (0.44 .. 1.0): Recording active in background while user is on Home Screen!
    val isRecording = cycle >= 0.42f
    val isTapping = cycle in 0.34f..0.42f

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
        // Bright Authentic Android Material You Wallpaper with Patterns
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base multi-stop vibrant gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF29B6F6), // Radiant Sky Blue
                        Color(0xFF7E57C2), // Orchid Purple
                        Color(0xFFFF7043), // Sunset Coral
                        Color(0xFFFFD54F)  // Warm Gold
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Pattern 1: Large luminous top-right petal arch
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF80DEEA).copy(alpha = 0.7f),
                        Color(0xFF26C6DA).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.18f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.85f, h * 0.18f),
                radius = w * 0.65f
            )

            // Pattern 2: Sweeping organic center wave petal
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF8A80).copy(alpha = 0.6f),
                        Color(0xFFFF5252).copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(-w * 0.35f, h * 0.32f),
                size = Size(w * 1.1f, h * 0.38f)
            )

            // Pattern 3: Overlapping bright amber floral arch
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF176).copy(alpha = 0.65f),
                        Color(0xFFFFD54F).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.75f, h * 0.62f),
                    radius = w * 0.5f
                ),
                center = Offset(w * 0.75f, h * 0.62f),
                radius = w * 0.5f
            )

            // Pattern 4: Bottom soft mint / lavender fluid curves
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB388FF).copy(alpha = 0.5f),
                        Color(0xFF69F0AE).copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(w * 0.1f, h * 0.68f),
                size = Size(w * 1.05f, h * 0.35f)
            )

            // Subtle decorative Android wallpaper rings
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                center = Offset(w * 0.85f, h * 0.18f),
                radius = w * 0.38f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                center = Offset(w * 0.75f, h * 0.62f),
                radius = w * 0.28f,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Android System UI & Widget Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Android Top Status Bar with Privacy Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & Android Camera Privacy Dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "09:41",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E24)
                    )
                    if (isRecording) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00C853))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color.White.copy(alpha = recBlink), CircleShape)
                            )
                        }
                    }
                }

                // Battery / Signal Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        Text(
                            "REC",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = Color(0xFF1E1E24),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Center Area: "At a Glance" Weather + Authentic 1-Tap Home Widget
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Google "At a Glance" Date & Weather
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        "Mon, Oct 31  ·  22°C Sunny",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E24)
                    )
                }

                // Authentic 2x1 Home Screen Quick Record Widget
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isRecording) Color(0xFF102616) else Color(0xFF16161A)
                        )
                        .border(
                            1.dp,
                            if (isRecording) Color(0xFF00C853).copy(alpha = recBlink) else Color(0xFF2C2C34),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: 1-Tap Interactive Shutter Trigger Button with Tap Pulse
                        Box(contentAlignment = Alignment.Center) {
                            if (isTapping) {
                                val tapProgress = ((cycle - 0.34f) / 0.08f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .size((28 + 14 * tapProgress).dp)
                                        .background(
                                            Color.White.copy(alpha = 0.4f * (1f - tapProgress)),
                                            CircleShape
                                        )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecording) Color(0xFF00C853) else Color(0xFF262A36)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRecording) {
                                    // Stop square
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White)
                                    )
                                } else {
                                    // Record red circle
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Center: Title & Live Subtitle (Single line, no wrapping!)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isRecording) "Recording Active" else "Quick Record",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isRecording) Color(0xFF00E676) else Color(0xFFF5F5F7)
                            )
                            Spacer(modifier = Modifier.height(1.5.dp))
                            Text(
                                text = if (isRecording) "00:08 · Tap to stop" else "Tap to record",
                                fontSize = 7.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isRecording) Color(0xFF69F0AE) else Color(0xFF8E8E93)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right: Minimal Status Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isRecording) Color(0xFF00C853).copy(alpha = 0.2f) else Color(0xFF222228)
                                )
                                .border(
                                    0.5.dp,
                                    if (isRecording) Color(0xFF00C853).copy(alpha = 0.5f) else Color(0xFF33333E),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 4.5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isRecording) "LIVE" else "1-TAP",
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) Color(0xFF00E676) else Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            // Bottom Section: Frosted App Dock & Android Navigation Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Frosted Glass Dock Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Phone (Green)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                        }
                        // Messages (Blue)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1976D2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                        }
                        // Chrome (Google Multi-Color ring)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(9.dp).background(Color(0xFF039BE5), CircleShape))
                        }
                        // Camera (Dark Slate)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF263238)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Bottom Android Gesture Navigation Bar Pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E1E24).copy(alpha = 0.75f))
                )
            }
        }
    }
}

/**
 * Slide 3: Custom Camouflage Cover Screens Simulation (Clock & Calculator)
 */
@Composable
private fun CustomCovers3DLiveScene() {
    val transition = rememberInfiniteTransition(label = "custom_covers_3d")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "covers_cycle"
    )

    // Phase 1 (0 .. 0.50): Minimalist OLED Night Clock Cover
    // Phase 2 (0.50 .. 1.0): Fake Calculator Disguise Cover
    val isCalculator = cycle >= 0.50f

    val recDotBlink by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_blink"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (!isCalculator) {
            // Authentic OLED Night Clock Cover
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Cover status hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Clock Cover",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555566)
                    )
                    // Covert recording micro dot
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color(0xFF7C4DFF).copy(alpha = recDotBlink), CircleShape)
                    )
                }

                // Center OLED Clock
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "22:45",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp,
                        color = Color(0xFFD4D4E0)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Friday, Oct 31  ·  98%",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF555566)
                    )
                }

                // Bottom stealth gesture hint
                Text(
                    text = "Double-tap screen to stop",
                    fontSize = 6.5.sp,
                    color = Color(0xFF3A3A48),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        } else {
            // Authentic Fake Calculator Disguise Cover
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Calculator Cover label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calculator Cover",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB388FF)
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color(0xFF7C4DFF).copy(alpha = recDotBlink), CircleShape)
                    )
                }

                // Calculator Display Result
                Text(
                    text = "1,420",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 4.dp)
                )

                // 4x4 Calculator Keypad Simulation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val keyRows = listOf(
                        listOf("C" to Color(0xFF424248), "÷" to Color(0xFFFF9800), "×" to Color(0xFFFF9800), "⌫" to Color(0xFF424248)),
                        listOf("7" to Color(0xFF26262E), "8" to Color(0xFF26262E), "9" to Color(0xFF26262E), "-" to Color(0xFFFF9800)),
                        listOf("4" to Color(0xFF26262E), "5" to Color(0xFF26262E), "6" to Color(0xFF26262E), "+" to Color(0xFFFF9800)),
                        listOf("1" to Color(0xFF26262E), "2" to Color(0xFF26262E), "3" to Color(0xFF26262E), "=" to Color(0xFF7C4DFF))
                    )

                    for (row in keyRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for ((label, bg) in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

/**
 * Slide 4: Hardware Physical Buttons & Motion Shake Simulation
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
                        tint = Color(0xFFFFD54F),
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
                        .background(Color(0xFF382F10)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
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
                    color = Color(0xFFFFD54F)
                )
            }
        }
    }
}
