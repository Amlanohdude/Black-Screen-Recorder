package com.example.ui.covers

import com.example.ui.theme.*
import com.example.ads.AdScreen
import com.example.ads.NativeAdCard
import com.example.localization.LocalAppStrings
import com.example.localization.getCoverName

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.ads.RewardedAdManager
import com.example.storage.EntitlementManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdsConfig
import androidx.compose.ui.platform.LocalContext
import com.example.covers.CoverConfig
import com.example.covers.CoverDisplay
import com.example.covers.CoverPresetId
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



@Composable
fun CoversTabScreen(
viewModel: MainViewModel,
onNavigateToPro: () -> Unit = {},
modifier: Modifier = Modifier
) {
val context = LocalContext.current
val settings by viewModel.userSettings.collectAsStateWithLifecycle()
val savedCustomCovers by viewModel.savedCustomCovers.collectAsStateWithLifecycle()
val entitlements by EntitlementManager.entitlements.collectAsStateWithLifecycle()
val isPro = entitlements.isPro
val activeCover = settings.activeCover
val strings = LocalAppStrings.current

var coverToDelete by remember { mutableStateOf<CoverConfig?>(null) }
var showCustomCoversLockedDialog by remember { mutableStateOf(false) }
var pendingCustomCoverToActivate by remember { mutableStateOf<CoverConfig?>(null) }

val builtInPresets = remember {
    CoverConfig.ALL_PRESETS
}

val firstPresetBatch = remember(builtInPresets) { builtInPresets.take(AdsConfig.coversFrequency) }
val remainingPresetBatch = remember(builtInPresets) { builtInPresets.drop(AdsConfig.coversFrequency) }

// Delete Confirmation Dialog
coverToDelete?.let { cover ->
AlertDialog(
onDismissRequest = { coverToDelete = null },
containerColor = SurfacePrimary,
title = {
Text(
text = strings.deleteCustomCoverTitle,
color = TextPrimary,
fontSize = 18.sp,
fontWeight = FontWeight.Bold
)
},
text = {
Text(
text = strings.deleteCustomCoverDesc,
color = TextSecondary,
fontSize = 14.sp
)
},
confirmButton = {
Button(
onClick = {
viewModel.deleteCustomCover(cover.id)
if (activeCover.id == cover.id) {
    viewModel.setActiveCover(CoverConfig.PURE_BLACK_PRESET)
}
coverToDelete = null
},
colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
) {
Text(strings.delete, color = Color.White)
}
},
dismissButton = {
TextButton(onClick = { coverToDelete = null }) {
Text(strings.cancel, color = TextSecondary)
}
}
)
}

if (showCustomCoversLockedDialog) {
val activity = context as? Activity
val coverToActivate = pendingCustomCoverToActivate ?: activeCover
AlertDialog(
onDismissRequest = {
showCustomCoversLockedDialog = false
pendingCustomCoverToActivate = null
},
containerColor = SurfacePrimary,
shape = RoundedCornerShape(16.dp),
title = {
Row(verticalAlignment = Alignment.CenterVertically) {
Icon(
imageVector = Icons.Default.PlayCircle,
contentDescription = null,
tint = AccentRed,
modifier = Modifier.size(22.dp)
)
Spacer(modifier = Modifier.width(8.dp))
Text(
text = strings.activateCustomCoverTitle,
style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
color = TextPrimary
)
}
},
text = {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
Text(
text = strings.activateCustomCoverDesc,
style = MaterialTheme.typography.bodyMedium,
color = TextSecondary
)
}
},
confirmButton = {
Button(
onClick = {
showCustomCoversLockedDialog = false
if (activity != null) {
RewardedAdManager.showAd(
activity = activity,
onUserEarnedReward = {
viewModel.setActiveCover(coverToActivate)
android.widget.Toast.makeText(context, "\"${coverToActivate.name}\" activated!", android.widget.Toast.LENGTH_SHORT).show()
pendingCustomCoverToActivate = null
}
)
}
},
colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
shape = RoundedCornerShape(8.dp)
) {
Icon(
imageVector = Icons.Default.PlayCircle,
contentDescription = null,
tint = Color.White,
modifier = Modifier.size(16.dp)
)
Spacer(modifier = Modifier.width(6.dp))
Text(strings.watchAdActivate, color = Color.White, fontWeight = FontWeight.Bold)
}
},
dismissButton = {
Row(verticalAlignment = Alignment.CenterVertically) {
TextButton(
onClick = {
showCustomCoversLockedDialog = false
pendingCustomCoverToActivate = null
}
) {
Text(strings.cancel, color = TextSecondary)
}
Spacer(modifier = Modifier.width(4.dp))
Button(
onClick = {
showCustomCoversLockedDialog = false
pendingCustomCoverToActivate = null
onNavigateToPro()
},
colors = ButtonDefaults.buttonColors(containerColor = SurfaceSecondary),
shape = RoundedCornerShape(8.dp)
) {
Icon(
imageVector = Icons.Default.WorkspacePremium,
contentDescription = null,
tint = Color(0xFFFFD54F),
modifier = Modifier.size(15.dp)
)
Spacer(modifier = Modifier.width(5.dp))
Text(
text = strings.goPro,
color = TextPrimary,
fontWeight = FontWeight.SemiBold,
fontSize = 12.sp
)
}
}
}
)
}

Column(
modifier = modifier
.fillMaxSize()
.background(BgDark)
) {
TabTopHeader(title = strings.coversTitle)

LazyVerticalGrid(
columns = GridCells.Fixed(2),
contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
horizontalArrangement = Arrangement.spacedBy(12.dp),
verticalArrangement = Arrangement.spacedBy(14.dp),
modifier = Modifier
.fillMaxSize()
.testTag("covers_grid")
) {
// Section 1: BUILT-IN PRESETS
item(span = { GridItemSpan(2) }) {
Text(
text = strings.builtInPresets,
style = MaterialTheme.typography.labelMedium.copy(
fontWeight = FontWeight.Bold,
letterSpacing = 1.2.sp,
fontSize = 11.sp
),
color = TextSecondary,
modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 4.dp)
)
}

                items(builtInPresets, key = { "preset_${it.presetId.name}" }) { preset ->
                    val isCurrentActive = (activeCover.id == preset.id || (activeCover.presetId == preset.presetId && activeCover.presetId != CoverPresetId.CUSTOM))

                    CoverCard(
                        cover = preset,
                        isActive = isCurrentActive,
                        onClick = {
                            viewModel.setActiveCover(preset)
                        }
                    )
                }

                // Native Ad Card
                item(key = "native_ad_covers", span = { GridItemSpan(2) }) {
                    NativeAdCard(
                        screen = AdScreen.COVERS,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Section 2: MY CUSTOM COVERS
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, top = 14.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.customCovers,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary
                            )
                            if (savedCustomCovers.isNotEmpty()) {
                                Text(
                                    text = "${savedCustomCovers.size} ${strings.saved}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (isPro) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                                    .border(0.5.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = strings.proUnlimited,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // "+ Design New Cover" Card
                item {
                    NewCoverActionCard(
                        onClick = {
                            viewModel.openCoverDesigner(CoverConfig.forPreset(CoverPresetId.CUSTOM))
                        }
                    )
                }

                // Saved Custom Covers
                items(savedCustomCovers, key = { "custom_${it.id}" }) { customCover ->
                    val isCurrentActive = (activeCover.id == customCover.id)

                    CustomCoverCard(
                        cover = customCover,
                        isActive = isCurrentActive,
                        onClick = {
                            if (!isCurrentActive) {
                                if (isPro) {
                                    viewModel.setActiveCover(customCover)
                                } else {
                                    pendingCustomCoverToActivate = customCover
                                    showCustomCoversLockedDialog = true
                                }
                            }
                        },
                        onEdit = {
                            viewModel.openCoverDesigner(customCover)
                        },
                        onDelete = {
                            coverToDelete = customCover
                        }
                    )
                }
            }
        }
    }

@Composable
private fun CoverCard(
    cover: CoverConfig,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 0.5.dp,
            color = if (isActive) AccentRed else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cover_card_${cover.presetId.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CoverDisplay(
                    cover = cover,
                    isCardPreview = true
                )

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(AccentRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val strings = LocalAppStrings.current
            Text(
                text = strings.getCoverName(cover.presetId, cover.name),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (isActive) Color.White else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CustomCoverCard(
    cover: CoverConfig,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 0.5.dp,
            color = if (isActive) AccentRed else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_cover_card_${cover.id}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CoverDisplay(
                    cover = cover,
                    isCardPreview = true
                )

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(AccentRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val strings = LocalAppStrings.current
                Text(
                    text = strings.getCoverName(cover.presetId, cover.name),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (isActive) Color.White else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("edit_custom_cover_${cover.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_custom_cover_${cover.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewCoverActionCard(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("create_new_cover_button"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AccentRed.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AccentRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val strings = LocalAppStrings.current
                    Text(
                        text = strings.newCover,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val strings = LocalAppStrings.current
            Text(
                text = strings.designNew,
                color = AccentRed,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}
