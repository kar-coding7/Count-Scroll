package com.kiru.countscroll.domain

import com.kiru.countscroll.data.local.entity.UsageSession

class SessionDurationTracker(
    private val maxSessionDurationMs: Long = 6 * 3_600_000L
) {
    private var activePackage: String? = null
    private var activeLabel: String? = null
    private var sessionStartMillis: Long? = null

    fun onForegroundAppChanged(
        newPackageName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<UsageSession> {
        val completed = mutableListOf<UsageSession>()

        if (activePackage != null && activePackage != newPackageName) {
            closeCurrentSession(nowMillis)?.let { completed.add(it) }
        }

        if (MonitoredApps.isMonitored(newPackageName) && activePackage != newPackageName) {
            activePackage = newPackageName
            activeLabel = MonitoredApps.getDisplayName(newPackageName)
            sessionStartMillis = nowMillis
        }

        return completed
    }

    fun onTrackingStopped(nowMillis: Long = System.currentTimeMillis()): UsageSession? {
        return closeCurrentSession(nowMillis)
    }

    fun closeCurrentSession(nowMillis: Long = System.currentTimeMillis()): UsageSession? {
        val pkg = activePackage ?: return null
        val label = activeLabel ?: MonitoredApps.getDisplayName(pkg)
        val start = sessionStartMillis ?: return null

        activePackage = null
        activeLabel = null
        sessionStartMillis = null

        val clampedEnd = nowMillis.coerceAtMost(start + maxSessionDurationMs)
        if (clampedEnd <= start + 500L) return null

        return UsageSession(
            packageName = pkg,
            appLabel = label,
            startTime = start,
            endTime = clampedEnd
        )
    }
}
