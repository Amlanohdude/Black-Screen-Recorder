package com.example.ui.pro

import android.app.Activity
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.RewardedAdManager
import com.example.billing.BillingManager
import com.example.billing.BillingUiState
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

private val GoldAccent = Color(0xFFFFD54F)
private val GoldAccentDark = Color(0xFFFFA000)
private val GreenUnlocked = Color(0xFF4CAF50)

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
unlockSuccessMsg = "1 Custom Cover use added."
),
CUSTOM_FOLDER(
title = "CUSTOM FOLDER EXPORT",
description = "Save copies to any chosen directory",
icon = Icons.Default.Folder,
unlockSuccessMsg = "1 Custom Folder Export use added."
),
AUTO_STOP(
title = "AUTO STOP",
description = "Automatically stop after set duration",
icon = Icons.Default.Timer,
unlockSuccessMsg = "1 Auto Stop use added."
),
NO_ADS(
title = "NO ADS",
description = "100% ad-free experience across the app",
icon = Icons.Default.Block,
unlockSuccessMsg = ""
)
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
    val billingState by BillingManager.uiState.collectAsStateWithLifecycle()

    val isPro = entitlements.isPro
    val heroBorderBrush = remember(isPro) {
        if (isPro) {
            Brush.linearGradient(listOf(GoldAccent, GoldAccentDark))
        } else {
            Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF222222)))
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
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UPGRADE TO PRO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextPrimary
                )
            }

// Animated Hero Banner Card
Card(
modifier = Modifier.fillMaxWidth(),
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
.background(if (entitlements.isPro) GoldAccent.copy(alpha = 0.15f) else SurfaceSecondary)
.border(
1.dp,
if (entitlements.isPro) GoldAccent.copy(alpha = 0.4f) else BorderSubtle,
RoundedCornerShape(20.dp)
)
.padding(horizontal = 14.dp, vertical = 5.dp)
) {
Row(verticalAlignment = Alignment.CenterVertically) {
Icon(
imageVector = if (entitlements.isPro) Icons.Default.CheckCircle else Icons.Default.ConfirmationNumber,
contentDescription = null,
tint = if (entitlements.isPro) GoldAccent else AccentRed,
modifier = Modifier.size(13.dp)
)
Spacer(modifier = Modifier.width(6.dp))
Text(
text = if (entitlements.isPro) "PRO LIFETIME ACTIVE" else "FREE TIER · PASS WALLET",
style = MaterialTheme.typography.labelSmall.copy(
fontWeight = FontWeight.Bold,
letterSpacing = 0.8.sp,
fontSize = 10.5.sp
),
color = if (entitlements.isPro) GoldAccent else TextSecondary
)
}
}

Spacer(modifier = Modifier.height(14.dp))

// Emblem
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(if (entitlements.isPro) GoldAccent.copy(alpha = 0.15f) else SurfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (entitlements.isPro) GoldAccent else TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

Spacer(modifier = Modifier.height(10.dp))

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
text = "Restoring Purchases...",
style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
color = TextSecondary
)
} else {
Text(
text = "Restore Purchases",
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
}
}
}
