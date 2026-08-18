package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.OverlayStateManager
import com.example.model.PassThroughState
import com.example.ui.components.FloatingReplyBar
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.ReplyFloatTheme
import com.example.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "replyfloat_overlay_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.example.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_PASS_THROUGH = "com.example.ACTION_TOGGLE_PASS_THROUGH"
        const val ACTION_TOGGLE_OVERLAY = "com.example.ACTION_TOGGLE_OVERLAY"

        var isRunning = false
            private set

        fun startService(context: Context) {
            if (!PermissionUtils.hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    // Pass-Through Recovery Window (Stays interactive so user can always disable pass-through directly)
    private var recoveryView: View? = null
    private var recoveryLayoutParams: WindowManager.LayoutParams? = null
    private var recoveryLifecycleOwner: OverlayLifecycleOwner? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateCollectJob: Job? = null

    // Screen boundary metrics
    private var screenWidth = 1080
    private var screenHeight = 2400

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        OverlayStateManager.setOverlayServiceRunning(true)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()
        createOverlayView()
        createRecoveryView()
        observeStateChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(NOTIFICATION_ID)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PASS_THROUGH -> {
                OverlayStateManager.togglePassThrough()
            }
            ACTION_TOGGLE_OVERLAY -> {
                val current = OverlayStateManager.state.value.isFloatingBarVisible
                OverlayStateManager.setFloatingBarVisible(!current)
            }
        }
        updateNotification()
        return START_STICKY
    }

    private fun updateScreenDimensions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager?.currentWindowMetrics
                val bounds = windowMetrics?.bounds
                if (bounds != null) {
                    screenWidth = bounds.width()
                    screenHeight = bounds.height()
                }
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.getMetrics(metrics)
                screenWidth = metrics.widthPixels
                screenHeight = metrics.heightPixels
            }
        } catch (ignored: Exception) {
        }
    }

    private fun createOverlayView() {
        if (overlayView != null || !PermissionUtils.hasOverlayPermission(this)) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val isPassThrough = OverlayStateManager.state.value.passThroughState == PassThroughState.ENABLED

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val initialFlags = if (isPassThrough) {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            baseFlags
        }

        val initialX = OverlayStateManager.overlayX.value.coerceIn(16, (screenWidth - 320).coerceAtLeast(100))
        val initialY = OverlayStateManager.overlayY.value.coerceIn(80, (screenHeight - 400).coerceAtLeast(200))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        layoutParams = params

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        owner.onResume()
        lifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)

            setContent {
                val state by OverlayStateManager.state.collectAsState()
                ReplyFloatTheme(
                    preset = state.settings.uiColorPreset,
                    customHex = state.settings.customUiColorHex,
                    opacity = state.settings.overlayOpacity
                ) {
                    if (state.isFloatingBarVisible) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = ReplyFloatDimens.floatingBarMaxWidth)
                                .padding(6.dp)
                        ) {
                            FloatingReplyBar(
                                status = state.assistantStatus,
                                detectedMessage = state.detectedMessage,
                                detectedSender = state.detectedSender,
                                detectedSourceApp = state.detectedSourceApp,
                                replies = state.replies,
                                visibleReplies = state.visibleReplies,
                                totalReplyCount = state.totalReplyCount,
                                isExpanded = state.isViewAllExpanded,
                                isMinimized = state.isFloatingBarMinimized,
                                passThroughState = state.passThroughState,
                                activeToneFilter = state.activeToneFilter,
                                lastCopiedId = state.lastCopiedReplyId,
                                isScreenAnalysisOn = state.settings.isScreenAnalysisOn,
                                responseMode = state.settings.responseMode,
                                recentResults = state.recentResults,
                                isLanguageBarActive = state.isLanguageBarActive,
                                languageData = state.languageData,
                                onReplyCopy = { reply ->
                                    OverlayStateManager.copyReply(this@FloatingOverlayService, reply)
                                },
                                onViewAllToggle = {
                                    OverlayStateManager.toggleViewAllExpanded()
                                },
                                onToneFilterSelect = { tone ->
                                    OverlayStateManager.selectToneFilter(tone)
                                },
                                onPassThroughToggle = {
                                    OverlayStateManager.togglePassThrough()
                                },
                                onScreenAnalysisToggle = {
                                    OverlayStateManager.toggleScreenAnalysis(this@FloatingOverlayService)
                                },
                                onResponseModeSelect = { mode ->
                                    OverlayStateManager.selectResponseMode(mode, this@FloatingOverlayService)
                                },
                                onDeleteRecentResult = { id ->
                                    OverlayStateManager.deleteRecentResult(id)
                                },
                                onLanguageBarToggle = {
                                    OverlayStateManager.toggleLanguageBar()
                                },
                                onCopyText = { text, label ->
                                    OverlayStateManager.copyText(this@FloatingOverlayService, text, label)
                                },
                                onMinimizeClick = {
                                    resetToWrapContent()
                                    OverlayStateManager.setFloatingBarMinimized(true)
                                },
                                onExpandClick = {
                                    resetToWrapContent()
                                    OverlayStateManager.setFloatingBarMinimized(false)
                                },
                                onCloseClick = {
                                    OverlayStateManager.setFloatingBarVisible(false)
                                },
                                onDrag = { dx, dy ->
                                    updatePosition(dx, dy)
                                },
                                onResize = { dw, dh ->
                                    updateSize(dw, dh)
                                }
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            overlayView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createRecoveryView() {
        if (recoveryView != null || !PermissionUtils.hasOverlayPermission(this)) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val initialX = OverlayStateManager.overlayX.value.coerceIn(16, (screenWidth - 260).coerceAtLeast(50))
        val initialY = (OverlayStateManager.overlayY.value - 60).coerceIn(40, (screenHeight - 100).coerceAtLeast(40))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        recoveryLayoutParams = params

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        owner.onResume()
        recoveryLifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)

            setContent {
                val state by OverlayStateManager.state.collectAsState()
                if (state.passThroughState == PassThroughState.ENABLED && state.isFloatingBarVisible) {
                    ReplyFloatTheme(
                        preset = state.settings.uiColorPreset,
                        customHex = state.settings.customUiColorHex,
                        opacity = state.settings.overlayOpacity
                    ) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            com.example.ui.components.FloatingPassThroughRecoveryPill(
                                onDisablePassThrough = {
                                    OverlayStateManager.setPassThrough(PassThroughState.DISABLED)
                                },
                                onDrag = { dx, dy ->
                                    updateRecoveryPosition(dx, dy)
                                }
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            recoveryView = composeView
            recoveryView?.visibility = if (OverlayStateManager.state.value.passThroughState == PassThroughState.ENABLED && OverlayStateManager.state.value.isFloatingBarVisible) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateRecoveryPosition(dx: Float, dy: Float) {
        val view = recoveryView ?: return
        val params = recoveryLayoutParams ?: return

        val newX = (params.x + dx.toInt()).coerceIn(0, (screenWidth - (view.width.takeIf { it > 0 } ?: 200)).coerceAtLeast(0))
        val newY = (params.y + dy.toInt()).coerceIn(30, (screenHeight - (view.height.takeIf { it > 0 } ?: 80)).coerceAtLeast(30))

        params.x = newX
        params.y = newY

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (ignored: Exception) {
        }
    }

    private fun resetToWrapContent() {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (ignored: Exception) {
        }
    }

    private fun updateSize(dw: Float, dh: Float) {
        val view = overlayView ?: return
        val params = layoutParams ?: return

        val currentW = if (params.width > 0) params.width else (view.width.takeIf { it > 0 } ?: 340)
        val currentH = if (params.height > 0) params.height else (view.height.takeIf { it > 0 } ?: 220)

        val minW = 260
        val maxW = (screenWidth - params.x).coerceAtLeast(minW)
        val minH = 150
        val maxH = (screenHeight - params.y).coerceAtLeast(minH)

        val newW = (currentW + dw.toInt()).coerceIn(minW, maxW)
        val newH = (currentH + dh.toInt()).coerceIn(minH, maxH)

        params.width = newW
        params.height = newH

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (ignored: Exception) {
        }
    }

    private fun updatePosition(dx: Float, dy: Float) {
        val view = overlayView ?: return
        val params = layoutParams ?: return

        val newX = (params.x + dx.toInt()).coerceIn(0, (screenWidth - (view.width.takeIf { it > 0 } ?: 300)).coerceAtLeast(0))
        val newY = (params.y + dy.toInt()).coerceIn(50, (screenHeight - (view.height.takeIf { it > 0 } ?: 150)).coerceAtLeast(100))

        params.x = newX
        params.y = newY
        OverlayStateManager.overlayX.value = newX
        OverlayStateManager.overlayY.value = newY

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (ignored: Exception) {
        }
    }

    private fun observeStateChanges() {
        stateCollectJob?.cancel()
        stateCollectJob = serviceScope.launch {
            OverlayStateManager.state.collectLatest { state ->
                val isPassThrough = state.passThroughState == PassThroughState.ENABLED
                applyPassThroughFlag(isPassThrough)
                updateOverlayVisibility(state.isFloatingBarVisible)
                updateRecoveryVisibility(isPassThrough && state.isFloatingBarVisible)
                updateNotification()
            }
        }
    }

    private fun updateRecoveryVisibility(isVisible: Boolean) {
        recoveryView?.visibility = if (isVisible) View.VISIBLE else View.GONE
        if (isVisible) {
            recoveryLayoutParams?.let { params ->
                val targetX = OverlayStateManager.overlayX.value.coerceIn(16, (screenWidth - 260).coerceAtLeast(50))
                val targetY = (OverlayStateManager.overlayY.value - 60).coerceIn(40, (screenHeight - 100).coerceAtLeast(40))
                params.x = targetX
                params.y = targetY
                try {
                    recoveryView?.let { windowManager?.updateViewLayout(it, params) }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun applyPassThroughFlag(isPassThrough: Boolean) {
        val view = overlayView ?: return
        val params = layoutParams ?: return

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val newFlags = if (isPassThrough) {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            baseFlags
        }

        if (params.flags != newFlags) {
            params.flags = newFlags
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun updateOverlayVisibility(isVisible: Boolean) {
        overlayView?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_overlay_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_overlay_service_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val currentState = OverlayStateManager.state.value
        val isPassThrough = currentState.passThroughState == PassThroughState.ENABLED

        // Intent to open Main Activity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Toggle Pass-Through (Crucial Safe Recovery Mechanism!)
        val passThroughIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_TOGGLE_PASS_THROUGH
        }
        val passThroughPendingIntent = PendingIntent.getService(
            this,
            1,
            passThroughIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Toggle Visibility
        val toggleVisibilityIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_TOGGLE_OVERLAY
        }
        val toggleVisibilityPendingIntent = PendingIntent.getService(
            this,
            2,
            toggleVisibilityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop Service
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val passThroughLabel = if (isPassThrough) "Disable Pass-Through" else "Enable Pass-Through"
        val visibilityLabel = if (currentState.isFloatingBarVisible) "Hide Bar" else "Show Bar"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReplyFloat AI Active")
            .setContentText(
                if (isPassThrough) {
                    "Pass-Through ON (Touches pass to apps below)"
                } else {
                    "${currentState.totalReplyCount} smart replies ready • Tap to open"
                }
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, passThroughLabel, passThroughPendingIntent)
            .addAction(0, visibilityLabel, toggleVisibilityPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onDestroy() {
        isRunning = false
        OverlayStateManager.setOverlayServiceRunning(false)
        stateCollectJob?.cancel()
        serviceScope.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)

        lifecycleOwner?.onPause()
        lifecycleOwner?.onStop()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null

        recoveryLifecycleOwner?.onPause()
        recoveryLifecycleOwner?.onStop()
        recoveryLifecycleOwner?.onDestroy()
        recoveryLifecycleOwner = null

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {
            }
            overlayView = null
        }

        recoveryView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {
            }
            recoveryView = null
        }

        super.onDestroy()
    }
}
