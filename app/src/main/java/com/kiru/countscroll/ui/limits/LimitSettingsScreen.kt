package com.kiru.countscroll.ui.limits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSettingsScreen(
    state: LimitSettingsUiState,
    onSaveTarget: (baseTarget: Int, gradualEnabled: Boolean, reductionPercent: Int, minTarget: Int) -> Unit,
    onSaveLimit: (
        limitEnabled: Boolean,
        warningPercent: Int,
        delayUnit: String,
        delayValue: Int,
        maxDelayPerDay: Int,
        maxTotalDelayPerDay: Int
    ) -> Unit,
    onClearMessage: () -> Unit
) {
    val s = state.settings
    var baseTargetText by remember(s.baseDailyScrollTarget) { mutableStateOf(s.baseDailyScrollTarget.toString()) }
    var gradualEnabled by remember(s.gradualReductionEnabled) { mutableStateOf(s.gradualReductionEnabled) }
    var reductionPercent by remember(s.weeklyReductionPercent) { mutableStateOf(s.weeklyReductionPercent.toFloat()) }
    var minTargetText by remember(s.minimumScrollTarget) { mutableStateOf(s.minimumScrollTarget.toString()) }

    var limitEnabled by remember(s.isLimitEnabled) { mutableStateOf(s.isLimitEnabled) }
    var warningPercent by remember(s.warningThresholdPercent) { mutableStateOf(s.warningThresholdPercent.toFloat()) }
    var delayUnit by remember(s.delayUnit) { mutableStateOf(s.delayUnit) }
    var delayValueText by remember(s.delayValue) { mutableStateOf(s.delayValue.toString()) }
    var maxDelayText by remember(s.maxDelayPerDay) { mutableStateOf(s.maxDelayPerDay.toString()) }
    var maxTotalDelayText by remember(s.maxTotalDelayPerDay) { mutableStateOf(s.maxTotalDelayPerDay.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TARGET & BATAS",
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
            state.savedMessage?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearMessage) {
                            Text("OK")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TARGET HARIAN & PENURUNAN BERTAHAP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Target aktif hari ini: ${state.effectiveTargetToday} scroll",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = baseTargetText,
                        onValueChange = { baseTargetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target awal per hari (scroll)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Turun otomatis per minggu", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Mengurangi target tiap 7 hari hingga batas minimum",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = gradualEnabled, onCheckedChange = { gradualEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Persentase turun per minggu", style = MaterialTheme.typography.bodyMedium)
                        Text("${reductionPercent.roundToInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = reductionPercent,
                        onValueChange = { reductionPercent = it },
                        valueRange = 0f..50f,
                        enabled = gradualEnabled
                    )

                    OutlinedTextField(
                        value = minTargetText,
                        onValueChange = { minTargetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Batas minimum target (scroll)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val base = baseTargetText.toIntOrNull()?.coerceAtLeast(1) ?: 100
                            val minT = minTargetText.toIntOrNull()?.coerceAtLeast(1) ?: 20
                            onSaveTarget(base, gradualEnabled, reductionPercent.roundToInt(), minT)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Target")
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PERINGATAN & BATAS (TUNDA / KELUAR)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aktifkan batas harian", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = limitEnabled, onCheckedChange = { limitEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ambang notifikasi peringatan", style = MaterialTheme.typography.bodyMedium)
                        Text("${warningPercent.roundToInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = warningPercent,
                        onValueChange = { warningPercent = it },
                        valueRange = 50f..95f
                    )

                    Text("Jenis tambahan tunda", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MENIT" to "Menit", "SCROLL" to "Jumlah Scroll").forEach { (code, label) ->
                            FilterChip(
                                selected = delayUnit == code,
                                onClick = { delayUnit = code },
                                modifier = Modifier.weight(1f),
                                label = { Text(label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = delayValueText,
                        onValueChange = { delayValueText = it.filter { ch -> ch.isDigit() } },
                        label = {
                            Text(if (delayUnit == "SCROLL") "Nilai tunda (x scroll)" else "Nilai tunda (menit)")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maxDelayText,
                            onValueChange = { maxDelayText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Maks tunda / hari") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxTotalDelayText,
                            onValueChange = { maxTotalDelayText = it.filter { ch -> ch.isDigit() } },
                            label = {
                                Text(if (delayUnit == "SCROLL") "Maks total scroll" else "Maks total menit")
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Tunda hari ini: ${state.snoozeCountToday}/${s.maxDelayPerDay} kali (total ${state.totalSnoozeValueToday})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val dVal = delayValueText.toIntOrNull()?.coerceAtLeast(1) ?: 5
                            val maxD = maxDelayText.toIntOrNull()?.coerceAtLeast(0) ?: 3
                            val maxTot = maxTotalDelayText.toIntOrNull()?.coerceAtLeast(1) ?: 15
                            onSaveLimit(
                                limitEnabled,
                                warningPercent.roundToInt(),
                                delayUnit,
                                dVal,
                                maxD,
                                maxTot
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Aturan Batas")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
