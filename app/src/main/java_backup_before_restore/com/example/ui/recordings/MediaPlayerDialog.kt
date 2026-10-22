package com.example.ui.recordings

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ads.AdScreen
import com.example.ads.NativeAdCard
import com.example.ads.NativeAdManager
import com.example.ads.NativeAdStyle
import com.example.storage.RecordingEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun RecordingDetailDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(recording.durationSeconds * 1000L) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val isVideo = recording.type == "VIDEO"
    val file = remember(recording.filePath) { File(recording.filePath) }

    // Audio playback setup
    DisposableEffect(recording.id) {
        if (!isVideo && file.exists()) {
            val player = MediaPlayer().apply {
                try {
                    setDataSource(context, Uri.fromFile(file))
                    prepare()
                    totalDurationMs = duration.toLong()
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = 0L
                    }
                } catch (_: Exception) {}
            }
            mediaPlayer = player
        }

        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            NativeAdManager.destroyAd(AdScreen.PLAYBACK)
        }
    }

    // Progress polling
    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPositionMs = player.currentPosition.toLong()
                }
            }
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition.toLong()
                }
            }
            delay(200L)
        }
    }

    Dialog(
        onDismissRequest = {
            mediaPlayer?.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .background(SurfacePrimary, RoundedCornerShape(16.dp))
                .testTag("recording_detail_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Mic,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = recording.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${MainViewModel.formatTimestamp(recording.timestamp)} • ${MainViewModel.formatFileSize(recording.fileSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            mediaPlayer?.stop()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Media Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isVideo) 16f / 9f else 2.5f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!file.exists()) {
                        Text(
                            text = "File not found",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (isVideo) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    videoViewRef = this
                                    setVideoURI(Uri.fromFile(file))
                                    setOnPreparedListener { mp ->
                                        totalDurationMs = mp.duration.toLong()
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                        currentPositionMs = 0L
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Audio visualizer placeholder
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = AccentRed.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Playback Control bar
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isVideo) {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            } else {
                                mediaPlayer?.let { mp ->
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = AccentRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = MainViewModel.formatDuration(currentPositionMs / 1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Slider(
                        value = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            val seekMs = (totalDurationMs * fraction).toLong()
                            currentPositionMs = seekMs
                            if (isVideo) {
                                videoViewRef?.seekTo(seekMs.toInt())
                            } else {
                                mediaPlayer?.seekTo(seekMs.toInt())
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = AccentRed,
                            activeTrackColor = AccentRed,
                            inactiveTrackColor = BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_slider")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = MainViewModel.formatDuration(totalDurationMs / 1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Share, Export, Rename, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Share
                    TextButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_share),
                            color = TextSecondary
                        )
                    }

                    // Export
                    if (onExport != null) {
                        TextButton(
                            onClick = onExport,
                            modifier = Modifier.testTag("detail_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export",
                                color = TextSecondary
                            )
                        }
                    }

                    // Rename
                    TextButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.testTag("detail_rename_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_rename),
                            color = TextSecondary
                        )
                    }

                    // Delete
                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = AccentRed
                        )
                    }
                }

                // Native Ad Card
                NativeAdCard(
                    screen = AdScreen.PLAYBACK,
                    style = NativeAdStyle.COMPACT,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("playback_native_ad_card")
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(recording.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SurfaceSecondary,
            title = {
                Text(
                    text = stringResource(R.string.rename_title),
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            onRename(trimmed)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceSecondary,
            title = {
                Text(
                    text = "Delete Recording",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this recording? This action cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
