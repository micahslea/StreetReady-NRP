package com.streetready.nrp.audio

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.streetready.nrp.MainActivity
import java.util.Locale

class NarrationService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var queuedChunks: List<String> = emptyList()
    private var chunkIndex = 0
    private var ready = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts?.language = Locale.US
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onError(utteranceId: String?) = advance()
            override fun onDone(utteranceId: String?) = advance()
        })
        if (queuedChunks.isNotEmpty()) speakCurrent()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_STOP -> {
                queuedChunks = emptyList(); chunkIndex = 0; tts?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY
            }
            ACTION_SPEAK -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "StreetReady lesson"
                queuedChunks = chunkText(text)
                chunkIndex = 0
                startForeground(NOTIFICATION_ID, notification(title))
                if (ready) speakCurrent()
            }
        }
        return START_STICKY
    }

    private fun chunkText(text: String, max: Int = 3000): List<String> {
        if (text.length <= max) return listOf(text)
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (current.length + sentence.length + 1 > max && current.isNotEmpty()) {
                out += current.toString(); current.clear()
            }
            if (sentence.length > max) {
                sentence.chunked(max).forEach { out += it }
            } else current.append(sentence).append(' ')
        }
        if (current.isNotEmpty()) out += current.toString()
        return out.filter { it.isNotBlank() }
    }

    private fun speakCurrent() {
        if (!ready || chunkIndex !in queuedChunks.indices) return
        tts?.speak(queuedChunks[chunkIndex], TextToSpeech.QUEUE_FLUSH, Bundle(), "streetready_$chunkIndex")
    }

    private fun advance() {
        chunkIndex++
        if (chunkIndex < queuedChunks.size) speakCurrent()
        else { queuedChunks = emptyList(); chunkIndex = 0; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    }

    private fun notification(title:String): Notification {
        val openPi = PendingIntent.getActivity(this,0,Intent(this, MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this,1,Intent(this, NarrationService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("Narrating lesson in the background")
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause,"Stop",stopPi)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID,"Lesson narration",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID="streetready_narration"
        const val NOTIFICATION_ID=991
        const val ACTION_SPEAK="streetready.SPEAK"
        const val ACTION_STOP="streetready.STOP"
        const val EXTRA_TEXT="text"
        const val EXTRA_TITLE="title"
    }
}
