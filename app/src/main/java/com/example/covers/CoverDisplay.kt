package com.example.covers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CoverDisplay(
    cover: CoverConfig,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    durationSeconds: Long = 0L,
    isCardPreview: Boolean = false
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Date()) }
    var batteryPercent by remember { mutableIntStateOf(85) }

    val hasClock = cover.clock.show
    val hasDate = cover.date.show
    val hasBattery = cover.battery.show
    val hasTimer = isRecording && cover.showRecordingTimer && !isCardPreview
    val hasVisibleElements = hasClock || hasDate || hasBattery || hasTimer
    val needsPerSecondUpdates = hasClock && cover.clock.showSecondsHand &&
        (cover.clock.mode == ClockMode.ANALOG || cover.clock.mode == ClockMode.HYBRID)

    LaunchedEffect(needsPerSecondUpdates, hasVisibleElements) {
        if (!hasVisibleElements) return@LaunchedEffect
        while (true) {
            currentTime = Date()
            delay(if (needsPerSecondUpdates) 1000L else 10000L)
        }
    }

    DisposableEffect(hasBattery, context) {
        if (!hasBattery) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level >= 0 && scale > 0) {
                        batteryPercent = (level * 100) / scale
                    }
                }
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = context.registerReceiver(receiver, filter)
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100) / scale
                }
            }
            onDispose {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {}
            }
        }
    }

    val density = LocalDensity.current
    val maxDriftPx = with(density) { 2.5.dp.toPx() }
    val driftCycle = if (!isRecording && !isCardPreview && cover.burnInProtection && hasVisibleElements) {
        ((currentTime.time / 60000) % 8).toInt()
    } else 0

    val (driftXPx, driftYPx) = remember(driftCycle, maxDriftPx, cover.burnInProtection, hasVisibleElements) {
        if (!cover.burnInProtection || !hasVisibleElements || isRecording || isCardPreview) {
            0f to 0f
        } else {
            val diag = (maxDriftPx * 0.7071f)
            when (driftCycle) {
                0 -> 0f to -maxDriftPx
                1 -> diag to -diag
                2 -> maxDriftPx to 0f
                3 -> diag to diag
                4 -> 0f to maxDriftPx
                5 -> -diag to diag
                6 -> -maxDriftPx to 0f
                7 -> -diag to -diag
                else -> 0f to 0f
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // LAYER 1: Background rendering
        CoverBackgroundLayer(bgConfig = cover.backgroundConfig)

        // LAYER 2: Optional subtle Vignette effect
        if (cover.vignetteLevel != VignetteLevel.OFF) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = cover.vignetteLevel.intensity * 0.85f)
                            )
                        )
                    )
            )
        }

        // LAYER 3: Optional tiny recording timer in top corner if enabled
        if (isRecording && cover.showRecordingTimer && !isCardPreview) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 20.dp)
                    .align(Alignment.TopStart)
            ) {
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Speaker slit indicator for Pure Black preview
        if (isCardPreview && !cover.showClock && !cover.showDate && !cover.showBattery &&
            cover.background == CoverBackground.PURE_BLACK && cover.presetId == CoverPresetId.PURE_BLACK
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(22.dp, 2.5.dp)
                    .background(Color(0xFF222222), RoundedCornerShape(2.dp))
            )
        }

        val formattedTimeStr = remember(currentTime) {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(currentTime)
        }

        // LAYER 4: Preset-specific content or Unified element layout engine
        when (cover.presetId) {
            CoverPresetId.SYSTEM_STATUS -> {
                SystemStatusCoverContent(
                    isCardPreview = isCardPreview,
                    batteryPercent = batteryPercent,
                    currentTime = formattedTimeStr,
                    driftXPx = driftXPx,
                    driftYPx = driftYPx
                )
            }
            CoverPresetId.DASHCAM_HUD -> {
                DashcamHudCoverContent(
                    isCardPreview = isCardPreview,
                    isRecording = isRecording,
                    currentTime = formattedTimeStr,
                    driftXPx = driftXPx,
                    driftYPx = driftYPx
                )
            }
            CoverPresetId.TIMER -> {
                TimerStopwatchCoverContent(
                    isCardPreview = isCardPreview,
                    isRecording = isRecording,
                    durationSeconds = durationSeconds,
                    driftXPx = driftXPx,
                    driftYPx = driftYPx
                )
            }
            else -> {
                CoverElementsEngine(
                    cover = cover,
                    currentTime = currentTime,
                    batteryPercent = batteryPercent,
                    isCardPreview = isCardPreview,
                    driftXPx = driftXPx,
                    driftYPx = driftYPx
                )
            }
        }
    }
}

@Composable
fun CoverBackgroundLayer(
    bgConfig: BackgroundConfig,
    modifier: Modifier = Modifier
) {
    when (bgConfig.type) {
        CoverBackground.PURE_BLACK -> {
            Box(modifier = modifier.fillMaxSize().background(Color.Black))
        }
        CoverBackground.VERY_DARK_GRAY -> {
            val factor = (bgConfig.brightnessPercent.coerceIn(1, 100) / 100f).coerceIn(0.2f, 1f)
            val baseColor = Color(0xFF10131A)
            val c = Color(
                red = (baseColor.red * factor).coerceIn(0f, 1f),
                green = (baseColor.green * factor).coerceIn(0f, 1f),
                blue = (baseColor.blue * factor).coerceIn(0f, 1f),
                alpha = bgConfig.opacity.coerceIn(0.1f, 1f)
            )
            Box(modifier = modifier.fillMaxSize().background(c))
        }
        CoverBackground.DEEP_NAVY -> {
            val factor = (bgConfig.brightnessPercent.coerceIn(1, 100) / 100f).coerceIn(0.2f, 1f)
            val baseColor = Color(0xFF0A1128)
            val c = Color(
                red = (baseColor.red * factor).coerceIn(0f, 1f),
                green = (baseColor.green * factor).coerceIn(0f, 1f),
                blue = (baseColor.blue * factor).coerceIn(0f, 1f),
                alpha = bgConfig.opacity.coerceIn(0.1f, 1f)
            )
            Box(modifier = modifier.fillMaxSize().background(c))
        }
        CoverBackground.CUSTOM_COLOR -> {
            val rawColor = CoverElementColors.parseColor(bgConfig.customColorHex, Color.Black)
            val factor = (bgConfig.brightnessPercent.coerceIn(1, 100) / 100f).coerceIn(0.05f, 1f)
            val finalColor = Color(
                red = (rawColor.red * factor).coerceIn(0f, 1f),
                green = (rawColor.green * factor).coerceIn(0f, 1f),
                blue = (rawColor.blue * factor).coerceIn(0f, 1f),
                alpha = bgConfig.opacity.coerceIn(0.1f, 1f)
            )
            Box(modifier = modifier.fillMaxSize().background(finalColor))
        }
        CoverBackground.GRADIENT -> {
            val startColor = CoverElementColors.parseColor(bgConfig.gradientStartHex, Color(0xFF1C1B33))
            val endColor = CoverElementColors.parseColor(bgConfig.gradientEndHex, Color(0xFF080811))
            val factor = (bgConfig.brightnessPercent.coerceIn(1, 100) / 100f).coerceIn(0.2f, 1f)
            val alpha = bgConfig.opacity.coerceIn(0.1f, 1f)
            val finalStart = Color(
                red = (startColor.red * factor).coerceIn(0f, 1f),
                green = (startColor.green * factor).coerceIn(0f, 1f),
                blue = (startColor.blue * factor).coerceIn(0f, 1f),
                alpha = alpha
            )
            val finalEnd = Color(
                red = (endColor.red * factor).coerceIn(0f, 1f),
                green = (endColor.green * factor).coerceIn(0f, 1f),
                blue = (endColor.blue * factor).coerceIn(0f, 1f),
                alpha = alpha
            )
            val brush = when (bgConfig.gradientDirection) {
                GradientDirection.TOP_TO_BOTTOM -> Brush.verticalGradient(listOf(finalStart, finalEnd))
                GradientDirection.LEFT_TO_RIGHT -> Brush.horizontalGradient(listOf(finalStart, finalEnd))
                GradientDirection.DIAGONAL_TR -> Brush.linearGradient(
                    listOf(finalStart, finalEnd),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
                GradientDirection.DIAGONAL_TL -> Brush.linearGradient(
                    listOf(finalEnd, finalStart),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            }
            Box(modifier = modifier.fillMaxSize().background(brush))
        }
        CoverBackground.IMAGE -> {
            val bitmap: Bitmap? = remember(bgConfig.imagePath) {
                CoverImageManager.loadCoverBitmap(bgConfig.imagePath)
            }
            if (bitmap != null) {
                val blurMod = if (bgConfig.blurLevel != BlurLevel.NONE) {
                    Modifier.blur(bgConfig.blurLevel.radiusDp.dp)
                } else Modifier

                Box(modifier = modifier.fillMaxSize().then(blurMod)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (!bgConfig.imageCropScale) ContentScale.Crop else ContentScale.Fit,
                        alpha = bgConfig.opacity.coerceIn(0.1f, 1f)
                    )
                }
                val dimAlpha = bgConfig.imageDimmingPercent.coerceIn(0, 100) / 100f
                if (dimAlpha > 0f) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
                }
            } else {
                Box(modifier = modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}

@Composable
fun CoverElementsEngine(
    cover: CoverConfig,
    currentTime: Date,
    batteryPercent: Int,
    isCardPreview: Boolean,
    driftXPx: Float,
    driftYPx: Float
) {
    val scale = if (isCardPreview) 0.38f else 1.0f
    val clockConfig = cover.clock
    val dateConfig = cover.date
    val batteryConfig = cover.battery

    data class PlacementKey(val vertical: ElementVerticalPosition, val horizontal: ElementHorizontalAlignment)

    val activePlacements = linkedSetOf<PlacementKey>()
    if (clockConfig.show) activePlacements.add(PlacementKey(clockConfig.verticalPosition, clockConfig.horizontalAlignment))
    if (dateConfig.show) activePlacements.add(PlacementKey(dateConfig.verticalPosition, dateConfig.horizontalAlignment))
    if (batteryConfig.show) activePlacements.add(PlacementKey(batteryConfig.verticalPosition, batteryConfig.horizontalAlignment))

    for (placement in activePlacements) {
        val align = resolveAlignment(placement.vertical, placement.horizontal)
        val colHorizontalAlign = resolveColumnAlignment(placement.horizontal)

        val boxModifier = if (isCardPreview) {
            val topPad = if (placement.vertical == ElementVerticalPosition.TOP) 16.dp else 4.dp
            val bottomPad = if (placement.vertical == ElementVerticalPosition.BOTTOM) 12.dp else 4.dp
            Modifier.padding(start = 8.dp, end = 8.dp, top = topPad, bottom = bottomPad)
        } else {
            val topPad = if (placement.vertical == ElementVerticalPosition.TOP) 88.dp else 16.dp
            val bottomPad = if (placement.vertical == ElementVerticalPosition.BOTTOM) 64.dp else 16.dp
            Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = topPad, bottom = bottomPad)
                .graphicsLayer {
                    translationX = driftXPx
                    translationY = driftYPx
                }
        }

        Box(
            modifier = Modifier.fillMaxSize().then(boxModifier),
            contentAlignment = align
        ) {
            Column(
                horizontalAlignment = colHorizontalAlign,
                verticalArrangement = Arrangement.spacedBy((4 * scale).dp)
            ) {
                if (clockConfig.show && clockConfig.verticalPosition == placement.vertical && clockConfig.horizontalAlignment == placement.horizontal) {
                    RenderClockElement(config = clockConfig, currentTime = currentTime, scale = scale)
                }
                if (dateConfig.show && dateConfig.verticalPosition == placement.vertical && dateConfig.horizontalAlignment == placement.horizontal) {
                    RenderDateElement(config = dateConfig, currentTime = currentTime, scale = scale)
                }
                if (batteryConfig.show && batteryConfig.verticalPosition == placement.vertical && batteryConfig.horizontalAlignment == placement.horizontal) {
                    RenderBatteryElement(config = batteryConfig, batteryPercent = batteryPercent, scale = scale)
                }
            }
        }
    }
}

@Composable
fun RenderClockElement(
    config: ClockElementConfig,
    currentTime: Date,
    scale: Float
) {
    when (config.mode) {
        ClockMode.DIGITAL -> RenderDigitalClockElement(config, currentTime, scale)
        ClockMode.ANALOG -> RenderAnalogClockElement(config, currentTime, scale)
        ClockMode.HYBRID -> RenderHybridClockElement(config, currentTime, scale)
    }
}

@Composable
fun RenderDigitalClockElement(
    config: ClockElementConfig,
    currentTime: Date,
    scale: Float,
    overrideSizeSp: Int? = null,
    overrideOffset: Dp? = null
) {
    val pattern = if (config.style == ClockStyle.MINIMAL) {
        if (config.is24Hour) "HH mm" else "h mm"
    } else {
        if (config.is24Hour) "HH:mm" else "h:mm"
    }
    val timeStr = SimpleDateFormat(pattern, Locale.getDefault()).format(currentTime)
    val fontWt = when (config.style) {
        ClockStyle.THIN -> FontWeight.Light
        ClockStyle.BOLD -> FontWeight.SemiBold
        ClockStyle.MINIMAL -> FontWeight.Light
        else -> FontWeight.Normal
    }
    val letterSpace = when (config.style) {
        ClockStyle.THIN -> (1.0f * scale).sp
        ClockStyle.BOLD -> (2.0f * scale).sp
        ClockStyle.MINIMAL -> (4.0f * scale).sp
        else -> (1.0f * scale).sp
    }
    val color = CoverElementColors.parseColor(config.colorHex, Color.White)
    val effectiveSize = overrideSizeSp ?: config.sizeSp
    val scaledSize = (effectiveSize * scale).sp
    val yOffset = overrideOffset ?: (config.yOffsetDp * scale).dp

    Text(
        text = timeStr,
        modifier = Modifier.offset(y = yOffset),
        color = color.copy(alpha = config.opacity.coerceIn(0.05f, 1f)),
        fontSize = scaledSize,
        fontWeight = fontWt,
        fontFamily = FontFamily.Monospace,
        letterSpacing = letterSpace,
        textAlign = resolveTextAlign(config.horizontalAlignment)
    )
}

@Composable
fun RenderAnalogClockElement(
    config: ClockElementConfig,
    currentTime: Date,
    scale: Float,
    overrideOffset: Dp? = null
) {
    val density = LocalDensity.current
    val sizeDp = (config.sizeSp.coerceAtLeast(40) * scale).dp
    val scaledYOffset = overrideOffset ?: (config.yOffsetDp * scale).dp
    val color = CoverElementColors.parseColor(config.colorHex, Color.White)
    val alpha = config.opacity.coerceIn(0.05f, 1f)

    val cal = Calendar.getInstance().apply { time = currentTime }
    val hours = cal.get(Calendar.HOUR)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)

    val hourAngle = ((hours % 12) + (minutes / 60.0f) + (seconds / 3600.0f)) * 30.0f
    val minuteAngle = (minutes + (seconds / 60.0f)) * 6.0f
    val secondAngle = seconds * 6.0f

    Canvas(
        modifier = Modifier
            .size(sizeDp)
            .offset(y = scaledYOffset)
    ) {
        val radius = size.minDimension / 2f
        val clockCenter = Offset(size.width / 2f, size.height / 2f)

        // Outer dial ring
        drawCircle(
            color = color.copy(alpha = alpha * 0.4f),
            radius = radius,
            center = clockCenter,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx() * scale)
        )

        // 12 Hour tick marks
        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30.0) - 90.0)
            val isMajor = (i % 3 == 0)
            val tickLen = (if (isMajor) 6.dp else 3.5.dp).toPx() * scale
            val tickStroke = (if (isMajor) 2.dp else 1.dp).toPx() * scale
            val startX = (clockCenter.x + (radius - tickLen) * cos(angleRad)).toFloat()
            val startY = (clockCenter.y + (radius - tickLen) * sin(angleRad)).toFloat()
            val endX = (clockCenter.x + radius * cos(angleRad)).toFloat()
            val endY = (clockCenter.y + radius * sin(angleRad)).toFloat()

            drawLine(
                color = color.copy(alpha = if (isMajor) alpha * 0.8f else alpha * 0.35f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickStroke,
                cap = StrokeCap.Round
            )
        }

        // Hour hand
        val hourRad = Math.toRadians((hourAngle - 90.0))
        val hourLen = radius * 0.52f
        drawLine(
            color = color.copy(alpha = alpha * 0.95f),
            start = clockCenter,
            end = Offset(
                (clockCenter.x + hourLen * cos(hourRad)).toFloat(),
                (clockCenter.y + hourLen * sin(hourRad)).toFloat()
            ),
            strokeWidth = 3.dp.toPx() * scale,
            cap = StrokeCap.Round
        )

        // Minute hand
        val minuteRad = Math.toRadians((minuteAngle - 90.0))
        val minuteLen = radius * 0.74f
        drawLine(
            color = color.copy(alpha = alpha * 0.95f),
            start = clockCenter,
            end = Offset(
                (clockCenter.x + minuteLen * cos(minuteRad)).toFloat(),
                (clockCenter.y + minuteLen * sin(minuteRad)).toFloat()
            ),
            strokeWidth = 2.dp.toPx() * scale,
            cap = StrokeCap.Round
        )

        // Optional second hand
        if (config.showSecondsHand) {
            val secondRad = Math.toRadians((secondAngle - 90.0))
            val secondLen = radius * 0.85f
            drawLine(
                color = Color(0xFFE53935).copy(alpha = alpha),
                start = clockCenter,
                end = Offset(
                    (clockCenter.x + secondLen * cos(secondRad)).toFloat(),
                    (clockCenter.y + secondLen * sin(secondRad)).toFloat()
                ),
                strokeWidth = 1.dp.toPx() * scale,
                cap = StrokeCap.Round
            )
        }

        // Center pin
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = 3.dp.toPx() * scale,
            center = clockCenter
        )
    }
}

@Composable
fun RenderHybridClockElement(
    config: ClockElementConfig,
    currentTime: Date,
    scale: Float
) {
    Column(
        horizontalAlignment = resolveColumnAlignment(config.horizontalAlignment),
        verticalArrangement = Arrangement.spacedBy((4 * scale).dp)
    ) {
        RenderAnalogClockElement(config = config, currentTime = currentTime, scale = scale)
        RenderDigitalClockElement(
            config = config,
            currentTime = currentTime,
            scale = scale * 0.65f,
            overrideOffset = 0.dp
        )
    }
}

@Composable
fun RenderDateElement(
    config: DateElementConfig,
    currentTime: Date,
    scale: Float
) {
    val dateStr = formatCoverDate(config.format, currentTime)
    val color = CoverElementColors.parseColor(config.colorHex, Color.White)
    val scaledSize = (config.sizeSp * scale).sp
    val yOffset = (config.yOffsetDp * scale).dp

    Text(
        text = dateStr,
        modifier = Modifier.offset(y = yOffset),
        color = color.copy(alpha = config.opacity.coerceIn(0.05f, 1f)),
        fontSize = scaledSize,
        fontWeight = FontWeight.Normal,
        textAlign = resolveTextAlign(config.horizontalAlignment)
    )
}

@Composable
fun RenderBatteryElement(
    config: BatteryElementConfig,
    batteryPercent: Int,
    scale: Float
) {
    val color = CoverElementColors.parseColor(config.colorHex, Color.White)
    val scaledSize = (config.sizeSp * scale).sp
    val yOffset = (config.yOffsetDp * scale).dp

    Row(
        modifier = Modifier.offset(y = yOffset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (config.showIcon) {
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                modifier = Modifier.size((config.sizeSp * scale * 1.1f).dp),
                tint = color.copy(alpha = config.opacity.coerceIn(0.05f, 1f))
            )
        }
        if (config.showPercent) {
            Text(
                text = "${if (config.showIcon) " " else ""}$batteryPercent%",
                color = color.copy(alpha = config.opacity.coerceIn(0.05f, 1f)),
                fontSize = scaledSize,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun formatCoverDate(format: DateFormatStyle, date: Date): String {
    val pattern = when (format) {
        DateFormatStyle.DAY_MONTH_YEAR -> "dd MMM yyyy"
        DateFormatStyle.MONTH_DAY_YEAR -> "MMM dd, yyyy"
        DateFormatStyle.SLASH_NUMERIC -> "dd/MM/yyyy"
        DateFormatStyle.FULL_DAY -> "EEEE, MMMM d"
        DateFormatStyle.FULL -> "EEEE, MMMM d, yyyy"
        DateFormatStyle.SHORT -> "dd/MM/yyyy"
        DateFormatStyle.MEDIUM -> "dd MMM yyyy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

fun resolveAlignment(
    vertical: ElementVerticalPosition,
    horizontal: ElementHorizontalAlignment
): Alignment {
    return when (vertical) {
        ElementVerticalPosition.TOP -> when (horizontal) {
            ElementHorizontalAlignment.START -> Alignment.TopStart
            ElementHorizontalAlignment.CENTER -> Alignment.TopCenter
            ElementHorizontalAlignment.END -> Alignment.TopEnd
        }
        ElementVerticalPosition.CENTER -> when (horizontal) {
            ElementHorizontalAlignment.START -> Alignment.CenterStart
            ElementHorizontalAlignment.CENTER -> Alignment.Center
            ElementHorizontalAlignment.END -> Alignment.CenterEnd
        }
        ElementVerticalPosition.BOTTOM -> when (horizontal) {
            ElementHorizontalAlignment.START -> Alignment.BottomStart
            ElementHorizontalAlignment.CENTER -> Alignment.BottomCenter
            ElementHorizontalAlignment.END -> Alignment.BottomEnd
        }
    }
}

fun resolveColumnAlignment(
    horizontal: ElementHorizontalAlignment
): Alignment.Horizontal {
    return when (horizontal) {
        ElementHorizontalAlignment.START -> Alignment.Start
        ElementHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        ElementHorizontalAlignment.END -> Alignment.End
    }
}

fun resolveTextAlign(
    horizontal: ElementHorizontalAlignment
): TextAlign {
    return when (horizontal) {
        ElementHorizontalAlignment.START -> TextAlign.Start
        ElementHorizontalAlignment.CENTER -> TextAlign.Center
        ElementHorizontalAlignment.END -> TextAlign.End
    }
}

// =========================================================================
// SYSTEM STATUS / BATTERY MONITOR PRESET IMPLEMENTATION
// =========================================================================

@Composable
fun SystemStatusCoverContent(
    isCardPreview: Boolean,
    batteryPercent: Int,
    currentTime: String,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    if (isCardPreview) {
        SystemStatusCoverPreview(batteryPercent = batteryPercent, currentTime = currentTime, modifier = modifier)
    } else {
        SystemStatusCoverFullScreen(
            batteryPercent = batteryPercent,
            currentTime = currentTime,
            driftXPx = driftXPx,
            driftYPx = driftYPx,
            modifier = modifier
        )
    }
}

@Composable
private fun SystemStatusCoverPreview(
    batteryPercent: Int,
    currentTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTime.ifEmpty { "12:00" },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$batteryPercent%",
                color = Color(0xFF00E5FF),
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Center Gauge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$batteryPercent%",
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            // Mini battery bar
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF1E2836), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((batteryPercent / 100f).coerceIn(0.05f, 1f))
                        .background(Color(0xFF00E5FF), RoundedCornerShape(2.dp))
                )
            }
        }

        // Mini status pills
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "STORAGE OK",
                color = Color(0xFF00E676),
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "SYSTEM ACTIVE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 5.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SystemStatusCoverFullScreen(
    batteryPercent: Int,
    currentTime: String,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .graphicsLayer {
                translationX = driftXPx
                translationY = driftYPx
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM MONITOR",
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = currentTime.ifEmpty { "12:00" },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Big Battery Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$batteryPercent%",
                color = Color(0xFF00E5FF),
                fontSize = 64.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "BATTERY STATUS · OPTIMAL",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            // Segmented battery progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .background(Color(0xFF16202C), RoundedCornerShape(5.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((batteryPercent / 100f).coerceIn(0.05f, 1f))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                            ),
                            shape = RoundedCornerShape(5.dp)
                        )
                )
            }
        }

        // Diagnostics Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SystemStatusInfoRow(title = "ENERGY SAVER", value = "AMOLED PIXELS OFF", highlight = Color(0xFF00E676))
            SystemStatusInfoRow(title = "THERMAL STATE", value = "31.4°C · NORMAL", highlight = Color(0xFF00E5FF))
            SystemStatusInfoRow(title = "MEMORY PROFILE", value = "OPTIMIZED", highlight = Color.White.copy(alpha = 0.8f))
            SystemStatusInfoRow(title = "BACKGROUND REC", value = "RUNNING CONTINUOUS", highlight = Color(0xFF00E676))
        }
    }
}

@Composable
private fun SystemStatusInfoRow(
    title: String,
    value: String,
    highlight: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1017), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = highlight,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// =========================================================================
// DASHCAM / HUD MODE PRESET IMPLEMENTATION
// =========================================================================

@Composable
fun DashcamHudCoverContent(
    isCardPreview: Boolean,
    isRecording: Boolean,
    currentTime: String,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    if (isCardPreview) {
        DashcamHudCoverPreview(currentTime = currentTime, modifier = modifier)
    } else {
        DashcamHudCoverFullScreen(
            isRecording = isRecording,
            currentTime = currentTime,
            driftXPx = driftXPx,
            driftYPx = driftYPx,
            modifier = modifier
        )
    }
}

@Composable
private fun DashcamHudCoverPreview(
    currentTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini Compass
        Text(
            text = "▲ N 045°",
            color = Color(0xFFFFB300),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        // Mini Speed
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "0",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "KM/H",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Mini Rec status
        Text(
            text = "● REC 1080P",
            color = Color(0xFFFF5252),
            fontSize = 6.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class DashcamSensorState(
    val heading: Int = 45,
    val speedKmh: Int = 0,
    val pitch: Int = 0,
    val roll: Int = 0,
    val gForce: Float = 1.0f
)

@Composable
private fun rememberDashcamSensorState(): DashcamSensorState {
    val context = LocalContext.current
    var sensorState by remember { mutableStateOf(DashcamSensorState()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            return@DisposableEffect onDispose {}
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        var lastHeading = 45
        var lastPitch = 0
        var lastRoll = 0
        var smoothedG = 1.0f
        var currentSpeed = 0f
        var lastTimestamp = System.currentTimeMillis()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (azimuthDeg < 0) azimuthDeg += 360f

                        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toInt()
                        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toInt()

                        lastHeading = azimuthDeg.toInt().coerceIn(0, 359)
                        lastPitch = pitchDeg.coerceIn(-90, 90)
                        lastRoll = rollDeg.coerceIn(-180, 180)

                        sensorState = sensorState.copy(
                            heading = lastHeading,
                            pitch = lastPitch,
                            roll = lastRoll
                        )
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = event.values[0]
                        val ay = event.values[1]
                        val az = event.values[2]
                        val totalAcc = sqrt(ax * ax + ay * ay + az * az)
                        val rawG = totalAcc / SensorManager.GRAVITY_EARTH
                        smoothedG = smoothedG * 0.82f + rawG * 0.18f

                        if (linearAccelSensor == null) {
                            val now = System.currentTimeMillis()
                            val dt = ((now - lastTimestamp).coerceIn(10L, 500L)) / 1000f
                            lastTimestamp = now

                            val linMag = abs(rawG - 1.0f) * 9.80665f
                            if (linMag > 0.35f) {
                                currentSpeed = (currentSpeed + linMag * dt * 2.8f).coerceIn(0f, 140f)
                            } else {
                                currentSpeed = (currentSpeed - 4.5f * dt).coerceAtLeast(0f)
                            }

                            sensorState = sensorState.copy(
                                gForce = smoothedG,
                                speedKmh = currentSpeed.toInt()
                            )
                        } else {
                            sensorState = sensorState.copy(gForce = smoothedG)
                        }
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        val lx = event.values[0]
                        val ly = event.values[1]
                        val lz = event.values[2]
                        val linMag = sqrt(lx * lx + ly * ly + lz * lz)

                        val now = System.currentTimeMillis()
                        val dt = ((now - lastTimestamp).coerceIn(10L, 500L)) / 1000f
                        lastTimestamp = now

                        if (linMag > 0.35f) {
                            currentSpeed = (currentSpeed + linMag * dt * 3.5f).coerceIn(0f, 140f)
                        } else {
                            currentSpeed = (currentSpeed - 4.0f * dt).coerceAtLeast(0f)
                        }

                        sensorState = sensorState.copy(
                            speedKmh = currentSpeed.toInt()
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        linearAccelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return sensorState
}

private fun formatCompassTape(heading: Int): String {
    val cardinal = getShortCardinal(heading)
    val leftAngle = (heading - 45 + 360) % 360
    val rightAngle = (heading + 45) % 360
    val leftLabel = getShortCardinal(leftAngle)
    val rightLabel = getShortCardinal(rightAngle)
    return String.format(Locale.US, "%s · · · ▲ %s %03d° · · · %s", leftLabel, cardinal, heading, rightLabel)
}

private fun getShortCardinal(angle: Int): String {
    return when (angle) {
        in 338..360, in 0..22 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        in 293..337 -> "NW"
        else -> "N"
    }
}

@Composable
private fun DashcamHudCoverFullScreen(
    isRecording: Boolean,
    currentTime: String,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    val sensorState = rememberDashcamSensorState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .graphicsLayer {
                translationX = driftXPx
                translationY = driftYPx
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Compass Tape
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatCompassTape(sensorState.heading),
                color = Color(0xFFFFB300),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(Color(0xFFFFB300).copy(alpha = 0.4f))
            )
        }

        // Center Speedometer & HUD Reticle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${sensorState.speedKmh}",
                    color = Color.White,
                    fontSize = 84.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "KM/H",
                    color = Color(0xFFFFB300),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            Text(
                text = "SENSOR LOCK · 3D ACTIVE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Bottom Telemetry & Recording Status
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isRecording) "● REC FHD 1080P" else "● STANDBY",
                    color = if (isRecording) Color(0xFFFF5252) else Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentTime.ifEmpty { "00:00" },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val pitchSign = if (sensorState.pitch >= 0) "+${sensorState.pitch}" else "${sensorState.pitch}"
                val rollSign = if (sensorState.roll >= 0) "+${sensorState.roll}" else "${sensorState.roll}"
                Text(
                    text = "PITCH: ${pitchSign}° · ROLL: ${rollSign}°",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.US, "G-FORCE: %.2fG", sensorState.gForce),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// =========================================================================
// STOPWATCH / TIMER PRESET IMPLEMENTATION
// =========================================================================

@Composable
fun TimerStopwatchCoverContent(
    isCardPreview: Boolean,
    isRecording: Boolean,
    durationSeconds: Long,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    if (isCardPreview) {
        TimerStopwatchCoverPreview(modifier = modifier)
    } else {
        TimerStopwatchCoverFullScreen(
            isRecording = isRecording,
            durationSeconds = durationSeconds,
            driftXPx = driftXPx,
            driftYPx = driftYPx,
            modifier = modifier
        )
    }
}

@Composable
private fun TimerStopwatchCoverPreview(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TIMER",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "00:05:32",
                color = Color(0xFF00E5FF),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = ".40",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF222222), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF331515), CircleShape)
            )
        }
    }
}

@Composable
private fun TimerStopwatchCoverFullScreen(
    isRecording: Boolean,
    durationSeconds: Long,
    driftXPx: Float,
    driftYPx: Float,
    modifier: Modifier = Modifier
) {
    val hours = durationSeconds / 3600
    val mins = (durationSeconds % 3600) / 60
    val secs = durationSeconds % 60
    val timerString = if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "00:%02d:%02d", mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 36.dp)
            .graphicsLayer {
                translationX = driftXPx
                translationY = driftYPx
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "STOPWATCH",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = if (isRecording) "SESSION ACTIVE" else "READY",
                color = if (isRecording) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Giant Stopwatch Numerals
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = timerString,
                color = Color(0xFF00E5FF),
                fontSize = 52.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = ".82",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Lap Split Times
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerLapRow(lapNum = 2, time = timerString, split = "+01:14")
            TimerLapRow(lapNum = 1, time = "00:03:45", split = "+03:45")
        }

        // Minimalist Stopwatch Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF18181A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LAP",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stop / Pause Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF2E1212), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STOP",
                    color = Color(0xFFEF5350),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TimerLapRow(
    lapNum: Int,
    time: String,
    split: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "LAP $lapNum",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = time,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = split,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
