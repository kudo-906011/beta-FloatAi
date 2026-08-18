package com.example.ui.viewmodel

import com.example.model.AnalysisStatus
import com.example.model.AppSettings
import com.example.model.AssistantStatus
import com.example.model.DockPosition
import com.example.model.HistoryEntry
import com.example.model.LanguageCardData
import com.example.model.PassThroughState
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.SampleMessageScenario

enum class NavigationTab(val label: String) {
    ASSISTANT("Assistant"),
    HISTORY("History"),
    SETTINGS("Settings")
}

/**
 * Strict separation: Production ReplyFloat Runtime State.
 * Holds ONLY real detected message content and real generated AI responses.
 * Never initialized with or polluted by preview/simulation scenario data.
 */
data class ReplyFloatUiState(
    val currentTab: NavigationTab = NavigationTab.ASSISTANT,
    val isOverlayActive: Boolean = false,
    val isFloatingBarVisible: Boolean = true,
    val isFloatingBarMinimized: Boolean = false,
    val isLanguageBarActive: Boolean = false,
    val languageData: LanguageCardData = LanguageCardData(),
    val passThroughState: PassThroughState = PassThroughState.DISABLED,
    val assistantStatus: AssistantStatus = AssistantStatus.IDLE,
    val analysisStatus: AnalysisStatus = AnalysisStatus.READY,
    val detectedMessage: String = "",
    val detectedSender: String = "",
    val detectedSourceApp: String = "",
    val replies: List<ReplySuggestion> = emptyList(),
    val recentResults: List<com.example.model.RecentResultItem> = emptyList(),
    val activeGenerationId: String? = null,
    val isViewAllExpanded: Boolean = false,
    val collapsedReplyCount: Int = 2,
    val activeToneFilter: ReplyTone? = null,
    val lastCopiedReplyId: String? = null,
    val historyList: List<HistoryEntry> = emptyList(),
    val settings: AppSettings = AppSettings(
        overlayPermissionGranted = false,
        accessibilityPermissionGranted = false,
        notificationPermissionGranted = false,
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

/**
 * Strict separation: Simulation & Interactive Preview State.
 * Confined exclusively to in-app simulation cards and demonstration UI.
 * Never leaks into production floating service or WindowManager overlay.
 */
data class SimulationState(
    val selectedScenarioId: String = "scen_work",
    val detectedMessage: String = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
    val detectedSender: String = "Sarah Jenkins",
    val detectedSourceApp: String = "Slack",
    val replies: List<ReplySuggestion> = defaultSampleReplies,
    val isLanguageBarActive: Boolean = false,
    val languageData: LanguageCardData = defaultSampleLanguageData,
    val isViewAllExpanded: Boolean = false,
    val isMinimized: Boolean = false,
    val isVisible: Boolean = true,
    val passThroughState: PassThroughState = PassThroughState.DISABLED,
    val activeToneFilter: ReplyTone? = null,
    val lastCopiedId: String? = null,
    val isScreenAnalysisOn: Boolean = true,
    val responseMode: com.example.model.ResponseMode = com.example.model.ResponseMode.PASSIVE
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
                filtered.take(2)
            }
        }

    val totalReplyCount: Int
        get() = if (activeToneFilter != null) {
            replies.count { it.tone == activeToneFilter }
        } else {
            replies.size
        }
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

val defaultSampleLanguageData = LanguageCardData(
    generationId = "sim_gen_1",
    detectedLanguage = "English",
    languageCode = "en",
    originalMessage = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
    englishTranslation = "Hey! Are you still free to review the Q3 product roadmap proposal before our 4 PM sync?",
    originalLanguageReply = "Yes, absolutely! I am going over the Q3 roadmap now and will have notes ready for our 4 PM sync.",
    englishReply = "Yes, absolutely! I am going over the Q3 roadmap now and will have notes ready for our 4 PM sync.",
    isHinglish = false,
    isLoading = false
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
        title = "Work Meeting Inquiry (English)",
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
        id = "scen_hinglish",
        title = "YouTube Channel Inquiry (Hinglish)",
        message = "bhai mujhe youtube channel kaise start karna chahiye?",
        sourceApp = "WhatsApp",
        suggestions = listOf(
            ReplySuggestion("s_hing_1", "To start a YouTube channel, pick a specific niche, focus on good audio and video quality, and upload consistently.", ReplyTone.PROFESSIONAL),
            ReplySuggestion("s_hing_2", "Choose a niche you love, start creating simple videos, and be consistent with your schedule!", ReplyTone.CASUAL),
            ReplySuggestion("s_hing_3", "Pick a niche, create high quality content regularly, and engage with your audience.", ReplyTone.CONCISE)
        )
    ),
    SampleMessageScenario(
        id = "scen_russian",
        title = "YouTube Creator Inquiry (Russian)",
        message = "Как мне стать ютубером?",
        sourceApp = "Telegram",
        suggestions = listOf(
            ReplySuggestion("s_ru_1", "To become a YouTuber, define your niche, produce valuable content, and maintain a consistent upload schedule.", ReplyTone.PROFESSIONAL),
            ReplySuggestion("s_ru_2", "Start with what you have, focus on engaging storytelling, and post regularly!", ReplyTone.CASUAL),
            ReplySuggestion("s_ru_3", "Choose a topic, produce quality videos, and upload consistently.", ReplyTone.CONCISE)
        )
    ),
    SampleMessageScenario(
        id = "scen_bengali",
        title = "YouTube Creator Inquiry (Bengali)",
        message = "আমি কীভাবে ইউটিউবার হতে পারি?",
        sourceApp = "Messenger",
        suggestions = listOf(
            ReplySuggestion("s_bn_1", "To become a YouTuber, choose a specific niche, focus on quality content, and maintain a regular upload schedule.", ReplyTone.PROFESSIONAL),
            ReplySuggestion("s_bn_2", "Start with simple tools, create helpful videos, and post consistently!", ReplyTone.CASUAL),
            ReplySuggestion("s_bn_3", "Pick a niche, maintain good video quality, and upload regularly.", ReplyTone.CONCISE)
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
        id = "scen_empty",
        title = "No Incoming Message (Empty Screen)",
        message = "",
        sourceApp = "Home Screen",
        suggestions = emptyList()
    )
)
