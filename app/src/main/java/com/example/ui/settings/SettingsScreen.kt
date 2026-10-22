package com.example.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ads.AdScreen
import com.example.ads.AdsConfig
import com.example.ads.NativeAdCard
import com.example.ads.NativeAdStyle
import com.example.ads.RewardedAdManager
import com.example.config.AppLinks
import com.example.covers.ControlGesture
import com.example.presets.RecordingPresetId
import com.example.storage.EntitlementManager
import com.example.storage.ExitGesture
import com.example.storage.ExitMethod
import com.example.storage.RecordingExportManager
import com.example.storage.SettingsPreferences
import com.example.storage.StopBehavior
import com.example.storage.StorageCapacityEstimator
import com.example.ui.MainViewModel
import com.example.ui.components.TabTopHeader
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToCovers: () -> Unit = {},
    onNavigateToPro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    var showCameraDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showAutoStopDialog by remember { mutableStateOf(false) }
    var showCustomAutoStopDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }
    var customSecondsInput by remember { mutableStateOf("") }
    var customInputError by remember { mutableStateOf<String?>(null) }
    var showGestureDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDaysDialog by remember { mutableStateOf(false) }
    var showStopBehaviorDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isChangingExistingPin by remember { mutableStateOf(false) }
    var pinDialogStep by remember { mutableIntStateOf(1) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinDialogBiometricOption by remember { mutableStateOf(false) }
    var pinChangeError by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStorageLocationDialog by remember { mutableStateOf(false) }
    var showAutoStopLockedDialog by remember { mutableStateOf(false) }
    var showExitMethodDialog by remember { mutableStateOf(false) }
    var showExitGestureDialog by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }
    var lastDevTapTime by remember { mutableLongStateOf(0L) }
    var showDevQaDialog by remember { mutableStateOf(false) }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            val folderName = RecordingExportManager.getFolderDisplayName(context, uri)
            viewModel.updateCustomExportFolder(uri.toString(), folderName)
            Toast.makeText(context, "Custom export folder selected.", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }
    val estimatedCapacitySeconds = remember(storageStats.availableBytes, settings.defaultRecordingType, settings.videoQuality) {
        StorageCapacityEstimator.getEstimatedRemainingSeconds(
            availableBytes = storageStats.availableBytes,
            recordingType = settings.defaultRecordingType,
            videoQuality = settings.videoQuality
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TabTopHeader(title = "SETTINGS")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dedicated PRO Entry Card at the very top of Settings
                ProSettingsCard(onNavigateToPro = onNavigateToPro)

                // Section 1: RECORDING & CONTROLS
                SettingsGroup(title = "RECORDING & CONTROLS") {
                    SettingsClickableRow(
                        title = "Active preset",
                        value = settings.activePreset.title,
                        onClick = { showPresetDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Camera",
                        value = if (settings.useFrontCamera) "Front" else "Back",
                        onClick = { showCameraDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Video quality",
                        value = settings.videoQuality,
                        onClick = { showQualityDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Audio format",
                        value = "M4A (AAC)",
                        onClick = { showAudioDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    val autoStopText = SettingsPreferences.formatAutoStopDuration(settings.autoStopSeconds)
                    SettingsClickableRow(
                        title = "Auto stop",
                        value = autoStopText,
                        onClick = {
                            if (!EntitlementManager.canUseAutoStop()) {
                                showAutoStopLockedDialog = true
                            } else {
                                showAutoStopDialog = true
                            }
                        }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Stop behavior",
                        value = settings.stopBehavior.displayName,
                        onClick = { showStopBehaviorDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "Background Recording",
                        checked = settings.backgroundRecordingEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateBackgroundRecordingEnabled(enabled)
                        }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "Exit App After Recording",
                        checked = settings.exitAfterRecordingEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateExitAfterRecordingEnabled(enabled)
                        }
                    )

                    if (settings.exitAfterRecordingEnabled) {
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                        SettingsClickableRow(
                            title = "Exit Method",
                            value = settings.exitMethod.title,
                            onClick = { showExitMethodDialog = true }
                        )

                        if (settings.exitMethod == ExitMethod.GESTURE) {
                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                            SettingsClickableRow(
                                title = "Exit Gesture",
                                value = settings.exitGesture.shortSubtitle,
                                onClick = { showExitGestureDialog = true }
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Reveal controls",
                        value = when (settings.activeCover.controlGesture) {
                            ControlGesture.SINGLE_TAP -> "Single Tap"
                            ControlGesture.DOUBLE_TAP -> "Double Tap"
                            ControlGesture.LONG_PRESS -> "Long Press"
                        },
                        onClick = { showGestureDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "Show recording timer",
                        checked = settings.activeCover.showRecordingTimer,
                        onCheckedChange = { enabled ->
                            viewModel.setActiveCover(settings.activeCover.copy(showRecordingTimer = enabled))
                        }
                    )
                }

                // Section 2: DISPLAY & COVER
                SettingsGroup(title = "DISPLAY & COVER") {
                    SettingsClickableRow(
                        title = "Default cover",
                        value = settings.activeCover.name,
                        onClick = onNavigateToCovers
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Recording brightness row with inline Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recording brightness",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = if (settings.activeCover.brightnessPercent <= 1) "Minimum" else "${settings.activeCover.brightnessPercent}%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = settings.activeCover.brightnessPercent.toFloat(),
                            onValueChange = { percent ->
                                viewModel.setActiveCover(settings.activeCover.copy(brightnessPercent = percent.toInt()))
                            },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentRed,
                                activeTrackColor = AccentRed,
                                inactiveTrackColor = SurfaceSecondary
                            )
                        )
                    }
                }

                // Section 3: STORAGE & DATA
                SettingsGroup(title = "STORAGE & DATA") {
                    // Storage used
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage used",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = MainViewModel.formatFileSize(storageStats.usedBytes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Available storage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = MainViewModel.formatFileSize(storageStats.availableBytes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Estimated recording time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated time left",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = StorageCapacityEstimator.formatEstimatedDuration(estimatedCapacitySeconds),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = AccentRed
                        )
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Private Storage
                    SettingsClickableRow(
                        title = "Private Storage",
                        value = "Default",
                        subtitle = "Recordings are stored securely inside the app.",
                        onClick = { showStorageLocationDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Export to Custom Folder
                    val hasCustomFolder = settings.hasCustomExportFolder
                    val customFolderName = settings.customExportFolderName
                    SettingsClickableRow(
                        title = "Export to Custom Folder",
                        value = if (hasCustomFolder) "Change Folder" else "Not configured",
                        subtitle = if (hasCustomFolder) "Accessible copies saved to: ${customFolderName ?: "Configured folder"}" else "Choose a folder to save accessible copies.",
                        onClick = { openFolderLauncher.launch(null) }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Auto delete toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto delete",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.autoDeleteEnabled) {
                                    "Deletes recordings older than ${settings.autoDeleteDays} days"
                                } else {
                                    "Off (Keep indefinitely)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = settings.autoDeleteEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateAutoDeleteEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSecondary
                            ),
                            modifier = Modifier.testTag("auto_delete_option")
                        )
                    }

                    if (settings.autoDeleteEnabled) {
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                        SettingsClickableRow(
                            title = "Delete after",
                            value = "${settings.autoDeleteDays} days",
                            onClick = { showAutoDeleteDaysDialog = true }
                        )
                    }
                }

                // Section 4: PRIVACY & SECURITY
                SettingsGroup(title = "PRIVACY & SECURITY") {
                    SettingsSwitchRow(
                        title = "App PIN lock",
                        checked = settings.lockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (settings.hasPinSet) {
                                    viewModel.updateLockEnabled(true)
                                } else {
                                    isChangingExistingPin = false
                                    pinDialogStep = 1
                                    currentPinInput = ""
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinChangeError = null
                                    showPinDialog = true
                                }
                            } else {
                                viewModel.updateLockEnabled(false)
                            }
                        },
                        subtitle = if (settings.lockEnabled) "App requires PIN to open vault" else "Off (Open access)"
                    )

                    if (settings.lockEnabled) {
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                        SettingsClickableRow(
                            title = "Change PIN",
                            value = "****",
                            onClick = {
                                isChangingExistingPin = true
                                pinDialogStep = 1
                                currentPinInput = ""
                                newPinInput = ""
                                confirmPinInput = ""
                                pinChangeError = null
                                showPinDialog = true
                            }
                        )

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                        SettingsSwitchRow(
                            title = "Biometric unlock",
                            checked = settings.biometricEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateBiometricEnabled(enabled)
                            },
                            subtitle = "Use fingerprint or face unlock"
                        )
                    }
                }

                // Native Ad (Lower Settings, before APP section)
                if (AdsConfig.showInSettings) {
                    NativeAdCard(
                        screen = AdScreen.SETTINGS,
                        style = NativeAdStyle.COMPACT,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Section 5: ABOUT & SUPPORT
                SettingsGroup(title = "ABOUT & SUPPORT") {
                    SettingsClickableRow(
                        title = "Share App",
                        subtitle = "Recommend Black Screen Recorder to friends",
                        value = "Share",
                        onClick = {
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, AppLinks.SHARE_APP_TEXT)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Black Screen Recorder")
                                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(shareIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to open share menu", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Rate App",
                        subtitle = "Help us improve on Google Play Store",
                        value = "Rate",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PLAY_STORE_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PLAY_STORE_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            }
                        }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    SettingsClickableRow(
                        title = "Privacy policy",
                        value = "Read",
                        onClick = { showPrivacyDialog = true }
                    )

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Version Row
                    SettingsClickableRow(
                        title = "Version",
                        value = "1.0.0",
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastDevTapTime > 2500L) {
                                devTapCount = 1
                            } else {
                                devTapCount++
                            }
                            lastDevTapTime = currentTime

                            if (devTapCount >= 5) {
                                devTapCount = 0
                                Toast.makeText(context, "Developer mode activated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Dialogs
    if (showCameraDialog) {
        SelectionDialog(
            title = "Camera",
            options = listOf(
                false to "Back Camera (Default)",
                true to "Front Camera (Selfie)"
            ),
            selected = settings.useFrontCamera,
            onSelect = { viewModel.updateCamera(it) },
            onDismiss = { showCameraDialog = false }
        )
    }

    if (showQualityDialog) {
        SelectionDialog(
            title = "Video quality",
            options = listOf(
                "1080p" to "1080p (FHD - High quality)",
                "720p" to "720p (HD - Standard)",
                "480p" to "480p (SD - Smaller file)"
            ),
            selected = settings.videoQuality,
            onSelect = { viewModel.updateVideoQuality(it) },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showAudioDialog) {
        SelectionDialog(
            title = "Audio format",
            options = listOf(
                "m4a" to "M4A (AAC - Standard)",
                "wav" to "WAV (Uncompressed)"
            ),
            selected = settings.audioFormat,
            onSelect = { viewModel.updateAudioFormat(it) },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showAutoStopDialog) {
        val options = listOf(
            0 to "Disabled",
            60 to "1 minute",
            300 to "5 minutes",
            600 to "10 minutes",
            1800 to "30 minutes",
            3600 to "1 hour",
            -1 to "Custom duration..."
        )
        AlertDialog(
            onDismissRequest = { showAutoStopDialog = false },
            title = {
                Text(
                    text = "Auto stop recording",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (seconds, label) ->
                        val isSelected = (seconds == settings.autoStopSeconds)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (seconds == -1) {
                                        showAutoStopDialog = false
                                        showCustomAutoStopDialog = true
                                    } else {
                                        viewModel.updateAutoStopSeconds(seconds)
                                        showAutoStopDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) AccentRed else TextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoStopDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showCustomAutoStopDialog) {
        AlertDialog(
            onDismissRequest = { showCustomAutoStopDialog = false },
            title = {
                Text(
                    text = "Custom Duration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set recording duration before automatic stop (5s to 300 min):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = {
                                customMinutesInput = it.filter { ch -> ch.isDigit() }
                                customInputError = null
                            },
                            placeholder = { Text("0", color = TextMuted) },
                            trailingIcon = {
                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = customInputError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customSecondsInput,
                            onValueChange = {
                                val filtered = it.filter { ch -> ch.isDigit() }
                                val num = filtered.toIntOrNull() ?: 0
                                if (filtered.length <= 2 && num <= 59) {
                                    customSecondsInput = filtered
                                } else if (filtered.length <= 2) {
                                    customSecondsInput = "59"
                                }
                                customInputError = null
                            },
                            placeholder = { Text("0", color = TextMuted) },
                            trailingIcon = {
                                Text(
                                    text = "sec",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = customInputError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (customInputError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = customInputError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = AccentRed, fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = customMinutesInput.toIntOrNull() ?: 0
                        val secs = customSecondsInput.toIntOrNull() ?: 0
                        val totalSeconds = (mins * 60) + secs
                        when {
                            totalSeconds < 5 -> {
                                customInputError = "Minimum duration is 5 seconds"
                            }
                            totalSeconds > 18000 -> {
                                customInputError = "Maximum duration is 300 minutes (5 hours)"
                            }
                            else -> {
                                viewModel.updateAutoStopSeconds(totalSeconds)
                                showCustomAutoStopDialog = false
                            }
                        }
                    }
                ) {
                    Text("Apply", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAutoStopDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showAutoStopLockedDialog) {
        val activity = context as? Activity
        AlertDialog(
            onDismissRequest = { showAutoStopLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AccentRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Auto Stop is PRO", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
            },
            text = {
                Text(
                    text = "Upgrade to PRO for unlimited auto stop timer, or watch a short sponsored ad to unlock 1 use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoStopLockedDialog = false
                        onNavigateToPro()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go PRO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAutoStopLockedDialog = false
                        if (activity != null) {
                            RewardedAdManager.showAd(
                                activity = activity,
                                onUserEarnedReward = {
                                    EntitlementManager.addAutoStopCredit()
                                    showAutoStopDialog = true
                                }
                            )
                        }
                    }
                ) {
                    Text("Watch Ad (1 Use)", color = TextSecondary)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showStopBehaviorDialog) {
        SelectionDialog(
            title = "Stop behavior",
            options = StopBehavior.values().map { it to it.displayName },
            selected = settings.stopBehavior,
            onSelect = { viewModel.updateStopBehavior(it) },
            onDismiss = { showStopBehaviorDialog = false }
        )
    }

    if (showExitMethodDialog) {
        SelectionDialog(
            title = "Exit Method",
            options = ExitMethod.values().map { it to it.title },
            selected = settings.exitMethod,
            onSelect = { viewModel.updateExitMethod(it) },
            onDismiss = { showExitMethodDialog = false }
        )
    }

    if (showExitGestureDialog) {
        SelectionDialog(
            title = "Exit Gesture",
            options = ExitGesture.values().map { it to it.shortSubtitle },
            selected = settings.exitGesture,
            onSelect = { viewModel.updateExitGesture(it) },
            onDismiss = { showExitGestureDialog = false }
        )
    }

    if (showGestureDialog) {
        SelectionDialog(
            title = "Reveal controls gesture",
            options = listOf(
                ControlGesture.SINGLE_TAP to "Single Tap",
                ControlGesture.DOUBLE_TAP to "Double Tap",
                ControlGesture.LONG_PRESS to "Long Press"
            ),
            selected = settings.activeCover.controlGesture,
            onSelect = { gesture ->
                viewModel.setActiveCover(settings.activeCover.copy(controlGesture = gesture))
            },
            onDismiss = { showGestureDialog = false }
        )
    }

    if (showStorageLocationDialog) {
        AlertDialog(
            onDismissRequest = { showStorageLocationDialog = false },
            title = {
                Text(text = "Storage Location", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Recordings are stored securely inside Black Screen Recorder's isolated private sandbox.\n\nOther apps cannot access these files directly unless you choose to export them to a custom folder.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageLocationDialog = false }) {
                    Text("OK", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showAutoDeleteDaysDialog) {
        SelectionDialog(
            title = "Delete recordings older than",
            options = listOf(
                1 to "1 day",
                3 to "3 days",
                7 to "7 days",
                14 to "14 days",
                30 to "30 days"
            ),
            selected = settings.autoDeleteDays,
            onSelect = { viewModel.updateAutoDeleteDays(it) },
            onDismiss = { showAutoDeleteDaysDialog = false }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                currentPinInput = ""
                newPinInput = ""
                confirmPinInput = ""
                pinChangeError = null
                pinDialogStep = 1
            },
            title = {
                Text(
                    text = if (!isChangingExistingPin) "Set App PIN" else "Change PIN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val promptText = when {
                        isChangingExistingPin && pinDialogStep == 1 -> "Enter your current 4-digit PIN"
                        (!isChangingExistingPin && pinDialogStep == 1) || (isChangingExistingPin && pinDialogStep == 2) -> "Enter a new 4-digit PIN"
                        else -> "Confirm your new 4-digit PIN"
                    }
                    Text(text = promptText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    val currentVal = when {
                        isChangingExistingPin && pinDialogStep == 1 -> currentPinInput
                        (!isChangingExistingPin && pinDialogStep == 1) || (isChangingExistingPin && pinDialogStep == 2) -> newPinInput
                        else -> confirmPinInput
                    }

                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                when {
                                    isChangingExistingPin && pinDialogStep == 1 -> currentPinInput = input
                                    (!isChangingExistingPin && pinDialogStep == 1) || (isChangingExistingPin && pinDialogStep == 2) -> newPinInput = input
                                    else -> confirmPinInput = input
                                }
                                pinChangeError = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinChangeError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinChangeError != null) {
                        Text(
                            text = pinChangeError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = AccentRed, fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isChangingExistingPin && pinDialogStep == 1) {
                            if (viewModel.verifyPin(currentPinInput)) {
                                pinDialogStep = 2
                                pinChangeError = null
                            } else {
                                pinChangeError = "Incorrect current PIN"
                            }
                        } else if ((!isChangingExistingPin && pinDialogStep == 1) || (isChangingExistingPin && pinDialogStep == 2)) {
                            if (newPinInput.length == 4) {
                                pinDialogStep = if (isChangingExistingPin) 3 else 2
                                pinChangeError = null
                            } else {
                                pinChangeError = "PIN must be 4 digits"
                            }
                        } else {
                            if (confirmPinInput == newPinInput) {
                                viewModel.setPin(newPinInput)
                                viewModel.updateLockEnabled(true)
                                showPinDialog = false
                                currentPinInput = ""
                                newPinInput = ""
                                confirmPinInput = ""
                                pinChangeError = null
                                pinDialogStep = 1
                                Toast.makeText(context, "PIN updated successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                pinChangeError = "PINs do not match"
                            }
                        }
                    }
                ) {
                    Text("Continue", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        currentPinInput = ""
                        newPinInput = ""
                        confirmPinInput = ""
                        pinChangeError = null
                        pinDialogStep = 1
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Text(text = "Recording Preset", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordingPresetId.values().forEach { preset ->
                        val isSelected = (settings.activePreset == preset)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceSecondary else Color.Transparent)
                                .clickable {
                                    viewModel.applyPreset(preset)
                                    showPresetDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(text = "Privacy Policy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Black Screen Recorder is committed to your privacy.\n\n" +
                                "1. Zero Cloud Uploads: All recordings stay exclusively on your local device.\n" +
                                "2. Isolated Sandbox: Recordings are stored in private internal app storage.\n" +
                                "3. No Analytics on Recordings: We never inspect, analyze, or transmit your media files.\n" +
                                "4. Permissions: Camera and Microphone are used strictly for recording upon your initiation.\n" +
                                "5. Security PIN: Optional PIN and biometric lock add local device protection.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfacePrimary,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun ProSettingsCard(
    onNavigateToPro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
    val isPro = entitlements.isPro

    val infiniteTransition = rememberInfiniteTransition(label = "pro_border_shimmer")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val goldColor = Color(0xFFFFD54F)
    val borderColor = if (isPro) {
        goldColor.copy(alpha = borderAlpha)
    } else {
        AccentRed.copy(alpha = borderAlpha * 0.7f)
    }

    Card(
        onClick = onNavigateToPro,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pro_settings_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPro) Icons.Default.Check else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isPro) goldColor else AccentRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPro) "PRO ACTIVE" else "PRO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isPro) goldColor else TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPro) "All features unlocked & ads removed" else "Unlock more control & remove ads",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isPro) goldColor else AccentRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            ),
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueColor: Color = TextSecondary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = valueColor
                )
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentRed,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceSecondary
            ),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    val isSelected = (value == selected)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceSecondary else Color.Transparent)
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.White else TextPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfacePrimary,
        shape = RoundedCornerShape(14.dp)
    )
}
