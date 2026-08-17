package com.example.ai

import com.example.model.AiLatencyMode
import com.example.model.AiReplyRequest
import com.example.model.AiReplyResult
import com.example.model.BotConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized Bot Manager orchestrating multiple AI Bots, provider registry,
 * single active bot routing, safe request dispatching, and dynamic provider extension.
 */
object AiBotManager {

    val DEFAULT_BOTS = listOf(
        BotConfig(
            id = "bot_gemini_flash",
            name = "Gemini 2.5 Flash",
            providerId = "gemini_flash",
            modelName = "gemini-2.5-flash",
            systemPrompt = "Speed-optimized generative AI",
            timeoutSeconds = 8,
            isEnabled = true,
            isConfigured = true,
            isCustom = false
        ),
        BotConfig(
            id = "bot_gemini_pro",
            name = "Gemini 2.5 Pro",
            providerId = "gemini_pro",
            modelName = "gemini-2.5-pro",
            systemPrompt = "Deep context & professional reasoning",
            timeoutSeconds = 12,
            isEnabled = true,
            isConfigured = true,
            isCustom = false
        ),
        BotConfig(
            id = "bot_local_engine",
            name = "ReplyFloat Local Engine",
            providerId = "local_engine",
            modelName = "on-device-v1",
            systemPrompt = "On-device semantic pattern engine",
            timeoutSeconds = 3,
            isEnabled = true,
            isConfigured = true,
            isCustom = false
        )
    )

    private val providers = ConcurrentHashMap<String, AiProvider>().apply {
        put("gemini_flash", GeminiFlashProvider())
        put("gemini_pro", GeminiProProvider())
        put("local_engine", LocalEngineProvider())
    }

    private val _configuredBots = MutableStateFlow<List<BotConfig>>(DEFAULT_BOTS)
    val configuredBots: StateFlow<List<BotConfig>> = _configuredBots.asStateFlow()

    private val _activeBotId = MutableStateFlow("bot_gemini_flash")
    val activeBotId: StateFlow<String> = _activeBotId.asStateFlow()

    fun setBots(bots: List<BotConfig>) {
        if (bots.isNotEmpty()) {
            _configuredBots.value = bots
            if (bots.none { it.id == _activeBotId.value }) {
                _activeBotId.value = bots.first().id
            }
        }
    }

    fun getActiveBot(): BotConfig {
        val currentId = _activeBotId.value
        return _configuredBots.value.find { it.id == currentId }
            ?: _configuredBots.value.firstOrNull()
            ?: DEFAULT_BOTS.first()
    }

    fun setActiveBot(botId: String) {
        val exists = _configuredBots.value.any { it.id == botId }
        if (exists) {
            _activeBotId.value = botId
        }
    }

    fun addBot(bot: BotConfig) {
        val current = _configuredBots.value.toMutableList()
        current.add(bot)
        _configuredBots.value = current
    }

    fun updateBot(bot: BotConfig) {
        val current = _configuredBots.value.map {
            if (it.id == bot.id) bot else it
        }
        _configuredBots.value = current
    }

    fun deleteBot(botId: String) {
        val current = _configuredBots.value.filterNot { it.id == botId }
        if (current.isNotEmpty()) {
            _configuredBots.value = current
            if (_activeBotId.value == botId) {
                _activeBotId.value = current.first().id
            }
        }
    }

    fun registerProvider(provider: AiProvider) {
        providers[provider.providerId] = provider
    }

    fun isBotAvailable(bot: BotConfig): Boolean {
        if (!bot.isEnabled) return false
        val provider = providers[bot.providerId] ?: return false
        return provider.isAvailable(bot)
    }

    fun getBotStatus(bot: BotConfig): String {
        if (!bot.isEnabled) return "Disabled"
        val provider = providers[bot.providerId]
        return when {
            provider == null -> "Not Configured"
            bot.providerId == "gemini_flash" -> "Available • High Speed"
            bot.providerId == "gemini_pro" -> "Available • Deep Reasoning"
            bot.providerId == "local_engine" -> "Active • On-Device Engine"
            else -> "Available • Custom Provider"
        }
    }

    suspend fun generateReplies(
        request: AiReplyRequest,
        latencyMode: AiLatencyMode,
        customTimeoutSeconds: Int = 10
    ): AiReplyResult {
        val activeBot = getActiveBot()

        if (!activeBot.isEnabled) {
            return AiReplyResult(
                generationId = request.generationId,
                currentMessage = request.currentMessage,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "Selected bot '${activeBot.name}' is currently disabled."
            )
        }

        val provider = providers[activeBot.providerId]
        if (provider == null) {
            return AiReplyResult(
                generationId = request.generationId,
                currentMessage = request.currentMessage,
                suggestions = emptyList(),
                isSuccess = false,
                errorMessage = "Provider '${activeBot.providerId}' is not configured for bot '${activeBot.name}'."
            )
        }

        val timeoutMillis = when (latencyMode) {
            AiLatencyMode.FAST -> 5_000L
            AiLatencyMode.BALANCED -> 10_000L
            AiLatencyMode.STABLE -> 15_000L
            AiLatencyMode.CUSTOM -> (customTimeoutSeconds.coerceIn(2, 60) * 1000L)
        }

        return withContext(Dispatchers.Default) {
            try {
                withTimeout(timeoutMillis) {
                    provider.generateReplies(request, activeBot)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                AiReplyResult(
                    generationId = request.generationId,
                    currentMessage = request.currentMessage,
                    suggestions = emptyList(),
                    isSuccess = false,
                    errorMessage = "Request timed out after ${timeoutMillis / 1000}s on ${activeBot.name}."
                )
            } catch (e: Exception) {
                AiReplyResult(
                    generationId = request.generationId,
                    currentMessage = request.currentMessage,
                    suggestions = emptyList(),
                    isSuccess = false,
                    errorMessage = "Error from ${activeBot.name}: ${e.message}"
                )
            }
        }
    }
}
