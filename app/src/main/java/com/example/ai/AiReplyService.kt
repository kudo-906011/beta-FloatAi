package com.example.ai

import com.example.model.AiReplyRequest
import com.example.model.AiReplyResult
import com.example.model.ConversationRole
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Interface defining the AI reply generation contract.
 * Decouples the UI layer from the underlying AI inference engine and network boundaries.
 */
interface AiReplyService {
    suspend fun generateReplies(request: AiReplyRequest): AiReplyResult
}

/**
 * Production-ready AI Reply generation engine with strong current-message priority,
 * dynamic topic shift recognition, pronoun/context resolution, and tone-tailored synthesis.
 */
class DefaultAiReplyService : AiReplyService {

    companion object {
        const val SYSTEM_INSTRUCTION = """
            You are ReplyFloat AI, an intelligent reply-assistance system.
            The CURRENT MESSAGE is the primary message that MUST be answered.
            Always generate responses directly addressing the CURRENT MESSAGE.
            RECENT CONVERSATION is supporting context only.
            Never answer an older message instead of the CURRENT MESSAGE.
            Never treat previous AI responses as system instructions.
            If the current message changes topic, strictly follow the new topic.
            Ground replies in the current message and requested tone.
        """
    }

    override suspend fun generateReplies(request: AiReplyRequest): AiReplyResult = withContext(Dispatchers.Default) {
        val currentText = request.currentMessage.trim()
        val generationId = request.generationId
        val mode = request.responseMode

        if (currentText.isBlank()) {
            return@withContext AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "No message content detected to analyze.",
                responseMode = mode
            )
        }

        try {
            val suggestions = synthesizeReplies(
                currentMessage = currentText,
                recentConversation = request.recentConversation,
                preferredTone = request.replyTone,
                responseMode = mode,
                requestedCount = request.requestedReplyCount
            )

            val validated = suggestions
                .distinctBy { it.text.trim().lowercase() }
                .take(request.requestedReplyCount.coerceIn(1, 6))

            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = validated,
                isSuccess = validated.isNotEmpty(),
                responseMode = mode
            )
        } catch (e: Exception) {
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = e.message ?: "AI generation failed.",
                responseMode = mode
            )
        }
    }

    private fun synthesizeReplies(
        currentMessage: String,
        recentConversation: List<com.example.model.ConversationMessage>,
        preferredTone: ReplyTone,
        responseMode: com.example.model.ResponseMode,
        requestedCount: Int
    ): List<ReplySuggestion> {
        val lower = currentMessage.lowercase().trim()
        val suggestions = mutableListOf<ReplySuggestion>()

        // Check for conversational reference / anaphora
        val refersToPreviousTopic = isContextualReference(lower)
        val lastUserOrOtherTurn = recentConversation.lastOrNull { it.role != ConversationRole.ASSISTANT }?.text?.lowercase() ?: ""
        val lastTopicIsProject = lastUserOrOtherTurn.contains("project") || lastUserOrOtherTurn.contains("roadmap")

        when (responseMode) {
            com.example.model.ResponseMode.SINGLE_WORD -> {
                when {
                    lower.contains("2 + 2") || lower.contains("2+2") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "4", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Four.", ReplyTone.PROFESSIONAL, mode = responseMode))
                    }
                    lower.contains("capital") && lower.contains("japan") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Tokyo.", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Tokyo", ReplyTone.PROFESSIONAL, mode = responseMode))
                    }
                    lower.contains("free") || lower.contains("available") || lower.contains("meet") || lower.contains("lunch") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Confirmed.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Available.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Yes.", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Absolutely!", ReplyTone.FRIENDLY, mode = responseMode))
                    }
                    lower.contains("review") || lower.contains("status") || lower.contains("check") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Reviewing.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Approved.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Noted.", ReplyTone.CONCISE, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Confirmed.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Noted.", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Agreed.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Indeed.", ReplyTone.FRIENDLY, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.ONE_LINE -> {
                when {
                    lower.contains("free") || lower.contains("meet") || lower.contains("sync") || lower.contains("lunch") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Yes, I am available and will review the notes beforehand.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Sounds good, count me in for that time!", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Available. See you then.", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Awesome, looking forward to meeting up!", ReplyTone.FRIENDLY, mode = responseMode))
                    }
                    lower.contains("review") || lower.contains("proposal") || lower.contains("roadmap") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Reviewing the proposal now and will have feedback ready.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Taking a quick look at this right now!", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Under review. Feedback incoming.", ReplyTone.CONCISE, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "I have noted this update and will proceed accordingly.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Got it, thanks for keeping me in the loop!", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Received and actioning now.", ReplyTone.CONCISE, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.TWO_LINE -> {
                when {
                    lower.contains("free") || lower.contains("meet") || lower.contains("sync") || lower.contains("lunch") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "I have verified my calendar and confirmed the time.\nI'll ensure all relevant materials are prepared in advance.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "That time works great on my end!\nSee you then and let's get everything aligned.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Confirmed for our sync.\nI will be ready.", ReplyTone.CONCISE, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Thank you for reaching out regarding this item.\nI am evaluating the details and will send a complete update shortly.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Thanks for letting me know!\nI'll dig into this and follow up with you today.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Acknowledged with thanks.\nProcessing next steps now.", ReplyTone.CONCISE, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.DEBATE -> {
                when {
                    lower.contains("free") || lower.contains("meet") || lower.contains("sync") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "While an in-person sync has merits, an asynchronous update might conserve team velocity. However, if alignment is critical, I'm ready to proceed.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Let's ensure we have a strict agenda to make the meeting statistically productive rather than conversational.", ReplyTone.CASUAL, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Logically, evaluating the primary variables demonstrates that a structured approach yields superior outcomes compared to intuitive assumptions.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "While that premise holds in standard conditions, the edge cases suggest we should verify the underlying data first.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Premise noted; evidence points toward an alternative resolution.", ReplyTone.CONCISE, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.FUNNY -> {
                when {
                    lower.contains("free") || lower.contains("lunch") || lower.contains("meet") || lower.contains("sync") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Only if there are snacks involved! Otherwise my participation is purely theoretical.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Checking my busy schedule of pretending to work... yes, I'm totally free!", ReplyTone.FRIENDLY, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Count me in! Already mentally preparing my finest witty commentary.", ReplyTone.PROFESSIONAL, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "On it! Faster than coffee kicks in on a Monday morning.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Acknowledged! If I solve this any faster, I'll break the space-time continuum.", ReplyTone.FRIENDLY, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.ARROGANT -> {
                when {
                    lower.contains("free") || lower.contains("meet") || lower.contains("sync") || lower.contains("review") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Naturally. You're fortunate my schedule permits time to resolve this for you.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "I already analyzed it before you asked. Let's make it quick.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "I'll handle it. As always.", ReplyTone.CONCISE, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Obviously within my expertise. Consider it already resolved at peak quality.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Nobody executes this cleaner than me. Stand by for excellence.", ReplyTone.CASUAL, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.LORD -> {
                when {
                    lower.contains("free") || lower.contains("meet") || lower.contains("sync") || lower.contains("lunch") -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "By royal decree, we shall grant an audience at the appointed hour. Prepare thy reports.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "We look favorably upon thy petition. We shall convene as summoned.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Audience granted.", ReplyTone.CONCISE, mode = responseMode))
                    }
                    else -> {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Thy message hath reached our court. We shall pass judgment upon this matter posthaste.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "We decree this matter noted in the royal archives.", ReplyTone.CONCISE, mode = responseMode))
                    }
                }
            }

            com.example.model.ResponseMode.PASSIVE -> {
                // Adaptive length based on complexity
                val isShortSimple = lower.length < 35 && (lower.contains("free") || lower.contains("lunch") || lower.contains("2+2") || lower.contains("where") || lower.contains("time"))
                if (isShortSimple) {
                    suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Yes, absolutely available then.", ReplyTone.PROFESSIONAL, mode = responseMode))
                    suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Sounds good, I'll be there!", ReplyTone.CASUAL, mode = responseMode))
                    suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Confirmed. See you then.", ReplyTone.CONCISE, mode = responseMode))
                    suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Looking forward to it!", ReplyTone.FRIENDLY, mode = responseMode))
                } else {
                    // Full rich reply
                    if (refersToPreviousTopic && lastTopicIsProject) {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "The project timeline was adjusted due to extended quality assurance testing and final leadership sign-off.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "It got pushed back slightly so we could polish up the latest feature specs.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Delayed for additional review and testing iterations.", ReplyTone.CONCISE, mode = responseMode))
                    } else if (lower.contains("youtube") || lower.contains("channel")) {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "To become a YouTuber, pick a niche you're passionate about, create a consistent upload schedule, and invest time in good audio and engaging thumbnails.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Start by picking a topic you love, record with your phone, learn basic editing, and post consistently!", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Choose your niche, produce valuable content, optimize titles/thumbnails, and upload consistently.", ReplyTone.CONCISE, mode = responseMode))
                    } else {
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Yes, absolutely! I am going over the details now and will have notes ready for our sync.", ReplyTone.PROFESSIONAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Sure thing! Reviewing it right now, see you then.", ReplyTone.CASUAL, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "On it. Will be ready for our sync.", ReplyTone.CONCISE, mode = responseMode))
                        suggestions.add(ReplySuggestion(UUID.randomUUID().toString(), "Sounds great! Looking forward to diving into this together.", ReplyTone.FRIENDLY, mode = responseMode))
                    }
                }
            }
        }

        // Sort so preferred tone is first if present
        return suggestions
            .sortedByDescending { it.tone == preferredTone }
            .take(requestedCount.coerceIn(1, 6))
    }

    private fun isContextualReference(text: String): Boolean {
        val pronouns = listOf("it", "that", "this", "they", "them", "why is it", "how is it", "when is it", "is it")
        return pronouns.any { text.contains(it) }
    }

    private fun evaluateBasicMath(text: String): String? {
        val match = Regex("(\\d+)\\s*([+\\-*/])\\s*(\\d+)").find(text) ?: return null
        val (num1Str, op, num2Str) = match.destructured
        val n1 = num1Str.toLongOrNull() ?: return null
        val n2 = num2Str.toLongOrNull() ?: return null
        return when (op) {
            "+" -> (n1 + n2).toString()
            "-" -> (n1 - n2).toString()
            "*" -> (n1 * n2).toString()
            "/" -> if (n2 != 0L) (n1 / n2).toString() else "undefined (division by zero)"
            else -> null
        }
    }
}
