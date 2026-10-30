package com.example.ui.recorder

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.covers.ControlGesture
import com.example.covers.CoverConfig
import com.example.covers.CoverDisplay
import com.example.recording.AudioMicSource
import com.example.recording.RecordingService
import com.example.recording.RecordingStage
import com.example.recording.RecordingType
import com.example.recording.VideoRecordingManager
import com.example.storage.EntitlementManager
import com.example.storage.ExitGesture
import com.example.storage.ExitMethod
import com.example.storage.StopBehavior
import com.example.storage.StorageCapacityEstimator
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentRed
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingScreen(
    type: RecordingType,
    viewModel: MainViewModel,
    onRecordingFinished: (RecordingSummaryInfo?) -> Unit,
    onQuickExit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val status by viewModel.recordingStatus.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val rawCover = settings.activeCover
    var hasConsumedAutoStopCredit by rememberSaveable { mutableStateOf(false) }
    var hasConsumed4kCredit by rememberSaveable { mutableStateOf(false) }

    // Stop protection state
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var isHoldingToStop by remember { mutableStateOf(false) }

    // Storage monitoring state
    var isLowStorageWarning by remember { mutableStateOf(false) }
    var availableStorageBytes by remember { mutableStateOf(0L) }
    var criticalStorageAlertMessage by remember { mutableStateOf<String?>(null) }

    // Immersive full-screen system bars handling
    DisposableEffect(lifecycleOwner) {
        val activity = context as? Activity
        val window = activity?.window

        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            window.attributes.layoutInDisplayCutoutMode
        } else null

        fun applyImmersiveMode() {
            window?.let { win ->
                WindowCompat.setDecorFitsSystemWindows(win, false)
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
        }

        fun restoreSystemBars() {
            window?.let { win ->
                WindowCompat.setDecorFitsSystemWindows(win, true)
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && originalCutoutMode != null) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode = originalCutoutMode
                    }
                }
            }
        }

        applyImmersiveMode()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                applyImmersiveMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            restoreSystemBars()
        }
    }

    val activeCover = rawCover

    val isAutoStopEffective = remember(settings.autoStopSeconds) {
        settings.autoStopSeconds > 0 && EntitlementManager.canUseAutoStop()
    }

    LaunchedEffect(Unit) {
        if (settings.autoStopSeconds > 0 && !hasConsumedAutoStopCredit && !EntitlementManager.isPro()) {
            EntitlementManager.consumeAutoStopCredit()
            hasConsumedAutoStopCredit = true
        }
        if (settings.videoQuality == "4K" && !hasConsumed4kCredit && !EntitlementManager.isPro()) {
            EntitlementManager.consume4kCredit()
            hasConsumed4kCredit = true
        }
    }

    // Video preview ref
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Controls visibility and interaction state
    var showControlsOverlay by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCameraPeek by remember { mutableStateOf(false) }

    // Quick exit state
    var isQuickExitRequested by remember { mutableStateOf(false) }
    var isFinalizing by remember { mutableStateOf(false) }
    var finalizationErrorMessage by remember { mutableStateOf<String?>(null) }
    var showStopAndExitConfirmDialog by remember { mutableStateOf(false) }

    // Stop execution
    fun executeStop(isQuickExit: Boolean = false) {
        if (isFinalizing) return
        val shouldExit = isQuickExit || (settings.exitAfterRecordingEnabled && settings.exitMethod == ExitMethod.NORMAL_STOP_CONTROLS)
        isFinalizing = true
        isQuickExitRequested = shouldExit
        viewModel.stopRecording(context)
    }

    fun triggerGestureExit() {
        if (isFinalizing) return
        if (settings.stopBehavior == StopBehavior.CONFIRM_TO_STOP) {
            showStopAndExitConfirmDialog = true
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            executeStop(isQuickExit = true)
        }
    }

    // Start recording service if not already recording
    LaunchedEffect(type) {
        if (type == RecordingType.AUDIO) {
            if (!status.isRecording) {
                viewModel.startAudio(context)
            }
        } else if (type == RecordingType.VIDEO) {
            if (!status.isRecording) {
                viewModel.startVideo(context)
            }
        }
    }

    // Lifecycle observer: if background recording is disabled, stop when leaving app
    DisposableEffect(lifecycleOwner, settings.backgroundRecordingEnabled, status.isRecording, isFinalizing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val currentStatus = viewModel.recordingStatus.value
                if (!settings.backgroundRecordingEnabled && currentStatus.isRecording && !isFinalizing) {
                    executeStop(isQuickExit = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Listen to volume down twice quick exit trigger
    LaunchedEffect(settings.exitAfterRecordingEnabled, settings.exitMethod, settings.exitGesture) {
        if (settings.exitAfterRecordingEnabled && settings.exitMethod == ExitMethod.GESTURE && settings.exitGesture == ExitGesture.VOLUME_DOWN_TWICE) {
            MainActivity.volumeDownExitTrigger.collect {
                triggerGestureExit()
            }
        }
    }

    // Universal error observer (startup error, runtime error, or finalization error)
    LaunchedEffect(status.stage, status.error) {
        if (status.stage == RecordingStage.FINALIZING) {
            isFinalizing = true
        } else if (status.stage == RecordingStage.ERROR) {
            isFinalizing = false
            isQuickExitRequested = false
            finalizationErrorMessage = status.error ?: "Recording encountered an unexpected error."
        }
    }

    // Quick exit safe completion observer
    LaunchedEffect(isFinalizing, isQuickExitRequested, status.stage, status.isRecording, status.lastSavedFilePath) {
        if (isFinalizing && isQuickExitRequested) {
            if (status.stage == RecordingStage.ERROR) {
                isFinalizing = false
                isQuickExitRequested = false
                finalizationErrorMessage = status.error ?: "Recording finalization encountered an error. App will not exit."
            } else if (!status.isRecording && status.stage == RecordingStage.IDLE) {
                val filePath = status.lastSavedFilePath
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        viewModel.lockRecordings()
                        onQuickExit()
                    } else {
                        isFinalizing = false
                        isQuickExitRequested = false
                        finalizationErrorMessage = "Saved recording file could not be verified on disk. App will not exit."
                    }
                } else {
                    isFinalizing = false
                    isQuickExitRequested = false
                    finalizationErrorMessage = "Recording finalization failed. App will not exit."
                }
            }
        }
    }

    // Normal finalization observer for summary
    LaunchedEffect(isFinalizing, isQuickExitRequested, status.isRecording, status.lastSavedFilePath, status.stage) {
        if (isFinalizing && !isQuickExitRequested && !status.isRecording && status.stage == RecordingStage.IDLE) {
            val filePath = status.lastSavedFilePath
            if (filePath != null) {
                val file = File(filePath)
                val cameraSide = if (settings.useFrontCamera) "Front Camera" else "Back Camera"
                val qualityOrFormat = if (type == RecordingType.VIDEO) "${settings.videoQuality} • $cameraSide" else "M4A (AAC)"
                val summary = RecordingSummaryInfo(
                    type = type,
                    title = status.lastSavedTitle ?: file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    durationSeconds = maxOf(1L, status.lastSavedDurationSeconds),
                    fileSizeBytes = file.length(),
                    qualityOrFormat = qualityOrFormat,
                    coverName = activeCover.name,
                    safetyStopReason = status.safetyStopReason
                )
                onRecordingFinished(summary)
            } else {
                delay(800L)
                onRecordingFinished(null)
            }
        }
    }

    // Periodic Storage Re-check during recording (every 5s)
    LaunchedEffect(status.isRecording) {
        while (isActive && status.isRecording && !isFinalizing) {
            val stats = withContext(Dispatchers.IO) {
                StorageCapacityEstimator.getStorageStats(context)
            }
            availableStorageBytes = stats.availableBytes

            if (StorageCapacityEstimator.isStorageCriticallyLow(stats.availableBytes)) {
                criticalStorageAlertMessage = "Storage is critically low (${MainViewModel.formatFileSize(stats.availableBytes)} remaining). Recording was automatically stopped to prevent corruption."
                executeStop()
                break
            } else if (StorageCapacityEstimator.isStorageLow(stats.availableBytes)) {
                isLowStorageWarning = true
            } else {
                isLowStorageWarning = false
            }
            delay(5000L)
        }
    }

    // Auto-stop recording when configured duration is reached
    LaunchedEffect(status.durationSeconds, isAutoStopEffective, settings.autoStopSeconds) {
        if (isAutoStopEffective && settings.autoStopSeconds > 0 && status.isRecording && !isFinalizing) {
            val maxSeconds = settings.autoStopSeconds.toLong()
            if (status.durationSeconds >= maxSeconds) {
                executeStop()
            }
        }
    }

    // Auto-hide controls overlay after 3.5s of inactivity
    LaunchedEffect(showControlsOverlay, lastInteractionTime) {
        if (showControlsOverlay && !isFinalizing && !isHoldingToStop && !showStopConfirmDialog && !showStopAndExitConfirmDialog) {
            delay(3500L)
            showControlsOverlay = false
        }
    }

    // Listen to notification action STOP events
    LaunchedEffect(Unit) {
        RecordingService.stopEvent.collect {
            executeStop(isQuickExit = false)
        }
    }

    // Hold-to-stop animated progress (1000ms duration)
    val holdProgress by animateFloatAsState(
        targetValue = if (isHoldingToStop) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "hold_to_stop_progress",
        finishedListener = { progress ->
            if (progress >= 1f && isHoldingToStop && !isFinalizing) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isHoldingToStop = false
                executeStop(isQuickExit = false)
            }
        }
    )

    // Back handler with stop protection
    BackHandler {
        when (settings.stopBehavior) {
            StopBehavior.TAP_TO_STOP -> executeStop()
            StopBehavior.HOLD_TO_STOP,
            StopBehavior.CONFIRM_TO_STOP -> {
                showStopConfirmDialog = true
            }
        }
    }

    fun onScreenGestureTriggered() {
        showControlsOverlay = !showControlsOverlay
        lastInteractionTime = System.currentTimeMillis()
    }

    val isGestureExitActive = settings.exitAfterRecordingEnabled && settings.exitMethod == ExitMethod.GESTURE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isGestureExitActive, settings.exitGesture) {
                if (!isGestureExitActive) return@pointerInput
                var lastTwoFingerTapTime = 0L
                var lastCornerTapTime = 0L

                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val downPointers = downEvent.changes.filter { it.pressed }
                        when (settings.exitGesture) {
                            ExitGesture.TWO_FINGER_DOUBLE_TAP -> {
                                if (downPointers.size >= 2) {
                                    downEvent.changes.forEach { it.consume() }
                                    val downTime = System.currentTimeMillis()
                                    var releasedQuickly = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val stillPressed = event.changes.filter { it.pressed }
                                        event.changes.forEach { it.consume() }
                                        if (stillPressed.isEmpty()) {
                                            releasedQuickly = (System.currentTimeMillis() - downTime) < 500L
                                            break
                                        }
                                        if (System.currentTimeMillis() - downTime > 500L) {
                                            break
                                        }
                                    }
                                    if (releasedQuickly) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTwoFingerTapTime < 500L) {
                                            lastTwoFingerTapTime = 0L
                                            triggerGestureExit()
                                        } else {
                                            lastTwoFingerTapTime = now
                                        }
                                    }
                                }
                            }
                            ExitGesture.THREE_FINGER_LONG_PRESS -> {
                                if (downPointers.size >= 3) {
                                    downEvent.changes.forEach { it.consume() }
                                    val downTime = System.currentTimeMillis()
                                    var heldLongEnough = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val stillPressed = event.changes.filter { it.pressed }
                                        event.changes.forEach { it.consume() }
                                        if (stillPressed.size < 3) {
                                            break
                                        }
                                        if (System.currentTimeMillis() - downTime >= 800L) {
                                            heldLongEnough = true
                                            break
                                        }
                                    }
                                    if (heldLongEnough) {
                                        triggerGestureExit()
                                    }
                                }
                            }
                            ExitGesture.DOUBLE_TAP_CORNER -> {
                                if (downPointers.size == 1) {
                                    val first = downPointers.first()
                                    val isLowerRight = first.position.x > size.width * 0.75f && first.position.y > size.height * 0.75f
                                    if (isLowerRight) {
                                        downEvent.changes.forEach { it.consume() }
                                        val downTime = System.currentTimeMillis()
                                        var tapReleased = false
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val stillPressed = event.changes.filter { it.pressed }
                                            event.changes.forEach { it.consume() }
                                            if (stillPressed.isEmpty()) {
                                                tapReleased = (System.currentTimeMillis() - downTime) < 400L
                                                break
                                            }
                                        }
                                        if (tapReleased) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastCornerTapTime < 500L) {
                                                lastCornerTapTime = 0L
                                                triggerGestureExit()
                                            } else {
                                                lastCornerTapTime = now
                                            }
                                        }
                                    }
                                }
                            }
                            ExitGesture.VOLUME_DOWN_TWICE -> {
                                // Handled via MainActivity.volumeDownExitTrigger Flow
                            }
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    if (activeCover.controlGesture == ControlGesture.SINGLE_TAP) {
                        onScreenGestureTriggered()
                    }
                },
                onDoubleClick = {
                    if (activeCover.controlGesture == ControlGesture.DOUBLE_TAP) {
                        onScreenGestureTriggered()
                    }
                },
                onLongClick = {
                    if (activeCover.controlGesture == ControlGesture.LONG_PRESS) {
                        onScreenGestureTriggered()
                    }
                }
            )
            .testTag("recording_screen_container")
    ) {
        // Video Preview under cover
        if (type == RecordingType.VIDEO) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef = this
                        RecordingService.activeVideoManager?.setPreviewSurfaceProvider(surfaceProvider)
                    }
                },
                update = { view ->
                    previewViewRef = view
                    RecordingService.activeVideoManager?.setPreviewSurfaceProvider(view.surfaceProvider)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showCameraPeek) 0.85f else 0.001f)
            )

            DisposableEffect(previewViewRef, status.isRecording) {
                if (previewViewRef != null && status.isRecording) {
                    RecordingService.activeVideoManager?.setPreviewSurfaceProvider(previewViewRef?.surfaceProvider)
                }
                onDispose {
                    RecordingService.activeVideoManager?.setPreviewSurfaceProvider(null)
                }
            }
        }

        // Active Cover View on top of preview (hidden while peeking camera)
        if (!showCameraPeek) {
            CoverDisplay(
                cover = activeCover,
                modifier = Modifier.fillMaxSize(),
                isRecording = true,
                durationSeconds = status.durationSeconds
            )
        }

        // Peek Camera Button (top-right corner, visible only during video recording)
        if (type == RecordingType.VIDEO && (showControlsOverlay || showCameraPeek)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = { showCameraPeek = !showCameraPeek },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .testTag("toggle_preview_button")
                ) {
                    Icon(
                        imageVector = if (showCameraPeek) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showCameraPeek) "Hide preview" else "Peek camera",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = showControlsOverlay,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 32.dp, vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLowStorageWarning) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF4A3800))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Low Storage (${MainViewModel.formatFileSize(availableStorageBytes)} left)",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        text = MainViewModel.formatDuration(status.durationSeconds),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    when (settings.stopBehavior) {
                        StopBehavior.HOLD_TO_STOP -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF261010))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                lastInteractionTime = System.currentTimeMillis()
                                                isHoldingToStop = true
                                                tryAwaitRelease()
                                                isHoldingToStop = false
                                            }
                                        )
                                    }
                                    .testTag("stop_recording_button"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(holdProgress)
                                        .fillMaxHeight()
                                        .background(AccentRed)
                                )
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHoldingToStop) "HOLDING..." else "HOLD TO STOP",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    when (settings.stopBehavior) {
                                        StopBehavior.TAP_TO_STOP -> executeStop()
                                        StopBehavior.CONFIRM_TO_STOP -> showStopConfirmDialog = true
                                        else -> executeStop()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(56.dp)
                                    .testTag("stop_recording_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "STOP",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isFinalizing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = AccentRed,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Saving recording safely...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Stop confirmation dialog
        if (showStopConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmDialog = false },
                title = {
                    Text(
                        text = "Stop Recording?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to stop and save this recording? Duration: ${MainViewModel.formatDuration(status.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStopConfirmDialog = false
                            executeStop()
                        }
                    ) {
                        Text("Stop & Save", color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmDialog = false }) {
                        Text("Keep Recording", color = TextSecondary)
                    }
                },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (showStopAndExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopAndExitConfirmDialog = false },
                title = {
                    Text("Stop and exit?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                },
                text = {
                    Text(
                        "Are you sure you want to stop recording and exit Black Screen Recorder?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStopAndExitConfirmDialog = false
                            executeStop(isQuickExit = true)
                        }
                    ) {
                        Text("STOP & EXIT", color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopAndExitConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Critical storage emergency alert
        if (criticalStorageAlertMessage != null) {
            AlertDialog(
                onDismissRequest = { criticalStorageAlertMessage = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Low Storage Alert",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = criticalStorageAlertMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { criticalStorageAlertMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (finalizationErrorMessage != null) {
            AlertDialog(
                onDismissRequest = {
                    finalizationErrorMessage = null
                    onRecordingFinished(null)
                },
                title = {
                    Text("Recording Error", color = AccentRed, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(finalizationErrorMessage ?: "", color = TextSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            finalizationErrorMessage = null
                            onRecordingFinished(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = SurfacePrimary,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
