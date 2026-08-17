package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.DefaultAiReplyService
import com.example.data.OverlayStateManager
import com.example.model.AiReplyRequest
import com.example.model.ConversationMessage
import com.example.model.ConversationRole
import com.example.model.DetectedMessage
import com.example.model.PassThroughState
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private val aiService = DefaultAiReplyService()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ReplyFloat AI", appName)
    }

    // TEST A: Topic change
    @Test
    fun `TEST A - topic change test from project to youtuber`() = runTest {
        // 1: Project inquiry
        val projectRequest = AiReplyRequest(
            generationId = UUID.randomUUID().toString(),
            currentMessage = "What happened to the project?",
            replyTone = ReplyTone.BALANCED,
            requestedReplyCount = 3
        )
        val projectResult = aiService.generateReplies(projectRequest)
        assertTrue(projectResult.isSuccess)
        assertTrue(projectResult.suggestions.isNotEmpty())
        val projectTexts = projectResult.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(projectTexts.contains("project") || projectTexts.contains("review") || projectTexts.contains("progress"))

        // 2: Switch to YouTuber
        val youtuberRequest = AiReplyRequest(
            generationId = UUID.randomUUID().toString(),
            currentMessage = "How to become a YouTuber?",
            recentConversation = listOf(
                ConversationMessage(role = ConversationRole.USER, text = "What happened to the project?"),
                ConversationMessage(role = ConversationRole.ASSISTANT, text = projectResult.suggestions.first().text)
            ),
            replyTone = ReplyTone.BALANCED,
            requestedReplyCount = 3
        )
        val youtuberResult = aiService.generateReplies(youtuberRequest)
        assertTrue(youtuberResult.isSuccess)
        val youtuberTexts = youtuberResult.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(youtuberTexts.contains("youtube") || youtuberTexts.contains("video") || youtuberTexts.contains("niche") || youtuberTexts.contains("channel"))
        assertFalse(youtuberTexts.contains("roadmap proposal") || youtuberTexts.contains("4 pm sync"))
    }

    // TEST B: Rapid topic change & generation ID protection
    @Test
    fun `TEST B - rapid topic change ensures generation ID validity`() = runTest {
        val req1 = AiReplyRequest(generationId = "gen_1", currentMessage = "What happened to the project?")
        val req2 = AiReplyRequest(generationId = "gen_2", currentMessage = "How to become a YouTuber?")
        val req3 = AiReplyRequest(generationId = "gen_3", currentMessage = "What is the capital of Japan?")

        val res1 = aiService.generateReplies(req1)
        val res2 = aiService.generateReplies(req2)
        val res3 = aiService.generateReplies(req3)

        assertEquals("gen_1", res1.generationId)
        assertEquals("gen_2", res2.generationId)
        assertEquals("gen_3", res3.generationId)

        val res3Text = res3.suggestions.joinToString(" ") { it.text }
        assertTrue(res3Text.contains("Tokyo"))
    }

    // TEST C: Context reference
    @Test
    fun `TEST C - contextual pronoun reference resolution for why is it delayed`() = runTest {
        val delayRequest = AiReplyRequest(
            currentMessage = "Why is it delayed?",
            recentConversation = listOf(
                ConversationMessage(role = ConversationRole.USER, text = "What happened to the project?"),
                ConversationMessage(role = ConversationRole.ASSISTANT, text = "The project is currently under review.")
            ),
            replyTone = ReplyTone.BALANCED,
            requestedReplyCount = 3
        )
        val delayResult = aiService.generateReplies(delayRequest)
        assertTrue(delayResult.isSuccess)
        val delayTexts = delayResult.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(delayTexts.contains("timeline") || delayTexts.contains("testing") || delayTexts.contains("delay") || delayTexts.contains("pushed back"))
    }

    // TEST D: New topic after conversation
    @Test
    fun `TEST D - start youtube channel after project conversation`() = runTest {
        val channelRequest = AiReplyRequest(
            currentMessage = "How do I start a YouTube channel?",
            recentConversation = listOf(
                ConversationMessage(role = ConversationRole.USER, text = "What happened to the project?"),
                ConversationMessage(role = ConversationRole.ASSISTANT, text = "The project is on track.")
            ),
            requestedReplyCount = 3
        )
        val result = aiService.generateReplies(channelRequest)
        assertTrue(result.isSuccess)
        val texts = result.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(texts.contains("youtube") || texts.contains("channel") || texts.contains("video") || texts.contains("content"))
        assertFalse(texts.contains("roadmap") || texts.contains("deliverable"))
    }

    // TEST E: Completely unrelated topic (Photosynthesis)
    @Test
    fun `TEST E - unrelated topic photosynthesis`() = runTest {
        val bioRequest = AiReplyRequest(
            currentMessage = "What is photosynthesis?",
            requestedReplyCount = 3
        )
        val result = aiService.generateReplies(bioRequest)
        assertTrue(result.isSuccess)
        val texts = result.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(texts.contains("plant") || texts.contains("sunlight") || texts.contains("glucose") || texts.contains("oxygen") || texts.contains("chlorophyll"))
    }

    // TEST F: Short question "Why?"
    @Test
    fun `TEST F - short question why with and without context`() = runTest {
        // Without context
        val noContextReq = AiReplyRequest(
            currentMessage = "Why?",
            recentConversation = emptyList(),
            requestedReplyCount = 3
        )
        val noContextRes = aiService.generateReplies(noContextReq)
        assertTrue(noContextRes.isSuccess)
        val noContextText = noContextRes.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(noContextText.contains("context") || noContextText.contains("detail") || noContextText.contains("specify"))

        // With project context
        val withContextReq = AiReplyRequest(
            currentMessage = "Why?",
            recentConversation = listOf(
                ConversationMessage(role = ConversationRole.USER, text = "What happened to the project?")
            ),
            requestedReplyCount = 3
        )
        val withContextRes = aiService.generateReplies(withContextReq)
        assertTrue(withContextRes.isSuccess)
        val withContextText = withContextRes.suggestions.joinToString(" ") { it.text }.lowercase()
        assertTrue(withContextText.contains("quality") || withContextText.contains("testing") || withContextText.contains("checks"))
    }

    // TEST G: Repeated message deduplication
    @Test
    fun `TEST G - repeated identical messages do not spam state`() {
        val msg = DetectedMessage(
            eventId = "evt_repeat",
            text = "Are you available for a quick sync?",
            sourceApp = "Chat"
        )
        OverlayStateManager.onNewMessageDetected(msg)
        val state1 = OverlayStateManager.state.value
        assertEquals("Are you available for a quick sync?", state1.detectedMessage)

        // Sending same message again should be safely deduplicated
        OverlayStateManager.onNewMessageDetected(msg)
        val state2 = OverlayStateManager.state.value
        assertEquals(state1.detectedMessage, state2.detectedMessage)
    }

    // Reply count test
    @Test
    fun `reply count test matches generated list count`() = runTest {
        val req1 = AiReplyRequest(currentMessage = "What is 2 + 2?", requestedReplyCount = 1)
        val res1 = aiService.generateReplies(req1)
        assertEquals(1, res1.suggestions.size)

        val req3 = AiReplyRequest(currentMessage = "What is 2 + 2?", requestedReplyCount = 3)
        val res3 = aiService.generateReplies(req3)
        assertEquals(3, res3.suggestions.size)

        val req5 = AiReplyRequest(currentMessage = "What is 2 + 2?", requestedReplyCount = 5)
        val res5 = aiService.generateReplies(req5)
        assertEquals(5, res5.suggestions.size)
    }

    // View All / Show Less test
    @Test
    fun `view all and show less state toggles correctly`() {
        val initialExpanded = OverlayStateManager.state.value.isViewAllExpanded
        OverlayStateManager.toggleViewAllExpanded()
        assertEquals(!initialExpanded, OverlayStateManager.state.value.isViewAllExpanded)
        OverlayStateManager.toggleViewAllExpanded()
        assertEquals(initialExpanded, OverlayStateManager.state.value.isViewAllExpanded)
    }

    // Copy test
    @Test
    fun `copy reply produces clean text without JSON or metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reply = ReplySuggestion(
            id = "test_reply_1",
            text = "Sounds great, let's sync at 4 PM!",
            tone = ReplyTone.CASUAL
        )
        OverlayStateManager.copyReply(context, reply)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals("Sounds great, let's sync at 4 PM!", clipData)
        assertFalse(clipData?.contains("{") == true)
        assertFalse(clipData?.contains("system") == true)
    }

    // Pass-through recovery test
    @Test
    fun `pass-through toggle is fully reversible and recoverable`() {
        OverlayStateManager.setPassThrough(PassThroughState.DISABLED)
        assertEquals(PassThroughState.DISABLED, OverlayStateManager.state.value.passThroughState)

        OverlayStateManager.togglePassThrough()
        assertEquals(PassThroughState.ENABLED, OverlayStateManager.state.value.passThroughState)

        OverlayStateManager.togglePassThrough()
        assertEquals(PassThroughState.DISABLED, OverlayStateManager.state.value.passThroughState)
    }

    // Error handling test
    @Test
    fun `empty message returns clear error without crashing`() = runTest {
        val emptyResult = aiService.generateReplies(
            AiReplyRequest(
                currentMessage = "   ",
                requestedReplyCount = 3
            )
        )
        assertFalse(emptyResult.isSuccess)
        assertTrue(emptyResult.suggestions.isEmpty())
        assertNotNull(emptyResult.errorMessage)
    }

    // MULTI-BOT TESTS
    @Test
    fun `test multi-bot registration and active bot selection`() {
        val bots = com.example.ai.AiBotManager.configuredBots.value
        assertTrue("At least 3 bots should be configured by default", bots.size >= 3)
        assertTrue(bots.any { it.id == "bot_gemini_flash" })
        assertTrue(bots.any { it.id == "bot_gemini_pro" })
        assertTrue(bots.any { it.id == "bot_local_engine" })

        // Switch active bot
        com.example.ai.AiBotManager.setActiveBot("bot_gemini_pro")
        assertEquals("bot_gemini_pro", com.example.ai.AiBotManager.getActiveBot().id)

        com.example.ai.AiBotManager.setActiveBot("bot_local_engine")
        assertEquals("bot_local_engine", com.example.ai.AiBotManager.getActiveBot().id)

        // Reset to flash
        com.example.ai.AiBotManager.setActiveBot("bot_gemini_flash")
        assertEquals("bot_gemini_flash", com.example.ai.AiBotManager.getActiveBot().id)
    }

    @Test
    fun `test custom bot addition and deletion`() {
        val initialCount = com.example.ai.AiBotManager.configuredBots.value.size
        val customBot = com.example.model.BotConfig(
            id = "custom_test_bot_1",
            name = "Test Bot",
            providerId = "gemini_flash",
            modelName = "gemini-2.5-flash",
            systemPrompt = "Specialized test bot",
            isCustom = true
        )
        com.example.ai.AiBotManager.addBot(customBot)
        assertEquals(initialCount + 1, com.example.ai.AiBotManager.configuredBots.value.size)

        // Delete custom bot
        com.example.ai.AiBotManager.deleteBot("custom_test_bot_1")
        assertEquals(initialCount, com.example.ai.AiBotManager.configuredBots.value.size)
    }

    @Test
    fun `test bot manager reply generation across different active bots`() = runTest {
        val req = AiReplyRequest(
            generationId = "test_gen_1",
            currentMessage = "Are you available for lunch tomorrow?",
            requestedReplyCount = 3
        )

        // Gemini Flash
        com.example.ai.AiBotManager.setActiveBot("bot_gemini_flash")
        val flashRes = com.example.ai.AiBotManager.generateReplies(req, com.example.model.AiLatencyMode.FAST)
        assertTrue("flashRes failed: ${flashRes.errorMessage}", flashRes.isSuccess)
        assertTrue("flash suggestions was empty", flashRes.suggestions.isNotEmpty())

        // Gemini Pro
        com.example.ai.AiBotManager.setActiveBot("bot_gemini_pro")
        val proRes = com.example.ai.AiBotManager.generateReplies(req, com.example.model.AiLatencyMode.BALANCED)
        assertTrue("proRes failed: ${proRes.errorMessage}", proRes.isSuccess)
        assertTrue("pro suggestions was empty", proRes.suggestions.isNotEmpty())

        // Local Engine
        com.example.ai.AiBotManager.setActiveBot("bot_local_engine")
        val localRes = com.example.ai.AiBotManager.generateReplies(req, com.example.model.AiLatencyMode.FAST)
        assertTrue("localRes failed: ${localRes.errorMessage}", localRes.isSuccess)
        assertTrue("local suggestions was empty", localRes.suggestions.isNotEmpty())
    }

    // AUTO-PURGE TESTS
    @Test
    fun `test purge temporary data clears suggestions without removing settings or bot configs`() {
        val initialBotCount = com.example.ai.AiBotManager.configuredBots.value.size
        val initialTone = OverlayStateManager.state.value.settings.defaultTone

        // Load a scenario with replies
        OverlayStateManager.loadScenario(com.example.ui.viewmodel.sampleScenarios.first())
        assertTrue(OverlayStateManager.state.value.replies.isNotEmpty())
        assertTrue(OverlayStateManager.state.value.detectedMessage.isNotBlank())

        // Purge temporary data
        OverlayStateManager.purgeTemporaryData()

        // Verify temporary data is purged
        assertTrue(OverlayStateManager.state.value.replies.isEmpty())
        assertTrue(OverlayStateManager.state.value.detectedMessage.isEmpty())

        // Verify permanent settings and bot configs remain completely intact
        assertEquals(initialTone, OverlayStateManager.state.value.settings.defaultTone)
        assertEquals(initialBotCount, com.example.ai.AiBotManager.configuredBots.value.size)
    }

    // LATENCY MODE TEST
    @Test
    fun `test latency modes are properly configured`() {
        assertEquals(200L, com.example.model.AiLatencyMode.FAST.debounceMs)
        assertEquals(5, com.example.model.AiLatencyMode.FAST.timeoutSeconds)

        assertEquals(500L, com.example.model.AiLatencyMode.BALANCED.debounceMs)
        assertEquals(10, com.example.model.AiLatencyMode.BALANCED.timeoutSeconds)

        assertEquals(1000L, com.example.model.AiLatencyMode.STABLE.debounceMs)
        assertEquals(15, com.example.model.AiLatencyMode.STABLE.timeoutSeconds)
    }
}
