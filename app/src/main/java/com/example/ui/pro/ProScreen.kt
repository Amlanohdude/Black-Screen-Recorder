package com.example.ui.pro

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.billing.BillingManager
import com.example.billing.BillingUiState
import com.example.localization.LocalAppStrings
import com.example.localization.AppStrings
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
    val billingState by BillingManager.uiState.collectAsStateWithLifecycle()
    val isAdLoading by RewardedAdManager.isLoading.collectAsStateWithLifecycle()

    var pendingFeatureToUnlock by remember { mutableStateOf<UnlockableFeature?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pro_screen_shimmer")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_shimmer"
    )
    val crownPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_pulse"
    )

    val isPro = entitlements.isPro
    val heroBorderBrush = remember(isPro, borderAlpha) {
        if (isPro) {
            Brush.linearGradient(
                listOf(
                    GoldAccent.copy(alpha = borderAlpha),
                    GoldAccentDark.copy(alpha = borderAlpha * 0.7f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    GoldAccent.copy(alpha = borderAlpha * 0.6f),
                    Color(0xFF333333)
                )
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
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

            // Animated Hero Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_purchase_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, heroBorderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Tier Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isPro) GoldAccent.copy(alpha = 0.15f) else SurfaceSecondary)
                            .border(
                                1.dp,
                                if (isPro) GoldAccent.copy(alpha = 0.4f) else BorderSubtle,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPro) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPro) GoldAccent else AccentRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPro) strings.proLifetimeActive else strings.proLifetimeUpgrade,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isPro) GoldAccent else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Emblem
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                scaleX = crownPulse
                                scaleY = crownPulse
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isPro) GoldAccent.copy(alpha = 0.18f) else SurfaceSecondary)
                            .border(
                                1.dp,
                                if (isPro) GoldAccent.copy(alpha = 0.45f) else BorderSubtle,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isPro) GoldAccent else AccentRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.proHeroTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (isPro) GoldAccent else TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = strings.proHeroSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Features Column
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSecondary.copy(alpha = 0.6f))
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                strings.proFeatureAdFree,
                                strings.proFeatureVideo4k,
                                strings.proFeatureCovers,
                                strings.proFeatureFolder,
                                strings.proFeatureAutoStop
                            ).forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Purchase State Section
                    if (isPro || billingState.isPro) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldAccent.copy(alpha = 0.12f))
                                .border(1.dp, GoldAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
                            shape = RoundedCornerShape(12.dp),
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
                        // Pricing & Buy Button
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
                                            fontSize = 14.sp,
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
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = GoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldAccent.copy(alpha = 0.15f))
                                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
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

                            Button(
                                onClick = {
                                    val activity = context as? Activity
                                    if (activity != null) {
                                        BillingManager.launchPurchaseFlow(activity)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("buy_pro_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.proUnlockProCta.format(activePrice),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = strings.proLifetimeOneTime,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

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
                                        BillingManager.startConnection {
                                            BillingManager.queryProductDetails()
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Restore Purchases Button
                    TextButton(
                        onClick = {
                            BillingManager.restorePurchases { _, message ->
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
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = strings.proRestorePurchases,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
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

            // Free Uses Card Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = strings.proFreeUsesTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.proFreeUsesSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }

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
                        val activity = context as? Activity
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
                icon = Icons.Default.Shield,
                title = strings.proComparisonBasicFeatures,
                freeContent = { IncludedBadge(false, strings.proBadgeIncluded) },
                proContent = { IncludedBadge(true, strings.proBadgeIncluded) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Block,
                title = strings.proComparisonAds,
                freeContent = { TextBadge(strings.proBadgeNativeAds, isMuted = true) },
                proContent = { TextBadge(strings.proBadgeNone, isGold = true) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Videocam,
                title = strings.proComparisonVideoQuality,
                freeContent = { TextBadge(strings.proBadge1080p) },
                proContent = { TextBadge(strings.proBadge4k, isGold = true) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Palette,
                title = strings.proComparisonCustomCovers,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = false
            )

            ComparisonRow(
                icon = Icons.Default.Folder,
                title = strings.proComparisonCustomFolder,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = true
            )

            ComparisonRow(
                icon = Icons.Default.Timer,
                title = strings.proComparisonAutoStop,
                freeContent = { TextBadge(strings.proBadge1Use) },
                proContent = { UnlimitedBadge(strings.proBadgeUnlimited) },
                isAltRow = false
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
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
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
                        else -> SurfaceSecondary
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
                                .background(if (targetCredits > 0) GreenUnlocked.copy(alpha = 0.15f) else SurfaceSecondary)
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
                                containerColor = SurfaceSecondary,
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
                            containerColor = SurfaceSecondary,
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
