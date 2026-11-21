package com.kiru.countscroll

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiru.countscroll.ui.home.HomeScreen
import com.kiru.countscroll.ui.home.HomeViewModel
import com.kiru.countscroll.ui.home.HomeViewModelFactory
import com.kiru.countscroll.ui.limits.LimitSettingsScreen
import com.kiru.countscroll.ui.limits.LimitSettingsViewModel
import com.kiru.countscroll.ui.limits.LimitSettingsViewModelFactory
import com.kiru.countscroll.ui.overlay.OverlaySettingsScreen
import com.kiru.countscroll.ui.overlay.OverlaySettingsViewModel
import com.kiru.countscroll.ui.overlay.OverlaySettingsViewModelFactory
import com.kiru.countscroll.ui.stats.StatsScreen
import com.kiru.countscroll.ui.stats.StatsViewModel
import com.kiru.countscroll.ui.stats.StatsViewModelFactory
import com.kiru.countscroll.ui.theme.CountScrollTheme
import com.kiru.countscroll.util.AccessibilityHelper

private enum class MainTab(val label: String) {
    HOME("Beranda"),
    STATS("Statistik"),
    OVERLAY("Overlay"),
    LIMITS("Batas")
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Ditangani sistem */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as CountScrollApp
        val homeFactory = HomeViewModelFactory(
            dao = app.database.countScrollDao(),
            settingsDataStore = app.settingsDataStore
        )
        val statsFactory = StatsViewModelFactory(
            dao = app.database.countScrollDao()
        )
        val overlayFactory = OverlaySettingsViewModelFactory(
            dao = app.database.countScrollDao(),
            settingsDataStore = app.settingsDataStore
        )
        val limitsFactory = LimitSettingsViewModelFactory(
            dao = app.database.countScrollDao(),
            settingsDataStore = app.settingsDataStore
        )

        setContent {
            val homeViewModel: HomeViewModel = viewModel(factory = homeFactory)
            val statsViewModel: StatsViewModel = viewModel(factory = statsFactory)
            val overlayViewModel: OverlaySettingsViewModel = viewModel(factory = overlayFactory)
            val limitsViewModel: LimitSettingsViewModel = viewModel(factory = limitsFactory)

            val homeState by homeViewModel.uiState.collectAsState()
            val statsState by statsViewModel.uiState.collectAsState()
            val overlayState by overlayViewModel.uiState.collectAsState()
            val limitsState by limitsViewModel.uiState.collectAsState()

            var currentTab by remember { mutableStateOf(MainTab.HOME) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    val a11y = AccessibilityHelper.isServiceEnabled(this@MainActivity)
                    val usage = AccessibilityHelper.hasUsageAccessPermission(this@MainActivity)
                    val overlayPerm = AccessibilityHelper.canDrawOverlays(this@MainActivity)
                    val batteryOk = AccessibilityHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                    homeViewModel.refreshPermissionStatuses(a11y, usage, overlayPerm, batteryOk)
                    statsViewModel.refreshUsageAccessStatus(usage)
                    overlayViewModel.refreshOverlayPermission(overlayPerm)
                }
            }

            CountScrollTheme(themeMode = homeState.themeMode) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == MainTab.HOME,
                                onClick = { currentTab = MainTab.HOME },
                                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                                label = { Text(MainTab.HOME.label) }
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.STATS,
                                onClick = { currentTab = MainTab.STATS },
                                icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                                label = { Text(MainTab.STATS.label) }
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.OVERLAY,
                                onClick = { currentTab = MainTab.OVERLAY },
                                icon = { Icon(Icons.Outlined.Layers, contentDescription = null) },
                                label = { Text(MainTab.OVERLAY.label) }
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.LIMITS,
                                onClick = { currentTab = MainTab.LIMITS },
                                icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                                label = { Text(MainTab.LIMITS.label) }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        MainTab.HOME -> {
                            HomeScreen(
                                state = homeState,
                                onToggleTracking = homeViewModel::toggleTracking,
                                onCycleTheme = homeViewModel::cycleTheme,
                                onOpenAccessibilitySettings = {
                                    AccessibilityHelper.openAccessibilitySettings(this@MainActivity)
                                },
                                onOpenOverlaySettings = {
                                    AccessibilityHelper.openOverlaySettings(this@MainActivity)
                                },
                                onOpenBatteryOptimizationSettings = {
                                    AccessibilityHelper.openBatteryOptimizationSettings(this@MainActivity)
                                }
                            )
                        }

                        MainTab.STATS -> {
                            StatsScreen(
                                state = statsState,
                                onSelectPeriod = statsViewModel::selectPeriod,
                                onSelectFilter = statsViewModel::selectFilter,
                                onSelectMetric = statsViewModel::selectMetric,
                                onOpenUsageAccessSettings = {
                                    AccessibilityHelper.openUsageAccessSettings(this@MainActivity)
                                },
                                onResetData = statsViewModel::resetAllData
                            )
                        }

                        MainTab.OVERLAY -> {
                            OverlaySettingsScreen(
                                state = overlayState,
                                onToggleOverlayEnabled = overlayViewModel::setOverlayEnabled,
                                onSizeChange = overlayViewModel::setOverlaySizeSp,
                                onAlphaChange = overlayViewModel::setOverlayAlpha,
                                onShapeChange = overlayViewModel::setOverlayShape,
                                onColorModeChange = overlayViewModel::setOverlayColorMode,
                                onResetPosition = overlayViewModel::resetOverlayPosition,
                                onOpenOverlayPermission = {
                                    AccessibilityHelper.openOverlaySettings(this@MainActivity)
                                }
                            )
                        }

                        MainTab.LIMITS -> {
                            LimitSettingsScreen(
                                state = limitsState,
                                onSaveTarget = limitsViewModel::saveTargetSettings,
                                onSaveLimit = limitsViewModel::saveLimitSettings,
                                onClearMessage = limitsViewModel::clearMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
