package com.example.musicplayer

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
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var mediaSessionToken : MediaSession.Token

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
                    Log.d("SERVICE", "GAIN FOCUS WHEN OTHER APP WAS PLAYING")
                    val intentForBroadcast = Intent("BROADCAST")
                    intentForBroadcast.putExtra("STOP",false)
                    applicationContext.sendBroadcast(intentForBroadcast)

                    val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                    intentForNotification.putExtra("STOP", false)
                    applicationContext.sendBroadcast(intentForNotification)
                }
                else -> {
                    if (mediaPlayer.isPlaying) {
                        Log.d("SERVICE", "LOSS FOCUS WHILE PLAYING")
                        val intentForBroadcast = Intent("BROADCAST")
                        intentForBroadcast.putExtra("STOP",true)
                        applicationContext.sendBroadcast(intentForBroadcast)

                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                        intentForNotification.putExtra("STOP", true)
                        applicationContext.sendBroadcast(intentForNotification)
                        mediaPlayer.pause()
                    }
                }
            }
        }

        //mediaSession = MediaSession(applicationContext, packageName+"mediaSessionPlayer")
        //mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

        mediaSession = MediaSessionCompat(applicationContext, packageName+"mediaSessionPlayer")

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                Log.d("MediaButtonEvent", mediaButtonIntent.extras?.get(Intent.EXTRA_KEY_EVENT).toString())

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
                                        audioManager.abandonAudioFocusRequest(audioFocusRequest)
                                        val intentForBroadcast = Intent("BROADCAST")
                                        intentForBroadcast.putExtra("STOP", true)
                                        applicationContext.sendBroadcast(intentForBroadcast)

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", true)
                                        applicationContext.sendBroadcast(intentForNotification)
                                    } else {
                                        mediaPlayer.start()

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForNotification)

                                        val intentForBroadcast = Intent("BROADCAST")
                                        intentForBroadcast.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForBroadcast)
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
                                        val intentForBroadcast = Intent("BROADCAST")
                                        intentForBroadcast.putExtra("STOP", true)
                                        applicationContext.sendBroadcast(intentForBroadcast)

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", true)
                                        applicationContext.sendBroadcast(intentForNotification)
                                    } else {
                                        mediaPlayer.start()

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForNotification)

                                        val intentForBroadcast = Intent("BROADCAST")
                                        intentForBroadcast.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForBroadcast)
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

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForNotification)

                                        val intentForActivity = Intent("BROADCAST")
                                        intentForActivity.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForActivity)
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

                                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                                        intentForNotification.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForNotification)

                                        val intentForActivity = Intent("BROADCAST")
                                        intentForActivity.putExtra("STOP", false)
                                        applicationContext.sendBroadcast(intentForActivity)
                                    }
                                }
                            } catch (error : Error){
                                Log.d("error","")
                            }
                        }
                    }
                }

                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })

        val state = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0F)
            .build()

        mediaSession.setPlaybackState(state)
        mediaSession.isActive = true

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ON CREATE", "")
    }

    companion object {
        lateinit var onAudioFocusChange : AudioManager.OnAudioFocusChangeListener
        lateinit var audioAttributes : AudioAttributes
        lateinit var audioManager : AudioManager
    }
}