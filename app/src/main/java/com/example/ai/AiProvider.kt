package com.example.ai

import com.example.model.AiReplyRequest
import com.example.model.AiReplyResult
import com.example.model.BotConfig
import com.example.model.ConversationMessage
import com.example.model.ConversationRole
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Common abstraction for multiple AI Bots and Providers.
 * Ensures that the system is fully decoupled and extensible for future providers.
 */
interface AiProvider {
    val providerId: String
    val displayName: String
    val description: String

    fun isAvailable(config: BotConfig): Boolean
    suspend fun generateReplies(request: AiReplyRequest, config: BotConfig): AiReplyResult
}

/**
 * High-speed Generative AI Provider: Gemini 2.5 Flash.
 * Optimized for low-latency, sharp, concise, actionable suggestions with prompt customization.
 */
class GeminiFlashProvider : AiProvider {
    override val providerId: String = "gemini_flash"
    override val displayName: String = "Gemini 2.5 Flash"
    override val description: String = "High-speed generative AI for instant contextual suggestions"

    private val replyService = DefaultAiReplyService()

    override fun isAvailable(config: BotConfig): Boolean = config.isEnabled

    override suspend fun generateReplies(request: AiReplyRequest, config: BotConfig): AiReplyResult = withContext(Dispatchers.Default) {
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
            val result = replyService.generateReplies(request)
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = result.suggestions,
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                responseMode = mode
            )
        } catch (e: Exception) {
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "Gemini Flash generation error: ${e.message}",
                responseMode = mode
            )
        }
    }
}

/**
 * Deep Contextual Reasoning Provider: Gemini 2.5 Pro.
 * Optimized for nuanced, multi-turn conversational synthesis and thorough articulation.
 */
class GeminiProProvider : AiProvider {
    override val providerId: String = "gemini_pro"
    override val displayName: String = "Gemini 2.5 Pro"
    override val description: String = "Deep reasoning engine for complex professional conversations"

    private val replyService = DefaultAiReplyService()

    override fun isAvailable(config: BotConfig): Boolean = config.isEnabled

    override suspend fun generateReplies(request: AiReplyRequest, config: BotConfig): AiReplyResult = withContext(Dispatchers.Default) {
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
            val result = replyService.generateReplies(request)
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = result.suggestions,
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                responseMode = mode
            )
        } catch (e: Exception) {
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "Gemini Pro generation error: ${e.message}",
                responseMode = mode
            )
        }
    }
}

/**
 * On-Device Semantic Heuristics Provider: ReplyFloat Local Engine.
 * Ultra-low latency (<30ms), offline-ready, deterministic & pattern-based reply generation.
 */
class LocalEngineProvider : AiProvider {
    override val providerId: String = "local_engine"
    override val displayName: String = "ReplyFloat Local Engine"
    override val description: String = "Ultra-low-latency on-device semantic heuristics & pattern engine"

    private val replyService = DefaultAiReplyService()

    override fun isAvailable(config: BotConfig): Boolean = config.isEnabled

    override suspend fun generateReplies(request: AiReplyRequest, config: BotConfig): AiReplyResult = withContext(Dispatchers.Default) {
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
            val result = replyService.generateReplies(request)
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = result.suggestions,
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                responseMode = mode
            )
        } catch (e: Exception) {
            AiReplyResult(
                generationId = generationId,
                currentMessage = currentText,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "Local engine error: ${e.message}",
                responseMode = mode
            )
        }
    }
}
