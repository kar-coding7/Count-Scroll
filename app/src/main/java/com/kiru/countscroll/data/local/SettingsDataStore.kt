package com.kiru.countscroll.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "count_scroll_settings")

enum class OverlayShape(val label: String) {
    CIRCLE("Bulat"),
    BOX("Kotak")
}

enum class OverlayColorMode(val label: String) {
    DARK("Hitam"),
    LIGHT("Putih")
}

data class UserSettings(
    val isTrackingEnabled: Boolean = false,
    val baseDailyScrollTarget: Int = 100,
    val gradualReductionEnabled: Boolean = true,
    val weeklyReductionPercent: Int = 10,
    val minimumScrollTarget: Int = 20,
    val targetAnchorMillis: Long = 0L,
    val warningThresholdPercent: Int = 80,
    val isLimitEnabled: Boolean = true,
    val delayUnit: String = "MENIT",
    val delayValue: Int = 5,
    val maxDelayPerDay: Int = 3,
    val maxTotalDelayPerDay: Int = 15,
    val snoozeUntilMillis: Long = 0L,
    val snoozeExtraScrolls: Int = 0,
    val isOverlayEnabled: Boolean = true,
    val overlaySizeSp: Float = 18f,
    val overlayAlpha: Float = 0.85f,
    val overlayShape: OverlayShape = OverlayShape.CIRCLE,
    val overlayColorMode: OverlayColorMode = OverlayColorMode.DARK,
    val overlayPosXRatio: Float = 0.80f,
    val overlayPosYRatio: Float = 0.22f,
    val themeMode: String = "SYSTEM",
    val lastBootTimestamp: Long = 0L
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_TRACKING_ENABLED = booleanPreferencesKey("is_tracking_enabled")
        val BASE_DAILY_SCROLL_TARGET = intPreferencesKey("base_daily_scroll_target")
        val GRADUAL_REDUCTION_ENABLED = booleanPreferencesKey("gradual_reduction_enabled")
        val WEEKLY_REDUCTION_PERCENT = intPreferencesKey("weekly_reduction_percent")
        val MINIMUM_SCROLL_TARGET = intPreferencesKey("minimum_scroll_target")
        val TARGET_ANCHOR_MILLIS = longPreferencesKey("target_anchor_millis")
        val WARNING_THRESHOLD_PERCENT = intPreferencesKey("warning_threshold_percent")
        val IS_LIMIT_ENABLED = booleanPreferencesKey("is_limit_enabled")
        val DELAY_UNIT = stringPreferencesKey("delay_unit")
        val DELAY_VALUE = intPreferencesKey("delay_value")
        val MAX_DELAY_PER_DAY = intPreferencesKey("max_delay_per_day")
        val MAX_TOTAL_DELAY_PER_DAY = intPreferencesKey("max_total_delay_per_day")
        val SNOOZE_UNTIL_MILLIS = longPreferencesKey("snooze_until_millis")
        val SNOOZE_EXTRA_SCROLLS = intPreferencesKey("snooze_extra_scrolls")
        val IS_OVERLAY_ENABLED = booleanPreferencesKey("is_overlay_enabled")
        val OVERLAY_SIZE_SP = floatPreferencesKey("overlay_size_sp")
        val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        val OVERLAY_SHAPE = stringPreferencesKey("overlay_shape")
        val OVERLAY_COLOR_MODE = stringPreferencesKey("overlay_color_mode")
        val OVERLAY_POS_X_RATIO = floatPreferencesKey("overlay_pos_x_ratio")
        val OVERLAY_POS_Y_RATIO = floatPreferencesKey("overlay_pos_y_ratio")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_BOOT_TIMESTAMP = longPreferencesKey("last_boot_timestamp")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val shape = runCatching {
            OverlayShape.valueOf(prefs[Keys.OVERLAY_SHAPE] ?: OverlayShape.CIRCLE.name)
        }.getOrDefault(OverlayShape.CIRCLE)

        val colorMode = runCatching {
            OverlayColorMode.valueOf(prefs[Keys.OVERLAY_COLOR_MODE] ?: OverlayColorMode.DARK.name)
        }.getOrDefault(OverlayColorMode.DARK)

        val now = System.currentTimeMillis()
        val anchor = prefs[Keys.TARGET_ANCHOR_MILLIS] ?: 0L

        UserSettings(
            isTrackingEnabled = prefs[Keys.IS_TRACKING_ENABLED] ?: false,
            baseDailyScrollTarget = (prefs[Keys.BASE_DAILY_SCROLL_TARGET] ?: 100).coerceAtLeast(1),
            gradualReductionEnabled = prefs[Keys.GRADUAL_REDUCTION_ENABLED] ?: true,
            weeklyReductionPercent = (prefs[Keys.WEEKLY_REDUCTION_PERCENT] ?: 10).coerceIn(0, 80),
            minimumScrollTarget = (prefs[Keys.MINIMUM_SCROLL_TARGET] ?: 20).coerceAtLeast(1),
            targetAnchorMillis = if (anchor > 0L) anchor else now,
            warningThresholdPercent = (prefs[Keys.WARNING_THRESHOLD_PERCENT] ?: 80).coerceIn(10, 99),
            isLimitEnabled = prefs[Keys.IS_LIMIT_ENABLED] ?: true,
            delayUnit = prefs[Keys.DELAY_UNIT] ?: "MENIT",
            delayValue = (prefs[Keys.DELAY_VALUE] ?: 5).coerceAtLeast(1),
            maxDelayPerDay = (prefs[Keys.MAX_DELAY_PER_DAY] ?: 3).coerceAtLeast(0),
            maxTotalDelayPerDay = (prefs[Keys.MAX_TOTAL_DELAY_PER_DAY] ?: 15).coerceAtLeast(1),
            snoozeUntilMillis = prefs[Keys.SNOOZE_UNTIL_MILLIS] ?: 0L,
            snoozeExtraScrolls = (prefs[Keys.SNOOZE_EXTRA_SCROLLS] ?: 0).coerceAtLeast(0),
            isOverlayEnabled = prefs[Keys.IS_OVERLAY_ENABLED] ?: true,
            overlaySizeSp = (prefs[Keys.OVERLAY_SIZE_SP] ?: 18f).coerceIn(13f, 28f),
            overlayAlpha = (prefs[Keys.OVERLAY_ALPHA] ?: 0.85f).coerceIn(0.25f, 1.0f),
            overlayShape = shape,
            overlayColorMode = colorMode,
            overlayPosXRatio = (prefs[Keys.OVERLAY_POS_X_RATIO] ?: 0.80f).coerceIn(0.05f, 0.90f),
            overlayPosYRatio = (prefs[Keys.OVERLAY_POS_Y_RATIO] ?: 0.22f).coerceIn(0.08f, 0.85f),
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            lastBootTimestamp = prefs[Keys.LAST_BOOT_TIMESTAMP] ?: 0L
        )
    }

    suspend fun recordBootTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BOOT_TIMESTAMP] = timestamp
        }
    }

    suspend fun ensureTargetAnchorInitialized() {
        context.dataStore.edit { prefs ->
            if ((prefs[Keys.TARGET_ANCHOR_MILLIS] ?: 0L) <= 0L) {
                prefs[Keys.TARGET_ANCHOR_MILLIS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_TRACKING_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun updateTargetSettings(
        baseTarget: Int,
        gradualEnabled: Boolean,
        reductionPercent: Int,
        minTarget: Int,
        resetAnchorNow: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_DAILY_SCROLL_TARGET] = baseTarget.coerceAtLeast(1)
            prefs[Keys.GRADUAL_REDUCTION_ENABLED] = gradualEnabled
            prefs[Keys.WEEKLY_REDUCTION_PERCENT] = reductionPercent.coerceIn(0, 80)
            prefs[Keys.MINIMUM_SCROLL_TARGET] = minTarget.coerceAtLeast(1)
            if (resetAnchorNow) {
                prefs[Keys.TARGET_ANCHOR_MILLIS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun updateLimitSettings(
        limitEnabled: Boolean,
        warningPercent: Int,
        delayUnit: String,
        delayValue: Int,
        maxDelayPerDay: Int,
        maxTotalDelayPerDay: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LIMIT_ENABLED] = limitEnabled
            prefs[Keys.WARNING_THRESHOLD_PERCENT] = warningPercent.coerceIn(10, 99)
            prefs[Keys.DELAY_UNIT] = if (delayUnit == "SCROLL") "SCROLL" else "MENIT"
            prefs[Keys.DELAY_VALUE] = delayValue.coerceAtLeast(1)
            prefs[Keys.MAX_DELAY_PER_DAY] = maxDelayPerDay.coerceAtLeast(0)
            prefs[Keys.MAX_TOTAL_DELAY_PER_DAY] = maxTotalDelayPerDay.coerceAtLeast(1)
        }
    }

    suspend fun applySnooze(unit: String, value: Int, nowMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            if (unit == "SCROLL") {
                val currentExtra = prefs[Keys.SNOOZE_EXTRA_SCROLLS] ?: 0
                prefs[Keys.SNOOZE_EXTRA_SCROLLS] = currentExtra + value.coerceAtLeast(1)
            } else {
                val currentUntil = prefs[Keys.SNOOZE_UNTIL_MILLIS] ?: 0L
                val base = maxOf(currentUntil, nowMillis)
                prefs[Keys.SNOOZE_UNTIL_MILLIS] = base + value.coerceAtLeast(1) * 60_000L
            }
        }
    }

    suspend fun consumeOneSnoozeScrollIfActive() {
        context.dataStore.edit { prefs ->
            val currentExtra = prefs[Keys.SNOOZE_EXTRA_SCROLLS] ?: 0
            if (currentExtra > 0) {
                prefs[Keys.SNOOZE_EXTRA_SCROLLS] = currentExtra - 1
            }
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setOverlaySizeSp(sizeSp: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_SIZE_SP] = sizeSp.coerceIn(13f, 28f)
        }
    }

    suspend fun setOverlayAlpha(alpha: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_ALPHA] = alpha.coerceIn(0.25f, 1.0f)
        }
    }

    suspend fun setOverlayShape(shape: OverlayShape) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_SHAPE] = shape.name
        }
    }

    suspend fun setOverlayColorMode(colorMode: OverlayColorMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_COLOR_MODE] = colorMode.name
        }
    }

    suspend fun setOverlayPositionRatio(xRatio: Float, yRatio: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_POS_X_RATIO] = xRatio.coerceIn(0.05f, 0.90f)
            prefs[Keys.OVERLAY_POS_Y_RATIO] = yRatio.coerceIn(0.08f, 0.85f)
        }
    }
}
