package com.example.musicplayer.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.musicplayer.MainActivity
import com.example.musicplayer.PlaybackService
import com.example.musicplayer.R
import com.example.musicplayer.classes.MyMediaPlayer
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
    private lateinit var deleteNotificationIntent : PendingIntent

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("RECEIVE IN NOTIFICATION",intent.extras?.getBoolean("STOP").toString())

            if (intent.extras?.getBoolean("STOP_RECEIVE") != null && intent.extras?.getBoolean("STOP_RECEIVE") as Boolean) {
                Log.d("STOP RECEIVE", "STOP RECEIVE")
                context.unregisterReceiver(this)
            } else if (intent.extras?.getBoolean("STOP") != null && intent.extras?.getBoolean("STOP") as Boolean) {
                //notificationMusicPlayer.build().actions[1] = Notification.Action.Builder(R.drawable.ic_baseline_play_circle_outline_24, "pausePlay", pausePlayIntent).build()
                updateNotification(true)

            } else if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                //notificationMusicPlayer.build().actions[1] = Notification.Action.Builder(R.drawable.ic_baseline_pause_circle_outline_24, "pausePlay", pausePlayIntent).build()
                updateNotification(false)
                Log.d("MUSIC NOTIFICATION", "POS : ${MyMediaPlayer.currentIndex}")
                val intentForBroadcast = Intent("BROADCAST")
                intentForBroadcast.putExtra("STOP", false)
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
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.notification_default)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activityIntent.action = Intent.ACTION_MAIN
        activityIntent.addCategory(Intent.CATEGORY_LAUNCHER)

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

        deleteNotificationIntent = PendingIntent.getBroadcast(
            context,
            5,
            Intent(context, DeletedNotificationIntentReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        notificationMusicPlayer = NotificationCompat.Builder(context, MUSIC_NOTIFICATION_CHANNEL_ID)
            .setLargeIcon(bitmap)
            .setSmallIcon(R.drawable.ic_saxophone_svg)
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

    private fun updateNotification(isMusicPaused : Boolean) {
        var bitmap : Bitmap? = null
        val pauseIcon : Int
        val musicState : Int
        if (isMusicPaused) {
            pauseIcon = R.drawable.ic_baseline_play_arrow_24
            musicState = PlaybackStateCompat.STATE_PAUSED
        } else {
            pauseIcon = R.drawable.ic_baseline_pause_24
            musicState = PlaybackStateCompat.STATE_PLAYING
        }

        if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.notification_default)
        }

        if (MyMediaPlayer.getInstance.isPlaying) {
            PlaybackService.mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        MyMediaPlayer.getInstance.duration.toLong()
                    )
                    .putString(
                        MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                        MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
                    )
                    // Pour les vieilles versions d'android
                    .putString(
                        MediaMetadata.METADATA_KEY_TITLE,
                        MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
                    )
                    .putString(
                        MediaMetadata.METADATA_KEY_ARTIST,
                        MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].artist
                    )
                    // A small bitmap for the artwork is also recommended
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                    .build()
            )
        }

        val state = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_SEEK_TO
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(musicState, MyMediaPlayer.getInstance.currentPosition.toLong(), 1.0F)
            .build()

        PlaybackService.mediaSession.setPlaybackState(state)

        notificationMusicPlayer
            .clearActions()
            .addAction(R.drawable.ic_baseline_skip_previous_24,"previous",previousMusicIntent)
            .addAction(pauseIcon,"pausePlay",pausePlayIntent)
            .addAction(R.drawable.ic_baseline_skip_next_24,"next",nextMusicIntent)
            .setDeleteIntent(deleteNotificationIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(PlaybackService.mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            )

        notificationManager.notify(1, notificationMusicPlayer.build())
    }

    companion object {
        const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_notification_channel"
    }
}