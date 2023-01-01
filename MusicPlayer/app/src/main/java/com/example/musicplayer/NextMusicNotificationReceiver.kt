package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import kotlin.contracts.Returns

class NextMusicNotificationReceiver: BroadcastReceiver(), AudioManager.OnAudioFocusChangeListener {
    private var mediaPlayer = MyMediaPlayer.getInstance
    private lateinit var service : MusicNotificationService

    override fun onReceive(context: Context, intent: Intent?) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        service = MusicNotificationService(context)


        if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex +=1
        }

        val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

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

            try {
                when (audioManager.requestAudioFocus(audioFocusRequest)) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(currentSong.path)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                        service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
                    }
                }
            } catch (error : Error){
                Log.d("error","")
            }

        } else {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }

    override fun onAudioFocusChange(audioFocusChange: Int) {
        Log.d("audioFocusNEXT", audioFocusChange.toString())
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
                mediaPlayer.start()
                service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
                }
            }
            -1 -> {
                Log.d("testHere","")
            }
        }
    }
}