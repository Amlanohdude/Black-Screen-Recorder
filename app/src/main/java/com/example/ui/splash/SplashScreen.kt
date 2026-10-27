package com.example.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startStrokeDraw by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var powerOnSensor by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startStrokeDraw = true
        delay(600L)
        showDetails = true
        delay(250L)
        powerOnSensor = true
        delay(200L)
        showTitle = true
        delay(1100L)
        isExiting = true
        delay(320L)
        onSplashFinished()
    }

    val strokeProgress by animateFloatAsState(
        targetValue = if (startStrokeDraw) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "stroke_draw_progress"
    )

    val detailsAlpha by animateFloatAsState(
        targetValue = if (showDetails && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "details_alpha"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isExiting) 0.8f else if (powerOnSensor) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "dot_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sensor_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "title_alpha"
    )

    val exitScale by animateFloatAsState(
        targetValue = if (isExiting) 1.06f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "exit_scale"
    )

    val exitAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "exit_alpha"
    )

    val bezelPath = remember {
        PathParser().parsePathString("M124.358,26.1193L211.696,26.0505L234.417,26.024C246.593,26.0118 257.36,25.1738 266.83,34.6578C273.747,41.5873 276.173,48.5115 276.265,58.132L276.41,232.814L276.42,287.015L276.407,303.223C276.395,306.675 276.487,311.138 275.997,314.508C274.27,326.408 264.602,336.625 252.837,339.07C249.525,339.758 246.509,339.775 243.132,339.807L160.446,340.09L133.231,340.212C128.121,340.23 120.387,340.497 115.603,339.707C110.056,338.795 104.897,336.28 100.76,332.475C94.3947,326.608 91.1177,318.96 90.961,310.395C90.8367,303.602 90.9132,296.702 90.92,289.905L90.9082,254.005L90.9157,143.917L90.9317,83.2903L90.9615,65.0985C90.9903,53.5648 90.956,44.8663 99.6162,35.857C106.997,28.1793 114.165,26.39 124.358,26.1193Z").toPath()
    }
    val pathMeasure = remember { PathMeasure() }
    val partialBezelPath = remember { Path() }

    val innerScreenPath = remember {
        PathParser().parsePathString("M238.591,35.373C246.897,35.2908 253.958,35.0553 260.467,41.4328C265.31,46.179 266.715,51.2903 266.767,57.8048L266.785,232.809L266.802,285.777L266.823,301.633C266.833,310.758 267.722,317.19 260.802,324.237C253.267,331.915 244.369,330.707 234.688,330.723L211.38,330.76L128.557,331.087C120.455,331.193 113.026,331.223 106.782,325.133C101.976,320.443 100.395,315.03 100.377,308.517C100.329,290.542 100.331,272.548 100.316,254.575L100.383,138.325L100.425,82.6275L100.435,65.7913C100.439,62.189 100.314,57.961 100.754,54.445C101.353,49.696 103.504,45.278 106.872,41.8778C114.374,34.2468 122.478,35.444 132.202,35.4513L155.038,35.4453L238.591,35.373Z").toPath()
    }
    val speakerPath = remember {
        PathParser().parsePathString("M162.761,45.0783C164.291,45.008 165.883,45.0328 167.419,45.0378C179.732,45.077 192.056,44.904 204.366,45.0845C205.167,45.0963 206.115,45.399 206.87,45.6615C209.991,48.7808 209.56,52.6008 205.03,53.7195C202.177,54.0338 198.953,53.952 196.05,53.936C185.372,53.8778 174.677,54.0965 164.004,53.8475C161.626,53.792 159.232,52.9325 158.693,50.3163C158.461,49.1985 158.699,48.034 159.352,47.0975C160.258,45.7745 161.298,45.3468 162.761,45.0783Z").toPath()
    }
    val rightButtonPath = remember {
        PathParser().parsePathString("M277.87,108.422C283.517,108.794 282.207,114.415 282.205,118.651L282.207,131.384L282.225,142.935C282.228,145.417 282.705,148.395 281.175,150.355C280.195,151.087 278.94,151.758 277.925,150.844C277.64,149.175 277.762,139.8 277.78,137.464C277.845,127.851 277.655,118.014 277.87,108.422Z").toPath()
    }
    val leftButton1Path = remember {
        PathParser().parsePathString("M88.9772,125.075L89.3587,125.271C89.6477,126.178 89.5563,154.456 89.2838,155.537L88.6438,155.791C87.4783,155.708 87.2405,155.572 86.2157,155.116C84.734,153.71 84.764,152.753 84.7685,150.812C84.7835,144.65 84.6877,138.479 84.752,132.317C84.7952,128.186 84.1787,125.916 88.9772,125.075Z").toPath()
    }
    val leftButton2Path = remember {
        PathParser().parsePathString("M88.2458,88.9752C88.7953,88.9992 88.7542,89.0175 89.284,89.147C89.3195,89.2252 89.4755,89.5342 89.4747,89.7187C89.426,99.1285 89.7125,108.659 89.3733,118.048C89.249,118.182 89.1248,118.317 89.0005,118.451C88.1373,118.65 87.6,118.408 86.7343,118.173C86.2288,117.729 85.3267,117.079 85.104,116.435C84.3875,114.365 84.448,92.5065 85.2855,90.9115C85.9003,89.7407 87.074,89.3495 88.2458,88.9752Z").toPath()
    }
    val recLetterR = remember {
        PathParser().parsePathString("M134.27,214.775C139.531,214.749 152.404,214.105 157.132,215.637C166.305,218.609 167.795,231.206 157.782,235.993C160.413,239.873 163.57,245.365 165.791,249.482C163.505,249.711 161.348,249.68 159.054,249.686L154.775,249.678C152.439,245.818 150.037,241.271 147.767,237.285C146.555,237.448 144.95,237.436 143.692,237.462C143.524,241.397 143.567,245.628 143.538,249.587C140.425,249.734 137.307,249.711 134.197,249.516C134.12,237.935 134.144,226.355 134.27,214.775Z").toPath()
    }
    val recLetterRHole = remember {
        PathParser().parsePathString("M143.711,222.209C147.942,222.161 159.07,221.333 153.806,229.153C151.171,230.624 146.636,230.252 143.553,230.234C143.55,227.474 143.577,224.964 143.711,222.209Z").toPath()
    }
    val recLetterE = remember {
        PathParser().parsePathString("M170.545,214.781C178.334,214.639 188.236,214.54 195.957,214.833C195.985,217.451 195.909,219.618 195.762,222.235C190.515,222.353 185.083,222.279 179.82,222.269C179.753,224.164 179.774,226.151 179.765,228.054C184.62,228.04 189.476,228.053 194.332,228.092C194.494,230.625 194.422,232.911 194.323,235.44L179.785,235.458L179.765,241.792C184.749,241.811 191.246,241.614 196.059,241.975C196.198,244.291 196.106,247.17 196.086,249.536C189.312,249.93 177.368,249.826 170.528,249.574C170.34,238.333 170.403,226.027 170.545,214.781Z").toPath()
    }
    val recLetterC = remember {
        PathParser().parsePathString("M215.927,214.086C219.464,213.469 224.963,214.442 227.986,216.449C232.215,219.255 233.767,221.969 234.877,226.691C231.67,226.992 228.767,227.166 225.553,227.327C225.069,226.066 224.82,225.373 223.845,224.378C222.352,222.869 220.304,222.04 218.182,222.084C206.681,222.231 207.407,242.971 218.654,242.275C222.902,242.012 223.743,240.166 225.788,237.128C226.151,236.589 227.53,236.888 228.188,236.949L234.942,237.355C233.6,244.723 228.963,248.961 221.323,250.187C203.603,253.035 194.461,234.325 203.992,220.515C206.681,216.619 211.153,214.711 215.927,214.086Z").toPath()
    }

    val pureOledBlack = Color(0xFF050303)
    val phoneWhite = Color(0xFFF9F9F9)
    val phoneSpeaker = Color(0xFF515152)
    val recRed = Color(0xFFFE1B1E)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pureOledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = exitScale
                scaleY = exitScale
                alpha = exitAlpha
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(width = 170.dp, height = 260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val factor = minOf(size.height / 366f, size.width / 367f)
                    val leftOffset = (size.width - 367f * factor) / 2f
                    val topOffset = (size.height - 366f * factor) / 2f

                    translate(left = leftOffset, top = topOffset) {
                        scale(factor, factor, pivot = Offset.Zero) {
                            // Inner screen fill
                            if (strokeProgress >= 0.85f) {
                                val alpha = ((strokeProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)
                                drawPath(innerScreenPath, color = pureOledBlack.copy(alpha = alpha))
                            }

                            // Bezel stroke
                            pathMeasure.setPath(bezelPath, false)
                            val length = pathMeasure.length
                            partialBezelPath.reset()
                            pathMeasure.getSegment(0f, length * strokeProgress, partialBezelPath, true)
                            drawPath(
                                path = partialBezelPath,
                                color = phoneWhite,
                                style = Stroke(
                                    width = 7f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Details
                            if (detailsAlpha > 0f) {
                                drawPath(speakerPath, color = phoneSpeaker.copy(alpha = detailsAlpha))
                                drawPath(rightButtonPath, color = phoneWhite.copy(alpha = detailsAlpha))
                                drawPath(leftButton1Path, color = phoneWhite.copy(alpha = detailsAlpha))
                                drawPath(leftButton2Path, color = phoneWhite.copy(alpha = detailsAlpha))
                                drawPath(recLetterR, color = phoneWhite.copy(alpha = detailsAlpha))
                                drawPath(recLetterRHole, color = pureOledBlack.copy(alpha = detailsAlpha))
                                drawPath(recLetterE, color = phoneWhite.copy(alpha = detailsAlpha))
                                drawPath(recLetterC, color = phoneWhite.copy(alpha = detailsAlpha))
                            }

                            // Red Sensor dot & pulse
                            if (dotScale > 0f) {
                                val center = Offset(182.5f, 160.5f)
                                val baseRadius = dotScale * 38.5f
                                val pulseRadius = baseRadius + (pulseProgress * 55f)
                                val ringAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.45f * dotScale

                                drawCircle(
                                    color = recRed.copy(alpha = ringAlpha),
                                    radius = pulseRadius,
                                    center = center,
                                    style = Stroke(width = 3.5f)
                                )

                                val glowBrush = Brush.radialGradient(
                                    colors = listOf(
                                        recRed.copy(alpha = dotScale * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = baseRadius * 1.7f
                                )
                                drawCircle(
                                    brush = glowBrush,
                                    radius = baseRadius * 1.7f,
                                    center = center
                                )

                                drawCircle(
                                    color = recRed,
                                    radius = baseRadius,
                                    center = center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "BLACK SCREEN RECORDER",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.5.sp
                ),
                color = TextPrimary.copy(alpha = titleAlpha)
            )
        }
    }
}
