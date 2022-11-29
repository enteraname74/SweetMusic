package com.example.musicplayer.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.musicplayer.MainActivity
import com.example.musicplayer.MusicPlayerActivity
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.R
import com.example.musicplayer.receivers.DeletedNotificationIntentReceiver
import com.example.musicplayer.receivers.NextMusicNotificationReceiver
import com.example.musicplayer.receivers.PausePlayNotificationReceiver
import com.example.musicplayer.receivers.PreviousMusicNotificationReceiver


class MusicNotificationService(private val context : Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationMusicPlayer : NotificationCompat.Builder
    private lateinit var pausePlayIntent : PendingIntent
    private lateinit var previousMusicIntent : PendingIntent
    private lateinit var nextMusicIntent : PendingIntent

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("RECEIVE IN NOTIFICATION",intent.extras?.getBoolean("STOP").toString())

            if (intent.extras?.getBoolean("STOP_RECEIVE") != null && intent.extras?.getBoolean("STOP_RECEIVE") as Boolean) {
                Log.d("STOP RECEIVE", "STOP RECEIVE")
                context.unregisterReceiver(this)
            } else if (intent.extras?.getBoolean("STOP") != null && intent.extras?.getBoolean("STOP") as Boolean) {
                //notificationMusicPlayer.build().actions[1] = Notification.Action.Builder(R.drawable.ic_baseline_play_circle_outline_24, "pausePlay", pausePlayIntent).build()
                updateNotification(R.drawable.ic_baseline_play_circle_outline_24)

                val intentForBroadcast = Intent("BROADCAST")
                intentForBroadcast.putExtra("STOP",true)
                context.sendBroadcast(intentForBroadcast)

            } else if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                //notificationMusicPlayer.build().actions[1] = Notification.Action.Builder(R.drawable.ic_baseline_pause_circle_outline_24, "pausePlay", pausePlayIntent).build()
                updateNotification(R.drawable.ic_baseline_pause_circle_outline_24)

                val intentForBroadcast = Intent("BROADCAST")
                intentForBroadcast.putExtra("STOP",false)
                context.sendBroadcast(intentForBroadcast)
            }
        }
    }

    fun showNotification(pausePlayIcon : Int){
        val currentSong= MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        var bitmap : Bitmap? = null

        context.registerReceiver(broadcastReceiver, IntentFilter("BROADCAST_NOTIFICATION"))

        if (currentSong.albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentSong.albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.michael)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            1,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        previousMusicIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, PreviousMusicNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        pausePlayIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, PausePlayNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        nextMusicIntent = PendingIntent.getBroadcast(
            context,
            4,
            Intent(context, NextMusicNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val deleteNotificationIntent = PendingIntent.getBroadcast(
            context,
            5,
            Intent(context, DeletedNotificationIntentReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        notificationMusicPlayer = NotificationCompat.Builder(context, MUSIC_NOTIFICATION_CHANNEL_ID)
            .setLargeIcon(bitmap)
            .setSmallIcon(R.drawable.icone_musique)
            .setContentTitle(currentSong.name)
            .setContentText(currentSong.artist)
            .setContentIntent(activityPendingIntent)
            .addAction(R.drawable.ic_baseline_skip_previous_24,"previous",previousMusicIntent)
            .addAction(pausePlayIcon,"pausePlay",pausePlayIntent)
            .addAction(R.drawable.ic_baseline_skip_next_24,"next",nextMusicIntent)
            .setDeleteIntent(deleteNotificationIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            )

        notificationManager.notify(
            1,
            notificationMusicPlayer.build()
        )
    }

    private fun updateNotification(pauseIcon : Int) {
        var bitmap : Bitmap? = null

        if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.michael)
        }
        notificationMusicPlayer
            .clearActions()
            .addAction(R.drawable.ic_baseline_skip_previous_24,"previous",previousMusicIntent)
            .addAction(pauseIcon,"pausePlay",pausePlayIntent)
            .addAction(R.drawable.ic_baseline_skip_next_24,"next",nextMusicIntent)
            .setContentTitle(MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name)
            .setContentText(MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].artist)
            .setLargeIcon(bitmap)

        notificationManager.notify(1, notificationMusicPlayer.build())
    }

    companion object {
        const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_notification_channel"
    }
}