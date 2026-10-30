package com.example.ui.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import com.example.ads.RewardedAdManager
import com.example.covers.CoverConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.covers.CoverDisplay
import com.example.presets.RecordingPresetId
import com.example.recording.AudioMicSource
import com.example.recording.AudioQuality
import com.example.storage.EntitlementManager
import com.example.storage.ExitMethod
import com.example.storage.StorageCapacityEstimator
import com.example.ui.MainViewModel
import com.example.ui.components.TabTopHeader
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun RecordTabScreen(
    viewModel: MainViewModel,
    onStartVideo: () -> Unit,
    onStartAudio: () -> Unit,
    onNavigateToCovers: () -> Unit,
    onNavigateToPro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
    val isPro = entitlements.isPro

    val activeCover = settings.activeCover
    val selectedMode = settings.defaultRecordingType // "VIDEO" or "AUDIO"
    val scrollState = rememberScrollState()

    var showQualityDropdown by remember { mutableStateOf(false) }
    var showAudioFormatDropdown by remember { mutableStateOf(false) }
    var showAudioQualityDropdown by remember { mutableStateOf(false) }
    var showCriticalStorageDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showAudioSettingsSheet by remember { mutableStateOf(false) }

    val isStorageCritical = remember(storageStats.availableBytes) {
        StorageCapacityEstimator.isStorageCriticallyLow(storageStats.availableBytes)
    }
    val estimatedCapacitySeconds = remember(storageStats.availableBytes, selectedMode, settings.videoQuality) {
        StorageCapacityEstimator.getEstimatedRemainingSeconds(
            availableBytes = storageStats.availableBytes,
            recordingType = selectedMode,
            videoQuality = settings.videoQuality
        )
    }

    fun showPermissionSnackbar(message: String) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Settings"
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            onStartVideo()
        } else {
            showPermissionSnackbar(context.getString(R.string.permission_camera_mic))
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            onStartAudio()
        } else {
            showPermissionSnackbar(context.getString(R.string.permission_camera_mic))
        }
    }

    fun executeRecordingAction() {
        if (selectedMode == "VIDEO") {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val cameraGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (cameraGranted) {
                onStartVideo()
            } else {
                videoPermissionLauncher.launch(permissions.toTypedArray())
            }
        } else {
            val permissions = mutableListOf(
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val micGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (micGranted) {
                onStartAudio()
            } else {
                audioPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    fun startCurrentRecording() {
        if (isStorageCritical) {
            showCriticalStorageDialog = true
            return
        }

        executeRecordingAction()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabTopHeader(
                title = "BLACK SCREEN RECORDER",
                titleModifier = Modifier.testTag("app_title_text"),
                actions = {
                    val goldColor = Color(0xFFFFD54F)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPro) goldColor.copy(alpha = 0.15f) else SurfaceSecondary)
                            .border(
                                width = 0.5.dp,
                                color = if (isPro) goldColor.copy(alpha = 0.4f) else BorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onNavigateToPro() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("record_tab_pro_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPro) Icons.Default.Check else Icons.Default.WorkspacePremium,
                            contentDescription = "Pro",
                            modifier = Modifier.size(15.dp),
                            tint = if (isPro) goldColor else AccentRed
                        )
                        Text(
                            text = "PRO",
                            color = if (isPro) goldColor else TextPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preset Selector Card
                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showPresetDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_selector_row"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preset",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = TextSecondary
                        )
                        val activePreset = if (selectedMode == "AUDIO") settings.activeAudioPreset else settings.activeVideoPreset
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activePreset.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Select Preset",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Mode Selector (Video vs Audio)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isVideo = selectedMode == "VIDEO"
                    Card(
                        onClick = { viewModel.updateDefaultRecordingType("VIDEO") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("mode_selector_video"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVideo) Color(0xFF221111) else SurfacePrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isVideo) 1.5.dp else 0.5.dp,
                            color = if (isVideo) AccentRed else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isVideo) AccentRed else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIDEO",
                                fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (isVideo) Color.White else TextSecondary
                            )
                        }
                    }

                    val isAudio = selectedMode == "AUDIO"
                    Card(
                        onClick = { viewModel.updateDefaultRecordingType("AUDIO") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("mode_selector_audio"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAudio) Color(0xFF221111) else SurfacePrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isAudio) 1.5.dp else 0.5.dp,
                            color = if (isAudio) AccentRed else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isAudio) AccentRed else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUDIO",
                                fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (isAudio) Color.White else TextSecondary
                            )
                        }
                    }
                }

                // Quick Config Bar
                if (selectedMode == "VIDEO") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_config_bar"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Camera Lens Switch
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.updateCamera(!settings.useFrontCamera) }
                                    .padding(vertical = 4.dp)
                                    .testTag("quick_toggle_camera"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Switch Camera Lens",
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (settings.useFrontCamera) "Front Camera" else "Back Camera",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }

                            // Quality Dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { showQualityDropdown = true }
                                        .padding(vertical = 4.dp)
                                        .testTag("quick_select_quality"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = settings.videoQuality,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Quality",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showQualityDropdown,
                                    onDismissRequest = { showQualityDropdown = false }
                                ) {
                                    listOf("1080p", "720p", "480p").forEach { quality ->
                                        DropdownMenuItem(
                                            text = { Text(quality) },
                                            onClick = {
                                                viewModel.updateVideoQuality(quality)
                                                showQualityDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_config_bar"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Microphone Source Indicator
                            val currentMic = AudioMicSource.fromId(settings.preferredMicrophone)
                            val micIcon = when (currentMic) {
                                AudioMicSource.PHONE_MIC -> Icons.Default.Mic
                                AudioMicSource.WIRED_HEADSET -> Icons.Default.Headphones
                                AudioMicSource.BLUETOOTH_MIC -> Icons.Default.Bluetooth
                            }
                            val micLabel = when (currentMic) {
                                AudioMicSource.PHONE_MIC -> "Built-in Mic"
                                AudioMicSource.WIRED_HEADSET -> "Wired Mic"
                                AudioMicSource.BLUETOOTH_MIC -> "Bluetooth Mic"
                            }

                            Row(
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showAudioSettingsSheet = true
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("quick_audio_mic"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = micIcon,
                                    contentDescription = "Microphone Settings",
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = micLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Configure Mic",
                                    tint = TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // 2 & 3. Contextual Audio Controls: Format Chip + Bitrate / Quality Dropdown Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isWav = settings.audioFormat.contains("wav", ignoreCase = true)
                                val currentQuality = AudioQuality.fromId(settings.audioQuality)
                                val bitrateLabel = when (currentQuality) {
                                    AudioQuality.HIGH -> "256 kbps"
                                    AudioQuality.STANDARD -> "128 kbps"
                                    AudioQuality.VOICE -> "64 kbps"
                                }

                                // Audio Format Chip
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceSecondary,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showAudioFormatDropdown = true
                                            }
                                            .testTag("quick_select_audio_format")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (isWav) "WAV" else "M4A",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Format",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showAudioFormatDropdown,
                                        onDismissRequest = { showAudioFormatDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "M4A (AAC)",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (!isWav) AccentRed else TextPrimary
                                                    )
                                                    Text(
                                                        text = "Standard AAC · Small file size",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateAudioFormat("m4a")
                                                showAudioFormatDropdown = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "WAV Lossless",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isWav) AccentRed else TextPrimary
                                                    )
                                                    Text(
                                                        text = "Uncompressed · Studio fidelity",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateAudioFormat("wav")
                                                showAudioFormatDropdown = false
                                            }
                                        )
                                    }
                                }

                                // Bitrate / Quality Dropdown Badge
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showAudioQualityDropdown = true
                                            }
                                            .padding(vertical = 4.dp)
                                            .testTag("quick_select_audio_quality"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = bitrateLabel,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentRed
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Quality",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAudioQualityDropdown,
                                        onDismissRequest = { showAudioQualityDropdown = false }
                                    ) {
                                        AudioQuality.entries.forEach { quality ->
                                            val isSelected = (currentQuality == quality)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = quality.title,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) AccentRed else TextPrimary
                                                        )
                                                        Text(
                                                            text = quality.description,
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.updateAudioQuality(quality.id)
                                                    showAudioQualityDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recording Options (Background Rec & Exit on Stop)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_options_card"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val bgEnabled = settings.backgroundRecordingEnabled
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bgEnabled) Color(0xFF221111) else SurfacePrimary)
                            .border(
                                width = if (bgEnabled) 1.5.dp else 0.5.dp,
                                color = if (bgEnabled) AccentRed else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newState = !bgEnabled
                                viewModel.updateBackgroundRecordingEnabled(newState)
                                if (newState) {
                                    Toast.makeText(context, "Recording will continue when you leave the app", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Recording will stop when you leave the app", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("toggle_background_recording"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (bgEnabled) AccentRed else TextMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Background Rec",
                                    fontSize = 12.sp,
                                    fontWeight = if (bgEnabled) FontWeight.Bold else FontWeight.Medium,
                                    color = if (bgEnabled) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (bgEnabled) AccentRed else Color.Transparent)
                                    .then(
                                        if (!bgEnabled) Modifier.border(1.dp, TextMuted.copy(alpha = 0.4f), CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }

                    val exitEnabled = settings.exitAfterRecordingEnabled
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (exitEnabled) Color(0xFF221111) else SurfacePrimary)
                            .border(
                                width = if (exitEnabled) 1.5.dp else 0.5.dp,
                                color = if (exitEnabled) AccentRed else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newState = !exitEnabled
                                viewModel.updateExitAfterRecordingEnabled(newState)
                                if (newState) {
                                    viewModel.updateExitMethod(ExitMethod.NORMAL_STOP_CONTROLS)
                                    Toast.makeText(context, "App will close after recording is safely saved", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "App will stay open after recording stops", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("toggle_exit_after_recording"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = if (exitEnabled) AccentRed else TextMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Exit on Stop",
                                    fontSize = 12.sp,
                                    fontWeight = if (exitEnabled) FontWeight.Bold else FontWeight.Medium,
                                    color = if (exitEnabled) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (exitEnabled) AccentRed else Color.Transparent)
                                    .then(
                                        if (!exitEnabled) Modifier.border(1.dp, TextMuted.copy(alpha = 0.4f), CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }

                // Storage Capacity Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("storage_capacity_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val isStorageWarning = remember(storageStats.availableBytes) {
                        StorageCapacityEstimator.isStorageLow(storageStats.availableBytes)
                    }

                    if (isStorageCritical || isStorageWarning) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isStorageCritical) AccentRed else Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    val availableStr = MainViewModel.formatFileSize(storageStats.availableBytes) + " free"
                    val durationStr = if (isStorageCritical) "0m" else StorageCapacityEstimator.formatEstimatedDuration(estimatedCapacitySeconds)
                    val modeQualityStr = if (selectedMode == "AUDIO") "audio" else "at ${settings.videoQuality}"

                    Text(
                        text = "$availableStr · $durationStr $modeQualityStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isStorageCritical) AccentRed else TextSecondary
                    )
                }

                // Main Big Start Recording Button
                Button(
                    onClick = { startCurrentRecording() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_recording_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START RECORDING",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Cover Preview Card
                Card(
                    onClick = onNavigateToCovers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cover_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(104.dp)
                                .height(175.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, Color(0xFF2E2E32), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CoverDisplay(
                                cover = activeCover,
                                isCardPreview = true
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(175.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "COVER",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        )
                                    )
                                }
                                Text(
                                    text = activeCover.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeCover.brightnessPercent}% Brightness · ${settings.stopBehavior.displayName}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Change Cover",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Change cover",
                                    modifier = Modifier.size(16.dp),
                                    tint = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Critical Storage Dialog
    if (showCriticalStorageDialog) {
        AlertDialog(
            onDismissRequest = { showCriticalStorageDialog = false },
            title = {
                Text(
                    text = "Critically Low Storage",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Cannot start recording because available storage is critically low (${MainViewModel.formatFileSize(storageStats.availableBytes)} left). Please delete old recordings or free up space to record safely.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showCriticalStorageDialog = false }) {
                    Text("OK", color = AccentRed)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = SurfacePrimary
        )
    }

    // Preset Selection Dialog
    if (showPresetDialog) {
        val isAudio = (selectedMode == "AUDIO")
        val presetsForMode = RecordingPresetId.getPresetsForMode(isAudio)
        val currentActivePreset = if (isAudio) settings.activeAudioPreset else settings.activeVideoPreset

        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Text(
                    text = if (isAudio) "Select Audio Preset" else "Select Video Preset",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetsForMode.forEach { preset ->
                        val isSelected = (currentActivePreset == preset)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF221111) else SurfacePrimary)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) AccentRed else BorderSubtle.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.applyPreset(preset)
                                    showPresetDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) TextSecondary else TextMuted
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = SurfacePrimary
        )
    }

    // Audio Settings Sheet
    if (showAudioSettingsSheet) {
        com.example.ui.record.AudioSettingsSheet(
            settings = settings,
            onDismiss = { showAudioSettingsSheet = false },
            onMicrophoneSelected = { mic -> viewModel.updatePreferredMicrophone(mic.id) },
            onQualitySelected = { q -> viewModel.updateAudioQuality(q.id) },
            onClearVoiceChanged = { enabled -> viewModel.updateAudioClearVoice(enabled) },
            onReduceEchoChanged = { enabled -> viewModel.updateAudioReduceEcho(enabled) },
            onInputVolumeChanged = { vol -> viewModel.updateAudioInputVolume(vol) },
            onChannelsChanged = { ch -> viewModel.updateAudioChannels(ch) },
            onBitrateChanged = { br -> viewModel.updateAudioBitrate(br) },
            onSampleRateChanged = { sr -> viewModel.updateAudioSampleRate(sr) }
        )
    }
}
