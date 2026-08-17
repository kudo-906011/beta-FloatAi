package com.example.data

import android.content.Context
import com.example.ai.AiBotManager
import com.example.model.AiLatencyMode
import com.example.model.AiReplyRequest
import com.example.model.AnalysisStatus
import com.example.model.AppSettings
import com.example.model.AssistantStatus
import com.example.model.BotConfig
import com.example.model.ConversationMessage
import com.example.model.ConversationRole
import com.example.model.DetectedMessage
import com.example.model.HistoryEntry
import com.example.model.PassThroughState
import com.example.model.PurgeDuration
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.SampleMessageScenario
import com.example.model.ScreenContext
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.ReplyFloatUiState
import com.example.ui.viewmodel.defaultSampleHistory
import com.example.ui.viewmodel.defaultSampleReplies
import com.example.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Singleton State Manager acting as the Single Source of Truth
 * across Activity UI, WindowManager Floating Overlay Service, Accessibility Service,
 * Notification Quick Actions, Auto-Purge Timer, and Multi-Bot Router.
 *
 * Implements strict generation-ID lifecycle tracking, stale response discard,
 * safe bot switching, auto-purge monitoring, and configurable latency response behavior.
 */
object OverlayStateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var activeGenerationJob: Job? = null
    @Volatile
    private var activeGenerationId: String? = null

    // Background purge timer monitor job
    private var purgeMonitorJob: Job? = null
    @Volatile
    private var lastActivityTimestamp: Long = System.currentTimeMillis()

    // Controlled rolling conversation history (max 6 turns to avoid context contamination)
    private val conversationHistory = mutableListOf<ConversationMessage>()

    private val _state = MutableStateFlow(
        ReplyFloatUiState(
            isOverlayActive = false,
            isFloatingBarVisible = true,
            isFloatingBarMinimized = false,
            passThroughState = PassThroughState.DISABLED,
            assistantStatus = AssistantStatus.READY,
            analysisStatus = AnalysisStatus.READY,
            detectedMessage = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
            detectedSender = "Sarah Jenkins",
            detectedSourceApp = "Work Chat",
            replies = defaultSampleReplies,
            historyList = defaultSampleHistory
        )
    )
    val state: StateFlow<ReplyFloatUiState> = _state.asStateFlow()

    // Overlay drag coordinates (pixels relative to screen)
    val overlayX = MutableStateFlow(40)
    val overlayY = MutableStateFlow(240)

    init {
        startPurgeMonitor()
    }

    private fun startPurgeMonitor() {
        purgeMonitorJob?.cancel()
        purgeMonitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000) // Check every 5 seconds
                checkAndPerformPurge()
            }
        }
    }

    /**
     * Checks if temporary reply, analysis, and conversation data has exceeded the purge duration.
     * Guaranteed NOT to purge while an active generation is in progress, and never purges permanent settings or bots.
     */
    fun checkAndPerformPurge() {
        val current = _state.value
        val settings = current.settings
        val now = System.currentTimeMillis()

        // 1. Independent Purge for Recent Results (Visibility Timer, default 2 minutes)
        val recentRetentionMillis = if (settings.recentRetentionDuration == com.example.model.RecentRetentionDuration.CUSTOM) {
            settings.customRecentRetentionMinutes.coerceAtLeast(1) * 60 * 1000L
        } else {
            settings.recentRetentionDuration.durationMillis
        }

        if (recentRetentionMillis > 0 && current.recentResults.isNotEmpty()) {
            val validRecent = current.recentResults.filter { item ->
                (now - item.timestamp) < recentRetentionMillis
            }
            if (validRecent.size != current.recentResults.size) {
                _state.update { it.copy(recentResults = validRecent) }
            }
        }

        // 2. Independent Purge for Application History & Inactivity (default 5 minutes)
        if (settings.purgeDuration != PurgeDuration.NEVER) {
            // Safety check: Never purge during active generation
            if (activeGenerationJob?.isActive != true) {
                val effectiveHistoryDurationMillis = if (settings.purgeDuration == PurgeDuration.CUSTOM) {
                    settings.customPurgeMinutes.coerceAtLeast(1) * 60 * 1000L
                } else {
                    settings.purgeDuration.durationMillis
                }

                val elapsed = now - lastActivityTimestamp
                if (elapsed >= effectiveHistoryDurationMillis) {
                    if (current.historyList.isNotEmpty() || current.replies.isNotEmpty()) {
                        purgeTemporaryData()
                    }
                }
            }
        }
    }

    /**
     * Automatically or manually clears all temporary AI replies, detected message text,
     * screen analysis context, recent results, and live conversation history.
     */
    fun purgeTemporaryData() {
        conversationHistory.clear()
        lastActivityTimestamp = System.currentTimeMillis()
        _state.update { current ->
            current.copy(
                detectedMessage = "",
                detectedSender = "",
                detectedSourceApp = "",
                replies = emptyList(),
                recentResults = emptyList(),
                isViewAllExpanded = false,
                assistantStatus = AssistantStatus.IDLE,
                analysisStatus = AnalysisStatus.READY,
                userNotice = "Temporary AI suggestions & analysis data purged"
            )
        }
    }

    /**
     * Synchronizes real system permissions on launch or resume and loads persisted settings.
     */
    fun refreshPermissions(context: Context) {
        val overlay = PermissionUtils.hasOverlayPermission(context)
        val accessibility = PermissionUtils.hasAccessibilityPermission(context)
        val notifications = PermissionUtils.hasNotificationPermission(context)

        // Load saved preferences & bots
        val loadedSettings = PreferencesManager.loadSettings(context)
        val loadedBots = PreferencesManager.loadBots(context)
        AiBotManager.setBots(loadedBots)
        AiBotManager.setActiveBot(loadedSettings.activeBotId)

        _state.update { current ->
            current.copy(
                settings = loadedSettings.copy(
                    overlayPermissionGranted = overlay,
                    accessibilityPermissionGranted = accessibility,
                    notificationPermissionGranted = notifications
                )
            )
        }
    }

    fun setOverlayServiceRunning(isRunning: Boolean) {
        _state.update { current ->
            current.copy(
                isOverlayActive = isRunning,
                isFloatingBarVisible = isRunning,
                userNotice = if (isRunning) "Floating Overlay Active" else "Floating Overlay Stopped"
            )
        }
    }

    fun setPassThrough(newState: PassThroughState) {
        _state.update { current ->
            current.copy(
                passThroughState = newState,
                userNotice = if (newState == PassThroughState.ENABLED) {
                    "Pass-Through ENABLED: Touches pass through to apps below"
                } else {
                    "Pass-Through DISABLED: Floating bar interactive"
                }
            )
        }
    }

    fun togglePassThrough() {
        val next = if (_state.value.passThroughState == PassThroughState.ENABLED) {
            PassThroughState.DISABLED
        } else {
            PassThroughState.ENABLED
        }
        setPassThrough(next)
    }

    fun setScreenAnalysis(enabled: Boolean, context: Context? = null) {
        updateSettings(context) { it.copy(isScreenAnalysisOn = enabled) }
        _state.update {
            it.copy(
                userNotice = if (enabled) "Screen Analysis ON: AI observing incoming questions" else "Screen Analysis OFF: AI paused"
            )
        }
    }

    fun toggleScreenAnalysis(context: Context? = null) {
        val current = _state.value.settings.isScreenAnalysisOn
        setScreenAnalysis(!current, context)
    }

    fun selectResponseMode(mode: com.example.model.ResponseMode, context: Context? = null) {
        updateSettings(context) { it.copy(responseMode = mode) }
        _state.update {
            it.copy(userNotice = "Response Mode: ${mode.title}")
        }
    }

    fun deleteRecentResult(id: String) {
        _state.update { current ->
            val updated = current.recentResults.filterNot { it.id == id }
            current.copy(recentResults = updated, userNotice = "Recent answer removed")
        }
    }

    fun clearRecentResults() {
        _state.update { it.copy(recentResults = emptyList(), userNotice = "Recent answers cleared") }
    }

    fun setFloatingBarVisible(visible: Boolean) {
        _state.update { it.copy(isFloatingBarVisible = visible) }
    }

    fun setFloatingBarMinimized(minimized: Boolean) {
        _state.update { it.copy(isFloatingBarMinimized = minimized) }
    }

    fun toggleViewAllExpanded() {
        _state.update { it.copy(isViewAllExpanded = !it.isViewAllExpanded) }
    }

    fun selectToneFilter(tone: ReplyTone?) {
        _state.update { current ->
            val nextTone = if (current.activeToneFilter == tone) null else tone
            current.copy(activeToneFilter = nextTone)
        }
    }

    fun selectTab(tab: NavigationTab) {
        _state.update { it.copy(currentTab = tab) }
    }

    fun updateSettings(context: Context? = null, transform: (AppSettings) -> AppSettings) {
        lastActivityTimestamp = System.currentTimeMillis()
        _state.update { current ->
            val newSettings = transform(current.settings)
            if (context != null) {
                PreferencesManager.saveSettings(context, newSettings)
            }
            current.copy(settings = newSettings, userNotice = "Settings updated")
        }
    }

    // ==========================================
    // MULTI-BOT SELECTION & MANAGEMENT
    // ==========================================

    fun selectActiveBot(botId: String, context: Context? = null) {
        // Safe bot switching: cancel running generation so old bot response cannot overwrite
        activeGenerationJob?.cancel()
        activeGenerationId = UUID.randomUUID().toString()

        AiBotManager.setActiveBot(botId)
        _state.update { current ->
            val updated = current.settings.copy(activeBotId = botId)
            if (context != null) {
                PreferencesManager.saveSettings(context, updated)
            }
            current.copy(
                settings = updated,
                userNotice = "Active AI Bot: ${AiBotManager.getActiveBot().name}"
            )
        }
    }

    fun addBot(bot: BotConfig, context: Context? = null) {
        AiBotManager.addBot(bot)
        if (context != null) {
            PreferencesManager.saveBots(context, AiBotManager.configuredBots.value)
        }
        _state.update { it.copy(userNotice = "Added bot '${bot.name}'") }
    }

    fun deleteBot(botId: String, context: Context? = null) {
        AiBotManager.deleteBot(botId)
        val activeId = AiBotManager.activeBotId.value
        _state.update { current ->
            val updated = current.settings.copy(activeBotId = activeId)
            if (context != null) {
                PreferencesManager.saveBots(context, AiBotManager.configuredBots.value)
                PreferencesManager.saveSettings(context, updated)
            }
            current.copy(settings = updated, userNotice = "Bot removed")
        }
    }

    // ==========================================
    // MESSAGE DETECTION & AI INFERENCE
    // ==========================================

    /**
     * Called when the Accessibility Service extracts a new message.
     * Prevents duplicate events, applies latency debounce, cancels any pending generation,
     * archives prior answers to recentResults, and guarantees that stale results never overwrite fresh state.
     */
    fun onNewMessageDetected(message: DetectedMessage) {
        val current = _state.value
        if (!current.settings.isScreenAnalysisOn) {
            return
        }

        lastActivityTimestamp = System.currentTimeMillis()
        val cleanText = message.text.trim()

        // Deduplication: Ignore empty or identical text to avoid spamming
        if (cleanText.isBlank() || cleanText.equals(current.detectedMessage.trim(), ignoreCase = true)) {
            return
        }

        val generationId = UUID.randomUUID().toString()
        activeGenerationId = generationId
        activeGenerationJob?.cancel()

        // Archive previous active answer/suggestions to recentResults before clearing
        val previousRecentItem = if (current.replies.isNotEmpty() && current.detectedMessage.isNotBlank()) {
            com.example.model.RecentResultItem(
                id = "rec_${System.currentTimeMillis()}",
                question = current.detectedMessage,
                sender = current.detectedSender,
                sourceApp = current.detectedSourceApp,
                suggestions = current.replies,
                timestamp = System.currentTimeMillis(),
                generationId = current.activeGenerationId ?: UUID.randomUUID().toString()
            )
        } else null

        val debounceDelay = when (current.settings.latencyMode) {
            AiLatencyMode.FAST -> 200L
            AiLatencyMode.BALANCED -> 500L
            AiLatencyMode.STABLE -> 1000L
            AiLatencyMode.CUSTOM -> current.settings.customDebounceMs.coerceIn(50L, 3000L)
        }

        activeGenerationJob = scope.launch {
            // Immediate state transition: archive old answers into recentResults and reset active replies
            _state.update { state ->
                val newRecent = if (previousRecentItem != null) {
                    (listOf(previousRecentItem) + state.recentResults).take(20)
                } else {
                    state.recentResults
                }
                state.copy(
                    assistantStatus = AssistantStatus.ANALYZING,
                    analysisStatus = AnalysisStatus.ANALYZING,
                    detectedMessage = cleanText,
                    detectedSender = message.sender,
                    detectedSourceApp = message.sourceApp,
                    replies = emptyList(),
                    recentResults = newRecent,
                    activeGenerationId = generationId,
                    isViewAllExpanded = false,
                    userNotice = "Analyzing message from ${message.sourceApp} (${AiBotManager.getActiveBot().name})..."
                )
            }

            // Latency control: apply debounce delay
            delay(debounceDelay)

            if (activeGenerationId != generationId) {
                return@launch
            }

            // Build structured AI request
            val request = AiReplyRequest(
                generationId = generationId,
                currentMessage = cleanText,
                recentConversation = conversationHistory.toList(),
                screenContext = ScreenContext(
                    sourceApplication = message.sourceApp,
                    packageName = message.packageName,
                    detectedTimestamp = message.timestamp,
                    senderName = message.sender,
                    confidence = message.confidence
                ),
                replyTone = current.settings.defaultTone,
                responseMode = current.settings.responseMode,
                requestedReplyCount = current.settings.maxSuggestionsCount
            )

            val result = AiBotManager.generateReplies(
                request = request,
                latencyMode = current.settings.latencyMode,
                customTimeoutSeconds = current.settings.customTimeoutSeconds
            )

            // CRITICAL STALE-RESPONSE CHECK: Discard if newer request or bot switch arrived in the meantime
            if (activeGenerationId != result.generationId) {
                return@launch
            }

            if (result.isSuccess && result.suggestions.isNotEmpty()) {
                addConversationTurn(
                    ConversationMessage(
                        role = ConversationRole.USER,
                        text = cleanText,
                        source = message.sourceApp
                    )
                )

                _state.update { state ->
                    state.copy(
                        assistantStatus = AssistantStatus.READY,
                        analysisStatus = AnalysisStatus.COMPLETED,
                        replies = result.suggestions,
                        activeGenerationId = generationId,
                        isFloatingBarVisible = true,
                        isFloatingBarMinimized = state.isFloatingBarMinimized,
                        userNotice = "New replies ready from ${message.sourceApp} (${AiBotManager.getActiveBot().name})"
                    )
                }
            } else {
                _state.update { state ->
                    state.copy(
                        assistantStatus = AssistantStatus.IDLE,
                        analysisStatus = AnalysisStatus.NO_CONTENT,
                        replies = emptyList(),
                        activeGenerationId = generationId,
                        userNotice = result.errorMessage ?: "No relevant replies generated."
                    )
                }
            }
        }
    }

    fun triggerAnalyzeScreen(sampleScenarioText: String? = null, sourceApp: String? = null) {
        lastActivityTimestamp = System.currentTimeMillis()
        val current = _state.value
        val messageText = (sampleScenarioText ?: current.detectedMessage).trim()

        val generationId = UUID.randomUUID().toString()
        activeGenerationId = generationId
        activeGenerationJob?.cancel()

        // Archive previous active answer/suggestions to recentResults before clearing
        val previousRecentItem = if (current.replies.isNotEmpty() && current.detectedMessage.isNotBlank()) {
            com.example.model.RecentResultItem(
                id = "rec_${System.currentTimeMillis()}",
                question = current.detectedMessage,
                sender = current.detectedSender,
                sourceApp = current.detectedSourceApp,
                suggestions = current.replies,
                timestamp = System.currentTimeMillis(),
                generationId = current.activeGenerationId ?: UUID.randomUUID().toString()
            )
        } else null

        val minWait = when (current.settings.latencyMode) {
            AiLatencyMode.FAST -> 100L
            AiLatencyMode.BALANCED -> 250L
            AiLatencyMode.STABLE -> 400L
            AiLatencyMode.CUSTOM -> (current.settings.customDebounceMs / 2).coerceIn(50L, 1000L)
        }

        activeGenerationJob = scope.launch {
            _state.update { state ->
                val newRecent = if (previousRecentItem != null) {
                    (listOf(previousRecentItem) + state.recentResults).take(20)
                } else {
                    state.recentResults
                }
                state.copy(
                    analysisStatus = AnalysisStatus.ANALYZING,
                    assistantStatus = AssistantStatus.ANALYZING,
                    replies = emptyList(),
                    recentResults = newRecent,
                    activeGenerationId = generationId,
                    userNotice = "Scanning active screen with ${AiBotManager.getActiveBot().name}..."
                )
            }

            delay(minWait)

            if (messageText.isBlank()) {
                if (activeGenerationId == generationId) {
                    _state.update {
                        it.copy(
                            analysisStatus = AnalysisStatus.NO_CONTENT,
                            assistantStatus = AssistantStatus.IDLE,
                            replies = emptyList(),
                            userNotice = "No active conversation message found on screen."
                        )
                    }
                }
                return@launch
            }

            val request = AiReplyRequest(
                generationId = generationId,
                currentMessage = messageText,
                recentConversation = conversationHistory.toList(),
                screenContext = ScreenContext(
                    sourceApplication = sourceApp ?: current.detectedSourceApp,
                    senderName = current.detectedSender
                ),
                replyTone = current.settings.defaultTone,
                responseMode = current.settings.responseMode,
                requestedReplyCount = current.settings.maxSuggestionsCount
            )

            val result = AiBotManager.generateReplies(
                request = request,
                latencyMode = current.settings.latencyMode,
                customTimeoutSeconds = current.settings.customTimeoutSeconds
            )

            // CRITICAL STALE-RESPONSE CHECK
            if (activeGenerationId != result.generationId) {
                return@launch
            }

            if (result.isSuccess && result.suggestions.isNotEmpty()) {
                addConversationTurn(
                    ConversationMessage(
                        role = ConversationRole.USER,
                        text = messageText,
                        source = sourceApp ?: current.detectedSourceApp
                    )
                )

                _state.update {
                    it.copy(
                        analysisStatus = AnalysisStatus.COMPLETED,
                        assistantStatus = AssistantStatus.READY,
                        detectedMessage = messageText,
                        replies = result.suggestions,
                        activeGenerationId = generationId,
                        isFloatingBarVisible = true,
                        isFloatingBarMinimized = it.isFloatingBarMinimized,
                        userNotice = "Analysis complete: ${result.suggestions.size} replies ready (${AiBotManager.getActiveBot().name})."
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        analysisStatus = AnalysisStatus.NO_CONTENT,
                        assistantStatus = AssistantStatus.IDLE,
                        replies = emptyList(),
                        activeGenerationId = generationId,
                        userNotice = result.errorMessage ?: "No relevant message detected."
                    )
                }
            }
        }
    }

    fun copyReply(context: Context, reply: ReplySuggestion) {
        lastActivityTimestamp = System.currentTimeMillis()
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("ReplyFloat AI Reply", reply.text)
            clipboard.setPrimaryClip(clip)

            val current = _state.value
            val historyEntry = HistoryEntry(
                id = "hist_${System.currentTimeMillis()}",
                originalMessage = current.detectedMessage.ifBlank { "Screen Context Reply" },
                selectedReply = reply.text,
                tone = reply.tone,
                allSuggestions = current.replies.map { it.text },
                timestamp = System.currentTimeMillis(),
                sourceApp = current.detectedSourceApp
            )

            addConversationTurn(
                ConversationMessage(
                    role = ConversationRole.ASSISTANT,
                    text = reply.text,
                    source = "ReplyFloat AI"
                )
            )

            _state.update {
                val updatedHistory = listOf(historyEntry) + it.historyList
                it.copy(
                    lastCopiedReplyId = reply.id,
                    historyList = updatedHistory,
                    userNotice = "Copied to clipboard!",
                    isFloatingBarMinimized = if (it.settings.autoMinimizeOnCopy) true else it.isFloatingBarMinimized
                )
            }

            scope.launch {
                delay(2500)
                _state.update { if (it.lastCopiedReplyId == reply.id) it.copy(lastCopiedReplyId = null) else it }
            }
        } catch (e: Exception) {
            _state.update { it.copy(userNotice = "Copy failed: ${e.message}") }
        }
    }

    private fun addConversationTurn(message: ConversationMessage) {
        conversationHistory.add(message)
        while (conversationHistory.size > 6) {
            conversationHistory.removeAt(0)
        }
    }

    fun loadScenario(scenario: SampleMessageScenario) {
        lastActivityTimestamp = System.currentTimeMillis()
        val genId = UUID.randomUUID().toString()
        activeGenerationId = genId
        activeGenerationJob?.cancel()

        conversationHistory.clear()
        conversationHistory.add(
            ConversationMessage(
                role = ConversationRole.USER,
                text = scenario.message,
                source = scenario.sourceApp
            )
        )

        _state.update { current ->
            current.copy(
                detectedMessage = scenario.message,
                detectedSourceApp = scenario.sourceApp,
                replies = scenario.suggestions,
                isViewAllExpanded = false,
                activeToneFilter = null,
                assistantStatus = if (scenario.suggestions.isEmpty()) AssistantStatus.IDLE else AssistantStatus.READY,
                analysisStatus = if (scenario.suggestions.isEmpty()) AnalysisStatus.NO_CONTENT else AnalysisStatus.READY,
                isFloatingBarVisible = true,
                isFloatingBarMinimized = false,
                userNotice = "Loaded scenario: ${scenario.title}"
            )
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
        _state.update { it.copy(historyList = emptyList(), userNotice = "History cleared") }
    }

    fun deleteHistoryEntry(id: String) {
        _state.update { current ->
            val updated = current.historyList.filterNot { it.id == id }
            current.copy(historyList = updated, userNotice = "History entry removed")
        }
    }

    fun clearNotice() {
        _state.update { it.copy(userNotice = null) }
    }
}
