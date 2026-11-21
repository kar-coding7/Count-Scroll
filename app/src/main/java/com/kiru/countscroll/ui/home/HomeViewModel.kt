package com.kiru.countscroll.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kiru.countscroll.data.local.SettingsDataStore
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.data.local.dao.CountScrollDao
import com.kiru.countscroll.data.local.entity.DelayLog
import com.kiru.countscroll.domain.LimitPolicyEngine
import com.kiru.countscroll.domain.MonitoredApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val isTrackingEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val todayTotalScrolls: Int = 0,
    val tiktokScrolls: Int = 0,
    val instagramScrolls: Int = 0,
    val youtubeScrolls: Int = 0,
    val todayDurationMillis: Long = 0L,
    val effectiveDailyTarget: Int = 100,
    val snoozeCountToday: Int = 0,
    val maxDelayPerDay: Int = 3,
    val themeMode: String = "SYSTEM"
)

class HomeViewModel(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val accessibilityEnabledFlow = MutableStateFlow(false)
    private val usageAccessFlow = MutableStateFlow(false)
    private val overlayPermFlow = MutableStateFlow(false)
    private val batteryUnrestrictedFlow = MutableStateFlow(false)

    private fun getStartOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val startOfToday = getStartOfTodayMillis()

    init {
        viewModelScope.launch {
            settingsDataStore.ensureTargetAnchorInitialized()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsDataStore.settingsFlow,
        accessibilityEnabledFlow,
        usageAccessFlow,
        overlayPermFlow,
        batteryUnrestrictedFlow,
        dao.observeTodayTotalScrolls(startOfToday),
        dao.observeTodayAppsScrolls(MonitoredApps.tiktokPackages, startOfToday),
        dao.observeTodayAppsScrolls(MonitoredApps.instagramPackages, startOfToday),
        dao.observeTodayAppsScrolls(MonitoredApps.youtubePackages, startOfToday),
        dao.observeTodayTotalDurationMillis(startOfToday),
        dao.observeDelayLogsSince(startOfToday)
    ) { values ->
        val settings = values[0] as UserSettings
        val a11yEnabled = values[1] as Boolean
        val usageEnabled = values[2] as Boolean
        val overlayEnabledPerm = values[3] as Boolean
        val batteryOk = values[4] as Boolean
        val total = values[5] as Int
        val tt = values[6] as Int
        val ig = values[7] as Int
        val yt = values[8] as Int
        val duration = values[9] as Long
        @Suppress("UNCHECKED_CAST")
        val delayLogs = values[10] as List<DelayLog>

        val limitStatus = LimitPolicyEngine.evaluate(
            settings = settings,
            todayScrolls = total,
            todayDelayLogs = delayLogs
        )

        HomeUiState(
            isTrackingEnabled = settings.isTrackingEnabled,
            isAccessibilityEnabled = a11yEnabled,
            hasUsageAccessPermission = usageEnabled,
            canDrawOverlays = overlayEnabledPerm,
            isBatteryUnrestricted = batteryOk,
            todayTotalScrolls = total,
            tiktokScrolls = tt,
            instagramScrolls = ig,
            youtubeScrolls = yt,
            todayDurationMillis = duration,
            effectiveDailyTarget = limitStatus.effectiveDailyTarget,
            snoozeCountToday = limitStatus.snoozeCountToday,
            maxDelayPerDay = settings.maxDelayPerDay,
            themeMode = settings.themeMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun refreshPermissionStatuses(
        accessibilityEnabled: Boolean,
        usageAccessEnabled: Boolean,
        canDrawOverlays: Boolean,
        isBatteryUnrestricted: Boolean
    ) {
        accessibilityEnabledFlow.value = accessibilityEnabled
        usageAccessFlow.value = usageAccessEnabled
        overlayPermFlow.value = canDrawOverlays
        batteryUnrestrictedFlow.value = isBatteryUnrestricted
    }

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTrackingEnabled(enabled)
        }
    }

    fun cycleTheme() {
        viewModelScope.launch {
            val next = when (uiState.value.themeMode) {
                "SYSTEM" -> "DARK"
                "DARK" -> "LIGHT"
                else -> "SYSTEM"
            }
            settingsDataStore.setThemeMode(next)
        }
    }
}

class HomeViewModelFactory(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(dao, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
