package com.kiru.countscroll.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kiru.countscroll.data.local.SettingsDataStore
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.data.local.dao.CountScrollDao
import com.kiru.countscroll.domain.LimitPolicyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LimitSettingsUiState(
    val settings: UserSettings = UserSettings(),
    val effectiveTargetToday: Int = 100,
    val todayScrolls: Int = 0,
    val snoozeCountToday: Int = 0,
    val totalSnoozeValueToday: Int = 0,
    val savedMessage: String? = null
)

class LimitSettingsViewModel(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    private fun getStartOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val startOfToday = getStartOfTodayMillis()

    val uiState: StateFlow<LimitSettingsUiState> = combine(
        settingsDataStore.settingsFlow,
        dao.observeTodayTotalScrolls(startOfToday),
        dao.observeDelayLogsSince(startOfToday),
        messageFlow
    ) { settings, scrolls, logs, msg ->
        val status = LimitPolicyEngine.evaluate(settings, scrolls, logs)
        LimitSettingsUiState(
            settings = settings,
            effectiveTargetToday = status.effectiveDailyTarget,
            todayScrolls = scrolls,
            snoozeCountToday = status.snoozeCountToday,
            totalSnoozeValueToday = status.totalSnoozeValueToday,
            savedMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LimitSettingsUiState()
    )

    fun saveTargetSettings(
        baseTarget: Int,
        gradualEnabled: Boolean,
        reductionPercent: Int,
        minTarget: Int
    ) {
        viewModelScope.launch {
            settingsDataStore.updateTargetSettings(
                baseTarget = baseTarget,
                gradualEnabled = gradualEnabled,
                reductionPercent = reductionPercent,
                minTarget = minTarget,
                resetAnchorNow = true
            )
            messageFlow.value = "Target & aturan penurunan disimpan"
        }
    }

    fun saveLimitSettings(
        limitEnabled: Boolean,
        warningPercent: Int,
        delayUnit: String,
        delayValue: Int,
        maxDelayPerDay: Int,
        maxTotalDelayPerDay: Int
    ) {
        viewModelScope.launch {
            settingsDataStore.updateLimitSettings(
                limitEnabled = limitEnabled,
                warningPercent = warningPercent,
                delayUnit = delayUnit,
                delayValue = delayValue,
                maxDelayPerDay = maxDelayPerDay,
                maxTotalDelayPerDay = maxTotalDelayPerDay
            )
            messageFlow.value = "Pengaturan peringatan & batas disimpan"
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }
}

class LimitSettingsViewModelFactory(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LimitSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LimitSettingsViewModel(dao, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
