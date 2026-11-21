package com.kiru.countscroll.domain

import com.kiru.countscroll.data.local.entity.ScrollEvent
import com.kiru.countscroll.data.local.entity.UsageSession
import java.util.Calendar
import java.util.Locale

enum class StatsPeriod(val label: String) {
    TODAY("Hari ini"),
    WEEK("Minggu"),
    MONTH("Bulan")
}

data class DailyBarPoint(
    val dayIndex: Int,
    val label: String,
    val scrollCount: Int,
    val durationMillis: Long
)

data class StatisticsReport(
    val period: StatsPeriod,
    val filter: AppFilter,
    val totalScrolls: Int,
    val totalDurationMillis: Long,
    val comparisonText: String,
    val dailyPoints: List<DailyBarPoint>
)

object StatisticsCalculator {

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getWindowBounds(period: StatsPeriod, nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val todayStart = startOfDay(nowMillis)
        val endExclusive = todayStart + 86_400_000L

        return when (period) {
            StatsPeriod.TODAY -> todayStart to endExclusive
            StatsPeriod.WEEK -> (todayStart - 6 * 86_400_000L) to endExclusive
            StatsPeriod.MONTH -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startMonth = cal.timeInMillis
                val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val endMonth = startMonth + maxDaysInMonth * 86_400_000L
                startMonth to endMonth
            }
        }
    }

    fun getPreviousWindowBounds(period: StatsPeriod, nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val (currentStart, currentEnd) = getWindowBounds(period, nowMillis)
        val duration = currentEnd - currentStart
        return (currentStart - duration) to currentStart
    }

    fun buildReport(
        period: StatsPeriod,
        filter: AppFilter,
        currentScrolls: List<ScrollEvent>,
        currentSessions: List<UsageSession>,
        previousScrolls: List<ScrollEvent>,
        nowMillis: Long = System.currentTimeMillis()
    ): StatisticsReport {
        val filteredScrolls = currentScrolls.filter { MonitoredApps.matchesFilter(it.packageName, filter) }
        val filteredSessions = currentSessions.filter { MonitoredApps.matchesFilter(it.packageName, filter) }
        val filteredPrevScrolls = previousScrolls.filter { MonitoredApps.matchesFilter(it.packageName, filter) }

        val totalScrolls = filteredScrolls.size
        val totalDurationMillis = filteredSessions.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }

        val prevCount = filteredPrevScrolls.size
        val comparisonText = buildComparisonText(period, totalScrolls, prevCount)

        val dailyPoints = buildDailyPoints(period, filteredScrolls, filteredSessions, nowMillis)

        return StatisticsReport(
            period = period,
            filter = filter,
            totalScrolls = totalScrolls,
            totalDurationMillis = totalDurationMillis,
            comparisonText = comparisonText,
            dailyPoints = dailyPoints
        )
    }

    private fun buildComparisonText(period: StatsPeriod, current: Int, previous: Int): String {
        val periodLabel = when (period) {
            StatsPeriod.TODAY -> "kemarin"
            StatsPeriod.WEEK -> "minggu lalu"
            StatsPeriod.MONTH -> "bulan lalu"
        }

        if (previous <= 0) {
            return if (current > 0) {
                "Belum ada data pembanding dari $periodLabel"
            } else {
                "Belum ada aktivitas scroll tercatat"
            }
        }

        val diffPercent = (((current - previous).toDouble() / previous.toDouble()) * 100.0).toInt()
        return when {
            diffPercent < 0 -> "Turun ${Math.abs(diffPercent)}% dari $periodLabel"
            diffPercent > 0 -> "Naik $diffPercent% dari $periodLabel"
            else -> "Sama dengan $periodLabel"
        }
    }

    private fun buildDailyPoints(
        period: StatsPeriod,
        scrolls: List<ScrollEvent>,
        sessions: List<UsageSession>,
        nowMillis: Long
    ): List<DailyBarPoint> {
        val todayStart = startOfDay(nowMillis)
        val dayLocale = Locale("id", "ID")

        return when (period) {
            StatsPeriod.TODAY -> {
                val cal = Calendar.getInstance(dayLocale).apply { timeInMillis = todayStart }
                val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, dayLocale) ?: "Hari"
                listOf(
                    DailyBarPoint(
                        dayIndex = 1,
                        label = dayName,
                        scrollCount = scrolls.size,
                        durationMillis = sessions.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }
                    )
                )
            }

            StatsPeriod.WEEK -> {
                (6 downTo 0).map { offset ->
                    val dayStart = todayStart - offset * 86_400_000L
                    val dayEnd = dayStart + 86_400_000L
                    val cal = Calendar.getInstance(dayLocale).apply { timeInMillis = dayStart }
                    val shortDay = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, dayLocale)
                        ?.take(3) ?: "H${7 - offset}"

                    val dayScrollCount = scrolls.count { it.timestamp in dayStart until dayEnd }
                    val dayDuration = sessions
                        .filter { it.startTime in dayStart until dayEnd }
                        .sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }

                    DailyBarPoint(
                        dayIndex = 7 - offset,
                        label = shortDay,
                        scrollCount = dayScrollCount,
                        durationMillis = dayDuration
                    )
                }
            }

            StatsPeriod.MONTH -> {
                val cal = Calendar.getInstance(dayLocale).apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val monthStart = cal.timeInMillis
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                (1..daysInMonth).map { dayOfMonth ->
                    val dayStart = monthStart + (dayOfMonth - 1) * 86_400_000L
                    val dayEnd = dayStart + 86_400_000L
                    val dayScrollCount = scrolls.count { it.timestamp in dayStart until dayEnd }
                    val dayDuration = sessions
                        .filter { it.startTime in dayStart until dayEnd }
                        .sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }

                    DailyBarPoint(
                        dayIndex = dayOfMonth,
                        label = dayOfMonth.toString(),
                        scrollCount = dayScrollCount,
                        durationMillis = dayDuration
                    )
                }
            }
        }
    }

    fun formatDurationHoursMinutes(durationMillis: Long): String {
        val totalMinutes = (durationMillis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return "${hours}j ${minutes}m"
    }
}
