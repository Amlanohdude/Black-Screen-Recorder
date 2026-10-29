package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import com.example.update.InAppUpdateManager
import com.example.update.UpdatePromptDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import com.example.covers.CoverConfig
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextPrimary
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.covers.CoverCustomizeDialog
import com.example.growth.InAppReviewManager
import com.example.recording.RecordingType
import com.example.ui.MainViewModel
import com.example.ui.covers.CoversTabScreen
import com.example.ui.record.RecordTabScreen
import com.example.ui.recorder.RecordingScreen
import com.example.ui.recorder.RecordingSummaryInfo
import com.example.ui.recorder.RecordingSummaryScreen
import com.example.ui.recordings.RecordingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ads.ConsentManager
import com.example.ads.NativeAdManager
import com.example.ads.RewardedAdManager
import com.example.billing.BillingManager
import com.example.storage.EntitlementManager
import com.example.ui.pro.ProScreen

enum class MainTab(val title: String, val icon: ImageVector, val tag: String) {
RECORD("Record", Icons.Default.Videocam, "tab_record"),
COVERS("Covers", Icons.Default.Palette, "tab_covers"),
RECORDINGS("Recordings", Icons.Default.VideoLibrary, "tab_recordings"),
SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {

private val viewModel: MainViewModel by viewModels()
private var lastVolumeDownPressTime: Long = 0L

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
enableEdgeToEdge()

// Initialize EntitlementManager from local storage
EntitlementManager.init(applicationContext)

// Initialize Google Play BillingManager
BillingManager.init(applicationContext)

// Gather consent in a non-blocking manner and initialize Mobile Ads
ConsentManager.gatherConsent(this) { canRequestAds ->
if (canRequestAds) {
NativeAdManager.initialize(applicationContext)
RewardedAdManager.loadAd(applicationContext)
}
}

// Non-blocking, 100% offline-safe in-app update check
InAppUpdateManager.checkForUpdateSilently(this)

setContent {
MyApplicationTheme {
var showSplash by remember { mutableStateOf(true) }
val settings by viewModel.userSettings.collectAsStateWithLifecycle()

val screenTarget = when {
showSplash -> "SPLASH"
!settings.onboardingCompleted -> "ONBOARDING"
else -> "MAIN"
}

AnimatedContent(
targetState = screenTarget,
transitionSpec = {
fadeIn(animationSpec = tween(400)) togetherWith
fadeOut(animationSpec = tween(350))
},
label = "root_navigation_transition"
) { screenState ->
when (screenState) {
"SPLASH" -> {
com.example.ui.splash.SplashScreen(
onSplashFinished = { showSplash = false }
)
}
"ONBOARDING" -> {
com.example.ui.onboarding.OnboardingScreen(
onFinished = {
viewModel.completeOnboarding()
}
)
}
else -> {
MainAppContent(viewModel = viewModel)
}
}
}
}
}
}

override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
val isRecording = viewModel.recordingStatus.value.isRecording
if (isRecording) {
val now = System.currentTimeMillis()
if (now - lastVolumeDownPressTime < 800L) {
lastVolumeDownPressTime = 0L
volumeDownExitTrigger.tryEmit(Unit)
} else {
lastVolumeDownPressTime = now
}
}
}
return super.onKeyDown(keyCode, event)
}

override fun onResume() {
super.onResume()
BillingManager.onResume()
InAppUpdateManager.onResume(this)
}

override fun onDestroy() {
super.onDestroy()
NativeAdManager.destroyAll()
InAppUpdateManager.onDestroy()
}

companion object {
val volumeDownExitTrigger = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
extraBufferCapacity = 1,
onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
)
}
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
val context = LocalContext.current
var selectedTab by remember { mutableStateOf(MainTab.RECORD) }
var activeRecordingType by remember { mutableStateOf<RecordingType?>(null) }
var completedSummary by remember { mutableStateOf<RecordingSummaryInfo?>(null) }
var isProScreenVisible by remember { mutableStateOf(false) }
val recordingStatus by viewModel.recordingStatus.collectAsStateWithLifecycle()
val isUpdateDownloaded by InAppUpdateManager.isUpdateDownloaded.collectAsStateWithLifecycle()
val isUpdatePromptVisible by InAppUpdateManager.isUpdatePromptVisible.collectAsStateWithLifecycle()
val coverToCustomize by viewModel.coverToCustomize.collectAsStateWithLifecycle()
var showExitConfirmDialog by remember { mutableStateOf(false) }
var showActivateCoverDialog by remember { mutableStateOf(false) }
var pendingCustomCoverToActivate by remember { mutableStateOf<CoverConfig?>(null) }

// Navigation Back Hierarchy: Return to RECORD tab when on other tabs
BackHandler(
enabled = selectedTab != MainTab.RECORD &&
activeRecordingType == null &&
completedSummary == null &&
coverToCustomize == null &&
!isProScreenVisible
) {
selectedTab = MainTab.RECORD
}

// On RECORD tab: Show exit confirmation dialog
BackHandler(
enabled = selectedTab == MainTab.RECORD &&
activeRecordingType == null &&
completedSummary == null &&
coverToCustomize == null &&
!isProScreenVisible &&
!showExitConfirmDialog
) {
showExitConfirmDialog = true
}

// Automatically navigate to active recording screen if service is running
LaunchedEffect(recordingStatus.isRecording, recordingStatus.type) {
if (recordingStatus.isRecording && recordingStatus.type != null) {
activeRecordingType = recordingStatus.type
}
}

// Full screen recording display without bottom bar
if (activeRecordingType != null) {
RecordingScreen(
type = activeRecordingType!!,
viewModel = viewModel,
onRecordingFinished = { summary ->
activeRecordingType = null
completedSummary = summary
},
onQuickExit = {
activeRecordingType = null
(context as? Activity)?.finish()
}
)
} else if (completedSummary != null) {
RecordingSummaryScreen(
summary = completedSummary!!,
viewModel = viewModel,
onDone = {
completedSummary = null
},
onNavigateToRecordings = {
    InAppReviewManager.onRecordingSavedSuccessfully(context as? Activity)
    completedSummary = null
    selectedTab = MainTab.RECORDINGS
}
)
} else if (coverToCustomize != null) {
CoverCustomizeDialog(
initialCover = coverToCustomize!!,
onDismiss = { viewModel.closeCoverDesigner() },
onSaveOnly = { newCover ->
viewModel.saveCustomCover(newCover)
android.widget.Toast.makeText(context, "Cover saved to My Custom Covers!", android.widget.Toast.LENGTH_SHORT).show()
viewModel.closeCoverDesigner()
},
onSaveAndApply = { newCover ->
viewModel.saveCustomCover(newCover)
if (EntitlementManager.isPro()) {
viewModel.setActiveCover(newCover)
android.widget.Toast.makeText(context, "Cover saved and applied!", android.widget.Toast.LENGTH_SHORT).show()
viewModel.closeCoverDesigner()
} else {
pendingCustomCoverToActivate = newCover
showActivateCoverDialog = true
}
},
                    onNavigateToPro = {
                        viewModel.closeCoverDesigner()
                        isProScreenVisible = true
                    }
                )
            } else if (isProScreenVisible) {
ProScreen(
onBack = { isProScreenVisible = false },
onNavigateToCovers = {
isProScreenVisible = false
selectedTab = MainTab.COVERS
},
onNavigateToSettings = {
isProScreenVisible = false
selectedTab = MainTab.SETTINGS
},
onNavigateToRecordings = {
isProScreenVisible = false
selectedTab = MainTab.RECORDINGS
}
)
} else {
Scaffold(
modifier = Modifier.fillMaxSize(),
bottomBar = {
Column {
if (isUpdateDownloaded && !recordingStatus.isRecording) {
UpdateDownloadedBanner(
onRestart = { InAppUpdateManager.completeUpdate() },
onDismiss = { InAppUpdateManager.dismissDownloadedBanner() }
)
}
CustomBottomNavigationBar(
currentTab = selectedTab,
onTabSelected = { selectedTab = it }
)
}
}
) { innerPadding ->
Box(
modifier = Modifier
.fillMaxSize()
.padding(innerPadding)
) {
AnimatedContent(
targetState = selectedTab,
transitionSpec = {
(fadeIn(animationSpec = tween(120, easing = FastOutSlowInEasing)))
.togetherWith(
fadeOut(animationSpec = tween(90, easing = FastOutSlowInEasing))
)
},
label = "tab_transition"
) { tab ->
when (tab) {
MainTab.RECORD -> RecordTabScreen(
viewModel = viewModel,
onStartVideo = { activeRecordingType = RecordingType.VIDEO },
onStartAudio = { activeRecordingType = RecordingType.AUDIO },
onNavigateToCovers = { selectedTab = MainTab.COVERS },
onNavigateToPro = { isProScreenVisible = true }
)

MainTab.COVERS -> CoversTabScreen(
viewModel = viewModel,
onNavigateToPro = { isProScreenVisible = true }
)

MainTab.RECORDINGS -> RecordingsScreen(
viewModel = viewModel,
onNavigateToRecord = { selectedTab = MainTab.RECORD }
)

MainTab.SETTINGS -> SettingsScreen(
viewModel = viewModel,
onNavigateToCovers = { selectedTab = MainTab.COVERS },
onNavigateToPro = { isProScreenVisible = true }
)
}
}
}
}
}

        if (showActivateCoverDialog && pendingCustomCoverToActivate != null) {
            val cover = pendingCustomCoverToActivate!!
            val activity = context as? Activity
            AlertDialog(
                onDismissRequest = {
                    showActivateCoverDialog = false
                    pendingCustomCoverToActivate = null
                    viewModel.closeCoverDesigner()
                },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activate Custom Cover",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = "Your cover \"${cover.name}\" is saved! Watch a short ad to make it your active cover now, or upgrade to PRO.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showActivateCoverDialog = false
                            viewModel.closeCoverDesigner()
                            if (activity != null) {
                                RewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        viewModel.setActiveCover(cover)
                                        android.widget.Toast.makeText(context, "\"${cover.name}\" activated!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            pendingCustomCoverToActivate = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Ad & Activate", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                showActivateCoverDialog = false
                                pendingCustomCoverToActivate = null
                                viewModel.closeCoverDesigner()
                                android.widget.Toast.makeText(context, "Saved to My Custom Covers!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Save Only", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                showActivateCoverDialog = false
                                pendingCustomCoverToActivate = null
                                viewModel.closeCoverDesigner()
                                isProScreenVisible = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go PRO", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            )
        }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Exit Black Screen Recorder?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (recordingStatus.isRecording) {
                        "A recording is currently in progress. Exiting the app will keep the background recording active in your notifications."
                    } else {
                        "Are you sure you want to exit the app?"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("exit_confirm_button")
                ) {
                    Text(
                        text = "Exit",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmDialog = false },
                    modifier = Modifier.testTag("exit_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    if (isUpdatePromptVisible) {
        UpdatePromptDialog(
            versionName = "1.0.2",
            onUpdateClick = {
                (context as? Activity)?.let { InAppUpdateManager.triggerUpdateFlow(it) }
            },
            onDismiss = {
                InAppUpdateManager.showUpdatePrompt(false)
            }
        )
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth().testTag("main_bottom_nav"),
        color = Color(0xFF000000)
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.values().forEach { tab ->
                    CustomBottomNavItem(
                        tab = tab,
                        isSelected = currentTab == tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected(tab)
                        },
                        modifier = Modifier.weight(1f).testTag(tab.tag)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomBottomNavItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "nav_scale_${tab.name}"
    )
    val iconOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-1.5).dp else 0.dp,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "nav_offset_${tab.name}"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AccentRed else Color(0xFF8A8A8A),
        animationSpec = tween(150),
        label = "nav_icon_color_${tab.name}"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF757575),
        animationSpec = tween(150),
        label = "nav_label_color_${tab.name}"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "nav_dot_alpha_${tab.name}"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.3f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "nav_dot_scale_${tab.name}"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    translationY = with(density) { iconOffsetY.toPx() }
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.title,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer {
                    alpha = dotAlpha
                    scaleX = dotScale
                    scaleY = dotScale
                }
                .clip(CircleShape)
                .background(AccentRed)
        )
    }
}

@Composable
fun UpdateDownloadedBanner(
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        color = SurfaceSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Update downloaded",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Restart app to complete update",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Restart", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
