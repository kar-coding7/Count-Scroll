package com.kiru.countscroll.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kiru.countscroll.data.local.OverlayColorMode
import com.kiru.countscroll.data.local.OverlayShape
import com.kiru.countscroll.data.local.SettingsDataStore
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.data.local.dao.CountScrollDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class OverlaySettingsUiState(
    val settings: UserSettings = UserSettings(),
    val previewScrollCount: Int = 24,
    val canDrawOverlays: Boolean = false
)

class OverlaySettingsViewModel(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val overlayPermFlow = MutableStateFlow(false)

    private fun getStartOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val uiState: StateFlow<OverlaySettingsUiState> = combine(
        settingsDataStore.settingsFlow,
        dao.observeTodayTotalScrolls(getStartOfTodayMillis()),
        overlayPermFlow
    ) { settings, todayCount, hasOverlayPerm ->
        OverlaySettingsUiState(
            settings = settings,
            previewScrollCount = if (todayCount > 0) todayCount else 24,
            canDrawOverlays = hasOverlayPerm
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverlaySettingsUiState()
    )

    fun refreshOverlayPermission(canDraw: Boolean) {
        overlayPermFlow.value = canDraw
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setOverlayEnabled(enabled)
        }
    }

    fun setOverlaySizeSp(sizeSp: Float) {
        viewModelScope.launch {
            settingsDataStore.setOverlaySizeSp(sizeSp)
        }
    }

    fun setOverlayAlpha(alpha: Float) {
        viewModelScope.launch {
            settingsDataStore.setOverlayAlpha(alpha)
        }
    }

    fun setOverlayShape(shape: OverlayShape) {
        viewModelScope.launch {
            settingsDataStore.setOverlayShape(shape)
        }
    }

    fun setOverlayColorMode(colorMode: OverlayColorMode) {
        viewModelScope.launch {
            settingsDataStore.setOverlayColorMode(colorMode)
        }
    }

    fun resetOverlayPosition() {
        viewModelScope.launch {
            settingsDataStore.setOverlayPositionRatio(0.80f, 0.22f)
        }
    }
}

class OverlaySettingsViewModelFactory(
    private val dao: CountScrollDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlaySettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OverlaySettingsViewModel(dao, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
