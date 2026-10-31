package com.example.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val badgeLabel: String
)

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
                icon = Icons.Default.Videocam,
                title = strings.onboarding.title1,
                description = strings.onboarding.desc1,
                accentColor = AccentRed,
                secondaryColor = Color(0xFFFF5252),
                badgeLabel = "DISCREET RECORDING"
            ),
            OnboardingPageData(
                icon = Icons.Default.Tune,
                title = strings.onboarding.titleNonTouch,
                description = strings.onboarding.descNonTouch,
                accentColor = Color(0xFF388E3C),
                secondaryColor = Color(0xFF81C784),
                badgeLabel = "HARDWARE GESTURES"
            ),
            OnboardingPageData(
                icon = Icons.Default.DarkMode,
                title = strings.onboarding.title2,
                description = strings.onboarding.desc2,
                accentColor = Color(0xFF00B0FF),
                secondaryColor = Color(0xFF40C4FF),
                badgeLabel = "OLED EFFICIENCY"
            ),
            OnboardingPageData(
                icon = Icons.Default.Lock,
                title = strings.onboarding.title3,
                description = strings.onboarding.desc3,
                accentColor = Color(0xFFFFD54F),
                secondaryColor = Color(0xFFFFA000),
                badgeLabel = "PRIVATE VAULT"
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })

    // Living Ambient Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_motion")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding_aura_scale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding_aura_alpha"
    )
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with Discreet Skip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Accent Pill
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

                // Skip Button
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

            Spacer(modifier = Modifier.height(12.dp))

            // Main Swipeable Horizontal Pager
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
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Tag Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(pageData.accentColor.copy(alpha = 0.12f))
                            .border(0.8.dp, pageData.accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
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

                    Spacer(modifier = Modifier.height(28.dp))

                    // Living Visual Hero Graphic with Aura Ring
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Breathing Aura
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .graphicsLayer {
                                    scaleX = auraScale
                                    scaleY = auraScale
                                }
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            pageData.accentColor.copy(alpha = auraAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        // Outer Concentric Glow Ring
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(38.dp))
                                .background(SurfaceSecondary.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    pageData.accentColor.copy(alpha = 0.35f),
                                    RoundedCornerShape(38.dp)
                                )
                        )

                        // Center Emblem Box
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            pageData.accentColor.copy(alpha = 0.28f),
                                            SurfacePrimary
                                        )
                                    )
                                )
                                .border(
                                    1.2.dp,
                                    pageData.accentColor.copy(alpha = 0.6f),
                                    RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = pageData.icon,
                                contentDescription = null,
                                tint = pageData.accentColor,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Slide Title
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slide Description
                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(28.dp))

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

            // Legal Transparency & Compliance Links (Visible on final slide)
            AnimatedVisibility(
                visible = isLastPage,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
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
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
