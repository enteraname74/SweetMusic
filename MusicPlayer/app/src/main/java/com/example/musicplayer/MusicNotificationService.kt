package com.example.musicplayer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat


class MusicNotificationService(private val context : Context) {
    private val  notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val pausePlayIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, PausePlayNotificationReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MUSIC_NOTIFICATION_CHANNEL_ID)
            .setLargeIcon(bitmap)
            .setSmallIcon(R.drawable.icone_musique)
            .setContentTitle(currentSong.name)
            .setContentText(currentSong.artist)
            .setContentIntent(activityPendingIntent)
            .addAction(pausePlayIcon,"pausePlay",pausePlayIntent)
            .build()

        notificationManager.notify(
            1,
            notification
        )
    }

    companion object {
        const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_notification_channel"
    }
}