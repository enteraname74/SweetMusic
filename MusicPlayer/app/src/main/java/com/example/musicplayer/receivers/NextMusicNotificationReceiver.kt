package com.example.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import com.example.musicplayer.MyMediaPlayer
import com.example.musicplayer.PlaybackService

class NextMusicNotificationReceiver : BroadcastReceiver() {
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent) {

        if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex +=1
        }

        val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]


        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        try {
            when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(currentSong.path)
                    mediaPlayer.prepare()
                    mediaPlayer.start()

                    val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                    intentForNotification.putExtra("STOP", false)
                    context.sendBroadcast(intentForNotification)

                    val intentForActivity = Intent("BROADCAST")
                    intentForActivity.putExtra("STOP", false)
                    context.sendBroadcast(intentForActivity)
                }
            }
        } catch (error : Error){
            Log.d("error","")
        }
    }
}