package com.example.ui.viewmodel

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
import com.example.model.SampleMessageScenario
import com.example.service.FloatingOverlayService
import com.example.util.PermissionUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReplyFloatViewModel : ViewModel() {

    val uiState: StateFlow<ReplyFloatUiState> = OverlayStateManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverlayStateManager.state.value
    )

    val configuredBots: StateFlow<List<BotConfig>> = AiBotManager.configuredBots

    val activeBotId: StateFlow<String> = AiBotManager.activeBotId

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

    fun loadScenario(scenario: SampleMessageScenario) {
        OverlayStateManager.loadScenario(scenario)
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
