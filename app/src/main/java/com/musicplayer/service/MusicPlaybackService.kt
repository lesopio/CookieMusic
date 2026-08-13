package com.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.MainActivity
import com.musicplayer.R
import kotlin.math.roundToInt

class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var preferences: SharedPreferences? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioEffectsSessionId: Int = 0
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in AUDIO_EFFECT_KEYS) {
            applyAudioEffects()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).also {
            it.registerOnSharedPreferenceChangeListener(preferenceListener)
        }
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                applyAudioEffects(audioSessionId)
            }
        })
        applyAudioEffects(player.audioSessionId)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        releaseAudioEffects()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    private fun applyAudioEffects(audioSessionId: Int = exoPlayer?.audioSessionId ?: 0) {
        if (audioSessionId == 0) return
        if (audioEffectsSessionId != 0 && audioEffectsSessionId != audioSessionId) {
            releaseAudioEffects()
        }
        audioEffectsSessionId = audioSessionId
        val prefs = preferences ?: return
        val hifiMode = prefs.getBoolean(KEY_HIFI_MODE, false)
        if (hifiMode) {
            releaseAudioEffects()
            exoPlayer?.setSkipSilenceEnabled(false)
            return
        }
        applyEqualizer(audioSessionId, prefs)
        applyBassBoost(audioSessionId, prefs)
        applyVirtualizer(audioSessionId, prefs)
        applyLoudness(audioSessionId, prefs)
    }

    private fun applyEqualizer(audioSessionId: Int, prefs: SharedPreferences) {
        val enabled = prefs.getBoolean(KEY_EQ_ENABLED, false)
        val levels = prefs.getString(KEY_EQ_BANDS, null)
            ?.split(',')
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 5 }
            ?: List(5) { 0f }
        val effect = equalizer ?: runCatching {
            Equalizer(0, audioSessionId).also { equalizer = it }
        }.getOrNull() ?: return
        runCatching {
            val bandCount = effect.numberOfBands.toInt().coerceAtLeast(1)
            val min = effect.bandLevelRange[0].toInt()
            val max = effect.bandLevelRange[1].toInt()
            levels.forEachIndexed { index, value ->
                val targetBand = if (levels.size == 1) 0 else (index * (bandCount - 1).toFloat() / (levels.size - 1)).roundToInt()
                effect.setBandLevel(targetBand.toShort(), (value * 100f).roundToInt().coerceIn(min, max).toShort())
            }
            effect.enabled = enabled
        }
    }

    private fun applyBassBoost(audioSessionId: Int, prefs: SharedPreferences) {
        val effect = bassBoost ?: runCatching {
            BassBoost(0, audioSessionId).also { bassBoost = it }
        }.getOrNull() ?: return
        runCatching {
            effect.setStrength((prefs.getFloat(KEY_BASS_STRENGTH, 0.45f).coerceIn(0f, 1f) * 1000f).roundToInt().toShort())
            effect.enabled = prefs.getBoolean(KEY_BASS_ENABLED, false)
        }
    }

    private fun applyVirtualizer(audioSessionId: Int, prefs: SharedPreferences) {
        val effect = virtualizer ?: runCatching {
            Virtualizer(0, audioSessionId).also { virtualizer = it }
        }.getOrNull() ?: return
        runCatching {
            effect.setStrength((prefs.getFloat(KEY_VIRTUALIZER_STRENGTH, 0.35f).coerceIn(0f, 1f) * 1000f).roundToInt().toShort())
            effect.enabled = prefs.getBoolean(KEY_VIRTUALIZER_ENABLED, false)
        }
    }

    private fun applyLoudness(audioSessionId: Int, prefs: SharedPreferences) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
        val effect = loudnessEnhancer ?: runCatching {
            LoudnessEnhancer(audioSessionId).also { loudnessEnhancer = it }
        }.getOrNull() ?: return
        runCatching {
            effect.setTargetGain((prefs.getFloat(KEY_LOUDNESS_GAIN, 0.35f).coerceIn(0f, 1f) * 1200f).roundToInt())
            effect.enabled = prefs.getBoolean(KEY_LOUDNESS_ENABLED, false)
        }
    }

    private fun releaseAudioEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        audioEffectsSessionId = 0
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_description) }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        private const val PREFERENCES_NAME = "music_library"
        private const val KEY_EQ_ENABLED = "eq_enabled"
        private const val KEY_EQ_BANDS = "eq_bands"
        private const val KEY_HIFI_MODE = "hifi_mode_enabled"
        private const val KEY_BASS_ENABLED = "bass_boost_enabled"
        private const val KEY_BASS_STRENGTH = "bass_boost_strength"
        private const val KEY_VIRTUALIZER_ENABLED = "virtualizer_enabled"
        private const val KEY_VIRTUALIZER_STRENGTH = "virtualizer_strength"
        private const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
        private const val KEY_LOUDNESS_GAIN = "loudness_gain"
        private val AUDIO_EFFECT_KEYS = setOf(
            KEY_EQ_ENABLED,
            KEY_EQ_BANDS,
            KEY_HIFI_MODE,
            KEY_BASS_ENABLED,
            KEY_BASS_STRENGTH,
            KEY_VIRTUALIZER_ENABLED,
            KEY_VIRTUALIZER_STRENGTH,
            KEY_LOUDNESS_ENABLED,
            KEY_LOUDNESS_GAIN
        )
    }
}
