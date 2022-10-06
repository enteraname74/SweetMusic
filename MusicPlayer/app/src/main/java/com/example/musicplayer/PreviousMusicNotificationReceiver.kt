package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PreviousMusicNotificationReceiver : BroadcastReceiver() {
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent?) {
        val service = MusicNotificationService(context)

        if(MyMediaPlayer.currentIndex ==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex -=1
        }

        val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        mediaPlayer.reset()
        mediaPlayer.setDataSource(currentSong.path)
        mediaPlayer.prepare()
        mediaPlayer.start()
        service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
    }
}