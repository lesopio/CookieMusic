package com.musicplayer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.musicplayer.data.OverlayLyricsSettings
import kotlin.math.roundToInt

class StatusLyricOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var lyricView: TextView? = null
    private var settings = OverlayLyricsSettings()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                settings = intent.readSettings()
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                val visible = intent.getBooleanExtra(EXTRA_VISIBLE, false)
                if (visible && text.isNotBlank()) show(text) else hide()
            }
            ACTION_CONFIG -> {
                settings = intent.readSettings()
                lyricView?.let { updateLayout(it) }
            }
            ACTION_HIDE -> hide()
            ACTION_STOP -> {
                hide()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hide()
        super.onDestroy()
    }

    private fun show(text: String) {
        if (!Settings.canDrawOverlays(this)) return
        val view = lyricView ?: TextView(this).also { textView ->
            textView.gravity = Gravity.CENTER
            textView.includeFontPadding = false
            textView.setSingleLine(true)
            textView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            textView.setPadding(dp(12f), 0, dp(12f), 0)
            lyricView = textView
            windowManager?.addView(textView, layoutParams())
        }
        view.text = text
        view.textSize = settings.fontSizeSp
        view.setTextColor(settings.textColorArgb.toInt())
        updateLayout(view)
    }

    private fun hide() {
        lyricView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        lyricView = null
    }

    private fun updateLayout(view: TextView) {
        view.textSize = settings.fontSizeSp
        runCatching {
            windowManager?.updateViewLayout(view, layoutParams())
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            dp(settings.widthDp),
            dp((settings.fontSizeSp * 2.2f).coerceIn(28f, 48f)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = dp(settings.offsetRightDp - settings.offsetLeftDp)
            y = statusBarHeight() + dp(settings.offsetTopDp - settings.offsetBottomDp)
        }
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24f)
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun Intent.readSettings() = OverlayLyricsSettings(
        enabled = getBooleanExtra(EXTRA_ENABLED, settings.enabled),
        hideInApp = getBooleanExtra(EXTRA_HIDE_IN_APP, settings.hideInApp),
        mediaMetadataLyricsEnabled = getBooleanExtra(EXTRA_MEDIA_METADATA, settings.mediaMetadataLyricsEnabled),
        textColorArgb = getLongExtra(EXTRA_TEXT_COLOR, settings.textColorArgb),
        offsetTopDp = getFloatExtra(EXTRA_TOP, settings.offsetTopDp),
        offsetBottomDp = getFloatExtra(EXTRA_BOTTOM, settings.offsetBottomDp),
        offsetLeftDp = getFloatExtra(EXTRA_LEFT, settings.offsetLeftDp),
        offsetRightDp = getFloatExtra(EXTRA_RIGHT, settings.offsetRightDp),
        widthDp = getFloatExtra(EXTRA_WIDTH, settings.widthDp),
        fontSizeSp = getFloatExtra(EXTRA_FONT, settings.fontSizeSp),
        pauseHideDelaySeconds = getFloatExtra(EXTRA_DELAY, settings.pauseHideDelaySeconds)
    )

    companion object {
        private const val ACTION_UPDATE = "com.musicplayer.action.STATUS_LYRIC_UPDATE"
        private const val ACTION_CONFIG = "com.musicplayer.action.STATUS_LYRIC_CONFIG"
        private const val ACTION_HIDE = "com.musicplayer.action.STATUS_LYRIC_HIDE"
        private const val ACTION_STOP = "com.musicplayer.action.STATUS_LYRIC_STOP"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_VISIBLE = "visible"
        private const val EXTRA_ENABLED = "enabled"
        private const val EXTRA_HIDE_IN_APP = "hide_in_app"
        private const val EXTRA_MEDIA_METADATA = "media_metadata"
        private const val EXTRA_TEXT_COLOR = "text_color"
        private const val EXTRA_TOP = "top"
        private const val EXTRA_BOTTOM = "bottom"
        private const val EXTRA_LEFT = "left"
        private const val EXTRA_RIGHT = "right"
        private const val EXTRA_WIDTH = "width"
        private const val EXTRA_FONT = "font"
        private const val EXTRA_DELAY = "delay"

        fun update(context: Context, text: String, visible: Boolean, settings: OverlayLyricsSettings) {
            runCatching {
                context.startService(baseIntent(context, ACTION_UPDATE, settings).apply {
                    putExtra(EXTRA_TEXT, text)
                    putExtra(EXTRA_VISIBLE, visible)
                })
            }
        }

        fun config(context: Context, settings: OverlayLyricsSettings) {
            runCatching { context.startService(baseIntent(context, ACTION_CONFIG, settings)) }
        }

        fun hide(context: Context) {
            runCatching { context.startService(Intent(context, StatusLyricOverlayService::class.java).setAction(ACTION_HIDE)) }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, StatusLyricOverlayService::class.java).setAction(ACTION_STOP)) }
        }

        private fun baseIntent(context: Context, action: String, settings: OverlayLyricsSettings) =
            Intent(context, StatusLyricOverlayService::class.java).setAction(action).apply {
                putExtra(EXTRA_ENABLED, settings.enabled)
                putExtra(EXTRA_HIDE_IN_APP, settings.hideInApp)
                putExtra(EXTRA_MEDIA_METADATA, settings.mediaMetadataLyricsEnabled)
                putExtra(EXTRA_TEXT_COLOR, settings.textColorArgb)
                putExtra(EXTRA_TOP, settings.offsetTopDp)
                putExtra(EXTRA_BOTTOM, settings.offsetBottomDp)
                putExtra(EXTRA_LEFT, settings.offsetLeftDp)
                putExtra(EXTRA_RIGHT, settings.offsetRightDp)
                putExtra(EXTRA_WIDTH, settings.widthDp)
                putExtra(EXTRA_FONT, settings.fontSizeSp)
                putExtra(EXTRA_DELAY, settings.pauseHideDelaySeconds)
            }
    }
}
