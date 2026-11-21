package com.kiru.countscroll.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.kiru.countscroll.data.local.OverlayColorMode
import com.kiru.countscroll.data.local.OverlayShape
import com.kiru.countscroll.data.local.UserSettings
import com.kiru.countscroll.util.AccessibilityHelper

class OverlayController(
    private val context: Context,
    private val onPositionSaved: (xRatio: Float, yRatio: Float) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false

    private var currentCount: Int = 0
    private var currentSettings: UserSettings = UserSettings()
    private var shouldShowOverlay: Boolean = false

    fun updateState(
        shouldShow: Boolean,
        count: Int,
        settings: UserSettings
    ) {
        mainHandler.post {
            this.shouldShowOverlay = shouldShow &&
                settings.isTrackingEnabled &&
                settings.isOverlayEnabled &&
                AccessibilityHelper.canDrawOverlays(context)
            this.currentCount = count
            this.currentSettings = settings

            if (!this.shouldShowOverlay) {
                removeOverlay()
            } else {
                ensureOverlayView()
                renderOverlay()
            }
        }
    }

    fun removeOverlay() {
        mainHandler.post {
            if (isAdded && overlayView != null) {
                runCatching { windowManager.removeView(overlayView) }
            }
            isAdded = false
            overlayView = null
            layoutParams = null
        }
    }

    private fun ensureOverlayView() {
        if (isAdded && overlayView != null && layoutParams != null) return

        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.coerceAtLeast(1)
        val screenHeight = metrics.heightPixels.coerceAtLeast(1)

        val initialX = ((screenWidth * currentSettings.overlayPosXRatio) - 60f)
            .toInt()
            .coerceIn(0, (screenWidth - 120).coerceAtLeast(0))
        val initialY = ((screenHeight * currentSettings.overlayPosYRatio) - 60f)
            .toInt()
            .coerceIn(0, (screenHeight - 120).coerceAtLeast(0))

        val typeFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val view = TextView(context).apply {
            gravity = Gravity.CENTER
            setOnTouchListener(createDragTouchListener(params))
        }

        runCatching {
            windowManager.addView(view, params)
            overlayView = view
            layoutParams = params
            isAdded = true
        }
    }

    private fun renderOverlay() {
        val view = overlayView ?: return
        val params = layoutParams ?: return

        view.text = currentCount.toString()
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSettings.overlaySizeSp)

        val padH = (currentSettings.overlaySizeSp * 0.75f).toInt().coerceAtLeast(12)
        val padV = (currentSettings.overlaySizeSp * 0.55f).toInt().coerceAtLeast(10)
        view.setPadding(padH, padV, padH, padV)

        val isDark = currentSettings.overlayColorMode == OverlayColorMode.DARK
        val bgColor = if (isDark) Color.parseColor("#0A0A0A") else Color.parseColor("#FFFFFF")
        val fgColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#0A0A0A")
        val strokeColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#0A0A0A")

        view.setTextColor(fgColor)
        view.alpha = currentSettings.overlayAlpha.coerceIn(0.25f, 1.0f)

        val shapeDrawable = GradientDrawable().apply {
            setColor(bgColor)
            setStroke(2, strokeColor)
            shape = when (currentSettings.overlayShape) {
                OverlayShape.CIRCLE -> GradientDrawable.OVAL
                OverlayShape.BOX -> GradientDrawable.RECTANGLE
            }
            if (currentSettings.overlayShape == OverlayShape.BOX) {
                cornerRadius = 18f
            }
        }
        view.background = shapeDrawable

        val minSize = (currentSettings.overlaySizeSp * 2.4f).toInt().coerceAtLeast(48)
        if (currentSettings.overlayShape == OverlayShape.CIRCLE) {
            view.minWidth = minSize
            view.minHeight = minSize
        } else {
            view.minWidth = minSize
            view.minHeight = 0
        }

        if (isAdded) {
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun createDragTouchListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        return View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
                        moved = true
                    }
                    params.x = (initialX + dx).coerceAtLeast(0)
                    params.y = (initialY + dy).coerceAtLeast(0)
                    if (isAdded) {
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        val metrics = context.resources.displayMetrics
                        val width = metrics.widthPixels.coerceAtLeast(1).toFloat()
                        val height = metrics.heightPixels.coerceAtLeast(1).toFloat()
                        val xRatio = ((params.x + v.width / 2f) / width).coerceIn(0.05f, 0.90f)
                        val yRatio = ((params.y + v.height / 2f) / height).coerceIn(0.08f, 0.85f)
                        onPositionSaved(xRatio, yRatio)
                    }
                    true
                }

                else -> false
            }
        }
    }
}
