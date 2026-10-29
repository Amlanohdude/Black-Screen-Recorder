package com.example.ui.recordings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ads.RewardedAdManager
import com.example.storage.EntitlementManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ads.AdScreen
import com.example.ads.AdsConfig
import com.example.ads.NativeAdCard
import com.example.ads.NativeAdStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.Bitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.storage.ExportErrorType
import com.example.storage.ExportProgress
import com.example.storage.ExportResult
import com.example.storage.RecordingEntity
import com.example.storage.RecordingExportManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File


enum class RecordingFilterType(val label: String) {
ALL("All"),
VIDEO("Video"),
AUDIO("Audio")
}

@Composable
fun RecordingsScreen(
viewModel: MainViewModel,
onNavigateBack: (() -> Unit)? = null,
onNavigateToRecord: (() -> Unit)? = null,
modifier: Modifier = Modifier
) {
val context = LocalContext.current
val haptic = LocalHapticFeedback.current
val recordings by viewModel.recordings.collectAsStateWithLifecycle()
val settings by viewModel.userSettings.collectAsStateWithLifecycle()
val isUnlocked by viewModel.isRecordingsUnlocked.collectAsStateWithLifecycle()
var showSetupLock by remember { mutableStateOf(false) }

if ((settings.lockEnabled && !isUnlocked) || showSetupLock) {
RecordingsLockView(
viewModel = viewModel,
onUnlocked = {
showSetupLock = false
viewModel.unlockRecordings()
},
onNavigateBack = {
if (showSetupLock && !settings.lockEnabled) {
showSetupLock = false
} else {
(onNavigateBack ?: onNavigateToRecord)?.invoke()
}
},
modifier = modifier
)
return
}

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(RecordingFilterType.ALL) }
    var showExportAdDialog by remember { mutableStateOf(false) }
    var pendingExportAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        viewModel.reconcileOrphanFiles()
    }

    val totalCount = recordings.size
    val videoCount = remember(recordings) { recordings.count { it.type == "VIDEO" } }
    val audioCount = remember(recordings) { recordings.count { it.type == "AUDIO" } }

    val filteredRecordings = remember(recordings, selectedFilter) {
        when (selectedFilter) {
            RecordingFilterType.ALL -> recordings
            RecordingFilterType.VIDEO -> recordings.filter { it.type == "VIDEO" }
            RecordingFilterType.AUDIO -> recordings.filter { it.type == "AUDIO" }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var pendingSingleExport by remember { mutableStateOf<RecordingEntity?>(null) }
    var pendingBatchExport by remember { mutableStateOf<List<RecordingEntity>?>(null) }
    var exportProgressState by remember { mutableStateOf<ExportProgress?>(null) }
    var currentExportJob by remember { mutableStateOf<Job?>(null) }
    var isExportCancelled by remember { mutableStateOf(false) }

    fun executeSingleExport(recording: RecordingEntity, folderUri: String, folderName: String?) {
        isExportCancelled = false
        exportProgressState = ExportProgress(
            currentFileIndex = 1,
            totalFiles = 1,
            bytesCopied = 0L,
            totalBytes = recording.fileSizeBytes,
            percent = 0,
            currentFileName = File(recording.filePath).name
        )
        currentExportJob = coroutineScope.launch {
            val result = viewModel.exportRecordingToCustomFolder(
                recording = recording,
                onProgress = { progress ->
                    exportProgressState = progress
                },
                isCancelled = { isExportCancelled }
            )
            exportProgressState = null
            when (result) {
                is ExportResult.Success -> {
                    Toast.makeText(
                        context,
                        "Recording exported to ${result.destinationFolderName}.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ExportResult.Failure -> {
                    if (result.errorType == ExportErrorType.FOLDER_PERMISSION_REVOKED) {
                        Toast.makeText(context, "Selected folder is no longer accessible. Please choose a folder again.", Toast.LENGTH_LONG).show()
                        viewModel.clearCustomExportFolder()
                    } else {
                        Toast.makeText(context, "Export failed: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is ExportResult.Cancelled -> {
                    Toast.makeText(context, "Export cancelled.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun executeBatchExport(recordingsToExport: List<RecordingEntity>, folderUri: String, folderName: String?) {
        isExportCancelled = false
        val firstFile = recordingsToExport.firstOrNull()
        val firstFileName = firstFile?.let { File(it.filePath).name } ?: ""
        val firstFileSize = firstFile?.fileSizeBytes ?: 0L
        exportProgressState = ExportProgress(
            currentFileIndex = 1,
            totalFiles = recordingsToExport.size,
            bytesCopied = 0L,
            totalBytes = firstFileSize,
            percent = 0,
            currentFileName = firstFileName
        )
        currentExportJob = coroutineScope.launch {
            val result = viewModel.exportBatchToCustomFolder(
                recordingsList = recordingsToExport,
                onProgress = { progress ->
                    exportProgressState = progress
                },
                isCancelled = { isExportCancelled }
            )
            exportProgressState = null
            when (result) {
                is ExportResult.Success -> {
                    EntitlementManager.consumeCustomFolderExportCredit()
                    isSelectionMode = false
                    selectedIds = emptySet()
                    Toast.makeText(context, "${result.exportedCount} recordings exported to ${result.destinationFolderName}.", Toast.LENGTH_SHORT).show()
                }
                is ExportResult.Failure -> {
                    if (result.errorType == ExportErrorType.FOLDER_PERMISSION_REVOKED) {
                        Toast.makeText(context, "Selected folder is no longer accessible. Please choose a folder again.", Toast.LENGTH_LONG).show()
                        viewModel.clearCustomExportFolder()
                    } else {
                        Toast.makeText(context, "Export failed: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is ExportResult.Cancelled -> {
                    Toast.makeText(context, "Export cancelled.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

val openExportFolderLauncher = rememberLauncherForActivityResult(
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

val single = pendingSingleExport
val batch = pendingBatchExport
pendingSingleExport = null
pendingBatchExport = null

if (single != null) {
executeSingleExport(single, uri.toString(), folderName)
} else if (!batch.isNullOrEmpty()) {
executeBatchExport(batch, uri.toString(), folderName)
}
} else {
pendingSingleExport = null
pendingBatchExport = null
}
}

fun startSingleExport(recording: RecordingEntity) {
if (!EntitlementManager.canUseCustomFolderExport()) {
pendingExportAction = { startSingleExport(recording) }
showExportAdDialog = true
return
}
val folderUri = settings.customExportFolderUri
val folderName = settings.customExportFolderName
if (folderUri.isNullOrBlank()) {
pendingSingleExport = recording
openExportFolderLauncher.launch(null)
} else {
executeSingleExport(recording, folderUri, folderName)
}
}

    fun startBatchExport(recordingsToExport: List<RecordingEntity>) {
        if (recordingsToExport.isEmpty()) return
        if (!EntitlementManager.canUseCustomFolderExport()) {
            pendingExportAction = { startBatchExport(recordingsToExport) }
            showExportAdDialog = true
            return
        }
        val folderUri = settings.customExportFolderUri
        val folderName = settings.customExportFolderName
        if (folderUri.isNullOrBlank()) {
            pendingBatchExport = recordingsToExport
            openExportFolderLauncher.launch(null)
        } else {
            executeBatchExport(recordingsToExport, folderUri, folderName)
        }
    }

    fun toggleSelection(id: Long) {
        selectedIds = if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
    }

    // Intercept back button to exit multi-select mode if active, or navigate back/to Record
    BackHandler(enabled = isSelectionMode || onNavigateBack != null || onNavigateToRecord != null) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds = emptySet()
        } else if (onNavigateBack != null) {
            onNavigateBack.invoke()
        } else {
            onNavigateToRecord?.invoke()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            if (isSelectionMode) {
                // Multi-selection Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.testTag("exit_selection_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentViewIds = filteredRecordings.map { it.id }.toSet()
                        val isAllInViewSelected = currentViewIds.isNotEmpty() && selectedIds.containsAll(currentViewIds)

                        IconButton(
                            onClick = {
                                selectedIds = if (isAllInViewSelected) {
                                    selectedIds - currentViewIds
                                } else {
                                    selectedIds + currentViewIds
                                }
                                if (selectedIds.isEmpty()) {
                                    isSelectionMode = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all in view",
                                tint = if (isAllInViewSelected) AccentRed else TextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                val itemsToExport = filteredRecordings.filter { selectedIds.contains(it.id) }
                                startBatchExport(itemsToExport)
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export selected",
                                tint = if (selectedIds.isNotEmpty()) TextPrimary else TextMuted
                            )
                        }

                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("delete_selected_recordings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedIds.isNotEmpty()) AccentRed else TextMuted
                            )
                        }
                    }
                }
                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
            } else {
                TabTopHeader(
title = "RECORDINGS",
navigationIcon = if (onNavigateBack != null) {
{
IconButton(
onClick = onNavigateBack,
modifier = Modifier
.size(40.dp)
.testTag("recordings_back_button")
) {
Icon(
imageVector = Icons.AutoMirrored.Filled.ArrowBack,
contentDescription = "Back",
tint = TextPrimary
)
}
Spacer(modifier = Modifier.width(8.dp))
}
} else null,
actions = {
if (recordings.isNotEmpty()) {
Text(
text = "$totalCount total",
style = MaterialTheme.typography.bodySmall.copy(
fontWeight = FontWeight.Medium,
fontSize = 12.sp
),
color = TextSecondary
)
}

if (settings.lockEnabled) {
Spacer(modifier = Modifier.width(8.dp))
IconButton(
onClick = { viewModel.lockRecordings() },
modifier = Modifier
.size(34.dp)
.testTag("lock_recordings_button")
) {
Icon(
imageVector = Icons.Default.Lock,
contentDescription = "Lock Recordings",
tint = TextSecondary,
modifier = Modifier.size(18.dp)
)
}
} else {
Spacer(modifier = Modifier.width(8.dp))
IconButton(
onClick = { showSetupLock = true },
modifier = Modifier
.size(34.dp)
.testTag("setup_lock_button")
) {
Icon(
imageVector = Icons.Default.LockOpen,
contentDescription = "Set up PIN Lock",
tint = TextMuted,
modifier = Modifier.size(18.dp)
)
}
}
}
)
}

// Segmented Pill Filter Bar: All · Video · Audio with subtle secondary counts
Box(
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 16.dp, vertical = 10.dp)
.clip(RoundedCornerShape(10.dp))
.background(SurfacePrimary)
.border(0.5.dp, BorderSubtle, RoundedCornerShape(10.dp))
.padding(3.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
RecordingFilterPill(
label = "All",
count = totalCount,
isSelected = selectedFilter == RecordingFilterType.ALL,
onClick = { selectedFilter = RecordingFilterType.ALL },
modifier = Modifier.weight(1f).testTag("filter_all")
)

RecordingFilterPill(
label = "Video",
count = videoCount,
isSelected = selectedFilter == RecordingFilterType.VIDEO,
onClick = { selectedFilter = RecordingFilterType.VIDEO },
modifier = Modifier.weight(1f).testTag("filter_video")
)

RecordingFilterPill(
label = "Audio",
count = audioCount,
isSelected = selectedFilter == RecordingFilterType.AUDIO,
onClick = { selectedFilter = RecordingFilterType.AUDIO },
modifier = Modifier.weight(1f).testTag("filter_audio")
)
}
}

// Content List or Empty State
if (filteredRecordings.isEmpty()) {
Box(
modifier = Modifier
.fillMaxSize()
.padding(horizontal = 32.dp),
contentAlignment = Alignment.Center
) {
Column(
horizontalAlignment = Alignment.CenterHorizontally
) {
val emptyIcon = when (selectedFilter) {
RecordingFilterType.ALL -> Icons.Default.VideoLibrary
RecordingFilterType.VIDEO -> Icons.Default.Videocam
RecordingFilterType.AUDIO -> Icons.Default.Mic
}
Box(
modifier = Modifier
.size(72.dp)
.background(SurfacePrimary, CircleShape),
contentAlignment = Alignment.Center
) {
Icon(
imageVector = emptyIcon,
contentDescription = null,
tint = TextMuted,
modifier = Modifier.size(36.dp)
)
}
Spacer(modifier = Modifier.height(16.dp))
Text(
text = when (selectedFilter) {
RecordingFilterType.ALL -> "No recordings yet"
RecordingFilterType.VIDEO -> "No video recordings"
RecordingFilterType.AUDIO -> "No audio recordings"
},
style = MaterialTheme.typography.titleMedium.copy(
fontWeight = FontWeight.Bold,
fontSize = 17.sp
),
color = TextPrimary
)
Spacer(modifier = Modifier.height(6.dp))
Text(
text = "Captured sessions will be stored here privately.",
style = MaterialTheme.typography.bodySmall,
color = TextSecondary,
textAlign = TextAlign.Center
)
Spacer(modifier = Modifier.height(20.dp))
if (onNavigateToRecord != null) {
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onNavigateToRecord()
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentRed,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = Modifier.testTag("empty_state_record_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White, CircleShape)
            )
            Text(
                text = "Start Your First Recording",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.White
            )
        }
    }
}
}
}
} else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = filteredRecordings,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isSelected = selectedIds.contains(item.id)
                    RecordingListItem(
                        recording = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(item.id)
                            } else {
                                selectedRecording = item
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds = setOf(item.id)
                            } else {
                                toggleSelection(item.id)
                            }
                        }
                    )

// Insert Native Ad after the configured frequency threshold (e.g. 4th item)
if (selectedFilter == RecordingFilterType.AUDIO) {
if (AdsConfig.showInRecordingsAudio && index == AdsConfig.recordingsAudioFrequency - 1) {
NativeAdCard(
screen = AdScreen.RECORDINGS_AUDIO,
style = NativeAdStyle.COMPACT,
modifier = Modifier.padding(top = 10.dp)
)
}
} else {
if (AdsConfig.showInRecordings && index == AdsConfig.recordingsFrequency - 1) {
NativeAdCard(
screen = AdScreen.RECORDINGS,
style = NativeAdStyle.FEED,
modifier = Modifier.padding(top = 10.dp)
)
}
}
}
}
}

// Single recording playback/detail dialog
selectedRecording?.let { recording ->
RecordingDetailDialog(
recording = recording,
onDismiss = { selectedRecording = null },
onShare = {
val shareIntent = viewModel.repository.getShareIntent(recording)
if (shareIntent != null) {
context.startActivity(shareIntent)
}
},
onRename = { newTitle ->
viewModel.renameRecording(recording, newTitle)
selectedRecording = recording.copy(title = newTitle)
},
onDelete = {
VideoThumbnailLoader.remove(recording.filePath)
viewModel.deleteRecording(recording)
selectedRecording = null
},
onExport = {
startSingleExport(recording)
}
)
}

// Multiple Deletion Confirmation Dialog
if (showDeleteConfirmDialog) {
AlertDialog(
onDismissRequest = { showDeleteConfirmDialog = false },
title = {
Text(
text = "Delete ${selectedIds.size} ${if (selectedIds.size == 1) "recording" else "recordings"}?",
style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
color = TextPrimary
)
},
text = {
Text(
text = "This will permanently remove the selected file(s) from your device.",
style = MaterialTheme.typography.bodyMedium,
color = TextSecondary
)
},
confirmButton = {
TextButton(
onClick = {
val itemsToDelete = recordings.filter { it.id in selectedIds }
itemsToDelete.forEach { VideoThumbnailLoader.remove(it.filePath) }
viewModel.deleteRecordings(itemsToDelete)
selectedIds = emptySet()
isSelectionMode = false
showDeleteConfirmDialog = false
},
modifier = Modifier.testTag("confirm_delete_multiple_button")
) {
Text(
text = "Delete",
color = AccentRed,
fontWeight = FontWeight.Bold
)
}
},
dismissButton = {
TextButton(onClick = { showDeleteConfirmDialog = false }) {
Text(text = "Cancel", color = TextSecondary)
}
},
containerColor = SurfacePrimary,
shape = RoundedCornerShape(14.dp)
)
}

// Export Progress Dialog
exportProgressState?.let { progress ->
ExportProgressDialog(
progress = progress,
onCancel = {
isExportCancelled = true
currentExportJob?.cancel()
exportProgressState = null
}
)
}

if (showExportAdDialog) {
AlertDialog(
onDismissRequest = {
showExportAdDialog = false
pendingExportAction = null
},
containerColor = SurfacePrimary,
shape = RoundedCornerShape(14.dp),
title = {
Text(
text = "Custom Folder Export",
style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
color = TextPrimary
)
},
text = {
Text(
text = "Exporting recordings to an external custom folder requires 1 export credit or Pro. Watch an ad to earn 1 export credit.",
style = MaterialTheme.typography.bodyMedium,
color = TextSecondary
)
},
confirmButton = {
Button(
onClick = {
showExportAdDialog = false
val activity = context as? Activity
if (activity != null) {
RewardedAdManager.showAd(
activity = activity,
onUserEarnedReward = {
EntitlementManager.addCustomFolderExportCredit()
val action = pendingExportAction
pendingExportAction = null
action?.invoke()
}
)
}
},
colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
shape = RoundedCornerShape(8.dp)
) {
Text(text = "Watch Ad · 1 Use", color = Color.White, fontWeight = FontWeight.Bold)
}
},
dismissButton = {
TextButton(
onClick = {
showExportAdDialog = false
pendingExportAction = null
}
) {
Text(text = "Cancel", color = TextSecondary)
}
}
)
}

        }
    }
}

@Composable
private fun VideoThumbnailView(
    filePath: String,
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(filePath) {
        thumbnail = VideoThumbnailLoader.loadThumbnail(filePath)
    }

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumbnail
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = MainViewModel.formatDuration(durationSeconds),
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AudioThumbnailView(
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 80.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceSecondary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = AccentRed,
            modifier = Modifier.size(24.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = MainViewModel.formatDuration(durationSeconds),
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RecordingListItem(
    recording: RecordingEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isVideo = recording.type.equals("VIDEO", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceSecondary else SurfacePrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isSelected) AccentRed else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) AccentRed else TextMuted,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Thumbnail Preview
            if (isVideo) {
                VideoThumbnailView(
                    filePath = recording.filePath,
                    durationSeconds = recording.durationSeconds
                )
            } else {
                AudioThumbnailView(
                    durationSeconds = recording.durationSeconds
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Recording Metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = MainViewModel.formatTimestamp(recording.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isVideo) AccentRed.copy(alpha = 0.15f) else Color(0xFF262626),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = if (isVideo) "VIDEO" else "AUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVideo) AccentRed else TextSecondary
                            )
                        )
                    }

                    Text(
                        text = MainViewModel.formatDuration(recording.durationSeconds),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )

                    Text("•", color = TextMuted, fontSize = 10.sp)

                    Text(
                        text = MainViewModel.formatFileSize(recording.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ExportProgressDialog(
    progress: ExportProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfacePrimary,
        title = {
            Text(
                text = if (progress.totalFiles > 1) "Exporting ${progress.currentFileIndex} of ${progress.totalFiles}..." else "Exporting to Custom Folder...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = progress.currentFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AccentRed,
                    trackColor = SurfaceSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progress.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${MainViewModel.formatFileSize(progress.bytesCopied)} / ${MainViewModel.formatFileSize(progress.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_export_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RecordingFilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceSecondary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (isSelected) Color.White else TextSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) AccentRed else TextMuted
            )
        }
    }
}
