package com.example.ui.viewmodel

import com.example.model.AnalysisStatus
import com.example.model.AppSettings
import com.example.model.AssistantStatus
import com.example.model.DockPosition
import com.example.model.HistoryEntry
import com.example.model.PassThroughState
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.SampleMessageScenario

enum class NavigationTab(val label: String) {
    ASSISTANT("Assistant"),
    HISTORY("History"),
    SETTINGS("Settings")
}

data class ReplyFloatUiState(
    val currentTab: NavigationTab = NavigationTab.ASSISTANT,
    val isOverlayActive: Boolean = true,
    val isFloatingBarVisible: Boolean = true,
    val isFloatingBarMinimized: Boolean = false,
    val passThroughState: PassThroughState = PassThroughState.DISABLED,
    val assistantStatus: AssistantStatus = AssistantStatus.READY,
    val analysisStatus: AnalysisStatus = AnalysisStatus.READY,
    val detectedMessage: String = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
    val detectedSender: String = "Sarah Jenkins",
    val detectedSourceApp: String = "Work Chat",
    val replies: List<ReplySuggestion> = defaultSampleReplies,
    val recentResults: List<com.example.model.RecentResultItem> = emptyList(),
    val activeGenerationId: String? = null,
    val isViewAllExpanded: Boolean = false,
    val collapsedReplyCount: Int = 2,
    val activeToneFilter: ReplyTone? = null,
    val lastCopiedReplyId: String? = null,
    val historyList: List<HistoryEntry> = defaultSampleHistory,
    val settings: AppSettings = AppSettings(
        overlayPermissionGranted = true,
        accessibilityPermissionGranted = false,
        passThroughDefault = false,
        defaultTone = ReplyTone.BALANCED,
        maxSuggestionsCount = 3,
        dockPosition = DockPosition.BOTTOM_RIGHT,
        autoMinimizeOnCopy = true
    ),
    val userNotice: String? = null
) {
    val visibleReplies: List<ReplySuggestion>
        get() {
            val filtered = if (activeToneFilter != null) {
                replies.filter { it.tone == activeToneFilter }
            } else {
                replies
            }
            return if (isViewAllExpanded) {
                filtered
            } else {
                filtered.take(collapsedReplyCount)
            }
        }

    val totalReplyCount: Int
        get() = if (activeToneFilter != null) {
            replies.count { it.tone == activeToneFilter }
        } else {
            replies.size
        }

    val hasMoreReplies: Boolean
        get() = totalReplyCount > collapsedReplyCount
}

val defaultSampleReplies = listOf(
    ReplySuggestion(
        id = "rep_1",
        text = "Yes, absolutely! I am going over the Q3 roadmap now and will have notes ready for our 4 PM sync.",
        tone = ReplyTone.PROFESSIONAL
    ),
    ReplySuggestion(
        id = "rep_2",
        text = "Sure thing! Reviewing it right now, see you at 4 PM.",
        tone = ReplyTone.CASUAL
    ),
    ReplySuggestion(
        id = "rep_3",
        text = "On it. Will be ready for the 4 PM sync.",
        tone = ReplyTone.CONCISE
    ),
    ReplySuggestion(
        id = "rep_4",
        text = "Sounds great! Looking forward to diving into the Q3 plans together at 4.",
        tone = ReplyTone.FRIENDLY
    )
)

val defaultSampleHistory = listOf(
    HistoryEntry(
        id = "hist_1",
        originalMessage = "Can you send the updated budget spreadsheet?",
        selectedReply = "Just shared the spreadsheet with you via Drive!",
        tone = ReplyTone.PROFESSIONAL,
        allSuggestions = listOf(
            "Just shared the spreadsheet with you via Drive!",
            "Sending it over right now.",
            "Done! Check your inbox."
        ),
        timestamp = System.currentTimeMillis() - 1000 * 60 * 42,
        sourceApp = "Messages"
    ),
    HistoryEntry(
        id = "hist_2",
        originalMessage = "Are we having lunch at 12:30 or 1:00 today?",
        selectedReply = "1:00 PM works best for me! Meet at the lobby?",
        tone = ReplyTone.CASUAL,
        allSuggestions = listOf(
            "1:00 PM works best for me! Meet at the lobby?",
            "Let's do 12:30 PM.",
            "1:00 PM is great."
        ),
        timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
        sourceApp = "Chat"
    )
)

val sampleScenarios = listOf(
    SampleMessageScenario(
        id = "scen_work",
        title = "Work Meeting Inquiry",
        message = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
        sourceApp = "Slack",
        suggestions = listOf(
            ReplySuggestion("s1_1", "Yes, absolutely! I am going over the Q3 roadmap now and will have notes ready for our 4 PM sync.", ReplyTone.PROFESSIONAL),
            ReplySuggestion("s1_2", "Sure thing! Reviewing it right now, see you at 4 PM.", ReplyTone.CASUAL),
            ReplySuggestion("s1_3", "On it. Will be ready for the 4 PM sync.", ReplyTone.CONCISE),
            ReplySuggestion("s1_4", "Sounds great! Looking forward to diving into the Q3 plans together at 4.", ReplyTone.FRIENDLY)
        )
    ),
    SampleMessageScenario(
        id = "scen_long",
        title = "Detailed Client Request (Long Text)",
        message = "Hi team, regarding the client feedback on the prototype navigation flow: they requested a persistent floating widget for instant replies rather than a nested sub-menu. Could we review if our current architecture supports touch pass-through and live screen detection without lag?",
        sourceApp = "Email",
        suggestions = listOf(
            ReplySuggestion("s2_1", "Thanks for sharing the update. Yes, our floating overlay architecture supports seamless pass-through and real-time screen parsing. I'll outline the implementation details shortly.", ReplyTone.PROFESSIONAL),
            ReplySuggestion("s2_2", "Understood! We already have the floating bar foundation and pass-through mode ready. Let's do a quick demo.", ReplyTone.CASUAL),
            ReplySuggestion("s2_3", "Yes, architecture fully supports floating overlay + pass-through.", ReplyTone.CONCISE)
        )
    ),
    SampleMessageScenario(
        id = "scen_casual",
        title = "Weekend Coffee Catchup",
        message = "Grabbing coffee downtown around 10am tomorrow, let me know if you want to join!",
        sourceApp = "WhatsApp",
        suggestions = listOf(
            ReplySuggestion("s3_1", "Would love to! See you downtown at 10am.", ReplyTone.CASUAL),
            ReplySuggestion("s3_2", "Thanks for the invite! I have an appointment then, but let's connect next week.", ReplyTone.FRIENDLY),
            ReplySuggestion("s3_3", "Count me in! 10am works.", ReplyTone.CONCISE)
        )
    ),
    SampleMessageScenario(
        id = "scen_empty",
        title = "No Incoming Message (Empty Screen)",
        message = "",
        sourceApp = "Home Screen",
        suggestions = emptyList()
    )
)
