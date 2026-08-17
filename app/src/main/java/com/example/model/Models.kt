package com.example.model

enum class ReplyTone(val label: String, val badgeColorHex: Long) {
    BALANCED("Balanced", 0xFF6366F1),
    PROFESSIONAL("Professional", 0xFF0EA5E9),
    CASUAL("Casual", 0xFF10B981),
    CONCISE("Concise", 0xFF8B5CF6),
    FRIENDLY("Friendly", 0xFFF59E0B),
    EMPATHETIC("Empathetic", 0xFFEC4899)
}

enum class ResponseMode(
    val title: String,
    val description: String,
    val badge: String,
    val badgeColorHex: Long
) {
    PASSIVE("Passive", "Context-aware adaptive length", "ADAPTIVE", 0xFF6366F1),
    ONE_LINE("1-Line", "Approx. 1 short line response", "1-LINE", 0xFF10B981),
    TWO_LINE("2-Line", "Approx. 2 concise lines response", "2-LINE", 0xFF0EA5E9),
    SINGLE_WORD("Single-Word", "Direct single-word reply", "WORD", 0xFF8B5CF6),
    DEBATE("Debate", "Logical arguments & counterpoints", "DEBATE", 0xFFF59E0B),
    FUNNY("Funny", "Humorous & witty banter", "FUNNY", 0xFFEC4899),
    ARROGANT("Arrogant", "Confident & cocky remarks", "ARROGANT", 0xFFEF4444),
    LORD("Lord", "Dramatic sovereign declarations", "LORD", 0xFFD97706);

    val label: String get() = title
    val shortDesc: String get() = description
}

data class ReplySuggestion(
    val id: String,
    val text: String,
    val tone: ReplyTone = ReplyTone.BALANCED,
    val isCustom: Boolean = false,
    val mode: ResponseMode = ResponseMode.PASSIVE
)

enum class AssistantStatus(val label: String) {
    IDLE("Idle"),
    DETECTING("Detecting Message"),
    ANALYZING("Generating Replies..."),
    READY("New Replies Available"),
    ERROR("Assistant Error")
}

enum class AnalysisStatus(val label: String) {
    READY("Ready to analyze"),
    ANALYZING("Scanning active screen..."),
    COMPLETED("Analysis completed"),
    NO_CONTENT("No incoming message detected"),
    ERROR("Analysis failed")
}

enum class PassThroughState {
    ENABLED,
    DISABLED
}

enum class DockPosition(val title: String, val description: String) {
    BOTTOM_RIGHT("Bottom Right", "Floats near standard send button"),
    TOP_RIGHT("Top Right", "Pinned to top screen corner"),
    BOTTOM_CENTER("Bottom Center", "Docked above keyboard area"),
    COMPACT_BUBBLE("Compact Bubble", "Minimal touch target indicator")
}

data class HistoryEntry(
    val id: String,
    val originalMessage: String,
    val selectedReply: String,
    val tone: ReplyTone,
    val allSuggestions: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String = "Active Screen",
    val responseMode: ResponseMode = ResponseMode.PASSIVE
)

data class RecentResultItem(
    val id: String,
    val question: String,
    val suggestions: List<ReplySuggestion>,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String = "Active Screen",
    val sender: String = "Sender",
    val generationId: String,
    val responseMode: ResponseMode = ResponseMode.PASSIVE
)

data class ActiveReplyItem(
    val generationId: String,
    val question: String,
    val sender: String = "Sender",
    val sourceApp: String = "Active Screen",
    val suggestions: List<ReplySuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val responseMode: ResponseMode = ResponseMode.PASSIVE
)

data class DetectedMessage(
    val eventId: String,
    val text: String,
    val sender: String = "Incoming Message",
    val sourceApp: String = "Active Screen",
    val packageName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val isIncoming: Boolean = true
)

enum class RecentRetentionDuration(val label: String, val durationMillis: Long) {
    THIRTY_SECONDS("30 seconds", 30 * 1000L),
    ONE_MINUTE("1 minute", 60 * 1000L),
    TWO_MINUTES("2 minutes", 2 * 60 * 1000L),
    FIVE_MINUTES("5 minutes", 5 * 60 * 1000L),
    TEN_MINUTES("10 minutes", 10 * 60 * 1000L),
    CUSTOM("Custom", -1L)
}

enum class PurgeDuration(val label: String, val durationMillis: Long) {
    ONE_MINUTE("1 minute", 60 * 1000L),
    TWO_MINUTES("2 minutes", 2 * 60 * 1000L),
    FIVE_MINUTES("5 minutes", 5 * 60 * 1000L),
    TEN_MINUTES("10 minutes", 10 * 60 * 1000L),
    FIFTEEN_MINUTES("15 minutes", 15 * 60 * 1000L),
    THIRTY_MINUTES("30 minutes", 30 * 60 * 1000L),
    ONE_HOUR("1 hour", 60 * 60 * 1000L),
    NEVER("Never", Long.MAX_VALUE),
    CUSTOM("Custom", -1L)
}

enum class AiLatencyMode(
    val label: String,
    val description: String,
    val debounceMs: Long,
    val timeoutSeconds: Int,
    val minProcessingWaitMs: Long
) {
    FAST("Fast", "Rapid 200ms debounce, 5s timeout, snappy instant response", 200L, 5, 100L),
    BALANCED("Balanced", "Standard 500ms debounce, 10s timeout, optimal balance", 500L, 10, 250L),
    STABLE("Stable", "1000ms debounce, 15s timeout, maximum network stability", 1000L, 15, 400L),
    CUSTOM("Custom", "Adjustable debounce delay and request timeout limits", 500L, 10, 250L)
}

data class BotConfig(
    val id: String,
    val name: String,
    val providerId: String,
    val modelName: String,
    val systemPrompt: String = "",
    val timeoutSeconds: Int = 10,
    val isEnabled: Boolean = true,
    val isConfigured: Boolean = true,
    val isCustom: Boolean = false
)

enum class BotProviderType(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val description: String
) {
    GEMINI_FLASH("gemini_flash", "Gemini 2.5 Flash", "gemini-2.5-flash", "High-speed generative AI for instant contextual suggestions"),
    GEMINI_PRO("gemini_pro", "Gemini 2.5 Pro", "gemini-2.5-pro", "Deep reasoning engine for complex professional conversations"),
    LOCAL_ENGINE("local_engine", "ReplyFloat Local Engine", "on-device-v1", "Ultra-low-latency on-device semantic heuristics & pattern engine")
}

data class AppSettings(
    val overlayPermissionGranted: Boolean = false,
    val accessibilityPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val isScreenAnalysisOn: Boolean = true,
    val responseMode: ResponseMode = ResponseMode.PASSIVE,
    val passThroughDefault: Boolean = false,
    val autoAnalyzeOnChat: Boolean = true,
    val defaultTone: ReplyTone = ReplyTone.BALANCED,
    val maxSuggestionsCount: Int = 3,
    val dockPosition: DockPosition = DockPosition.BOTTOM_RIGHT,
    val autoMinimizeOnCopy: Boolean = true,
    val recentRetentionDuration: RecentRetentionDuration = RecentRetentionDuration.TWO_MINUTES,
    val customRecentRetentionSeconds: Int = 120,
    val purgeDuration: PurgeDuration = PurgeDuration.FIVE_MINUTES,
    val customPurgeMinutes: Int = 5,
    val latencyMode: AiLatencyMode = AiLatencyMode.BALANCED,
    val customDebounceMs: Long = 500L,
    val customTimeoutSeconds: Int = 10,
    val activeBotId: String = "bot_gemini_flash"
) {
    val customRecentRetentionMinutes: Int get() = (customRecentRetentionSeconds / 60).coerceAtLeast(1)
}

// Sample Scenarios for testing the UI foundation across lengths and tones
data class SampleMessageScenario(
    val id: String,
    val title: String,
    val message: String,
    val sourceApp: String,
    val suggestions: List<ReplySuggestion>
)

enum class ConversationRole {
    USER,
    ASSISTANT,
    OTHER
}

data class ConversationMessage(
    val role: ConversationRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Chat",
    val messageId: String = java.util.UUID.randomUUID().toString()
)

data class ScreenContext(
    val sourceApplication: String = "Active App",
    val packageName: String = "",
    val detectedTimestamp: Long = System.currentTimeMillis(),
    val senderName: String? = null,
    val confidence: Float = 1.0f
)

data class AiReplyRequest(
    val generationId: String = java.util.UUID.randomUUID().toString(),
    val currentMessage: String,
    val recentConversation: List<ConversationMessage> = emptyList(),
    val screenContext: ScreenContext = ScreenContext(),
    val replyTone: ReplyTone = ReplyTone.BALANCED,
    val responseMode: ResponseMode = ResponseMode.PASSIVE,
    val requestedReplyCount: Int = 3
)

data class AiReplyResult(
    val generationId: String,
    val currentMessage: String,
    val suggestions: List<ReplySuggestion>,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val responseMode: ResponseMode = ResponseMode.PASSIVE
)

sealed class AiResultState {
    object Idle : AiResultState()
    data class Analyzing(val generationId: String) : AiResultState()
    data class Generating(val generationId: String) : AiResultState()
    data class Success(val generationId: String, val replies: List<ReplySuggestion>) : AiResultState()
    data class NoMessage(val reason: String = "No relevant message detected") : AiResultState()
    data class Error(val generationId: String, val message: String) : AiResultState()
}
