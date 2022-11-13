package com.example.musicplayer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.IBinder
import android.util.Log

class PlaybackService : Service() {
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onBind(intent: Intent): IBinder? {
        Log.d("ON BIND","")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("START COMMAND","")
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d("SERVICE", "GAIN FOCUS WHEN OTHER APP WAS PLAYING")
                    val intentForBroadcast = Intent("BROADCAST")
                    intentForBroadcast.putExtra("STOP",false)
                    applicationContext.sendBroadcast(intentForBroadcast)

                    val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                    intentForNotification.putExtra("STOP", false)
                    applicationContext.sendBroadcast(intentForNotification)
                }
                else -> {
                    if (mediaPlayer.isPlaying) {
                        Log.d("SERVICE", "LOSS FOCUS WHILE PLAYING")
                        val intentForBroadcast = Intent("BROADCAST")
                        intentForBroadcast.putExtra("STOP",true)
                        applicationContext.sendBroadcast(intentForBroadcast)

                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                        intentForNotification.putExtra("STOP", true)
                        applicationContext.sendBroadcast(intentForNotification)
                        mediaPlayer.pause()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ON CREATE", "")
    }

    companion object {
        lateinit var onAudioFocusChange : AudioManager.OnAudioFocusChangeListener
        lateinit var audioAttributes : AudioAttributes
        lateinit var audioManager : AudioManager
    }
}