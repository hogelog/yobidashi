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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ListenerService : Service() {
    companion object {
        const val NTFY_MESSAGE_RECEIVED = "io.heckel.ntfy.MESSAGE_RECEIVED"
        const val ACTION_PLAY = "org.hogel.yobidashi.PLAY"
        const val EXTRA_URL = "url"
        private const val CHANNEL_ID = "listener"
        private const val NOTIFICATION_ID = 1
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "oga", "opus", "m4a", "aac", "wav", "flac")

        private val mutableRunning = MutableStateFlow(false)
        val running: StateFlow<Boolean> = mutableRunning
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var leadIn: MediaPlayer? = null
    private var playRequest = 0

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
            val settings = Settings.load(context)
            if (!AudioOutputs.autoPlayAllowed(context, settings.allowedOutputs)) {
                EventLog.add("$topic: $name (no allowed output, tap to play)", url)
                return
            }
            EventLog.add("$topic: playing $name", url)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) {
            intent.getStringExtra(EXTRA_URL)?.let { play(it) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        scope.cancel()
        stopPlayer()
        mutableRunning.value = false
        EventLog.add("listener stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun play(url: String) {
        val request = ++playRequest
        stopPlayer()
        scope.launch {
            val file = AudioCache.get(this@ListenerService, url)
                ?: withContext(Dispatchers.IO) {
                    AudioCache.download(
                        this@ListenerService,
                        url,
                        Settings.load(this@ListenerService).headersFor(url),
                    )
                }
                ?: return@launch
            if (request == playRequest) {
                startPlayer(file)
            }
        }
    }

    private fun startPlayer(file: File) {
        player = MediaPlayer().apply {
            setAudioAttributes(audioAttributes())
            setWakeMode(this@ListenerService, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(file.path)
            setOnPreparedListener { startWithLeadIn(it) }
            setOnCompletionListener { stopPlayer() }
            setOnErrorListener { _, what, extra ->
                EventLog.add("playback error ($what, $extra)")
                stopPlayer()
                true
            }
            prepareAsync()
        }
    }

    // Plays a short silent clip first so the audio route (Bluetooth in
    // particular) is already open when the real audio starts; otherwise its
    // first moments get dropped while the route spins up.
    private fun startWithLeadIn(real: MediaPlayer) {
        val silence = MediaPlayer.create(this, R.raw.silence, audioAttributes(), real.audioSessionId)
        if (silence == null) {
            real.start()
            return
        }
        leadIn = silence
        silence.setOnCompletionListener {
            it.release()
            leadIn = null
            if (player === real) {
                real.start()
            }
        }
        silence.start()
    }

    private fun audioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun stopPlayer() {
        leadIn?.release()
        leadIn = null
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
