package com.example.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.example.musicplayer.MyMediaPlayer
import com.example.musicplayer.PlaybackService

class PausePlayNotificationReceiver : BroadcastReceiver() {
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent?) {
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                if(mediaPlayer.isPlaying){
                    mediaPlayer.pause()
                    val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                    intentForNotification.putExtra("STOP", true)
                    context.sendBroadcast(intentForNotification)

                    val intentForActivity = Intent("BROADCAST")
                    intentForActivity.putExtra("STOP", true)
                    context.sendBroadcast(intentForActivity)
                } else {
                    mediaPlayer.start()
                    val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                    intentForNotification.putExtra("STOP", false)
                    context.sendBroadcast(intentForNotification)

                    val intentForActivity = Intent("BROADCAST")
                    intentForActivity.putExtra("STOP", false)
                    context.sendBroadcast(intentForActivity)
                }
            }
        }
    }
}