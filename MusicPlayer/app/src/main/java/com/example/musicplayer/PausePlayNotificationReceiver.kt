package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PausePlayNotificationReceiver: BroadcastReceiver() {
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent?) {
        val service = MusicNotificationService(context)
        if (mediaPlayer.isPlaying){
            mediaPlayer.pause()
            service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
        } else {
            mediaPlayer.start()
            service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
        }
    }
}