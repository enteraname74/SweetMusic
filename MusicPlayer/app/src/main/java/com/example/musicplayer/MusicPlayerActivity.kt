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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

// Classe représentant la lecture d'une musique :
class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var titleTv : TextView
    lateinit var currentTimeTv : TextView
    private lateinit var totalTimeTv : TextView
    lateinit var seekBar : SeekBar
    private lateinit var pausePlay : ImageView
    private lateinit var nextBtn : ImageView
    private lateinit var previousBtn : ImageView
    private lateinit var musicIcon : ImageView
    private lateinit var favoriteBtn : ImageView
    private lateinit var currentSong : Music
    private val saveFile = "allMusics.musics"
    private var myThread = Thread(FunctionnalSeekBar(this))

    private var musics = ArrayList<Music>()
    private var sameMusic : Boolean = false
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        musics = intent.getSerializableExtra("LIST") as ArrayList<Music>
        sameMusic = intent.getSerializableExtra("SAME MUSIC") as Boolean
        val position = intent.getSerializableExtra("POSITION") as Int

        // Vérifions si on change de playlist :
        if (musics != MyMediaPlayer.currentPlaylist) {
            Log.d("CHANGEMENT PLAYLIST","")
            MyMediaPlayer.currentPlaylist = musics
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position

        Log.d("SAME MUSIC ?", sameMusic.toString())

        titleTv = findViewById(R.id.song_title)
        currentTimeTv = findViewById(R.id.current_time)
        totalTimeTv = findViewById(R.id.total_time)
        seekBar = findViewById(R.id.seek_bar)
        pausePlay = findViewById(R.id.pause_play)
        nextBtn = findViewById(R.id.next)
        previousBtn = findViewById(R.id.previous)
        musicIcon = findViewById(R.id.album_cover_big)
        favoriteBtn = findViewById(R.id.favorite)

        titleTv.setSelected(true)

        setRessourcesWithMusic()

        this@MusicPlayerActivity.runOnUiThread(myThread)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    private fun setRessourcesWithMusic(){
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)

        currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        Log.d("CURRENT SONG", currentSong.toString())
        musicIcon.setImageResource(R.drawable.michael)
        titleTv.text = currentSong.name
        songTitleInfo?.text = currentSong.name
        totalTimeTv.text = convertDuration(currentSong.duration as Long)
        // Vérifions si la musique est en favoris :
        getFavoriteState()
        pausePlay.setOnClickListener(View.OnClickListener{pausePlay()})
        nextBtn.setOnClickListener(View.OnClickListener { playNextSong() })
        previousBtn.setOnClickListener(View.OnClickListener { playPreviousSong() })
        favoriteBtn.setOnClickListener(View.OnClickListener { setFavorite() })

        playMusic()
    }

    private fun playMusic(){
        /*
        Si la musique est la même, alors on ne met à jour que la seekBar (elle se remettra au bon niveau automatiquement)
         */

        if (!sameMusic) {
            mediaPlayer.reset()
            try {
                mediaPlayer.setDataSource(currentSong.path)
                mediaPlayer.prepare()
                mediaPlayer.start()
                seekBar.progress = 0
                seekBar.max = mediaPlayer.duration
                pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            seekBar.progress = 0
            seekBar.max = mediaPlayer.duration

            if (!mediaPlayer.isPlaying){
                pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
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
            pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }

    // Permet de savoir si une chanson est en favoris :
    private fun getFavoriteState(){
        if(currentSong.favorite){
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
        } else {
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }
    }

    // Permet de changer le statut favoris de la chanson :
    private fun setFavorite(){
        if(currentSong.favorite){
            currentSong.favorite = false
            musics[MyMediaPlayer.currentIndex].favorite = false
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        } else {
            currentSong.favorite = true
            musics[MyMediaPlayer.currentIndex].favorite = true
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
        }
        writeObjectToFile(saveFile, musics)
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

            Handler(Looper.getMainLooper()).postDelayed(this,1000)

        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "RESUME MUSIC")
    }

    private fun writeObjectToFile(filename : String, content : ArrayList<Music>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
            Toast.makeText(this,"ALL MUSICS SAVED", Toast.LENGTH_SHORT).show()
        } catch (error : IOException){
            Log.d("Error","")
        }
    }

}
