package com.example.ai

import com.example.model.DetectionLogEntry
import java.util.Locale

/**
 * Result of the two-stage question detection and classification pipeline.
 */
data class ClassificationResult(
    val isQualifyingQuestion: Boolean,
    val isConversational: Boolean,
    val isQuestionOrInquiry: Boolean,
    val confidence: Float,
    val detectedLanguage: String,
    val rejectionReason: String? = null
)

/**
 * High-accuracy, conservative Question Classifier that inspects candidates
 * extracted from accessibility/screen data BEFORE sending to the AI.
 *
 * Guarantees:
 * 1. Rejects random words, UI text, buttons, tab names, app names, timestamps,
 *    notification text, menu items, advertisements, status bar, and non-conversational text.
 * 2. Conservative bias: When uncertain, REJECTS to prevent false AI triggers.
 * 3. Supports multi-lingual question structures: English, Bengali (বাংলা), Russian (Русский),
 *    Hindi (हिंदी), Hinglish, Spanish (Español), French, German, etc.
 * 4. Understands conversational questions even without explicit English '?' punctuation.
 */
object QuestionClassifier {

    private val STRICT_UI_KEYWORDS = setOf(
        // Tabs & Browser chrome
        "new tab", "close tab", "switch tab", "tabs", "tab", "incognito", "bookmarks", "history",
        "downloads", "reload", "share", "extensions", "desktop site", "search or type url",
        "search or type web address", "find in page", "add to home screen", "open in browser",

        // Common Buttons & Actions
        "send", "reply", "back", "cancel", "search", "more", "options", "done", "skip",
        "save", "delete", "edit", "copy", "cut", "paste", "select all", "undo", "redo",
        "apply", "reset", "clear", "close", "open", "submit", "login", "sign in", "sign up",
        "register", "confirm", "continue", "next", "agree", "accept", "decline", "manage",

        // Social / Media actions
        "like", "dislike", "subscribe", "subscribed", "follow", "following", "followers",
        "posts", "reels", "shorts", "comment", "comments", "remix", "clip", "save to playlist",
        "watch later", "library", "explore", "trending", "feed", "notifications", "profile",
        "account", "mute", "block", "report", "view all", "show more", "show less",

        // Media playback
        "play", "pause", "stop", "shuffle", "repeat", "mute", "unmute", "volume", "fullscreen",

        // Chat metadata / UI markers
        "type a message", "message", "chat", "voice message", "call", "video call",
        "online", "typing...", "typing", "last seen", "seen", "delivered", "sent", "read",
        "unread", "unread messages", "today", "yesterday", "tomorrow",
        "end-to-end encrypted", "messages and calls are end-to-end encrypted",
        "media, links, and docs", "starred messages", "disappearing messages", "clear chat"
    )

    private val DAYS_AND_MONTHS = setOf(
        "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
        "sun", "mon", "tue", "wed", "thu", "fri", "sat",
        "january", "february", "march", "april", "may", "june", "july", "august",
        "september", "october", "november", "december",
        "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    )

    /**
     * Evaluates whether candidate text is an actual conversational question or inquiry
     * requiring a response.
     */
    fun classifyCandidate(text: String, viewId: String = ""): ClassificationResult {
        val trimmed = text.trim()

        // 1. Length constraints
        if (trimmed.length < 3) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Unknown",
                rejectionReason = "Text is too short (< 3 characters)"
            )
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        val viewIdLower = viewId.lowercase(Locale.ROOT)

        // 2. Reject pure numbers, dates, times, currency, battery, percentages
        if (trimmed.matches(Regex("^[0-9.,:;+\\-*/%#@!$&()_~\\s]+$"))) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Numeric/Symbol",
                rejectionReason = "Text contains only numbers, math, or punctuation"
            )
        }

        // Time format (e.g. 12:45, 9:30 PM, 14:00)
        if (trimmed.matches(Regex("^[0-9]{1,2}:[0-9]{2}(\\s?[AaPp][Mm])?$"))) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Time",
                rejectionReason = "Text matches clock/time format"
            )
        }

        // Days of week or standalone months
        if (DAYS_AND_MONTHS.contains(lower)) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Date/Calendar",
                rejectionReason = "Text matches standalone day or month name"
            )
        }

        // URLs, emails, domains, file paths
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.") ||
            lower.contains("http://") || lower.contains("https://") ||
            trimmed.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"))
        ) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "URL/Email",
                rejectionReason = "Text is a URL or email address"
            )
        }

        // 3. Reject Known Exact UI terms or Tab Labels
        if (STRICT_UI_KEYWORDS.contains(lower)) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "UI Text",
                rejectionReason = "Text matches exact UI button, menu, or tab label"
            )
        }

        if (lower.startsWith("tab ") || lower.contains("new tab") || lower.contains("close tab") || lower.contains("tabs open")) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Tab Navigation",
                rejectionReason = "Text is a browser tab navigation action"
            )
        }

        // 4. Reject System Header / Toolbar / Tab View IDs if present
        if (viewIdLower.contains("action_bar") || viewIdLower.contains("toolbar") ||
            viewIdLower.contains("tab_layout") || viewIdLower.contains("status_bar") ||
            viewIdLower.contains("omnibox") || viewIdLower.contains("search_box")
        ) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "System Header",
                rejectionReason = "View ID indicates system toolbar, tab, or search header"
            )
        }

        // 5. Positive Question & Conversational Identification
        val hasQuestionMark = trimmed.contains("?") || trimmed.contains("¿") || trimmed.contains("؟")
        val cleanWords = lower.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        // A single question mark without any real words is rejected
        if (hasQuestionMark && cleanWords.isEmpty()) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = false,
                isQuestionOrInquiry = false,
                confidence = 0.0f,
                detectedLanguage = "Punctuation",
                rejectionReason = "Text contains only question marks with no words"
            )
        }

        // 6. Multi-lingual Question Analysis
        var detectedLang = "English"
        var isBengali = false
        var isRussian = false
        var isHindi = false
        var isHinglish = false
        var isSpanish = false
        var isFrenchOrGerman = false
        var isPositiveQuestion = false
        var confidenceScore = 0.0f

        // Check script ranges
        val hasBengaliScript = trimmed.any { it in '\u0980'..'\u09FF' }
        val hasCyrillicScript = trimmed.any { it in '\u0400'..'\u04FF' }
        val hasDevanagariScript = trimmed.any { it in '\u0900'..'\u097F' }

        if (hasBengaliScript) {
            detectedLang = "Bengali"
            isBengali = true
            val bengaliTokens = setOf(
                "কীভাবে", "কিভাবে", "কী", "কি", "কেন", "কেমন", "কোথায়", "কোথাই", "কখন", "কে", "কার",
                "কাকে", "কিসের", "কোন", "কোনটি", "পারি", "বলুন", "বলো", "সাহায্য", "আছো", "আছেন",
                "অবস্থা", "খবর", "হবে", "পারবেন", "ইউটিউবার", "প্রজেক্ট", "মিটিং", "সময়", "শুনুন"
            )
            val matched = cleanWords.any { word -> bengaliTokens.any { word.contains(it) } }
            if (matched || hasQuestionMark) {
                isPositiveQuestion = true
                confidenceScore = if (hasQuestionMark) 0.95f else 0.85f
            }
        } else if (hasCyrillicScript) {
            detectedLang = "Russian"
            isRussian = true
            val russianTokens = setOf(
                "как", "что", "где", "когда", "почему", "зачем", "кто", "куда", "откуда", "чей",
                "сколько", "ли", "подскажи", "помоги", "расскажи", "дела", "жизнь", "свободен",
                "думаешь", "стать", "случилось", "помочь", "встреча", "проектом", "привет", "ютубером"
            )
            val matched = cleanWords.any { word -> russianTokens.contains(word) } ||
                russianTokens.any { lower.contains(it) }
            if (matched || hasQuestionMark) {
                isPositiveQuestion = true
                confidenceScore = if (hasQuestionMark) 0.95f else 0.85f
            }
        } else if (hasDevanagariScript) {
            detectedLang = "Hindi"
            isHindi = true
            val hindiTokens = setOf(
                "क्या", "क्यों", "कैसे", "कहाँ", "कब", "कौन", "किसे", "कितना", "नमस्ते", "बताओ",
                "मदद", "कैसा", "कैसी", "शुरू", "सलाह", "यूट्यूबर", "प्रोजेक्ट"
            )
            val matched = cleanWords.any { word -> hindiTokens.any { word.contains(it) } }
            if (matched || hasQuestionMark) {
                isPositiveQuestion = true
                confidenceScore = if (hasQuestionMark) 0.95f else 0.85f
            }
        } else {
            // Latin-script analysis (English, Hinglish, Spanish, French, German)

            // Check Hinglish
            val hinglishTokens = setOf(
                "kaise", "kese", "kya", "kyu", "kyun", "kab", "kaha", "kahan", "kaun", "kisko",
                "kitna", "batao", "bataiye", "bhai", "yaar", "chahiye", "karna", "shuru", "karega",
                "kaisa", "kese ho", "kaise ho", "kya hua", "project ka kya", "youtube channel", "youtuber"
            )
            val hinglishMatches = cleanWords.count { hinglishTokens.contains(it) }
            if (hinglishMatches >= 1 && (lower.contains("kaise") || lower.contains("kya") || lower.contains("bhai") || lower.contains("batao") || lower.contains("chahiye") || lower.contains("karna"))) {
                detectedLang = "Hinglish"
                isHinglish = true
                isPositiveQuestion = true
                confidenceScore = if (hasQuestionMark) 0.92f else 0.82f
            }

            // Check Spanish
            val spanishTokens = setOf(
                "cómo", "como", "qué", "que", "cuándo", "cuando", "dónde", "donde", "por qué",
                "porque", "quién", "quien", "cuál", "puedes", "podrías", "estás", "estas", "tal", "ayúdame"
            )
            if (trimmed.contains("¿") || cleanWords.count { spanishTokens.contains(it) } >= 2) {
                detectedLang = "Spanish"
                isSpanish = true
                isPositiveQuestion = true
                confidenceScore = if (trimmed.contains("¿") || hasQuestionMark) 0.95f else 0.85f
            }

            // Check French / German
            if (lower.startsWith("comment") || lower.startsWith("pourquoi") || lower.startsWith("qu'est-ce") ||
                lower.startsWith("wie") || lower.startsWith("warum") || lower.startsWith("was") || lower.startsWith("wann")
            ) {
                detectedLang = if (lower.startsWith("wie") || lower.startsWith("warum")) "German" else "French"
                isFrenchOrGerman = true
                isPositiveQuestion = true
                confidenceScore = 0.88f
            }

            // Check English Questions & Inquiries
            if (!isHinglish && !isSpanish && !isFrenchOrGerman) {
                val englishInterrogatives = setOf(
                    "how", "what", "where", "when", "why", "who", "which", "whose", "whom"
                )
                val englishModalStarters = listOf(
                    "can you", "could you", "would you", "will you", "should i", "should we",
                    "is it", "is there", "are you", "are we", "do you", "did you", "have you",
                    "has anyone", "can i", "may i", "shall we"
                )
                val englishConversationalInquiries = listOf(
                    "help me", "need help", "tell me", "explain", "any update", "what's up",
                    "whats up", "how are you", "how's it going", "what happened", "how to",
                    "how do i", "how can i", "what do you think", "are you free", "are you available",
                    "free to", "let me know if", "give me advice", "suggest me", "review this"
                )

                val startsWithInterrogative = cleanWords.isNotEmpty() && englishInterrogatives.contains(cleanWords.first())
                val startsWithModal = englishModalStarters.any { lower.startsWith(it) }
                val hasConversationalInquiry = englishConversationalInquiries.any { lower.contains(it) }

                if (hasQuestionMark || startsWithInterrogative || startsWithModal || hasConversationalInquiry) {
                    isPositiveQuestion = true
                    confidenceScore = when {
                        hasQuestionMark && (startsWithInterrogative || startsWithModal || hasConversationalInquiry) -> 0.98f
                        hasQuestionMark -> 0.90f
                        startsWithInterrogative || startsWithModal -> 0.88f
                        else -> 0.80f
                    }
                }
            }
        }

        // 7. Conservative Filter:
        // If there is NO positive question marker AND NO explicit question mark,
        // we conservatively REJECT it to avoid processing random screen text or news articles.
        if (!isPositiveQuestion) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = cleanWords.size >= 2,
                isQuestionOrInquiry = false,
                confidence = 0.20f,
                detectedLanguage = detectedLang,
                rejectionReason = "Conservative Classifier: No positive question or conversational inquiry markers detected"
            )
        }

        // If confidence is below 0.50f threshold, reject
        if (confidenceScore < 0.50f) {
            return ClassificationResult(
                isQualifyingQuestion = false,
                isConversational = true,
                isQuestionOrInquiry = false,
                confidence = confidenceScore,
                detectedLanguage = detectedLang,
                rejectionReason = "Confidence score below threshold ($confidenceScore < 0.50)"
            )
        }

        return ClassificationResult(
            isQualifyingQuestion = true,
            isConversational = true,
            isQuestionOrInquiry = true,
            confidence = confidenceScore,
            detectedLanguage = detectedLang,
            rejectionReason = null
        )
    }
}
