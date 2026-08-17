package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.OverlayStateManager
import com.example.model.DetectedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Accessibility Service responsible for observing on-screen conversation events,
 * debouncing high-frequency UI updates, and extracting structured incoming messages.
 */
class ReplyAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceConnected: Boolean = false
            private set

        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.touchtype.swiftkey",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3"
        )

        private val IGNORED_TEXT_EXACT = setOf(
            "send", "reply", "back", "cancel", "search", "more", "options", "done",
            "type a message", "message", "chat", "voice message", "call", "video call",
            "online", "typing...", "unread messages", "today", "yesterday"
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analysisDebounceJob: Job? = null
    private var lastExtractedText: String = ""
    private var lastExtractedTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        OverlayStateManager.refreshPermissions(this)

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 300
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // If screen analysis is toggled OFF, immediately return to save resources and battery
        if (!OverlayStateManager.state.value.settings.isScreenAnalysisOn) return

        val pkgName = event.packageName?.toString() ?: return

        // Skip events from our own app to avoid feedback loops
        if (pkgName == packageName) return

        // Skip system keyboards and launcher UI
        if (IGNORED_PACKAGES.contains(pkgName)) return

        // Process only relevant event types
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                scheduleScreenAnalysis(pkgName)
            }
        }
    }

    /**
     * Debounces rapid UI changes (such as fast typing or continuous animations)
     * before running node tree analysis.
     */
    private fun scheduleScreenAnalysis(packageName: String) {
        analysisDebounceJob?.cancel()
        analysisDebounceJob = serviceScope.launch {
            delay(400) // 400ms debounce window
            extractAndProcessLatestMessage(packageName)
        }
    }

    private fun extractAndProcessLatestMessage(packageName: String) {
        val rootNode = rootInActiveWindow ?: return

        try {
            val appLabel = getAppLabel(packageName)
            val extractedNodes = mutableListOf<NodeTextData>()

            traverseNodeHierarchy(rootNode, extractedNodes, depth = 0, maxDepth = 18)

            val candidate = findBestCandidateMessage(extractedNodes) ?: return

            val cleanText = candidate.text.trim()
            val now = System.currentTimeMillis()

            // Deduplication: Avoid re-triggering for identical text within 2 seconds
            if (cleanText.equals(lastExtractedText, ignoreCase = true) && (now - lastExtractedTime) < 2000) {
                return
            }

            lastExtractedText = cleanText
            lastExtractedTime = now

            val detectedMessage = DetectedMessage(
                eventId = UUID.randomUUID().toString(),
                text = cleanText,
                sender = candidate.sender ?: "Sender",
                sourceApp = appLabel,
                packageName = packageName,
                timestamp = now,
                confidence = candidate.confidence,
                isIncoming = true
            )

            serviceScope.launch(Dispatchers.Main) {
                OverlayStateManager.onNewMessageDetected(detectedMessage)
            }
        } catch (e: Exception) {
            // Safe silent error boundary to prevent accessibility service crash
        } finally {
            try {
                rootNode.recycle()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun traverseNodeHierarchy(
        node: AccessibilityNodeInfo?,
        results: MutableList<NodeTextData>,
        depth: Int,
        maxDepth: Int
    ) {
        if (node == null || depth > maxDepth || results.size > 80) return

        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""

        val candidateText = when {
            !text.isNullOrBlank() -> text
            !contentDesc.isNullOrBlank() -> contentDesc
            else -> null
        }

        if (!candidateText.isNullOrBlank() && isValidMessageText(candidateText)) {
            val isLikelyEditText = className.contains("edittext") || viewId.contains("input") || viewId.contains("entry")
            val isLikelyTimestamp = candidateText.matches(Regex("^[0-9]{1,2}:[0-9]{2}(\\s?[AaPp][Mm])?$"))

            if (!isLikelyEditText && !isLikelyTimestamp) {
                var confidence = 0.5f
                if (viewId.contains("message") || viewId.contains("text") || viewId.contains("body") || viewId.contains("msg")) {
                    confidence += 0.3f
                }
                if (candidateText.contains("?") || candidateText.length > 20) {
                    confidence += 0.2f
                }

                results.add(
                    NodeTextData(
                        text = candidateText,
                        viewId = viewId,
                        depth = depth,
                        confidence = confidence.coerceIn(0.1f, 1.0f)
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNodeHierarchy(child, results, depth + 1, maxDepth)
            try {
                child?.recycle()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun isValidMessageText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 3) return false
        if (IGNORED_TEXT_EXACT.contains(trimmed.lowercase())) return false
        // Exclude pure numbers / icons
        if (trimmed.matches(Regex("^[0-9.,:;+]+$"))) return false
        return true
    }

    private fun findBestCandidateMessage(nodes: List<NodeTextData>): NodeTextData? {
        if (nodes.isEmpty()) return null

        // In most chat apps, latest incoming messages appear near the bottom or have high message viewId relevance
        return nodes
            .filter { it.confidence >= 0.5f }
            .maxByOrNull { it.confidence * 2f + it.depth }
            ?: nodes.lastOrNull()
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    override fun onInterrupt() {
        isServiceConnected = false
    }

    override fun onDestroy() {
        isServiceConnected = false
        analysisDebounceJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private data class NodeTextData(
        val text: String,
        val viewId: String,
        val depth: Int,
        val confidence: Float,
        val sender: String? = null
    )
}
