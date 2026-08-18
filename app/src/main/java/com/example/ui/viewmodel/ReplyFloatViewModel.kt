package com.example.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiBotManager
import com.example.data.OverlayStateManager
import com.example.model.AppSettings
import com.example.model.BotConfig
import com.example.model.PassThroughState
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.ResponseMode
import com.example.model.SampleMessageScenario
import com.example.service.FloatingOverlayService
import com.example.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplyFloatViewModel : ViewModel() {

    // Production State: single source of truth for the real overlay service
    val uiState: StateFlow<ReplyFloatUiState> = OverlayStateManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverlayStateManager.state.value
    )

    // Simulation / Preview State: strictly isolated for in-app demonstration
    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    val configuredBots: StateFlow<List<BotConfig>> = AiBotManager.configuredBots

    val activeBotId: StateFlow<String> = AiBotManager.activeBotId

    // ==========================================
    // SIMULATION MODE ACTIONS (Isolated from Overlay)
    // ==========================================

    fun selectSimulationScenario(scenario: SampleMessageScenario) {
        val langCard = com.example.ai.LanguageDetectionEngine.processLanguageCard(
            originalMessage = scenario.message,
            englishReply = scenario.suggestions.firstOrNull()?.text ?: "",
            generationId = "sim_${System.currentTimeMillis()}",
            responseMode = ResponseMode.PASSIVE
        )
        _simulationState.update { current ->
            current.copy(
                selectedScenarioId = scenario.id,
                detectedMessage = scenario.message,
                detectedSourceApp = scenario.sourceApp,
                detectedSender = if (scenario.message.isNotBlank()) "Sarah Jenkins" else "",
                replies = scenario.suggestions,
                languageData = langCard,
                isLanguageBarActive = false,
                activeToneFilter = null,
                isViewAllExpanded = false,
                isVisible = true
            )
        }
    }

    fun toggleSimulationLanguageBar() {
        _simulationState.update { it.copy(isLanguageBarActive = !it.isLanguageBarActive) }
    }

    fun toggleSimulationViewAll() {
        _simulationState.update { it.copy(isViewAllExpanded = !it.isViewAllExpanded) }
    }

    fun selectSimulationToneFilter(tone: ReplyTone?) {
        _simulationState.update { current ->
            val nextTone = if (current.activeToneFilter == tone) null else tone
            current.copy(activeToneFilter = nextTone)
        }
    }

    fun toggleSimulationPassThrough() {
        _simulationState.update { current ->
            val next = if (current.passThroughState == PassThroughState.ENABLED) PassThroughState.DISABLED else PassThroughState.ENABLED
            current.copy(passThroughState = next)
        }
    }

    fun toggleSimulationScreenAnalysis() {
        _simulationState.update { it.copy(isScreenAnalysisOn = !it.isScreenAnalysisOn) }
    }

    fun selectSimulationResponseMode(mode: ResponseMode) {
        _simulationState.update { it.copy(responseMode = mode) }
    }

    fun toggleSimulationMinimize() {
        _simulationState.update { it.copy(isMinimized = !it.isMinimized) }
    }

    fun expandSimulationBar() {
        _simulationState.update { it.copy(isMinimized = false, isVisible = true) }
    }

    fun toggleSimulationVisibility() {
        _simulationState.update { it.copy(isVisible = !it.isVisible) }
    }

    fun copySimulationReply(context: Context, reply: ReplySuggestion) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ReplyFloat AI Simulation Reply", reply.text)
            clipboard.setPrimaryClip(clip)
            _simulationState.update { it.copy(lastCopiedId = reply.id) }
            viewModelScope.launch {
                delay(2500)
                _simulationState.update { if (it.lastCopiedId == reply.id) it.copy(lastCopiedId = null) else it }
            }
        } catch (ignored: Exception) {}
    }

    fun copySimulationText(context: Context, text: String, label: String = "Text") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ReplyFloat AI $label", text)
            clipboard.setPrimaryClip(clip)
            OverlayStateManager.notifyUser("Copied $label to clipboard")
        } catch (ignored: Exception) {}
    }

    // ==========================================
    // PRODUCTION ACTIONS (Real Service & Overlay)
    // ==========================================

    fun refreshPermissions(context: Context) {
        OverlayStateManager.refreshPermissions(context)
    }

    fun selectTab(tab: NavigationTab) {
        OverlayStateManager.selectTab(tab)
    }

    fun toggleOverlayService(context: Context) {
        val hasPermission = PermissionUtils.hasOverlayPermission(context)
        if (!hasPermission) {
            PermissionUtils.openOverlaySettings(context)
            OverlayStateManager.updateSettings(context) { it.copy(overlayPermissionGranted = false) }
            return
        }

        if (FloatingOverlayService.isRunning) {
            FloatingOverlayService.stopService(context)
            OverlayStateManager.setOverlayServiceRunning(false)
        } else {
            FloatingOverlayService.startService(context)
            OverlayStateManager.setOverlayServiceRunning(true)
        }
    }

    fun togglePassThroughMode() {
        OverlayStateManager.togglePassThrough()
    }

    fun setPassThroughMode(state: PassThroughState) {
        OverlayStateManager.setPassThrough(state)
    }

    fun toggleFloatingBarVisibility() {
        val current = uiState.value.isFloatingBarVisible
        OverlayStateManager.setFloatingBarVisible(!current)
    }

    fun toggleMinimizeFloatingBar() {
        val current = uiState.value.isFloatingBarMinimized
        OverlayStateManager.setFloatingBarMinimized(!current)
    }

    fun expandFloatingBar() {
        OverlayStateManager.setFloatingBarMinimized(false)
        OverlayStateManager.setFloatingBarVisible(true)
    }

    fun toggleViewAllReplies() {
        OverlayStateManager.toggleViewAllExpanded()
    }

    fun selectToneFilter(tone: ReplyTone?) {
        OverlayStateManager.selectToneFilter(tone)
    }

    fun triggerAnalyzeScreen() {
        OverlayStateManager.triggerAnalyzeScreen()
    }

    fun copyReply(context: Context, reply: ReplySuggestion) {
        OverlayStateManager.copyReply(context, reply)
    }

    fun clearHistory() {
        OverlayStateManager.clearHistory()
    }

    fun deleteHistoryEntry(id: String) {
        OverlayStateManager.deleteHistoryEntry(id)
    }

    fun purgeTemporaryData() {
        OverlayStateManager.purgeTemporaryData()
    }

    fun selectActiveBot(botId: String, context: Context? = null) {
        OverlayStateManager.selectActiveBot(botId, context)
    }

    fun addBot(bot: BotConfig, context: Context? = null) {
        OverlayStateManager.addBot(bot, context)
    }

    fun deleteBot(botId: String, context: Context? = null) {
        OverlayStateManager.deleteBot(botId, context)
    }

    fun updateSettings(context: Context? = null, transform: (AppSettings) -> AppSettings) {
        OverlayStateManager.updateSettings(context, transform)
    }

    fun handleOverlayPermissionToggle(context: Context) {
        PermissionUtils.openOverlaySettings(context)
    }

    fun handleAccessibilityPermissionToggle(context: Context) {
        PermissionUtils.openAccessibilitySettings(context)
    }

    fun handleNotificationPermissionToggle(context: Context) {
        PermissionUtils.openNotificationSettings(context)
    }

    fun clearNotice() {
        OverlayStateManager.clearNotice()
    }
}
