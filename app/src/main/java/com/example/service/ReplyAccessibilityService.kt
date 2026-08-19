package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ai.QuestionClassifier
import com.example.data.OverlayStateManager
import com.example.model.DetectedMessage
import com.example.model.DetectionLogEntry
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
 *
 * Implements a strict two-stage detection & classification pipeline:
 * 1. Accessibility Event & Node Tree Traversal -> Filters system chrome, keyboards, launchers.
 * 2. Conservative Question Classification (QuestionClassifier) -> Rejects non-questions, buttons,
 *    tab changes, random screen text, timestamps, and UI noise.
 * 3. Robust Identity & Deduplication -> Guarantees a message is processed once and only once.
 * 4. Priority to the Newest Question -> Immediate state reset & cancellation of stale generations.
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
            "com.android.launcher3",
            "com.google.android.googlequicksearchbox"
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
    private val processedMessageHashes = ArrayDeque<Int>(100)
    private val processedMessageTexts = ArrayDeque<String>(100)
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
        info.notificationTimeout = 300
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // If screen analysis is toggled OFF, immediately return to save battery & CPU
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
     * Debounces rapid UI changes before running the classification pipeline.
     */
    private fun scheduleScreenAnalysis(packageName: String) {
        analysisDebounceJob?.cancel()
        analysisDebounceJob = serviceScope.launch {
            delay(350) // 350ms debounce
            extractAndProcessLatestMessage(packageName)
        }
    }

    private fun extractAndProcessLatestMessage(packageName: String) {
        val rootNode = rootInActiveWindow ?: return

        try {
            val appLabel = getAppLabel(packageName)
            val extractedRawCandidates = mutableListOf<CandidateMessageNode>()

            traverseNodeHierarchy(rootNode, extractedRawCandidates, depth = 0, maxDepth = 20)

            if (extractedRawCandidates.isEmpty()) return

            val now = System.currentTimeMillis()
            val currentlyActiveQuestion = OverlayStateManager.state.value.detectedMessage.trim()
            val currentlyActiveNormalized = normalizeMessage(currentlyActiveQuestion)

            // Stage 2: Question Classification & Filtering
            val qualifiedCandidates = mutableListOf<QualifiedCandidate>()

            for (candidate in extractedRawCandidates) {
                val cleanText = candidate.text.trim()
                val normalized = normalizeMessage(cleanText)
                val textHash = normalized.hashCode()

                val classification = QuestionClassifier.classifyCandidate(cleanText, candidate.viewId)

                // Check if new or already processed
                val isAlreadyActive = normalized.isNotBlank() && normalized.equals(currentlyActiveNormalized, ignoreCase = true)
                val isAlreadyProcessed = processedMessageHashes.contains(textHash) || processedMessageTexts.contains(normalized)
                val isGenuinelyNew = !isAlreadyActive && !isAlreadyProcessed

                // Record internal debug log entry
                val logEntry = DetectionLogEntry(
                    timestamp = now,
                    candidateText = cleanText.take(120),
                    sourceApp = appLabel,
                    packageName = packageName,
                    isNew = isGenuinelyNew,
                    isConversational = classification.isConversational,
                    isQuestionOrRequest = classification.isQuestionOrInquiry,
                    rejectionReason = if (!classification.isQualifyingQuestion) classification.rejectionReason
                    else if (isAlreadyActive) "Matches currently active question"
                    else if (isAlreadyProcessed) "Already processed in session history"
                    else null,
                    aiProcessed = classification.isQualifyingQuestion && isGenuinelyNew,
                    confidence = classification.confidence
                )
                OverlayStateManager.recordDetectionLog(logEntry)

                if (classification.isQualifyingQuestion && isGenuinelyNew) {
                    qualifiedCandidates.add(
                        QualifiedCandidate(
                            text = cleanText,
                            normalized = normalized,
                            textHash = textHash,
                            screenY = candidate.screenY,
                            depth = candidate.depth,
                            confidence = classification.confidence,
                            sender = candidate.sender,
                            viewId = candidate.viewId
                        )
                    )
                }
            }

            if (qualifiedCandidates.isEmpty()) {
                // If no qualifying candidates were found, DO NOTHING.
                // Do NOT call AI, do NOT clear current replies, do NOT create fake questions.
                return
            }

            // Stage 3: Pick the NEWEST question (bottom-most in chat timeline / largest screenY)
            val newestCandidate = qualifiedCandidates.maxByOrNull {
                it.screenY.toFloat() * 2.0f + (it.confidence * 200f) + it.depth
            } ?: return

            // Record as processed in ring buffers to guarantee single processing
            lastProcessedNormalized = newestCandidate.normalized
            lastProcessedTimestamp = now
            if (processedMessageHashes.size >= 80) {
                processedMessageHashes.removeFirst()
                processedMessageTexts.removeFirst()
            }
            processedMessageHashes.addLast(newestCandidate.textHash)
            processedMessageTexts.addLast(newestCandidate.normalized)

            val detectedMessage = DetectedMessage(
                eventId = UUID.randomUUID().toString(),
                text = newestCandidate.text,
                sender = newestCandidate.sender ?: "Incoming Message",
                sourceApp = appLabel,
                packageName = packageName,
                timestamp = now,
                confidence = newestCandidate.confidence,
                isIncoming = true
            )

            // Dispatch atomic update to state manager
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
        if (node == null || depth > maxDepth || results.size > 80) return

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

        if (!rawText.isNullOrBlank() && !isIgnoredClass) {
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
                    sender = null
                )
            )
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
        val sender: String? = null
    )

    private data class QualifiedCandidate(
        val text: String,
        val normalized: String,
        val textHash: Int,
        val screenY: Int,
        val depth: Int,
        val confidence: Float,
        val sender: String? = null,
        val viewId: String = ""
    )
}
