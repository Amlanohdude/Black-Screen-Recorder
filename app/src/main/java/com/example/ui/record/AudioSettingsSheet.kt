package com.example.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recording.AudioDeviceRouter
import com.example.recording.AudioMicSource
import com.example.recording.AudioQuality
import com.example.storage.UserSettings
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsSheet(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onMicrophoneSelected: (AudioMicSource) -> Unit,
    onQualitySelected: (AudioQuality) -> Unit,
    onClearVoiceChanged: (Boolean) -> Unit,
    onReduceEchoChanged: (Boolean) -> Unit,
    onInputVolumeChanged: (Float) -> Unit,
    onChannelsChanged: (Int) -> Unit,
    onBitrateChanged: (Int) -> Unit,
    onSampleRateChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    val availableMics = remember {
        AudioDeviceRouter.getAvailableMicrophones(context)
    }

    val currentMic = AudioMicSource.fromId(settings.preferredMicrophone)
    val currentQuality = AudioQuality.fromId(settings.audioQuality)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePrimary,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(BorderSubtle, CircleShape)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDIO SETTINGS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ==========================================
            // 1. MICROPHONE SOURCE (SHARED AUDIO & VIDEO)
            // ==========================================
            Text(
                text = "MICROPHONE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    availableMics.forEachIndexed { index, mic ->
                        val isSelected = (currentMic == mic)
                        val micIcon = when (mic) {
                            AudioMicSource.PHONE_MIC -> Icons.Default.PhoneAndroid
                            AudioMicSource.WIRED_HEADSET -> Icons.Default.Headphones
                            AudioMicSource.BLUETOOTH_MIC -> Icons.Default.Bluetooth
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMicrophoneSelected(mic) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentRed.copy(alpha = 0.15f) else SurfacePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = micIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) AccentRed else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = mic.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        ),
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    Text(
                                        text = mic.iconDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextMuted
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (index < availableMics.size - 1) {
                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 2. RECORDING QUALITY
            // ==========================================
            Text(
                text = "RECORDING QUALITY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AudioQuality.values().forEachIndexed { index, quality ->
                        val isSelected = (currentQuality == quality)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelected(quality) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = quality.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (isSelected) TextSecondary else TextMuted
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (index < AudioQuality.values().size - 1) {
                            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 3. AUDIO ENHANCEMENT
            // ==========================================
            Text(
                text = "AUDIO ENHANCEMENT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Clear Voice toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Clear Voice",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Reduce background noise and improve speech clarity",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = settings.audioClearVoice,
                            onCheckedChange = onClearVoiceChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentRed,
                                uncheckedTrackColor = SurfacePrimary
                            ),
                            modifier = Modifier.testTag("toggle_clear_voice")
                        )
                    }

                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Reduce Echo toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Reduce Echo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Helps reduce echo when recording near speakers",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = settings.audioReduceEcho,
                            onCheckedChange = onReduceEchoChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentRed,
                                uncheckedTrackColor = SurfacePrimary
                            ),
                            modifier = Modifier.testTag("toggle_reduce_echo")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 4. INPUT VOLUME
            // ==========================================
            Text(
                text = "INPUT VOLUME",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Low",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                        Text(
                            text = when {
                                settings.audioInputVolume < 0.9f -> "Quiet"
                                settings.audioInputVolume > 1.1f -> "High"
                                else -> "Normal"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "High",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }

                    Slider(
                        value = settings.audioInputVolume,
                        onValueChange = onInputVolumeChanged,
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentRed,
                            activeTrackColor = AccentRed,
                            inactiveTrackColor = BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_input_volume")
                    )

                    Text(
                        text = "Increase this if recordings sound too quiet.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // 5. MORE / ADVANCED SETTINGS (EXPANDABLE)
            // ==========================================
            Card(
                onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_audio_settings_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More Audio Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isAdvancedExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Format
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Format", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("M4A (AAC)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Bitrate Selection Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bitrate", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(128_000 to "128 kbps", 192_000 to "192 kbps", 256_000 to "256 kbps").forEach { (br, label) ->
                                    val isBrSelected = (settings.audioBitrate == br)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isBrSelected) AccentRed else SurfaceSecondary)
                                            .clickable { onBitrateChanged(br) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isBrSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isBrSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Channels Selection Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Channels", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1 to "Mono", 2 to "Stereo").forEach { (ch, label) ->
                                    val isChSelected = (settings.audioChannels == ch)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChSelected) AccentRed else SurfaceSecondary)
                                            .clickable { onChannelsChanged(ch) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isChSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isChSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Sample Rate Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sample Rate", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(44_100 to "44.1 kHz", 48_000 to "48.0 kHz").forEach { (sr, label) ->
                                    val isSrSelected = (settings.audioSampleRate == sr)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSrSelected) AccentRed else SurfaceSecondary)
                                            .clickable { onSampleRateChanged(sr) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSrSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isSrSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
