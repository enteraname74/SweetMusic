package com.example.musicplayer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat


class MusicNotificationService(private val context : Context) : AudioManager.OnAudioFocusChangeListener {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mediaPlayer = MyMediaPlayer.getInstance

    fun showNotification(pausePlayIcon : Int){
        val currentSong= MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        var bitmap : Bitmap? = null

        if (currentSong.albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentSong.albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources,R.drawable.michael)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            1,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val previousMusicIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, PreviousMusicNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pausePlayIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, PausePlayNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val nextMusicIntent = PendingIntent.getBroadcast(
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

        val notification = NotificationCompat.Builder(context, MUSIC_NOTIFICATION_CHANNEL_ID)
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
            .build()

        notificationManager.notify(
            1,
            notification
        )
    }

    companion object {
        const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_notification_channel"
    }

    override fun onAudioFocusChange(audioFocusChange: Int) {
        Log.d("testNOTIF1", "test")
        when (audioFocusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                println("test")
                mediaPlayer.start()
                showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    showNotification(R.drawable.ic_baseline_play_circle_outline_24)
                }
            }
        }
    }
}