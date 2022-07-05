package com.example.musicplayer
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

// Classe représentant la lecture d'une musique :
class MusicPlayerActivity : AppCompatActivity() {

    private var titleTv : TextView? = null
    var currentTimeTv : TextView? = null
    private var totalTimeTv : TextView? = null
    var seekBar : SeekBar? = null
    private var pausePlay : ImageView? = null
    private var nextBtn : ImageView? = null
    private var previousBtn : ImageView? = null
    private var musicIcon : ImageView? = null
    private var currentSong : Music? = null
    private var myThread = Thread(FunctionnalSeekBar(this))

    private var musics = ArrayList<Music>()
    private var sameMusic : Boolean = false
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())

        musics = intent.getSerializableExtra("LIST") as ArrayList<Music>
        sameMusic = intent.getSerializableExtra("SAME MUSIC") as Boolean

        Log.d("MUSIC", musics.toString())
        Log.d("SAME MUSIC ?", sameMusic.toString())

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
                if(fromUser){
                    Log.d("THERE", progress.toString())
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    private fun setRessourcesWithMusic(){
        currentSong = musics.get(MyMediaPlayer.currentIndex)
        titleTv?.text = currentSong?.name
        totalTimeTv?.text = convertDuration(currentSong?.duration as Long)
        pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
        nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
        previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })

        playMusic()
    }

    private fun playMusic(){
        /*
        Si la musique est la même, alors on ne met à jour que la seekBar (elle se remettra au bon niveau automatiquement)
         */

        if (!sameMusic) {
            mediaPlayer.reset()
            try {
                mediaPlayer.setDataSource(currentSong?.path)
                mediaPlayer.prepare()
                mediaPlayer.start()
                seekBar?.progress = 0
                seekBar?.max = mediaPlayer.duration
                pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            seekBar?.progress = 0
            seekBar?.max = mediaPlayer.duration

            if (!mediaPlayer.isPlaying){
                pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            }
        }
    }

    private fun playNextSong(){
        sameMusic = false
        if(MyMediaPlayer.currentIndex==(musics.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        mediaPlayer.reset()
        setRessourcesWithMusic()
    }

    private fun playPreviousSong(){
        sameMusic = false
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (musics.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        mediaPlayer.reset()
        setRessourcesWithMusic()
    }

    private fun pausePlay(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }

    fun convertDuration(duration: Long): String {
        val minutes: Float = duration.toFloat() / 1000 / 60
        val seconds: Float = duration.toFloat() / 1000 % 60

        val strMinutes: String = minutes.toString().split(".")[0]

        val strSeconds = if (seconds < 10.0) {
            "0" + seconds.toString().split(".")[0]
        } else {
            seconds.toString().split(".")[0]
        }

        return "$strMinutes:$strSeconds"
    }

    class FunctionnalSeekBar(private var musicPlayerActivity: MusicPlayerActivity) : Runnable{

        override fun run() {
            musicPlayerActivity.seekBar?.progress = musicPlayerActivity.mediaPlayer.currentPosition
            musicPlayerActivity.currentTimeTv?.text = musicPlayerActivity.convertDuration(musicPlayerActivity.mediaPlayer.currentPosition.toLong())

            // Si nous arrivons au bout de la barre (donc, au bout de la musique), on passe à la musique suivante :
            if(musicPlayerActivity.currentTimeTv?.text == musicPlayerActivity.totalTimeTv?.text){
                musicPlayerActivity.playNextSong()
            }

            Handler(Looper.getMainLooper()).postDelayed(this,1000)

        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "RESUME MUSIC")
    }

}
