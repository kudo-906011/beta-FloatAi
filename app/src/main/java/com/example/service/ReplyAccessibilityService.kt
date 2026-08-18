package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ai.LanguageDetectionEngine
import com.example.data.OverlayStateManager
import com.example.model.DetectedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

/**
 * Intelligent Accessibility Service that responds ONLY to genuinely NEW questions/messages.
 * Ignores UI clutter (tabs, menus, buttons, status bars, timestamps, scrolling, old conversation).
 * Multi-lingual question detection: English, Hinglish, Bengali, Russian, Hindi, Spanish, etc.
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

        private val IGNORED_EXACT_OR_PREFIX = setOf(
            "send", "reply", "back", "cancel", "search", "more", "options", "done",
            "type a message", "message", "chat", "voice message", "call", "video call",
            "online", "typing...", "unread messages", "today", "yesterday", "home",
            "new tab", "tabs", "search or type url", "bookmarks", "history", "downloads",
            "settings", "reload", "share", "close tab", "switch tab", "menu", "open",
            "close", "save", "delete", "edit", "copy", "cut", "paste", "select all",
            "subscribe", "like", "follow", "comment", "posts", "reels", "shorts",
            "notifications", "view all", "show more", "show less", "mute", "block",
            "report", "clear chat", "media, links, and docs", "starred messages"
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

    // Ring-buffer for deduplicating already processed messages across screens & scrolling
    private val processedMessageHashes = ArrayDeque<Int>(60)
    private val processedMessageTexts = ArrayDeque<String>(60)
    private var lastProcessedNormalized: String = ""
    private var lastProcessedTimestamp: Long = 0L

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
        info.notificationTimeout = 350
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

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                scheduleScreenAnalysis(pkgName)
            }
        }
    }

    /**
     * Debounces rapid UI changes before running node tree analysis.
     */
    private fun scheduleScreenAnalysis(packageName: String) {
        analysisDebounceJob?.cancel()
        analysisDebounceJob = serviceScope.launch {
            delay(400) // 400ms debounce
            extractAndProcessLatestMessage(packageName)
        }
    }

    private fun extractAndProcessLatestMessage(packageName: String) {
        val rootNode = rootInActiveWindow ?: return

        try {
            val appLabel = getAppLabel(packageName)
            val extractedCandidates = mutableListOf<CandidateMessageNode>()

            traverseNodeHierarchy(rootNode, extractedCandidates, depth = 0, maxDepth = 20)

            // Identify the NEWEST valid conversational message/question
            val newestCandidate = findNewestValidCandidate(extractedCandidates) ?: return

            val cleanText = newestCandidate.text.trim()
            val normalized = normalizeMessage(cleanText)
            val textHash = normalized.hashCode()
            val now = System.currentTimeMillis()

            // Strict Deduplication Check:
            // 1. If exact normalized text matches the currently active question
            // 2. If it is already in our processed ring-buffer from earlier in this session
            // 3. If it was processed very recently (< 5 seconds ago)
            if (normalized == lastProcessedNormalized || processedMessageHashes.contains(textHash) || processedMessageTexts.contains(normalized)) {
                return
            }

            // Record as processed
            lastProcessedNormalized = normalized
            lastProcessedTimestamp = now
            if (processedMessageHashes.size >= 50) {
                processedMessageHashes.removeFirst()
                processedMessageTexts.removeFirst()
            }
            processedMessageHashes.addLast(textHash)
            processedMessageTexts.addLast(normalized)

            val detectedMessage = DetectedMessage(
                eventId = UUID.randomUUID().toString(),
                text = cleanText,
                sender = newestCandidate.sender ?: "Incoming Message",
                sourceApp = appLabel,
                packageName = packageName,
                timestamp = now,
                confidence = newestCandidate.confidence,
                isIncoming = true
            )

            serviceScope.launch(Dispatchers.Main) {
                OverlayStateManager.onNewMessageDetected(detectedMessage)
            }
        } catch (e: Exception) {
            // Safe boundary
        } finally {
            try {
                rootNode.recycle()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun traverseNodeHierarchy(
        node: AccessibilityNodeInfo?,
        results: MutableList<CandidateMessageNode>,
        depth: Int,
        maxDepth: Int
    ) {
        if (node == null || depth > maxDepth || results.size > 70) return

        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
        val className = node.className?.toString()?.lowercase(Locale.ROOT) ?: ""

        val rawText = when {
            !text.isNullOrBlank() -> text
            !contentDesc.isNullOrBlank() -> contentDesc
            else -> null
        }

        val isIgnoredClass = IGNORED_CLASS_NAMES.any { className.contains(it) }

        if (!rawText.isNullOrBlank() && !isIgnoredClass && isPotentialConversationalMessage(rawText)) {
            val score = scoreMessageQuality(rawText, viewId)
            if (score >= 0.50f) {
                val bounds = Rect()
                try {
                    node.getBoundsInScreen(bounds)
                } catch (ignored: Exception) {
                }

                results.add(
                    CandidateMessageNode(
                        text = rawText,
                        viewId = viewId,
                        depth = depth,
                        screenY = bounds.bottom,
                        confidence = score.coerceIn(0.1f, 1.0f)
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

    /**
     * Filters out non-conversational text, buttons, tabs, timestamps, etc.
     */
    private fun isPotentialConversationalMessage(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 3) return false

        val lower = trimmed.lowercase(Locale.ROOT)

        // Ignore exact matches or common button/tab labels
        if (IGNORED_EXACT_OR_PREFIX.contains(lower)) return false
        if (lower.startsWith("tab ") || lower.contains("new tab") || lower.contains("close tab") || lower.contains("tabs open")) return false
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) return false

        // Exclude pure numbers, percentages, or status clock formats (e.g. "12:45", "98%", "5G", "12/04/2026")
        if (trimmed.matches(Regex("^[0-9.,:;+\\-*/%#@!$&()\\s]+$"))) return false
        if (trimmed.matches(Regex("^[0-9]{1,2}:[0-9]{2}(\\s?[AaPp][Mm])?$"))) return false

        return true
    }

    /**
     * Scores how likely the text is an actual user question or conversational incoming message.
     * Evaluates multilingual question tokens (English, Hinglish, Bengali, Russian, Hindi, Spanish, etc.)
     */
    private fun scoreMessageQuality(text: String, viewId: String): Float {
        var score = 0.0f
        val lower = text.lowercase(Locale.ROOT)

        // 1. Explicit Question Marks (Universal)
        if (text.contains("?") || text.contains("¿") || text.contains("؟")) {
            score += 0.45f
        }

        // 2. Chat View ID Signals
        if (viewId.contains("message") || viewId.contains("chat") || viewId.contains("msg") ||
            viewId.contains("body") || viewId.contains("conversation") || viewId.contains("bubble") ||
            viewId.contains("text_view")
        ) {
            score += 0.35f
        }

        // 3. Multi-Lingual Question & Conversational Keywords
        val isBengaliQuestion = text.any { it in '\u0980'..'\u09FF' } &&
            (lower.contains("কী") || lower.contains("কেন") || lower.contains("কিভাবে") || lower.contains("কীভাবে") ||
                lower.contains("কেমন") || lower.contains("কোথায়") || lower.contains("কখন") || lower.contains("পারি") || lower.contains("হবে"))

        val isRussianQuestion = text.any { it in '\u0400'..'\u04FF' } &&
            (lower.contains("как") || lower.contains("что") || lower.contains("где") || lower.contains("когда") ||
                lower.contains("почему") || lower.contains("зачем") || lower.contains("кто") || lower.contains("ли") || lower.contains("привет"))

        val isHindiQuestion = text.any { it in '\u0900'..'\u097F' } &&
            (lower.contains("क्या") || lower.contains("क्यों") || lower.contains("कैसे") || lower.contains("कहाँ") ||
                lower.contains("कब") || lower.contains("कौन") || lower.contains("नमस्ते"))

        val isHinglishQuestion = (lower.contains("kaise") || lower.contains("kya") || lower.contains("kyu") || lower.contains("kyun") ||
            lower.contains("karna") || lower.contains("chahiye") || lower.contains("bhai") || lower.contains("batao") ||
            lower.contains("shuru") || lower.contains("start karna") || lower.contains("kese"))

        val isEnglishQuestionOrChat = lower.startsWith("how") || lower.startsWith("what") || lower.startsWith("where") ||
            lower.startsWith("when") || lower.startsWith("why") || lower.startsWith("who") || lower.startsWith("can you") ||
            lower.startsWith("could you") || lower.startsWith("would you") || lower.startsWith("is it") || lower.startsWith("are you") ||
            lower.startsWith("do you") || lower.contains("review") || lower.contains("free to") || lower.contains("available") ||
            lower.contains("meeting") || lower.contains("lunch") || lower.contains("sync")

        if (isBengaliQuestion || isRussianQuestion || isHindiQuestion || isHinglishQuestion || isEnglishQuestionOrChat) {
            score += 0.40f
        }

        // 4. Sentence Structure (Has spaces and reasonable length)
        if (text.length >= 12 && text.contains(" ")) {
            score += 0.20f
        }

        return score
    }

    /**
     * Determines which candidate is the NEWEST valid user question.
     * In standard chat apps, the newest message appears at the bottom of the screen (largest screenY)
     * or deepest in the node hierarchy.
     */
    private fun findNewestValidCandidate(candidates: List<CandidateMessageNode>): CandidateMessageNode? {
        if (candidates.isEmpty()) return null

        val highQuality = candidates.filter { it.confidence >= 0.50f }
        if (highQuality.isEmpty()) return null

        // Pick bottom-most (largest screenY) with strong confidence, or deepest node
        return highQuality.maxByOrNull { it.screenY.toFloat() * 1.5f + (it.confidence * 200f) + it.depth }
    }

    private fun normalizeMessage(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
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

    private data class CandidateMessageNode(
        val text: String,
        val viewId: String,
        val depth: Int,
        val screenY: Int,
        val confidence: Float,
        val sender: String? = null
    )
}
