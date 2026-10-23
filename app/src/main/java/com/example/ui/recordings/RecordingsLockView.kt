package com.example.ui.recordings

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentRedDark
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfacePrimary
import com.example.ui.theme.SurfaceSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SetupStep {
    CREATE_PIN,
    CONFIRM_PIN
}

@Composable
fun RecordingsLockView(
    viewModel: MainViewModel,
    onUnlocked: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    val isSetupMode = !settings.hasPinSet
    var setupStep by remember { mutableStateOf(SetupStep.CREATE_PIN) }
    var tempFirstPin by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    BackHandler(enabled = true) {
        if (isSetupMode && setupStep == SetupStep.CONFIRM_PIN) {
            setupStep = SetupStep.CREATE_PIN
            enteredPin = ""
            tempFirstPin = ""
            errorMessage = null
        } else {
            onNavigateBack?.invoke()
        }
    }

    fun triggerShakeAndError(msg: String) {
        isError = true
        errorMessage = msg
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            for (i in 0..2) {
                shakeOffset.animateTo(20f, tween(50))
                shakeOffset.animateTo(-20f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
            delay(400L)
            enteredPin = ""
            isError = false
        }
    }

    fun authenticateWithBiometrics() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle("Unlock Vault")
                    .setSubtitle("Authenticate using biometrics to view recordings")
                    .setNegativeButton("Use PIN", context.mainExecutor) { _, _ -> }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    context.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUnlocked()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            // Biometric prompt dismissed or canceled
                        }
                    }
                )
            } catch (_: Exception) {}
        }
    }

    // Auto-prompt biometrics once when unlocking existing vault
    LaunchedEffect(Unit) {
        if (!isSetupMode) {
            delay(150L)
            authenticateWithBiometrics()
        }
    }

    fun handleDigitPress(digit: String) {
        if (enteredPin.length >= 4 || isError) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val newPin = enteredPin + digit
        enteredPin = newPin

        if (newPin.length == 4) {
            if (isSetupMode) {
                when (setupStep) {
                    SetupStep.CREATE_PIN -> {
                        tempFirstPin = newPin
                        coroutineScope.launch {
                            delay(200L)
                            setupStep = SetupStep.CONFIRM_PIN
                            enteredPin = ""
                            errorMessage = null
                        }
                    }
                    SetupStep.CONFIRM_PIN -> {
                        if (newPin == tempFirstPin) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.settingsPrefs.setPin(newPin, enableBiometric = true)
                            onUnlocked()
                        } else {
                            triggerShakeAndError("PINs do not match. Try again.")
                            coroutineScope.launch {
                                delay(600L)
                                setupStep = SetupStep.CREATE_PIN
                                tempFirstPin = ""
                                errorMessage = null
                            }
                        }
                    }
                }
            } else {
                // Verify existing PIN
                coroutineScope.launch {
                    delay(150L)
                    if (viewModel.settingsPrefs.verifyPin(newPin)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnlocked()
                    } else {
                        triggerShakeAndError("Incorrect PIN. Try again.")
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty() && !isError) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    // ROOT LAYOUT
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1215),
                        BgDark,
                        Color(0xFF070709)
                    ),
                    radius = 1400f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (onNavigateBack != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(SurfaceSecondary, CircleShape)
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clip(CircleShape)
                            .clickable { onNavigateBack.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // SHIELD & PADLOCK ICON BADGE (RED GLOW)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentRed.copy(alpha = 0.28f),
                                AccentRedDark.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = AccentRed.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(38.dp)
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(17.dp)
                        .padding(top = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TITLE
            val titleText = when {
                !isSetupMode -> "Enter Security PIN"
                setupStep == SetupStep.CREATE_PIN -> "Create Security PIN"
                else -> "Confirm Security PIN"
            }
            Text(
                text = titleText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // SUBTITLE / ENCRYPTED BADGE
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentRed,
                    textAlign = TextAlign.Center
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSetupMode) "Protect your recordings with a 4-digit PIN" else "Protected Storage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                    if (!isSetupMode) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = AccentRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = AccentRed.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ENCRYPTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 PIN INDICATOR DOTS (WITH SHAKE ANIMATION)
            Row(
                modifier = Modifier
                    .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isError -> AccentRedDark
                            isFilled -> AccentRed
                            else -> SurfacePrimary
                        },
                        label = "dot_color"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isError -> AccentRed
                            isFilled -> AccentRed
                            else -> BorderSubtle
                        },
                        label = "border_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(dotColor, CircleShape)
                            .border(1.5.dp, borderColor, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3x4 KEYPAD GRID (NO EMOJIS - USES VECTOR ICONS)
            val keypadRows = listOf(
                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("BIO" to "", "0" to "", "DEL" to "")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for ((key, sub) in row) {
                            when (key) {
                                "BIO" -> {
                                    // BIOMETRIC BUTTON
                                    if (!isSetupMode) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(
                                                    color = AccentRed.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = AccentRed.copy(alpha = 0.35f),
                                                    shape = CircleShape
                                                )
                                                .clip(CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    authenticateWithBiometrics()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = AccentRed,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    } else {
                                        // Invisible spacer in setup mode
                                        Spacer(modifier = Modifier.size(64.dp))
                                    }
                                }
                                "DEL" -> {
                                    // BACKSPACE BUTTON
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable { handleBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    // NUMERIC KEY BUTTON
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(SurfaceSecondary, CircleShape)
                                            .border(1.dp, BorderSubtle, CircleShape)
                                            .clip(CircleShape)
                                            .clickable { handleDigitPress(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = key,
                                                fontSize = 25.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                lineHeight = 26.sp
                                            )
                                            if (sub.isNotEmpty()) {
                                                Text(
                                                    text = sub,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextMuted,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
