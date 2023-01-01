package com.example.musicplayer

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import com.example.musicplayer.classes.MyMediaPlayer

class PlaybackService : Service() {
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onBind(intent: Intent): IBinder? {
        Log.d("ON BIND","")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("START COMMAND","")
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d("PLAYBACK SERVICE", "GAIN FOCUS WHEN OTHER APP WAS PLAYING")
                    updateUI(false)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    if (audioManager.isMusicActive) {
                        if (mediaPlayer.isPlaying) {
                            Log.d("PLAYBACK SERVICE", "LOSS FOCUS WHILE PLAYING")
                            mediaPlayer.pause()
                            updateUI(true)
                        }
                    }
                }
                else -> {
                    if (mediaPlayer.isPlaying) {
                        Log.d("PLAYBACK SERVICE", "LOSS FOCUS ELSE WHILE PLAYING")
                        mediaPlayer.pause()
                        updateUI(true)
                    }
                }
            }
        }

        //mediaSession = MediaSession(applicationContext, packageName+"mediaSessionPlayer")
        //mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

        mediaSession = MediaSessionCompat(applicationContext, packageName+"mediaSessionPlayer")

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                mediaPlayer.seekTo(pos.toInt())
                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", !mediaPlayer.isPlaying)
                applicationContext.sendBroadcast(intentForNotification)
            }

            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                Log.d("PLAYBACK SERVICE", mediaButtonIntent.extras?.get(Intent.EXTRA_KEY_EVENT).toString())
                if (MyMediaPlayer.currentIndex != -1) {
                    val keyEvent = mediaButtonIntent.extras?.get(Intent.EXTRA_KEY_EVENT) as KeyEvent
                    if (keyEvent.action == KeyEvent.ACTION_DOWN){
                        when(keyEvent.keyCode){
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                    .setAudioAttributes(audioAttributes)
                                    .setAcceptsDelayedFocusGain(true)
                                    .setOnAudioFocusChangeListener(onAudioFocusChange)
                                    .build()

                                when (audioManager.requestAudioFocus(audioFocusRequest)) {
                                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                                        if (mediaPlayer.isPlaying) {
                                            mediaPlayer.pause()
                                            updateUI(true)
                                        } else {
                                            mediaPlayer.start()
                                            updateUI(false)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                    .setAudioAttributes(audioAttributes)
                                    .setAcceptsDelayedFocusGain(true)
                                    .setOnAudioFocusChangeListener(onAudioFocusChange)
                                    .build()

                                when (audioManager.requestAudioFocus(audioFocusRequest)) {
                                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                                        if (mediaPlayer.isPlaying) {
                                            mediaPlayer.pause()
                                            updateUI(true)

                                        } else {
                                            mediaPlayer.start()
                                            updateUI(false)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
                                    MyMediaPlayer.currentIndex = 0
                                } else {
                                    MyMediaPlayer.currentIndex +=1
                                }

                                val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

                                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                    .setAudioAttributes(audioAttributes)
                                    .setAcceptsDelayedFocusGain(true)
                                    .setOnAudioFocusChangeListener(onAudioFocusChange)
                                    .build()

                                try {
                                    when (audioManager.requestAudioFocus(audioFocusRequest)) {
                                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(currentSong.path)
                                            mediaPlayer.prepare()
                                            mediaPlayer.start()
                                            updateUI(false)
                                        }
                                    }
                                } catch (error : Error){
                                    Log.d("error","")
                                }
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                if(MyMediaPlayer.currentIndex ==0){
                                    MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
                                } else {
                                    MyMediaPlayer.currentIndex -=1
                                }
                                val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

                                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                    .setAudioAttributes(audioAttributes)
                                    .setAcceptsDelayedFocusGain(true)
                                    .setOnAudioFocusChangeListener(onAudioFocusChange)
                                    .build()

                                try {
                                    when (audioManager.requestAudioFocus(audioFocusRequest)) {
                                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(currentSong.path)
                                            mediaPlayer.prepare()
                                            mediaPlayer.start()
                                            updateUI(false)
                                        }
                                    }
                                } catch (error : Error){
                                    Log.d("error","")
                                }
                            }
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })

        mediaSession.setPlaybackState(updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN))
        mediaSession.isActive = true

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ON CREATE", "")
    }

    /*
    override fun onDestroy() {
        super.onDestroy()
        Log.d("PLAYBACK SERVICE", "DESTROYED")
        stopMusic()
    }

     */

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("PLAYBACK SERVICE", "REMOVED")
        stopMusic()
    }

    private fun updateUI(isMusicStopped : Boolean) {
        val intentForBroadcast = Intent("BROADCAST")
        intentForBroadcast.putExtra("STOP", isMusicStopped)
        applicationContext.sendBroadcast(intentForBroadcast)

        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
        intentForNotification.putExtra("STOP", isMusicStopped)
        applicationContext.sendBroadcast(intentForNotification)
    }

    private fun stopMusic(){
        MyMediaPlayer.currentIndex = -1
        MyMediaPlayer.currentPlaylist = ArrayList<Music>()
        MyMediaPlayer.initialPlaylist = ArrayList<Music>()

        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
        intentForNotification.putExtra("STOP_RECEIVE", true)
        applicationContext.sendBroadcast(intentForNotification)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    companion object {
        lateinit var onAudioFocusChange : AudioManager.OnAudioFocusChangeListener
        lateinit var audioAttributes : AudioAttributes
        lateinit var audioManager : AudioManager
        lateinit var mediaSession : MediaSessionCompat

        fun updateMediaSessionState(musicState: Int, musicPosition: Long): PlaybackStateCompat {
            return PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_SEEK_TO
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(
                    musicState,
                    musicPosition,
                    1.0F
                )
                .build()
        }
    }
}