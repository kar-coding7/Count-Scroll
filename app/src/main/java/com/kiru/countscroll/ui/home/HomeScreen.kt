package com.kiru.countscroll.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiru.countscroll.domain.StatisticsCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleTracking: (Boolean) -> Unit,
    onCycleTheme: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit
) {
    val animatedScrollCount by animateIntAsState(
        targetValue = state.todayTotalScrolls,
        animationSpec = tween(durationMillis = 250),
        label = "scrollCountAnim"
    )

    val rawProgress = if (state.effectiveDailyTarget > 0) {
        (state.todayTotalScrolls.toFloat() / state.effectiveDailyTarget.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 350),
        label = "progressAnim"
    )

    val switchContainerColor by animateColorAsState(
        targetValue = if (state.isTrackingEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "switchBgAnim"
    )

    val switchContentColor by animateColorAsState(
        targetValue = if (state.isTrackingEnabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "switchFgAnim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "COUNT SCROLL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onCycleTheme) {
                        Icon(
                            imageVector = Icons.Outlined.BrightnessMedium,
                            contentDescription = "Ganti Tema (${state.themeMode})"
                        )
                    }
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleTracking(!state.isTrackingEnabled) },
                color = switchContainerColor,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(switchContentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PowerSettingsNew,
                                contentDescription = null,
                                tint = switchContentColor
                            )
                        }
                        Column {
                            Text(
                                text = if (state.isTrackingEnabled) "PEMANTAUAN AKTIF" else "PEMANTAUAN NONAKTIF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = switchContentColor
                            )
                            Text(
                                text = if (state.isTrackingEnabled) {
                                    "Menghitung scroll & batas di latar belakang"
                                } else {
                                    "Ketuk untuk mulai memantau"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = switchContentColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Switch(
                        checked = state.isTrackingEnabled,
                        onCheckedChange = onToggleTracking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            if (!state.isAccessibilityEnabled) {
                PermissionWarningCard(
                    icon = Icons.Outlined.AccessibilityNew,
                    title = "Layanan Aksesibilitas Belum Aktif",
                    subtitle = "Diperlukan untuk mendeteksi scroll",
                    onClick = onOpenAccessibilitySettings
                )
            }

            if (!state.canDrawOverlays) {
                PermissionWarningCard(
                    icon = Icons.Outlined.Layers,
                    title = "Izin Tampil di Atas Aplikasi Belum Aktif",
                    subtitle = "Diperlukan untuk overlay & dialog batas",
                    onClick = onOpenOverlaySettings
                )
            }

            if (!state.isBatteryUnrestricted) {
                PermissionWarningCard(
                    icon = Icons.Outlined.BatteryAlert,
                    title = "Optimasi Baterai Masih Membatasi",
                    subtitle = "Bebaskan agar pemantauan tetap stabil di latar belakang",
                    onClick = onOpenBatteryOptimizationSettings
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SCROLL HARI INI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = animatedScrollCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Durasi: ${StatisticsCalculator.formatDurationHoursMinutes(state.todayDurationMillis)} • Tunda: ${state.snoozeCountToday}/${state.maxDelayPerDay}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progres Target Aktif",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.todayTotalScrolls} / ${state.effectiveDailyTarget}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppStatCard(
                    label = "TikTok",
                    count = state.tiktokScrolls,
                    modifier = Modifier.weight(1f)
                )
                AppStatCard(
                    label = "IG Reels",
                    count = state.instagramScrolls,
                    modifier = Modifier.weight(1f)
                )
                AppStatCard(
                    label = "YT Shorts",
                    count = state.youtubeScrolls,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionWarningCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onClick) {
                Text("Buka")
            }
        }
    }
}

@Composable
private fun AppStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
