package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeletedNotificationIntentReceiver: BroadcastReceiver() {
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent?) {
        mediaPlayer.pause()
    }
}