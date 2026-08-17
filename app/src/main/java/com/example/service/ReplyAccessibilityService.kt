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
            "online", "typing...", "unread messages", "today", "yesterday", "home",
            "new tab", "tabs", "search or type url", "bookmarks", "history", "downloads",
            "settings", "reload", "share", "close tab", "switch tab", "menu", "open",
            "close", "save", "delete", "edit", "copy", "cut", "paste", "select all"
        )

        private val IGNORED_CLASS_NAMES = setOf(
            "android.widget.button",
            "android.widget.imagebutton",
            "android.widget.tabwidget",
            "android.widget.seekbar",
            "android.widget.progressbar",
            "android.widget.switch",
            "android.widget.radiobutton",
            "android.widget.checkbox",
            "android.widget.edittext"
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
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 400
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // If screen analysis is toggled OFF, immediately return to save resources and battery
        if (!OverlayStateManager.state.value.settings.isScreenAnalysisOn) return

        val pkgName = event.packageName?.toString() ?: return

        // Skip events from our own app to avoid feedback loops
        if (pkgName == packageName) return

        // Skip system keyboards, launcher UI, and system bars
        if (IGNORED_PACKAGES.contains(pkgName)) return

        // Process only relevant event types
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
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
            delay(500) // 500ms debounce window
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

            // Deduplication: Avoid re-triggering for identical text within 3 seconds
            if (cleanText.equals(lastExtractedText, ignoreCase = true) && (now - lastExtractedTime) < 3000) {
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

        val isIgnoredClass = IGNORED_CLASS_NAMES.any { className.contains(it) }

        if (!candidateText.isNullOrBlank() && !isIgnoredClass && isValidMessageText(candidateText)) {
            val isLikelyTimestamp = candidateText.matches(Regex("^[0-9]{1,2}:[0-9]{2}(\\s?[AaPp][Mm])?$"))
            val isLikelyUrl = candidateText.contains("http://") || candidateText.contains("https://") ||
                candidateText.contains("www.") || candidateText.endsWith(".com") || candidateText.endsWith(".org")

            if (!isLikelyTimestamp && !isLikelyUrl) {
                var confidence = 0.0f
                val lower = candidateText.lowercase()

                // Chat view ID signals
                if (viewId.contains("message") || viewId.contains("chat") || viewId.contains("msg") ||
                    viewId.contains("body") || viewId.contains("conversation") || viewId.contains("bubble")
                ) {
                    confidence += 0.45f
                }

                // Question signal
                if (candidateText.contains("?")) {
                    confidence += 0.35f
                }

                // Conversational keywords / starters
                val conversationalPhrases = listOf(
                    "hey", "hello", "hi ", "can you", "what", "where", "how", "when", "why", "who",
                    "are you", "will you", "could you", "would you", "is it", "let's", "do you", "thank",
                    "please", "sure", "sounds good", "free", "meet", "lunch", "sync", "time", "call"
                )
                if (conversationalPhrases.any { lower.contains(it) }) {
                    confidence += 0.30f
                }

                // Sentence length signal
                if (candidateText.length >= 15 && candidateText.contains(" ")) {
                    confidence += 0.15f
                }

                if (confidence >= 0.45f) {
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
        val lower = trimmed.lowercase()
        if (IGNORED_TEXT_EXACT.contains(lower)) return false
        if (lower.startsWith("tab ") || lower.contains("new tab") || lower.contains("close tab")) return false
        // Exclude pure numbers / symbols
        if (trimmed.matches(Regex("^[0-9.,:;+\\-*/%#@!$&()]+$"))) return false
        return true
    }

    private fun findBestCandidateMessage(nodes: List<NodeTextData>): NodeTextData? {
        if (nodes.isEmpty()) return null

        // Require minimum confidence of 0.45 so random UI elements don't trigger
        val highConfidenceNodes = nodes.filter { it.confidence >= 0.45f }
        if (highConfidenceNodes.isEmpty()) return null

        return highConfidenceNodes.maxByOrNull { it.confidence * 2f + it.depth }
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
