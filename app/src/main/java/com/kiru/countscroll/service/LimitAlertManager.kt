package com.kiru.countscroll.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.kiru.countscroll.MainActivity
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.domain.LimitStatus
import com.kiru.countscroll.util.AccessibilityHelper

class LimitAlertManager(
    private val context: Context,
    private val onSnoozeClicked: (unit: String, value: Int) -> Unit,
    private val onExitToHomeClicked: () -> Unit
) {
    companion object {
        private const val CHANNEL_ID = "count_scroll_limits"
        private const val WARNING_NOTIF_ID = 2001
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var dialogView: View? = null
    private var isDialogAdded = false
    private var lastWarningDayKey: String? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan & Batas Scroll",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat mendekati batas scroll harian"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWarningNotificationIfNeeded(
        dayKey: String,
        status: LimitStatus,
        settings: UserSettings
    ) {
        if (lastWarningDayKey == dayKey) return
        lastWarningDayKey = dayKey

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Mendekati Batas Scroll (${settings.warningThresholdPercent}%)")
            .setContentText(
                "Sudah ${status.currentScrolls} dari target ${status.effectiveDailyTarget} scroll hari ini."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            notificationManager.notify(WARNING_NOTIF_ID, notification)
        }
    }

    fun showLimitDialog(status: LimitStatus, settings: UserSettings) {
        mainHandler.post {
            if (!AccessibilityHelper.canDrawOverlays(context)) return@post
            if (isDialogAdded && dialogView != null) {
                return@post
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(56, 48, 56, 48)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#141414"))
                    setStroke(3, Color.parseColor("#FFFFFF"))
                    cornerRadius = 36f
                }
            }

            val titleTv = TextView(context).apply {
                text = "Batas Scroll Tercapai"
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 20f
                setPadding(0, 0, 0, 16)
            }

            val infoTv = TextView(context).apply {
                text = "Kamu sudah mencapai ${status.currentScrolls} / ${status.effectiveDailyTarget} scroll hari ini.
Tunda terpakai: ${status.snoozeCountToday}/${settings.maxDelayPerDay}x."
                setTextColor(Color.parseColor("#D4D4D4"))
                textSize = 14f
                setPadding(0, 0, 0, 32)
            }

            val actionsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val exitBtn = Button(context).apply {
                text = "Keluar"
                setBackgroundColor(Color.parseColor("#FFFFFF"))
                setTextColor(Color.parseColor("#0A0A0A"))
                setOnClickListener {
                    dismissLimitDialog()
                    onExitToHomeClicked()
                }
            }

            actionsRow.addView(exitBtn)

            if (status.canSnoozeMore) {
                val snoozeLabel = if (settings.delayUnit == "SCROLL") {
                    "Tunda (${settings.delayValue}x scroll)"
                } else {
                    "Tunda (${settings.delayValue} menit)"
                }

                val snoozeBtn = Button(context).apply {
                    text = snoozeLabel
                    setBackgroundColor(Color.parseColor("#262626"))
                    setTextColor(Color.parseColor("#FFFFFF"))
                    setOnClickListener {
                        dismissLimitDialog()
                        onSnoozeClicked(settings.delayUnit, settings.delayValue)
                    }
                }

                val spacer = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(24, 1)
                }
                actionsRow.addView(spacer)
                actionsRow.addView(snoozeBtn)
            }

            container.addView(titleTv)
            container.addView(infoTv)
            container.addView(actionsRow)

            val typeFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                typeFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                x = 0
                y = 0
            }

            runCatching {
                windowManager.addView(container, params)
                dialogView = container
                isDialogAdded = true
            }
        }
    }

    fun dismissLimitDialog() {
        mainHandler.post {
            if (isDialogAdded && dialogView != null) {
                runCatching { windowManager.removeView(dialogView) }
            }
            isDialogAdded = false
            dialogView = null
        }
    }
}
