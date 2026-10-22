package com.example.covers

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun CoverCustomizeDialog(
    initialCover: CoverConfig,
    onDismiss: () -> Unit,
    onSaveAndApply: (CoverConfig) -> Unit,
    onSaveOnly: ((CoverConfig) -> Unit)? = null,
    onNavigateToPro: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var cover by remember { mutableStateOf(initialCover) }
    var selectedTab by remember { mutableStateOf(DesignerTab.ELEMENTS) }
    var selectedElement by remember { mutableStateOf("Clock") }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Customize Cover",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { onSaveAndApply(cover) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }

            // LIVE PREVIEW AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                CoverDisplay(
                    cover = cover,
                    modifier = Modifier.fillMaxSize(),
                    isCardPreview = false
                )
            }

            // TAB ROW
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = SurfacePrimary,
                contentColor = AccentRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = AccentRed
                    )
                }
            ) {
                DesignerTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                color = if (selectedTab == tab) AccentRed else TextSecondary,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // TAB CONTENT (SCROLLABLE CONTROLS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(SurfaceSecondary)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    DesignerTab.ELEMENTS -> ElementsTabContent(
                        cover = cover,
                        onCoverChanged = { cover = it }
                    )
                    DesignerTab.BACKGROUND -> BackgroundTabContent(
                        cover = cover,
                        onCoverChanged = { cover = it }
                    )
                    DesignerTab.CLOCK_STYLE -> ClockStyleTabContent(
                        cover = cover,
                        selectedElement = selectedElement,
                        onSelectElement = { selectedElement = it },
                        onCoverChanged = { cover = it }
                    )
                    DesignerTab.LAYOUT -> LayoutTabContent(
                        cover = cover,
                        selectedElement = selectedElement,
                        onSelectElement = { selectedElement = it },
                        onCoverChanged = { cover = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ElementsTabContent(
    cover: CoverConfig,
    onCoverChanged: (CoverConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Active Elements", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)

        // Clock Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfacePrimary)
                .clickable {
                    onCoverChanged(cover.withClock(cover.clock.copy(show = !cover.clock.show)))
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Clock", color = TextPrimary, fontSize = 14.sp)
            CompactSelectableChip(
                label = if (cover.clock.show) "ON" else "OFF",
                selected = cover.clock.show,
                onClick = {
                    onCoverChanged(cover.withClock(cover.clock.copy(show = !cover.clock.show)))
                }
            )
        }

        // Date Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfacePrimary)
                .clickable {
                    onCoverChanged(cover.withDate(cover.date.copy(show = !cover.date.show)))
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Date", color = TextPrimary, fontSize = 14.sp)
            CompactSelectableChip(
                label = if (cover.date.show) "ON" else "OFF",
                selected = cover.date.show,
                onClick = {
                    onCoverChanged(cover.withDate(cover.date.copy(show = !cover.date.show)))
                }
            )
        }

        // Battery Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfacePrimary)
                .clickable {
                    onCoverChanged(cover.withBattery(cover.battery.copy(show = !cover.battery.show)))
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Battery", color = TextPrimary, fontSize = 14.sp)
            CompactSelectableChip(
                label = if (cover.battery.show) "ON" else "OFF",
                selected = cover.battery.show,
                onClick = {
                    onCoverChanged(cover.withBattery(cover.battery.copy(show = !cover.battery.show)))
                }
            )
        }

        // Burn-in protection toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfacePrimary)
                .clickable {
                    onCoverChanged(cover.copy(burnInProtection = !cover.burnInProtection))
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("OLED Burn-in Protection", color = TextPrimary, fontSize = 14.sp)
                Text("Subtle pixel drift over time", color = TextMuted, fontSize = 11.sp)
            }
            CompactSelectableChip(
                label = if (cover.burnInProtection) "ON" else "OFF",
                selected = cover.burnInProtection,
                onClick = {
                    onCoverChanged(cover.copy(burnInProtection = !cover.burnInProtection))
                }
            )
        }
    }
}

@Composable
fun BackgroundTabContent(
    cover: CoverConfig,
    onCoverChanged: (CoverConfig) -> Unit
) {
    val bgConfig = cover.backgroundConfig
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = CoverImageManager.saveImageForCover(context, uri)
                if (savedPath != null) {
                    onCoverChanged(cover.withBackground(bgConfig.copy(
                        type = CoverBackground.IMAGE,
                        imagePath = savedPath
                    )))
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Background Type", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactSelectableChip(
                label = "Pure Black",
                selected = bgConfig.type == CoverBackground.PURE_BLACK,
                onClick = { onCoverChanged(cover.withBackground(bgConfig.copy(type = CoverBackground.PURE_BLACK))) },
                modifier = Modifier.weight(1f)
            )
            CompactSelectableChip(
                label = "Slate",
                selected = bgConfig.type == CoverBackground.VERY_DARK_GRAY,
                onClick = { onCoverChanged(cover.withBackground(bgConfig.copy(type = CoverBackground.VERY_DARK_GRAY))) },
                modifier = Modifier.weight(1f)
            )
            CompactSelectableChip(
                label = "Navy",
                selected = bgConfig.type == CoverBackground.DEEP_NAVY,
                onClick = { onCoverChanged(cover.withBackground(bgConfig.copy(type = CoverBackground.DEEP_NAVY))) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactSelectableChip(
                label = "Gradient",
                selected = bgConfig.type == CoverBackground.GRADIENT,
                onClick = { onCoverChanged(cover.withBackground(bgConfig.copy(type = CoverBackground.GRADIENT))) },
                modifier = Modifier.weight(1f)
            )
            CompactSelectableChip(
                label = "Custom Image",
                selected = bgConfig.type == CoverBackground.IMAGE,
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            )
        }

        // Vignette setting
        Text("Vignette Level", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VignetteLevel.values().forEach { level ->
                CompactSelectableChip(
                    label = level.name,
                    selected = cover.vignetteLevel == level,
                    onClick = { onCoverChanged(cover.copy(vignetteLevel = level)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Opacity slider
        SliderWithLabel(
            label = "Background Opacity",
            value = bgConfig.opacity * 100f,
            valueRange = 10f..100f,
            unit = "%",
            onValueChange = { onCoverChanged(cover.withBackground(bgConfig.copy(opacity = it / 100f))) }
        )
    }
}

@Composable
fun ClockStyleTabContent(
    cover: CoverConfig,
    selectedElement: String,
    onSelectElement: (String) -> Unit,
    onCoverChanged: (CoverConfig) -> Unit
) {
    val clock = cover.clock

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Clock Display Mode", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClockMode.values().forEach { mode ->
                CompactSelectableChip(
                    label = mode.name,
                    selected = clock.mode == mode,
                    onClick = { onCoverChanged(cover.withClock(clock.copy(mode = mode))) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (clock.mode == ClockMode.DIGITAL || clock.mode == ClockMode.HYBRID) {
            Text("Digital Clock Style", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClockStyle.values().forEach { style ->
                    CompactSelectableChip(
                        label = style.name,
                        selected = clock.style == style,
                        onClick = { onCoverChanged(cover.withClock(clock.copy(style = style))) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactSelectableChip(
                    label = "12-Hour",
                    selected = !clock.is24Hour,
                    onClick = { onCoverChanged(cover.withClock(clock.copy(is24Hour = false))) },
                    modifier = Modifier.weight(1f)
                )
                CompactSelectableChip(
                    label = "24-Hour",
                    selected = clock.is24Hour,
                    onClick = { onCoverChanged(cover.withClock(clock.copy(is24Hour = true))) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (clock.mode == ClockMode.ANALOG || clock.mode == ClockMode.HYBRID) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfacePrimary)
                    .clickable {
                        onCoverChanged(cover.withClock(clock.copy(showSecondsHand = !clock.showSecondsHand)))
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Seconds Hand", color = TextPrimary, fontSize = 14.sp)
                CompactSelectableChip(
                    label = if (clock.showSecondsHand) "ON" else "OFF",
                    selected = clock.showSecondsHand,
                    onClick = {
                        onCoverChanged(cover.withClock(clock.copy(showSecondsHand = !clock.showSecondsHand)))
                    }
                )
            }
        }

        SliderWithLabel(
            label = "Clock Size",
            value = clock.sizeSp.toFloat(),
            valueRange = 24f..96f,
            unit = "sp",
            onValueChange = { onCoverChanged(cover.withClock(clock.copy(sizeSp = it.toInt()))) }
        )

        SliderWithLabel(
            label = "Clock Opacity",
            value = clock.opacity * 100f,
            valueRange = 10f..100f,
            unit = "%",
            onValueChange = { onCoverChanged(cover.withClock(clock.copy(opacity = it / 100f))) }
        )
    }
}

@Composable
fun LayoutTabContent(
    cover: CoverConfig,
    selectedElement: String,
    onSelectElement: (String) -> Unit,
    onCoverChanged: (CoverConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Target Element", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Clock", "Date", "Battery").forEach { elem ->
                CompactSelectableChip(
                    label = elem,
                    selected = selectedElement == elem,
                    onClick = { onSelectElement(elem) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val (currentVert, currentHoriz) = when (selectedElement) {
            "Date" -> cover.date.verticalPosition to cover.date.horizontalAlignment
            "Battery" -> cover.battery.verticalPosition to cover.battery.horizontalAlignment
            else -> cover.clock.verticalPosition to cover.clock.horizontalAlignment
        }

        Text("Vertical Position", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElementVerticalPosition.values().forEach { v ->
                CompactSelectableChip(
                    label = v.name,
                    selected = currentVert == v,
                    onClick = {
                        val updated = when (selectedElement) {
                            "Date" -> cover.withDate(cover.date.copy(verticalPosition = v))
                            "Battery" -> cover.withBattery(cover.battery.copy(verticalPosition = v))
                            else -> cover.withClock(cover.clock.copy(verticalPosition = v))
                        }
                        onCoverChanged(updated)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Horizontal Alignment", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElementHorizontalAlignment.values().forEach { h ->
                CompactSelectableChip(
                    label = h.name,
                    selected = currentHoriz == h,
                    onClick = {
                        val updated = when (selectedElement) {
                            "Date" -> cover.withDate(cover.date.copy(horizontalAlignment = h))
                            "Battery" -> cover.withBattery(cover.battery.copy(horizontalAlignment = h))
                            else -> cover.withClock(cover.clock.copy(horizontalAlignment = h))
                        }
                        onCoverChanged(updated)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CompactSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AccentRed else SurfacePrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text("${value.toInt()}$unit", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentRed,
                activeTrackColor = AccentRed,
                inactiveTrackColor = SurfacePrimary
            )
        )
    }
}
