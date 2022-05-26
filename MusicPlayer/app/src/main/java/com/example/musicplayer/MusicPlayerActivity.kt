package com.example.musicplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MusicPlayerActivity : AppCompatActivity() {

    var titleTv : TextView? = null;
    var currentTimeTv : TextView? = null;
    var totalTimeTv : TextView? = null;
    var seekBar : SeekBar? = null;
    var pausePlay : ImageView? = null;
    var nextBtn : ImageView? = null;
    var previousBtn : ImageView? = null;
    var musicIcon : ImageView? = null;
    var currentSong : Music? = null
    var myThread = Thread(FunctionnalSeekBar(this))

    var musics = ArrayList<Music>()
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())

        musics = intent.getSerializableExtra("LIST") as ArrayList<Music>

        Log.d("MUSIC", musics.toString())

        titleTv = findViewById(R.id.song_title)
        currentTimeTv = findViewById(R.id.current_time)
        totalTimeTv = findViewById(R.id.total_time)
        seekBar = findViewById(R.id.seek_bar)
        pausePlay = findViewById(R.id.pause_play)
        nextBtn = findViewById(R.id.next)
        previousBtn = findViewById(R.id.previous)
        musicIcon = findViewById(R.id.album_cover_big)

        titleTv?.setSelected(true)

        setRessourcesWithMusic()

        this@MusicPlayerActivity.runOnUiThread(myThread)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(mediaPlayer != null && fromUser){
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    fun setRessourcesWithMusic(){
        currentSong = musics?.get(MyMediaPlayer.currentIndex)
        Log.d("DURATION",currentSong?.duration.toString())
        titleTv?.text = currentSong?.name
        totalTimeTv?.text = convertDuration(currentSong?.duration as Long)
        pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
        nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
        previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })

        playMusic()

    }

    fun playMusic(){
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(currentSong?.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            seekBar?.progress = 0
            seekBar?.max = mediaPlayer.duration
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            Log.d("TRACK INFO", mediaPlayer.trackInfo.toString())
        } catch (e : IOException){
            e.printStackTrace()
        }
    }

    fun playNextSong(){
        if(MyMediaPlayer.currentIndex==(musics.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        mediaPlayer.reset()
        setRessourcesWithMusic()
    }

    fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (musics.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        mediaPlayer.reset()
        setRessourcesWithMusic()
    }

    fun pausePlay(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }

    fun convertDuration(duration : Long) : String{
        var minutes : Float = duration.toFloat() / 1000 / 60
        var seconds : Float = duration.toFloat() / 1000 % 60

        var strMinutes : String = minutes.toString().split(".")[0]

        var strSeconds : String = ""
        if (seconds < 10.0) {
            strSeconds = "0"+seconds.toString().split(".")[0]
        } else {
            strSeconds = seconds.toString().split(".")[0]
        }

        var strDuration : String = strMinutes+":"+strSeconds

        return strDuration
    }

    class FunctionnalSeekBar(var musicPlayerActivity: MusicPlayerActivity) : Runnable{

        override fun run() {
            if(musicPlayerActivity.mediaPlayer != null){
                musicPlayerActivity.seekBar?.progress = musicPlayerActivity.mediaPlayer.currentPosition
                musicPlayerActivity.currentTimeTv?.text = musicPlayerActivity.convertDuration(musicPlayerActivity.mediaPlayer.currentPosition.toLong())

                Handler(Looper.getMainLooper()).postDelayed(this,1000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "RESUME MUSIC")
    }
}
