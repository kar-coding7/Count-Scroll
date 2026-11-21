package com.kiru.countscroll.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kiru.countscroll.domain.AppFilter
import com.kiru.countscroll.domain.DailyBarPoint
import com.kiru.countscroll.domain.StatisticsCalculator
import com.kiru.countscroll.domain.StatsPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onSelectFilter: (AppFilter) -> Unit,
    onSelectMetric: (ChartMetric) -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onResetData: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Semua Data?") },
            text = {
                Text("Riwayat scroll, durasi sesi, dan log tunda akan dihapus permanen dari perangkat.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    }
                ) {
                    Text("Ya, Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "STATISTIK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Reset Data"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsPeriod.entries.forEach { period ->
                    val selected = state.selectedPeriod == period
                    if (selected) {
                        Button(
                            onClick = { onSelectPeriod(period) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(period.label, maxLines = 1, softWrap = false)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectPeriod(period) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(period.label, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SCROLL",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.report.totalScrolls.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL DURASI",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = StatisticsCalculator.formatDurationHoursMinutes(
                                    state.report.totalDurationMillis
                                ),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Text(
                        text = state.report.comparisonText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = state.selectedMetric == metric,
                        onClick = { onSelectMetric(metric) },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = metric.label,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 6 }
            ) {
                DailyBarChartCard(
                    points = state.report.dailyPoints,
                    metric = state.selectedMetric
                )
            }

            if (!state.hasUsageAccessPermission) {
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.QueryStats, contentDescription = null)
                            Column {
                                Text(
                                    text = "Akses Penggunaan Belum Aktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Aktifkan untuk verifikasi durasi aplikasi di sistem",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = onOpenUsageAccessSettings) {
                            Text("Buka")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DailyBarChartCard(
    points: List<DailyBarPoint>,
    metric: ChartMetric
) {
    val maxValue = remember(points, metric) {
        val rawMax = when (metric) {
            ChartMetric.SCROLLS -> points.maxOfOrNull { it.scrollCount } ?: 0
            ChartMetric.DURATION -> points.maxOfOrNull { (it.durationMillis / 60_000L).toInt() } ?: 0
        }
        rawMax.coerceAtLeast(1)
    }

    val isMonthly = points.size > 14

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (metric == ChartMetric.SCROLLS) {
                        "GRAFIK SCROLL PER HARI"
                    } else {
                        "GRAFIK DURASI PER HARI (MENIT)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${points.size} hari",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .then(
                        if (isMonthly) Modifier.horizontalScroll(rememberScrollState())
                        else Modifier
                    ),
                horizontalArrangement = Arrangement.spacedBy(if (isMonthly) 6.dp else 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val value = when (metric) {
                        ChartMetric.SCROLLS -> point.scrollCount
                        ChartMetric.DURATION -> (point.durationMillis / 60_000L).toInt()
                    }
                    val targetFraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = if (value > 0) targetFraction else 0.02f,
                        animationSpec = tween(durationMillis = 350),
                        label = "barFraction_${point.dayIndex}"
                    )

                    Column(
                        modifier = if (isMonthly) Modifier.width(24.dp) else Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (!isMonthly && value > 0) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (value > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
