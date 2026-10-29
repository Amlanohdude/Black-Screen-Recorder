package com.example.ui.recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.RewardedAdManager
import com.example.recording.RecordingType
import com.example.storage.EntitlementManager
import com.example.storage.ExportErrorType
import com.example.storage.ExportProgress
import com.example.storage.ExportResult
import com.example.storage.RecordingEntity
import com.example.storage.RecordingExportManager
import com.example.storage.SettingsPreferences
import com.example.ui.MainViewModel
import com.example.ui.recordings.ExportProgressDialog
import com.example.ui.recordings.RecordingDetailDialog
import com.example.ui.recordings.VideoThumbnailLoader
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

@Composable
fun RecordingSummaryScreen(
    summary: RecordingSummaryInfo,
    viewModel: MainViewModel? = null,
    onDone: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val prefs = remember { SettingsPreferences(context.applicationContext) }
    val settings by prefs.settings.collectAsStateWithLifecycle()
    var isExported by remember { mutableStateOf(false) }
    var exportProgressState by remember { mutableStateOf<ExportProgress?>(null) }
    var currentExportJob by remember { mutableStateOf<Job?>(null) }
    var isExportCancelled by remember { mutableStateOf(false) }
    var showExportAdDialog by remember { mutableStateOf(false) }

    BackHandler {
        onDone()
    }

    fun shareRecording() {
        val file = File(summary.filePath)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = if (summary.type == RecordingType.VIDEO) "video/mp4" else "audio/mp4"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share recording"))
        } catch (_: Exception) {}
    }

    fun executeExport(folderUri: String, folderName: String) {
        val dummyEntity = RecordingEntity(
            title = summary.title,
            filePath = summary.filePath,
            durationSeconds = summary.durationSeconds,
            fileSizeBytes = summary.fileSizeBytes,
            type = summary.type.name
        )
        isExportCancelled = false
        exportProgressState = ExportProgress(
            currentFileIndex = 1,
            totalFiles = 1,
            bytesCopied = 0L,
            totalBytes = summary.fileSizeBytes,
            percent = 0,
            currentFileName = File(summary.filePath).name
        )
        currentExportJob = coroutineScope.launch {
            val result = RecordingExportManager.exportRecording(
                context = context,
                recording = dummyEntity,
                folderUriString = folderUri,
                onProgress = { exportProgressState = it },
                isCancelled = { isExportCancelled }
            )
            exportProgressState = null
            when (result) {
                is ExportResult.Success -> {
                    EntitlementManager.consumeCustomFolderExportCredit()
                    isExported = true
                    Toast.makeText(context, "Recording exported to $folderName.", Toast.LENGTH_SHORT).show()
                }
                is ExportResult.Failure -> {
                    if (result.errorType == ExportErrorType.FOLDER_PERMISSION_REVOKED) {
                        Toast.makeText(context, "Selected folder is no longer accessible. Please choose a folder again.", Toast.LENGTH_LONG).show()
                        prefs.clearCustomExportFolder()
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

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val folderName = RecordingExportManager.getFolderDisplayName(context, uri)
            prefs.updateCustomExportFolder(uri.toString(), folderName)
            Toast.makeText(context, "Custom export folder selected.", Toast.LENGTH_SHORT).show()
            if (EntitlementManager.canUseCustomFolderExport()) {
                executeExport(uri.toString(), folderName)
            } else {
                showExportAdDialog = true
            }
        }
    }

    fun startExport() {
        if (!EntitlementManager.canUseCustomFolderExport()) {
            showExportAdDialog = true
            return
        }
        val customUri = settings.customExportFolderUri
        val customName = settings.customExportFolderName
        if (!customUri.isNullOrEmpty()) {
            executeExport(customUri, customName ?: "Custom Folder")
        } else {
            pickFolderLauncher.launch(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Recording Saved",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Saved securely in app storage",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    SummaryField(
                        label = "TYPE",
                        primaryValue = summary.type.name,
                        secondaryValue = summary.qualityOrFormat
                    )

                    HorizontalDivider(
                        color = BorderSubtle.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SummaryField(
                        label = "DURATION",
                        primaryValue = MainViewModel.formatDuration(summary.durationSeconds),
                        isMonospace = true
                    )

                    HorizontalDivider(
                        color = BorderSubtle.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SummaryField(
                        label = "SIZE",
                        primaryValue = MainViewModel.formatFileSize(summary.fileSizeBytes)
                    )

                    HorizontalDivider(
                        color = BorderSubtle.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SummaryField(
                        label = "LOCATION",
                        primaryValue = "Private App Storage",
                        secondaryValue = "(Protected)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showPlayerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("summary_play_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = { shareRecording() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("summary_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { startExport() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("summary_export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isExported) "Exported ✓" else "Export to Custom Folder",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceSecondary),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("summary_done_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("summary_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AccentRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Recording", color = AccentRed.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }

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

        if (showPlayerDialog) {
            val dummyEntity = RecordingEntity(
                title = summary.title,
                filePath = summary.filePath,
                durationSeconds = summary.durationSeconds,
                fileSizeBytes = summary.fileSizeBytes,
                type = summary.type.name
            )
            RecordingDetailDialog(
                recording = dummyEntity,
                onDismiss = { showPlayerDialog = false },
                onShare = { shareRecording() },
                onRename = { newTitle ->
                    viewModel?.let { vm ->
                        coroutineScope.launch {
                            val entity = vm.repository.getRecordingByFilePath(summary.filePath)
                            if (entity != null) {
                                vm.renameRecording(entity, newTitle)
                            }
                        }
                    }
                },
                onDelete = {
                    showPlayerDialog = false
                    try {
                        val file = File(summary.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (_: Exception) {}
                    viewModel?.deleteRecordingByFilePath(summary.filePath)
                    VideoThumbnailLoader.remove(summary.filePath)
                    Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(14.dp),
                title = {
                    Text(
                        text = "Delete Recording",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this recording? It will be permanently removed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            try {
                                val file = File(summary.filePath)
                                if (file.exists()) {
                                    file.delete()
                                }
                            } catch (_: Exception) {}
                            viewModel?.deleteRecordingByFilePath(summary.filePath)
                            VideoThumbnailLoader.remove(summary.filePath)
                            Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
                            onDone()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_summary_delete_button")
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showExportAdDialog) {
            AlertDialog(
                onDismissRequest = { showExportAdDialog = false },
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
                                        startExport()
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
                    TextButton(onClick = { showExportAdDialog = false }) {
                        Text(text = "Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SummaryField(
    label: String,
    primaryValue: String,
    secondaryValue: String? = null,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            ),
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                ),
                color = TextPrimary
            )
            if (!secondaryValue.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
        }
    }
}
