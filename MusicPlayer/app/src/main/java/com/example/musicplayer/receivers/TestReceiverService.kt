package com.example.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.util.Log

class TestReceiverService : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("INTENT RECEIVED TO THE RECEIVER", "")
        val extras = intent.extras
        val intentForActivity = Intent("BROADCAST")
        intentForActivity.putExtra("DATA_TO_PASS", extras?.getString("DATA"))

        context.sendBroadcast(intentForActivity)
    }
}