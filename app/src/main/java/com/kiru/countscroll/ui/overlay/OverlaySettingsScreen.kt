package com.kiru.countscroll.ui.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiru.countscroll.data.local.OverlayColorMode
import com.kiru.countscroll.data.local.OverlayShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsScreen(
    state: OverlaySettingsUiState,
    onToggleOverlayEnabled: (Boolean) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onShapeChange: (OverlayShape) -> Unit,
    onColorModeChange: (OverlayColorMode) -> Unit,
    onResetPosition: () -> Unit,
    onOpenOverlayPermission: () -> Unit
) {
    val settings = state.settings
    val isDarkBubble = settings.overlayColorMode == OverlayColorMode.DARK
    val bubbleBg = if (isDarkBubble) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
    val bubbleFg = if (isDarkBubble) Color(0xFFFFFFFF) else Color(0xFF0A0A0A)
    val bubbleShape = when (settings.overlayShape) {
        OverlayShape.CIRCLE -> CircleShape
        OverlayShape.BOX -> RoundedCornerShape(14.dp)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OVERLAY ANGKA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!state.canDrawOverlays) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Overlay Belum Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aktifkan izin tampil di atas aplikasi lain",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onOpenOverlayPermission) {
                            Text("Buka")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tampilkan Overlay Melayang",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Dapat dimatikan tanpa mematikan penghitungan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isOverlayEnabled,
                        onCheckedChange = onToggleOverlayEnabled
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "PRATINJAU LANGSUNG",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Text(
                        text = "Geser overlay dengan jari saat berada di aplikasi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    if (settings.isOverlayEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 18.dp)
                                .alpha(settings.overlayAlpha)
                                .clip(bubbleShape)
                                .background(bubbleBg)
                                .border(1.5.dp, bubbleFg, bubbleShape)
                                .padding(
                                    horizontal = (settings.overlaySizeSp * 0.65f).dp,
                                    vertical = (settings.overlaySizeSp * 0.45f).dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.previewScrollCount.toString(),
                                color = bubbleFg,
                                fontSize = settings.overlaySizeSp.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = "Overlay disembunyikan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BENTUK ANGKA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverlayShape.entries.forEach { shape ->
                            FilterChip(
                                selected = settings.overlayShape == shape,
                                onClick = { onShapeChange(shape) },
                                modifier = Modifier.weight(1f),
                                label = { Text(shape.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text(
                        text = "WARNA OVERLAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverlayColorMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.overlayColorMode == mode,
                                onClick = { onColorModeChange(mode) },
                                modifier = Modifier.weight(1f),
                                label = { Text(mode.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "UKURAN ANGKA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.overlaySizeSp.roundToInt()} sp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.overlaySizeSp,
                        onValueChange = onSizeChange,
                        valueRange = 13f..28f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TRANSPARANSI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(settings.overlayAlpha * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.overlayAlpha,
                        onValueChange = onAlphaChange,
                        valueRange = 0.25f..1.0f
                    )

                    OutlinedButton(
                        onClick = onResetPosition,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Posisi Overlay")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
