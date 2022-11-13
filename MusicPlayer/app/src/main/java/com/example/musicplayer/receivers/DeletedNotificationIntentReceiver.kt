package com.example.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.musicplayer.MyMediaPlayer

class DeletedNotificationIntentReceiver : BroadcastReceiver() {

    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onReceive(context: Context, intent: Intent?) {
        mediaPlayer.pause()

        val intentForActivity = Intent("BROADCAST")
        intentForActivity.putExtra("STOP", true)
        context.sendBroadcast(intentForActivity)
    }
}