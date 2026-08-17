package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ai.AiBotManager
import com.example.model.AiLatencyMode
import com.example.model.AppSettings
import com.example.model.BotConfig
import com.example.model.DockPosition
import com.example.model.PurgeDuration
import com.example.model.ReplyTone
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences storage manager for robust persistence of:
 * - App settings
 * - Selected active bot ID
 * - Configured custom and built-in bots
 * - Purge timer preferences
 * - Latency & response behavior configurations
 */
object PreferencesManager {

    private const val PREF_NAME = "reply_float_prefs"

    private const val KEY_OVERLAY_PERM = "key_overlay_perm"
    private const val KEY_ACCESSIBILITY_PERM = "key_accessibility_perm"
    private const val KEY_NOTIFICATION_PERM = "key_notification_perm"
    private const val KEY_SCREEN_ANALYSIS_ON = "key_screen_analysis_on"
    private const val KEY_RESPONSE_MODE = "key_response_mode"
    private const val KEY_PASS_THROUGH_DEF = "key_pass_through_def"
    private const val KEY_AUTO_ANALYZE_CHAT = "key_auto_analyze_chat"
    private const val KEY_DEFAULT_TONE = "key_default_tone"
    private const val KEY_MAX_SUGGESTIONS = "key_max_suggestions"
    private const val KEY_DOCK_POSITION = "key_dock_position"
    private const val KEY_AUTO_MINIMIZE = "key_auto_minimize"

    private const val KEY_RECENT_RETENTION = "key_recent_retention"
    private const val KEY_CUSTOM_RECENT_RETENTION_SEC = "key_custom_recent_retention_sec"
    private const val KEY_PURGE_DURATION = "key_purge_duration"
    private const val KEY_CUSTOM_PURGE_MINUTES = "key_custom_purge_minutes"
    private const val KEY_LATENCY_MODE = "key_latency_mode"
    private const val KEY_CUSTOM_DEBOUNCE_MS = "key_custom_debounce_ms"
    private const val KEY_CUSTOM_TIMEOUT_SEC = "key_custom_timeout_sec"
    private const val KEY_ACTIVE_BOT_ID = "key_active_bot_id"
    private const val KEY_CONFIGURED_BOTS_JSON = "key_configured_bots_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SCREEN_ANALYSIS_ON, settings.isScreenAnalysisOn)
            putString(KEY_RESPONSE_MODE, settings.responseMode.name)
            putBoolean(KEY_PASS_THROUGH_DEF, settings.passThroughDefault)
            putBoolean(KEY_AUTO_ANALYZE_CHAT, settings.autoAnalyzeOnChat)
            putString(KEY_DEFAULT_TONE, settings.defaultTone.name)
            putInt(KEY_MAX_SUGGESTIONS, settings.maxSuggestionsCount)
            putString(KEY_DOCK_POSITION, settings.dockPosition.name)
            putBoolean(KEY_AUTO_MINIMIZE, settings.autoMinimizeOnCopy)
            putString(KEY_RECENT_RETENTION, settings.recentRetentionDuration.name)
            putInt(KEY_CUSTOM_RECENT_RETENTION_SEC, settings.customRecentRetentionSeconds)
            putString(KEY_PURGE_DURATION, settings.purgeDuration.name)
            putInt(KEY_CUSTOM_PURGE_MINUTES, settings.customPurgeMinutes)
            putString(KEY_LATENCY_MODE, settings.latencyMode.name)
            putLong(KEY_CUSTOM_DEBOUNCE_MS, settings.customDebounceMs)
            putInt(KEY_CUSTOM_TIMEOUT_SEC, settings.customTimeoutSeconds)
            putString(KEY_ACTIVE_BOT_ID, settings.activeBotId)
            apply()
        }
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = getPrefs(context)
        val modeName = prefs.getString(KEY_RESPONSE_MODE, com.example.model.ResponseMode.PASSIVE.name) ?: com.example.model.ResponseMode.PASSIVE.name
        val toneName = prefs.getString(KEY_DEFAULT_TONE, ReplyTone.BALANCED.name) ?: ReplyTone.BALANCED.name
        val dockName = prefs.getString(KEY_DOCK_POSITION, DockPosition.BOTTOM_RIGHT.name) ?: DockPosition.BOTTOM_RIGHT.name
        val recentRetentionName = prefs.getString(KEY_RECENT_RETENTION, com.example.model.RecentRetentionDuration.TWO_MINUTES.name) ?: com.example.model.RecentRetentionDuration.TWO_MINUTES.name
        val purgeName = prefs.getString(KEY_PURGE_DURATION, PurgeDuration.FIVE_MINUTES.name) ?: PurgeDuration.FIVE_MINUTES.name
        val latencyName = prefs.getString(KEY_LATENCY_MODE, AiLatencyMode.BALANCED.name) ?: AiLatencyMode.BALANCED.name

        val responseMode = try { com.example.model.ResponseMode.valueOf(modeName) } catch (e: Exception) { com.example.model.ResponseMode.PASSIVE }
        val defaultTone = try { ReplyTone.valueOf(toneName) } catch (e: Exception) { ReplyTone.BALANCED }
        val dockPosition = try { DockPosition.valueOf(dockName) } catch (e: Exception) { DockPosition.BOTTOM_RIGHT }
        val recentRetention = try { com.example.model.RecentRetentionDuration.valueOf(recentRetentionName) } catch (e: Exception) { com.example.model.RecentRetentionDuration.TWO_MINUTES }
        val purgeDuration = try { PurgeDuration.valueOf(purgeName) } catch (e: Exception) { PurgeDuration.FIVE_MINUTES }
        val latencyMode = try { AiLatencyMode.valueOf(latencyName) } catch (e: Exception) { AiLatencyMode.BALANCED }

        return AppSettings(
            isScreenAnalysisOn = prefs.getBoolean(KEY_SCREEN_ANALYSIS_ON, true),
            responseMode = responseMode,
            passThroughDefault = prefs.getBoolean(KEY_PASS_THROUGH_DEF, false),
            autoAnalyzeOnChat = prefs.getBoolean(KEY_AUTO_ANALYZE_CHAT, true),
            defaultTone = defaultTone,
            maxSuggestionsCount = prefs.getInt(KEY_MAX_SUGGESTIONS, 3),
            dockPosition = dockPosition,
            autoMinimizeOnCopy = prefs.getBoolean(KEY_AUTO_MINIMIZE, true),
            recentRetentionDuration = recentRetention,
            customRecentRetentionSeconds = prefs.getInt(KEY_CUSTOM_RECENT_RETENTION_SEC, 120),
            purgeDuration = purgeDuration,
            customPurgeMinutes = prefs.getInt(KEY_CUSTOM_PURGE_MINUTES, 5),
            latencyMode = latencyMode,
            customDebounceMs = prefs.getLong(KEY_CUSTOM_DEBOUNCE_MS, 500L),
            customTimeoutSeconds = prefs.getInt(KEY_CUSTOM_TIMEOUT_SEC, 10),
            activeBotId = prefs.getString(KEY_ACTIVE_BOT_ID, "bot_gemini_flash") ?: "bot_gemini_flash"
        )
    }

    fun saveBots(context: Context, bots: List<BotConfig>) {
        val jsonArray = JSONArray()
        for (bot in bots) {
            val obj = JSONObject().apply {
                put("id", bot.id)
                put("name", bot.name)
                put("providerId", bot.providerId)
                put("modelName", bot.modelName)
                put("systemPrompt", bot.systemPrompt)
                put("timeoutSeconds", bot.timeoutSeconds)
                put("isEnabled", bot.isEnabled)
                put("isConfigured", bot.isConfigured)
                put("isCustom", bot.isCustom)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_CONFIGURED_BOTS_JSON, jsonArray.toString()).apply()
    }

    fun loadBots(context: Context): List<BotConfig> {
        val jsonString = getPrefs(context).getString(KEY_CONFIGURED_BOTS_JSON, null)
        if (jsonString.isNullOrBlank()) {
            return AiBotManager.DEFAULT_BOTS
        }
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<BotConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BotConfig(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        providerId = obj.getString("providerId"),
                        modelName = obj.optString("modelName", ""),
                        systemPrompt = obj.optString("systemPrompt", ""),
                        timeoutSeconds = obj.optInt("timeoutSeconds", 10),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isConfigured = obj.optBoolean("isConfigured", true),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
            if (list.isNotEmpty()) list else AiBotManager.DEFAULT_BOTS
        } catch (e: Exception) {
            AiBotManager.DEFAULT_BOTS
        }
    }
}
