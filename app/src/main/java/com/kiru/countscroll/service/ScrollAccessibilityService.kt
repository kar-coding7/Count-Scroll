package com.kiru.countscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kiru.countscroll.CountScrollApp
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.data.local.entity.DelayLog
import com.kiru.countscroll.data.local.entity.ScrollEvent
import com.kiru.countscroll.domain.LimitEvaluationState
import com.kiru.countscroll.domain.LimitPolicyEngine
import com.kiru.countscroll.domain.MonitoredApps
import com.kiru.countscroll.domain.ScrollDetectorEngine
import com.kiru.countscroll.domain.SessionDurationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScrollAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val detector = ScrollDetectorEngine()
    private val durationTracker = SessionDurationTracker()
    private lateinit var overlayController: OverlayController
    private lateinit var limitAlertManager: LimitAlertManager

    private val activeMonitoredAppFlow = MutableStateFlow(false)
    private val tickFlow = MutableStateFlow(0L)
    private var latestSettings = UserSettings()
    private var latestTodayScrolls = 0

    private fun getStartOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as CountScrollApp

        overlayController = OverlayController(this) { xRatio, yRatio ->
            serviceScope.launch {
                app.settingsDataStore.setOverlayPositionRatio(xRatio, yRatio)
            }
        }

        limitAlertManager = LimitAlertManager(
            context = this,
            onSnoozeClicked = { unit, value ->
                serviceScope.launch {
                    app.settingsDataStore.applySnooze(unit, value)
                    app.database.countScrollDao().insertDelayLog(
                        DelayLog(
                            actionType = "TUNDA",
                            delayUnit = unit,
                            delayValue = value
                        )
                    )
                    tickFlow.value = System.currentTimeMillis()
                }
            },
            onExitToHomeClicked = {
                serviceScope.launch {
                    app.database.countScrollDao().insertDelayLog(
                        DelayLog(
                            actionType = "KELUAR",
                            delayUnit = latestSettings.delayUnit,
                            delayValue = 0
                        )
                    )
                }
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { startActivity(homeIntent) }
            }
        )

        // Ticker ringan tiap 15 detik hanya ketika pengguna berada di aplikasi yang dipantau
        // agar masa tunda berbasis menit otomatis berakhir tepat waktu tanpa boros baterai.
        serviceScope.launch {
            while (true) {
                delay(15_000L)
                if (activeMonitoredAppFlow.value) {
                    tickFlow.value = System.currentTimeMillis()
                }
            }
        }

        val startOfToday = getStartOfTodayMillis()
        serviceScope.launch {
            combine(
                app.settingsDataStore.settingsFlow,
                app.database.countScrollDao().observeTodayTotalScrolls(startOfToday),
                app.database.countScrollDao().observeDelayLogsSince(startOfToday),
                activeMonitoredAppFlow,
                tickFlow
            ) { settings, todayCount, delayLogs, isInMonitoredApp, _ ->
                latestSettings = settings
                latestTodayScrolls = todayCount
                LimitQuad(settings, todayCount, delayLogs, isInMonitoredApp)
            }.collect { quad ->
                val settings = quad.settings
                val todayCount = quad.todayCount
                val isInMonitoredApp = quad.isInMonitoredApp

                overlayController.updateState(
                    shouldShow = isInMonitoredApp,
                    count = todayCount,
                    settings = settings
                )

                if (!settings.isTrackingEnabled || !isInMonitoredApp) {
                    limitAlertManager.dismissLimitDialog()
                    return@collect
                }

                val status = LimitPolicyEngine.evaluate(
                    settings = settings,
                    todayScrolls = todayCount,
                    todayDelayLogs = quad.delayLogs
                )

                when (status.state) {
                    LimitEvaluationState.WARNING_THRESHOLD -> {
                        limitAlertManager.showWarningNotificationIfNeeded(
                            dayKey = todayKey(),
                            status = status,
                            settings = settings
                        )
                    }

                    LimitEvaluationState.LIMIT_REACHED -> {
                        limitAlertManager.showLimitDialog(status, settings)
                    }

                    LimitEvaluationState.NORMAL,
                    LimitEvaluationState.SNOOZED -> {
                        limitAlertManager.dismissLimitDialog()
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        val pkg = safeEvent.packageName?.toString() ?: return
        val app = application as CountScrollApp

        serviceScope.launch {
            val currentSettings = app.settingsDataStore.settingsFlow.first()
            if (!currentSettings.isTrackingEnabled) {
                activeMonitoredAppFlow.value = false
                overlayController.removeOverlay()
                limitAlertManager.dismissLimitDialog()
                durationTracker.onTrackingStopped()?.let { session ->
                    app.database.countScrollDao().insertUsageSession(session)
                }
                return@launch
            }

            when (safeEvent.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (safeEvent.className != null) {
                        val isMonitoredNow = MonitoredApps.isMonitored(pkg)
                        activeMonitoredAppFlow.value = isMonitoredNow
                        if (!isMonitoredNow) {
                            overlayController.removeOverlay()
                            limitAlertManager.dismissLimitDialog()
                        }

                        val closedSessions = durationTracker.onForegroundAppChanged(pkg)
                        for (session in closedSessions) {
                            app.database.countScrollDao().insertUsageSession(session)
                        }
                    }

                    if (MonitoredApps.isMonitored(pkg)) {
                        detector.onWindowStateChanged(pkg, rootInActiveWindow)
                    }
                }

                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    if (!MonitoredApps.isMonitored(pkg)) return@launch
                    val detected = detector.evaluateScrollEvent(safeEvent, rootInActiveWindow)
                    if (detected) {
                        app.database.countScrollDao().insertScrollEvent(
                            ScrollEvent(
                                packageName = pkg,
                                appLabel = MonitoredApps.getDisplayName(pkg)
                            )
                        )
                        if (currentSettings.delayUnit == "SCROLL" && currentSettings.snoozeExtraScrolls > 0) {
                            app.settingsDataStore.consumeOneSnoozeScrollIfActive()
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        overlayController.removeOverlay()
        limitAlertManager.dismissLimitDialog()
        val app = application as CountScrollApp
        serviceScope.launch {
            durationTracker.closeCurrentSession()?.let { session ->
                app.database.countScrollDao().insertUsageSession(session)
            }
        }
    }

    override fun onDestroy() {
        overlayController.removeOverlay()
        limitAlertManager.dismissLimitDialog()
        val app = application as CountScrollApp
        serviceScope.launch {
            durationTracker.closeCurrentSession()?.let { session ->
                app.database.countScrollDao().insertUsageSession(session)
            }
        }
        super.onDestroy()
        serviceScope.cancel()
    }

    private data class LimitQuad(
        val settings: UserSettings,
        val todayCount: Int,
        val delayLogs: List<DelayLog>,
        val isInMonitoredApp: Boolean
    )
}
