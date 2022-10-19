package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import android.widget.Toast

class PausePlayNotificationReceiver: BroadcastReceiver(), AudioManager.OnAudioFocusChangeListener {
    private var mediaPlayer = MyMediaPlayer.getInstance
    private lateinit var service : MusicNotificationService

    override fun onReceive(context: Context, intent: Intent?) {

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        service = MusicNotificationService(context)

        if (audioManager.isMusicActive){
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()

            when (audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    if(mediaPlayer.isPlaying){
                        mediaPlayer.pause()
                        service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
                    } else {
                        mediaPlayer.start()
                        service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
                    }
                }
            }
        } else {
            if(mediaPlayer.isPlaying){
                mediaPlayer.pause()
                service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                mediaPlayer.start()
                service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            }
        }
    }

    override fun onAudioFocusChange(audioFocusChange: Int) {
        Log.d("testNOTIF", "test")
        Log.d("audioFocusPAUSE", audioFocusChange.toString())
        if(audioFocusChange == -1){
            if(mediaPlayer.isPlaying){
                service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            } else{
                service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
            }
            return
        }
        when (audioFocusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                println("test")
                mediaPlayer.start()
                service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                println("loss")
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
                }
            }
            else -> {
                service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            }
        }
    }
}