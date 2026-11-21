package com.kiru.countscroll.domain

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScrollDetectorEngine(
    private val minIntervalMs: Long = 350L
) {
    private var lastCountedTimestamp: Long = 0L
    private var lastPackageName: String? = null
    private var lastYouTubeItemIndex: Int? = null
    private var isYouTubeShortsActive: Boolean = false

    fun onWindowStateChanged(packageName: String, rootNode: AccessibilityNodeInfo?) {
        if (packageName != lastPackageName) {
            lastPackageName = packageName
            lastYouTubeItemIndex = null
        }

        if (packageName == MonitoredApps.YOUTUBE) {
            isYouTubeShortsActive = detectYouTubeShortsContext(rootNode)
        } else {
            isYouTubeShortsActive = false
        }
    }

    fun evaluateScrollEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false

        val pkg = event.packageName?.toString() ?: return false
        if (!MonitoredApps.isMonitored(pkg)) return false

        if (nowMillis - lastCountedTimestamp < minIntervalMs) {
            return false
        }

        return when {
            pkg in MonitoredApps.tiktokPackages -> {
                evaluateVerticalFeedScroll(event, rootNode, nowMillis)
            }

            pkg in MonitoredApps.instagramPackages -> {
                evaluateInstagramReelsScroll(event, rootNode, nowMillis)
            }

            pkg in MonitoredApps.youtubePackages -> {
                evaluateYouTubeShortsScroll(event, rootNode, nowMillis)
            }

            else -> false
        }
    }

    private fun evaluateVerticalFeedScroll(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        nowMillis: Long
    ): Boolean {
        val deltaX = Math.abs(event.scrollDeltaX)
        val deltaY = Math.abs(event.scrollDeltaY)

        if (deltaY == 0 && deltaX == 0 && event.fromIndex < 0 && event.toIndex < 0) {
            return false
        }
        if (deltaY in 1..24 && event.fromIndex == event.toIndex) {
            return false
        }
        if (!isFullscreenVerticalFeed(event, rootNode)) {
            return false
        }

        lastCountedTimestamp = nowMillis
        return true
    }

    private fun evaluateInstagramReelsScroll(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        nowMillis: Long
    ): Boolean {
        if (isModalOrSheetOpen(rootNode)) {
            return false
        }
        return evaluateVerticalFeedScroll(event, rootNode, nowMillis)
    }

    private fun evaluateYouTubeShortsScroll(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        nowMillis: Long
    ): Boolean {
        isYouTubeShortsActive = detectYouTubeShortsContext(rootNode)
        if (!isYouTubeShortsActive) {
            return false
        }

        val currentItemIndex = if (event.currentItemIndex >= 0) {
            event.currentItemIndex
        } else if (event.fromIndex >= 0) {
            event.fromIndex
        } else {
            -1
        }

        if (currentItemIndex >= 0) {
            val prevIndex = lastYouTubeItemIndex
            lastYouTubeItemIndex = currentItemIndex
            if (prevIndex != null && prevIndex != currentItemIndex) {
                lastCountedTimestamp = nowMillis
                return true
            }
        }

        return evaluateVerticalFeedScroll(event, rootNode, nowMillis)
    }

    private fun isFullscreenVerticalFeed(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        val source = event.source ?: rootNode ?: return true
        val rect = Rect()
        source.getBoundsInScreen(rect)
        return rect.height() >= 300 || rect.isEmpty
    }

    private fun isModalOrSheetOpen(rootNode: AccessibilityNodeInfo?): Boolean {
        val node = rootNode ?: return false
        return hasNodeWithTextOrIdHint(
            node = node,
            hints = listOf("add a comment", "tambahkan komentar", "comments", "komentar", "reply")
        )
    }

    private fun detectYouTubeShortsContext(rootNode: AccessibilityNodeInfo?): Boolean {
        val node = rootNode ?: return false
        return hasNodeWithTextOrIdHint(
            node = node,
            hints = listOf("shorts", "reel_recycler", "shorts_pivot_button", "dislike", "suka")
        )
    }

    private fun hasNodeWithTextOrIdHint(
        node: AccessibilityNodeInfo,
        hints: List<String>
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        var visited = 0

        while (queue.isNotEmpty() && visited < 120) {
            val current = queue.removeFirst()
            visited++

            val viewId = current.viewIdResourceName?.lowercase().orEmpty()
            val text = current.text?.toString()?.lowercase().orEmpty()
            val desc = current.contentDescription?.toString()?.lowercase().orEmpty()

            if (hints.any { hint ->
                    viewId.contains(hint) || text.contains(hint) || desc.contains(hint)
                }
            ) {
                return true
            }

            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }
}
