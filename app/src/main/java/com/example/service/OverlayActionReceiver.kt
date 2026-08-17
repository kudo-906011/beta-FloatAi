package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.OverlayStateManager

/**
 * BroadcastReceiver handling system notification actions and external control broadcasts.
 */
class OverlayActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            FloatingOverlayService.ACTION_TOGGLE_PASS_THROUGH -> {
                OverlayStateManager.togglePassThrough()
            }
            FloatingOverlayService.ACTION_TOGGLE_OVERLAY -> {
                val current = OverlayStateManager.state.value.isFloatingBarVisible
                OverlayStateManager.setFloatingBarVisible(!current)
            }
            FloatingOverlayService.ACTION_STOP -> {
                FloatingOverlayService.stopService(context)
            }
        }
    }
}
