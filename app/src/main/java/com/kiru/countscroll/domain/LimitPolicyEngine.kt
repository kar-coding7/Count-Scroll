package com.kiru.countscroll.domain

import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.data.local.entity.DelayLog
import kotlin.math.pow
import kotlin.math.roundToInt

enum class LimitEvaluationState {
    NORMAL,
    WARNING_THRESHOLD,
    SNOOZED,
    LIMIT_REACHED
}

data class LimitStatus(
    val effectiveDailyTarget: Int,
    val currentScrolls: Int,
    val state: LimitEvaluationState,
    val snoozeCountToday: Int,
    val totalSnoozeValueToday: Int,
    val canSnoozeMore: Boolean,
    val snoozeRemainingMinutes: Int,
    val snoozeRemainingScrolls: Int
)

object LimitPolicyEngine {

    private const val WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L

    fun calculateEffectiveDailyTarget(
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val base = settings.baseDailyScrollTarget.coerceAtLeast(1)
        val minTarget = settings.minimumScrollTarget.coerceAtLeast(1)

        if (!settings.gradualReductionEnabled || settings.weeklyReductionPercent <= 0) {
            return base.coerceAtLeast(minTarget)
        }

        val anchor = if (settings.targetAnchorMillis > 0L) settings.targetAnchorMillis else nowMillis
        val elapsedWeeks = ((nowMillis - anchor).coerceAtLeast(0L) / WEEK_MILLIS).toInt()
        if (elapsedWeeks <= 0) {
            return base.coerceAtLeast(minTarget)
        }

        val factor = (1.0 - (settings.weeklyReductionPercent / 100.0)).pow(elapsedWeeks.toDouble())
        val reduced = (base * factor).roundToInt()
        return reduced.coerceAtLeast(minTarget)
    }

    fun evaluate(
        settings: UserSettings,
        todayScrolls: Int,
        todayDelayLogs: List<DelayLog>,
        nowMillis: Long = System.currentTimeMillis()
    ): LimitStatus {
        val target = calculateEffectiveDailyTarget(settings, nowMillis)
        val snoozeLogs = todayDelayLogs.filter { it.actionType == "TUNDA" }
        val snoozeCountToday = snoozeLogs.size
        val totalSnoozeValueToday = snoozeLogs.sumOf { it.delayValue.coerceAtLeast(0) }

        val canSnoozeMore = snoozeCountToday < settings.maxDelayPerDay &&
            (totalSnoozeValueToday + settings.delayValue) <= settings.maxTotalDelayPerDay

        val remainingMinutes = if (settings.snoozeUntilMillis > nowMillis) {
            (((settings.snoozeUntilMillis - nowMillis) + 59_999L) / 60_000L).toInt()
        } else 0

        val remainingScrolls = settings.snoozeExtraScrolls.coerceAtLeast(0)

        if (!settings.isLimitEnabled) {
            return LimitStatus(
                effectiveDailyTarget = target,
                currentScrolls = todayScrolls,
                state = LimitEvaluationState.NORMAL,
                snoozeCountToday = snoozeCountToday,
                totalSnoozeValueToday = totalSnoozeValueToday,
                canSnoozeMore = canSnoozeMore,
                snoozeRemainingMinutes = 0,
                snoozeRemainingScrolls = 0
            )
        }

        val baseLimitReached = todayScrolls >= target
        if (baseLimitReached) {
            val isCurrentlySnoozed = (settings.delayUnit == "MENIT" && remainingMinutes > 0) ||
                (settings.delayUnit == "SCROLL" && remainingScrolls > 0)

            val evalState = if (isCurrentlySnoozed) {
                LimitEvaluationState.SNOOZED
            } else {
                LimitEvaluationState.LIMIT_REACHED
            }

            return LimitStatus(
                effectiveDailyTarget = target,
                currentScrolls = todayScrolls,
                state = evalState,
                snoozeCountToday = snoozeCountToday,
                totalSnoozeValueToday = totalSnoozeValueToday,
                canSnoozeMore = canSnoozeMore,
                snoozeRemainingMinutes = remainingMinutes,
                snoozeRemainingScrolls = remainingScrolls
            )
        }

        val thresholdScrolls = ((target * settings.warningThresholdPercent) / 100.0).toInt().coerceAtLeast(1)
        val evalState = if (todayScrolls >= thresholdScrolls) {
            LimitEvaluationState.WARNING_THRESHOLD
        } else {
            LimitEvaluationState.NORMAL
        }

        return LimitStatus(
            effectiveDailyTarget = target,
            currentScrolls = todayScrolls,
            state = evalState,
            snoozeCountToday = snoozeCountToday,
            totalSnoozeValueToday = totalSnoozeValueToday,
            canSnoozeMore = canSnoozeMore,
            snoozeRemainingMinutes = remainingMinutes,
            snoozeRemainingScrolls = remainingScrolls
        )
    }
}
