package org.hogel.yobidashi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ListenerService : Service() {
    companion object {
        const val NTFY_MESSAGE_RECEIVED = "io.heckel.ntfy.MESSAGE_RECEIVED"
        private const val CHANNEL_ID = "listener"
        private const val NOTIFICATION_ID = 1
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "oga", "opus", "m4a", "aac", "wav", "flac")

        private val mutableRunning = MutableStateFlow(false)
        val running: StateFlow<Boolean> = mutableRunning
    }

    private var player: MediaPlayer? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val topic = intent.getStringExtra("topic") ?: "?"
            val name = intent.getStringExtra("attachment_name") ?: ""
            val type = intent.getStringExtra("attachment_type") ?: ""
            val url = intent.getStringExtra("attachment_url")
            if (url.isNullOrEmpty()) {
                EventLog.add("$topic: no attachment, ignored")
                return
            }
            if (!type.startsWith("audio/") && name.substringAfterLast('.').lowercase() !in AUDIO_EXTENSIONS) {
                EventLog.add("$topic: $name is not audio, ignored")
                return
            }
            EventLog.add("$topic: playing $name")
            play(url)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        registerReceiver(receiver, IntentFilter(NTFY_MESSAGE_RECEIVED), RECEIVER_EXPORTED)
        mutableRunning.value = true
        EventLog.add("listener started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(receiver)
        stopPlayer()
        mutableRunning.value = false
        EventLog.add("listener stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun play(url: String) {
        stopPlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setWakeMode(this@ListenerService, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(url)
            setOnPreparedListener { it.start() }
            setOnCompletionListener { stopPlayer() }
            setOnErrorListener { _, what, extra ->
                EventLog.add("playback error ($what, $extra)")
                stopPlayer()
                true
            }
            prepareAsync()
        }
    }

    private fun stopPlayer() {
        player?.release()
        player = null
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(getString(R.string.notification_listening))
            .setOngoing(true)
            .build()
    }
}
