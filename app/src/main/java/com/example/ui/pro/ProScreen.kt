package com.example.ui.pro

import android.app.Activity
import com.example.util.findActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.RewardedAdManager
import com.example.billing.AppBillingManager
import com.example.billing.BillingUiState
import com.example.localization.AppStrings
import com.example.localization.LocalAppStrings
import com.example.localization.proLaunchSpecialBadge
import com.example.localization.proSingleSessionDrawerDesc
import com.example.localization.proSingleSessionDrawerTitle
import com.example.localization.proTrustNoSubs
import com.example.localization.proTrustOffline
import com.example.localization.proTrustSecure
import com.example.storage.EntitlementManager
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

val GoldAccent = Color(0xFFFFD54F)
val GoldAccentDark = Color(0xFFFFA000)
val GreenUnlocked = Color(0xFF4CAF50)

enum class UnlockableFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val unlockSuccessMsg: String
) {
    CUSTOM_COVERS(
        title = "CUSTOM COVERS",
        description = "Tailored visual disguise covers",
        icon = Icons.Default.Palette,
        unlockSuccessMsg = "1 Custom Cover free use added."
    ),
    CUSTOM_FOLDER(
        title = "CUSTOM FOLDER EXPORT",
        description = "Save copies to any chosen directory",
        icon = Icons.Default.Folder,
        unlockSuccessMsg = "1 Custom Folder Export free use added."
    ),
    AUTO_STOP(
        title = "AUTO STOP",
        description = "Automatically stop after set duration",
        icon = Icons.Default.Timer,
        unlockSuccessMsg = "1 Auto Stop free use added."
    ),
    VIDEO_4K(
        title = "4K ULTRA HD RECORDING",
        description = "Maximum resolution for supported cameras",
        icon = Icons.Default.Videocam,
        unlockSuccessMsg = "1 session of 4K Ultra HD added."
    )
}

fun AppStrings.getFeatureTitle(feature: UnlockableFeature): String = when (feature) {
    UnlockableFeature.CUSTOM_COVERS -> featureCustomCoversTitle
    UnlockableFeature.CUSTOM_FOLDER -> featureCustomFolderTitle
    UnlockableFeature.AUTO_STOP -> featureAutoStopTitle
    UnlockableFeature.VIDEO_4K -> featureVideo4kTitle
}

fun AppStrings.getFeatureDesc(feature: UnlockableFeature): String = when (feature) {
    UnlockableFeature.CUSTOM_COVERS -> featureCustomCoversDesc
    UnlockableFeature.CUSTOM_FOLDER -> featureCustomFolderDesc
    UnlockableFeature.AUTO_STOP -> featureAutoStopDesc
    UnlockableFeature.VIDEO_4K -> featureVideo4kDesc
}

fun AppStrings.getFeatureSuccessMsg(feature: UnlockableFeature): String = when (feature) {
    UnlockableFeature.CUSTOM_COVERS -> featureCustomCoversSuccess
    UnlockableFeature.CUSTOM_FOLDER -> featureCustomFolderSuccess
    UnlockableFeature.AUTO_STOP -> featureAutoStopSuccess
    UnlockableFeature.VIDEO_4K -> featureVideo4kSuccess
}

@Composable
fun ProScreen(
    onBack: () -> Unit,
    onNavigateToCovers: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToRecordings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
    val billingState by AppBillingManager.uiState.collectAsStateWithLifecycle()
    val isAdLoading by RewardedAdManager.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(billingState.userMessage) {
        val msg = billingState.userMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    var pendingFeatureToUnlock by remember { mutableStateOf<UnlockableFeature?>(null) }
    var showAdDrawer by remember { mutableStateOf(false) }

    // Multi-Layer Fluid Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pro_screen_motion")

    // Perimeter Border Sweep
    val borderSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_sweep"
    )

    // Ambient Aura Breathing Behind Emblem
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    // Halo Ring Pulse Around Emblem
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Launch Pass Badge Pulse
    val badgePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    // Crown Icon Micro-Pulse
    val crownPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_pulse"
    )

    // Primary CTA Button Light-Beam Gleam Sweep
    val ctaBeamOffset by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cta_beam_offset"
    )

    // Primary CTA Ambient Drop Glow Pulse
    val ctaGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_glow_alpha"
    )
    val ctaGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_glow_scale"
    )

    // Expandable Drawer Arrow Animation
    val drawerArrowRotation by animateFloatAsState(
        targetValue = if (showAdDrawer) 180f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "drawer_arrow"
    )

    val isPro = entitlements.isPro
    val heroBorderBrush = remember(isPro, borderSweep) {
        if (isPro) {
            Brush.linearGradient(
                colors = listOf(
                    GoldAccent.copy(alpha = 0.9f),
                    GoldAccentDark.copy(alpha = 0.35f),
                    GoldAccent.copy(alpha = 0.9f)
                ),
                start = Offset(borderSweep, 0f),
                end = Offset(borderSweep + 450f, 450f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    GoldAccent.copy(alpha = 0.8f),
                    Color(0xFF26262B),
                    GoldAccent.copy(alpha = 0.8f)
                ),
                start = Offset(borderSweep, 0f),
                end = Offset(borderSweep + 500f, 500f)
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("pro_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.cancel,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.proUpgradeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextPrimary
                )
            }

            // Animated Luxury Hero Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_purchase_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, heroBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Tier Special Launch Badge with Breathing Pulse
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = if (!isPro) badgePulse else 1f
                                scaleY = if (!isPro) badgePulse else 1f
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(GoldAccent.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                GoldAccent.copy(alpha = 0.45f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPro) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPro) strings.proLifetimeActive else "${strings.proLaunchSpecialBadge} · 50% OFF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.5.sp
                                ),
                                color = GoldAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Living Emblem with Animated Ambient Aura and Halo Ring
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Radial Glow Orb
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .graphicsLayer {
                                    scaleX = auraScale
                                    scaleY = auraScale
                                }
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            GoldAccent.copy(alpha = auraAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        // Pulsing Halo Ring
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    scaleX = haloScale
                                    scaleY = haloScale
                                }
                                .border(
                                    1.dp,
                                    GoldAccent.copy(alpha = haloAlpha),
                                    RoundedCornerShape(18.dp)
                                )
                        )

                        // Center Crown Emblem Container
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .graphicsLayer {
                                    scaleX = crownPulse
                                    scaleY = crownPulse
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            GoldAccent.copy(alpha = 0.24f),
                                            SurfaceSecondary
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    GoldAccent.copy(alpha = 0.5f),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.proHeroTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp
                        ),
                        color = if (isPro) GoldAccent else TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streamlined, Visual-First Feature List (Zero Paragraph Clutter)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceSecondary.copy(alpha = 0.45f))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CleanBenefitRow(
                                icon = Icons.Default.Block,
                                title = "100% Ad-Free Experience",
                                badgeText = "ZERO ADS",
                                isAlt = false
                            )
                            CleanBenefitRow(
                                icon = Icons.Default.Videocam,
                                title = "4K Ultra HD Recording",
                                badgeText = "ULTRA HD",
                                isAlt = true
                            )
                            CleanBenefitRow(
                                icon = Icons.Default.Palette,
                                title = "All Stealth Disguise Covers",
                                badgeText = "UNLIMITED",
                                isAlt = false
                            )
                            CleanBenefitRow(
                                icon = Icons.Default.Timer,
                                title = "Auto-Stop Duration Timer",
                                badgeText = "AUTO-STOP",
                                isAlt = true
                            )
                            CleanBenefitRow(
                                icon = Icons.Default.Folder,
                                title = "Private Vault & Custom Export",
                                badgeText = "PROTECTED",
                                isAlt = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Purchase State Section
                    if (isPro || billingState.isPro) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldAccent.copy(alpha = 0.12f))
                                .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.proActive,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = strings.proLifetimeAccess,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else if (billingState.isPurchasePending) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSecondary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.proPurchasePending,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.proPurchasePendingDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        // Pricing & Dynamic Living CTA Button
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val activePrice = billingState.formattedPrice ?: "PRO"
                            val regPrice = billingState.originalFormattedPrice ?: billingState.standardFormattedPrice
                            val discount = billingState.discountPercentage

                            if (regPrice != null && regPrice != activePrice && discount != null && discount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = regPrice,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 14.5.sp,
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        color = GoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activePrice,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = GoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = badgePulse
                                                scaleY = badgePulse
                                            }
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldAccent.copy(alpha = 0.15f))
                                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$discount% OFF",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = GoldAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            } else if (billingState.formattedPrice != null) {
                                Text(
                                    text = activePrice,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Dynamic Gleaming Primary CTA Button with Ambient Drop-Glow Aura
                            val ctaBrush = Brush.linearGradient(
                                colors = listOf(
                                    AccentRed,
                                    Color(0xFFFF5E5E),
                                    AccentRed
                                ),
                                start = Offset(ctaBeamOffset, 0f),
                                end = Offset(ctaBeamOffset + 240f, 160f)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing Red Ambient Drop Glow Behind CTA
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(42.dp)
                                        .graphicsLayer {
                                            scaleX = ctaGlowScale
                                            scaleY = ctaGlowScale
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AccentRed.copy(alpha = ctaGlowAlpha))
                                )

                                // Main CTA Surface
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ctaBrush)
                                        .clickable {
                                            val activity = context.findActivity()
                                            if (activity != null) {
                                                val initiated = AppBillingManager.launchPurchaseFlow(activity)
                                                if (!initiated) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Unable to initiate purchase. Please check store connection.")
                                                    }
                                                }
                                            } else {
                                                android.util.Log.e("ProScreen", "Cannot find enclosing Activity to launch purchase flow")
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Unable to connect to purchase window. Please restart the app.")
                                                }
                                            }
                                        }
                                        .testTag("buy_pro_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            modifier = Modifier.size(19.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = strings.proUnlockProCta.format(activePrice),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.6.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "One-time payment • Lifetime access",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Trust Badges Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrustBadge(icon = Icons.Default.Shield, label = strings.proTrustOffline)
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(BorderSubtle, CircleShape)
                                )
                                TrustBadge(icon = Icons.Default.AllInclusive, label = strings.proTrustNoSubs)
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(BorderSubtle, CircleShape)
                                )
                                TrustBadge(icon = Icons.Default.Lock, label = strings.proTrustSecure)
                            }

                            if (billingState.isConnecting) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSecondary)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.proConnecting,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (billingState.userMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = billingState.userMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = AccentRed,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = {
                                        AppBillingManager.startConnection {
                                            AppBillingManager.queryProductDetails()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = strings.proRetry,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AccentRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Restore Purchases Button
                    TextButton(
                        onClick = {
                            AppBillingManager.restorePurchases { _, message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        },
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        if (billingState.isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.proRestoringPurchases,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = strings.proRestorePurchases,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Plan Comparison Table Card
            PlanComparisonCard()

            // Anti-Cannibalization Collapsible Drawer for Single-Session Rewarded Ad Passes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showAdDrawer = !showAdDrawer },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceSecondary)
                                .border(0.5.dp, BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.proSingleSessionDrawerTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.proSingleSessionDrawerDesc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = drawerArrowRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdDrawer,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProFeatureCard(
                                feature = UnlockableFeature.CUSTOM_COVERS,
                                isPro = isPro,
                                credits = entitlements.customCoverCredits,
                                isAdLoading = isAdLoading,
                                onWatchAdClick = {
                                    pendingFeatureToUnlock = UnlockableFeature.CUSTOM_COVERS
                                },
                                onUseClick = onNavigateToCovers
                            )

                            ProFeatureCard(
                                feature = UnlockableFeature.CUSTOM_FOLDER,
                                isPro = isPro,
                                credits = entitlements.customFolderExportCredits,
                                isAdLoading = isAdLoading,
                                onWatchAdClick = {
                                    pendingFeatureToUnlock = UnlockableFeature.CUSTOM_FOLDER
                                },
                                onUseClick = onNavigateToSettings ?: onNavigateToRecordings
                            )

                            ProFeatureCard(
                                feature = UnlockableFeature.AUTO_STOP,
                                isPro = isPro,
                                credits = entitlements.autoStopCredits,
                                isAdLoading = isAdLoading,
                                onWatchAdClick = {
                                    pendingFeatureToUnlock = UnlockableFeature.AUTO_STOP
                                },
                                onUseClick = onNavigateToSettings
                            )

                            ProFeatureCard(
                                feature = UnlockableFeature.VIDEO_4K,
                                isPro = isPro,
                                credits = entitlements.video4kCredits,
                                isAdLoading = isAdLoading,
                                onWatchAdClick = {
                                    pendingFeatureToUnlock = UnlockableFeature.VIDEO_4K
                                },
                                onUseClick = onNavigateToSettings
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Rewarded Ad Dialog
    pendingFeatureToUnlock?.let { feature ->
        val featureTitle = strings.getFeatureTitle(feature)
        AlertDialog(
            onDismissRequest = { pendingFeatureToUnlock = null },
            title = {
                Text(
                    text = strings.proAdDialogTitle.format(featureTitle),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = strings.proAdDialogDesc.format(featureTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val featureToUnlock = feature
                        pendingFeatureToUnlock = null
                        val activity = context.findActivity()
                        if (activity != null) {
                            RewardedAdManager.showAd(
                                activity = activity,
                                onUserEarnedReward = { _ ->
                                    when (featureToUnlock) {
                                        UnlockableFeature.CUSTOM_COVERS -> EntitlementManager.addCustomCoverCredit()
                                        UnlockableFeature.CUSTOM_FOLDER -> EntitlementManager.addCustomFolderExportCredit()
                                        UnlockableFeature.AUTO_STOP -> EntitlementManager.addAutoStopCredit()
                                        UnlockableFeature.VIDEO_4K -> EntitlementManager.add4kCredit()
                                    }
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(strings.getFeatureSuccessMsg(featureToUnlock))
                                    }
                                },
                                onFailedToShow = { errorMsg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(errorMsg)
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("confirm_watch_ad_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text(
                        text = strings.proAdDialogConfirm,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFeatureToUnlock = null }) {
                    Text(
                        text = strings.cancel,
                        color = TextSecondary
                    )
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CleanBenefitRow(
    icon: ImageVector,
    title: String,
    badgeText: String,
    isAlt: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isAlt) SurfaceSecondary.copy(alpha = 0.45f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldAccent.copy(alpha = 0.14f))
                .border(0.5.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(GoldAccent.copy(alpha = 0.12f))
                .border(0.5.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = GoldAccent
            )
        }
    }
}

@Composable
private fun TrustBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun PlanComparisonCard(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.proFreeVsPro,
                    modifier = Modifier.weight(1.3f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = strings.proFree,
                    modifier = Modifier.weight(1.0f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = TextSecondary
                )
                Text(
                    text = strings.pro,
                    modifier = Modifier.weight(1.0f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = GoldAccent
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = BorderSubtle
            )

            ComparisonRow(
                icon = Icons.Default.Videocam,
                title = strings.proComparisonAudioVideo,
                freeContent = { IncludedBadge(false, strings.proBadgeIncluded) },
                proContent = { IncludedBadge(true, strings.proBadgeIncluded) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Tune,
                title = strings.proComparisonHardwareStop,
                freeContent = { IncludedBadge(false, strings.proBadgeIncluded) },
                proContent = { IncludedBadge(true, strings.proBadgeIncluded) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Shield,
                title = strings.proComparisonBasicFeatures,
                freeContent = { IncludedBadge(false, strings.proBadgeIncluded) },
                proContent = { IncludedBadge(true, strings.proBadgeIncluded) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Block,
                title = strings.proComparisonAds,
                freeContent = { TextBadge(strings.proBadgeNativeAds, isMuted = true) },
                proContent = { TextBadge(strings.proBadgeNone, isGold = true) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Videocam,
                title = strings.proComparisonVideoQuality,
                freeContent = { TextBadge(strings.proBadge1080p) },
                proContent = { TextBadge(strings.proBadge4k, isGold = true) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Palette,
                title = strings.proComparisonCustomCovers,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Folder,
                title = strings.proComparisonCustomFolder,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Timer,
                title = strings.proComparisonAutoStop,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = true
            )
        }
    }
}

@Composable
fun ComparisonRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    freeContent: @Composable () -> Unit,
    proContent: @Composable () -> Unit,
    isAltRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAltRow) SurfaceSecondary.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextPrimary
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(start = 19.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp
                    ),
                    color = TextMuted
                )
            }
        }

        Box(
            modifier = Modifier.weight(1.0f),
            contentAlignment = Alignment.Center
        ) {
            freeContent()
        }

        Box(
            modifier = Modifier.weight(1.0f),
            contentAlignment = Alignment.Center
        ) {
            proContent()
        }
    }
}

@Composable
fun IncludedBadge(isPro: Boolean, label: String = "Included") {
    val tintColor = if (isPro) GoldAccent else TextSecondary
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = label,
            modifier = Modifier.size(13.dp),
            tint = tintColor
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (isPro) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = tintColor
        )
    }
}

@Composable
fun UnlimitedBadge(label: String = "Unlimited") {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GoldAccent.copy(alpha = 0.15f))
            .border(0.5.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AllInclusive,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = GoldAccent
        )
    }
}

@Composable
fun TextBadge(
    text: String,
    isMuted: Boolean = false,
    isGold: Boolean = false
) {
    val textColor = when {
        isGold -> GoldAccent
        isMuted -> TextMuted
        else -> TextSecondary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 10.5.sp,
            fontWeight = if (isGold) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        color = textColor
    )
}

@Composable
private fun ProFeatureCard(
    feature: UnlockableFeature,
    isPro: Boolean,
    credits: Int,
    isAdLoading: Boolean,
    onWatchAdClick: () -> Unit,
    onUseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lowerCase = feature.name.lowercase(Locale.ROOT)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pro_feature_card_$lowerCase"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPro) GoldAccent.copy(alpha = 0.45f) else BorderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.0f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconBg = when {
                        isPro -> GoldAccent.copy(alpha = 0.15f)
                        credits > 0 -> GreenUnlocked.copy(alpha = 0.15f)
                        else -> SurfacePrimary
                    }
                    val iconBorder = when {
                        isPro -> GoldAccent.copy(alpha = 0.4f)
                        credits > 0 -> GreenUnlocked.copy(alpha = 0.4f)
                        else -> BorderSubtle
                    }
                    val iconTint = when {
                        isPro -> GoldAccent
                        credits > 0 -> GreenUnlocked
                        else -> TextSecondary
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(iconBg, CircleShape)
                            .border(0.5.dp, iconBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = strings.getFeatureTitle(feature),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.getFeatureDesc(feature),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Status Badge
                if (isPro) {
                    UnlimitedBadge(strings.proBadgeUnlimited)
                } else {
                    AnimatedContent(
                        targetState = credits,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "credit_counter_$lowerCase"
                    ) { targetCredits ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (targetCredits > 0) GreenUnlocked.copy(alpha = 0.15f) else SurfacePrimary)
                                .border(
                                    0.5.dp,
                                    if (targetCredits > 0) GreenUnlocked.copy(alpha = 0.4f) else BorderSubtle,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (targetCredits > 0) strings.proUsesCount.format(targetCredits) else strings.proZeroUses,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (targetCredits > 0) GreenUnlocked else TextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color = BorderSubtle
            )

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPro) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.proIncludedWithLifetime,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = GoldAccent
                        )
                    }
                } else if (credits > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GreenUnlocked,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.proReadyToUse,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = GreenUnlocked
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onUseClick != null) {
                            Button(
                                onClick = onUseClick,
                                modifier = Modifier.testTag("use_button_$lowerCase"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentRed,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = strings.proUse,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onWatchAdClick,
                            modifier = Modifier.testTag("watch_ad_button_$lowerCase"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfacePrimary,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isAdLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = strings.proWatchAd,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = strings.proWatchAdToGet1,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )

                    Button(
                        onClick = onWatchAdClick,
                        modifier = Modifier.testTag("watch_ad_button_$lowerCase"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfacePrimary,
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isAdLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = strings.proWatchAd,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
