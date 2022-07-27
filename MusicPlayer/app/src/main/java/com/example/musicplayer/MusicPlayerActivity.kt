package com.example.musicplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.palette.graphics.Palette
import java.io.*


// Classe représentant la lecture d'une musique :
class MusicPlayerActivity : Tools() {

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
    private var myThread = Thread(FunctionalSeekBar(this))

    private var sameMusic = false
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)


        sameMusic = intent.getSerializableExtra("SAME MUSIC") as Boolean
        val position = intent.getSerializableExtra("POSITION") as Int

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
        val background = findViewById<RelativeLayout>(R.id.music_player)
        var bitmap : Bitmap? = null

        currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        Log.d("CURRENT SONG", currentSong.toString())
        if (currentSong.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentSong.albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            musicIcon.setImageBitmap(bitmap)
        } else {
            musicIcon.setImageResource(R.drawable.michael)
            val drawable = musicIcon.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            bitmap = bitmapDrawable.bitmap
        }
        Log.d("BITMAP", bitmap.toString())
        Log.d("palette",Palette.from(bitmap as Bitmap).generate().swatches[0].toString())
        val backgroundColor : Palette.Swatch? = if (Palette.from(bitmap).generate().darkVibrantSwatch == null){
            Log.d("here","")
            Palette.from(bitmap).generate().swatches[0]
        } else {
            Palette.from(bitmap).generate().darkVibrantSwatch
        }

        background.setBackgroundColor(backgroundColor?.rgb as Int)
        titleTv.setTextColor(backgroundColor.titleTextColor)
        seekBar.thumb.setTint(backgroundColor.titleTextColor)
        seekBar.progressDrawable.setTint(backgroundColor.titleTextColor)

        titleTv.text = currentSong.name
        songTitleInfo?.text = currentSong.name
        totalTimeTv.text = convertDuration(currentSong.duration)
        // Vérifions si la musique est en favoris :
        getFavoriteState()
        pausePlay.setOnClickListener{ pausePlay() }
        nextBtn.setOnClickListener{ playNextSong() }
        previousBtn.setOnClickListener{ playPreviousSong() }
        favoriteBtn.setOnClickListener{ setFavorite() }

        playMusic()
    }

    override fun playMusic(){
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
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
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
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        mediaPlayer.reset()
        setRessourcesWithMusic()
    }

    override fun pausePlay(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }

    // Permet de savoir si une chanson est en favoris :
    fun getFavoriteState(){
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
            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = false
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        } else {
            currentSong.favorite = true
            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = true

            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
        }

        // il faut maintenant sauvegardé l'état de la musique dans TOUTES les playlists :
        // Commencons par la playlist principale :
        val allSongs  = readAllMusicsFromFile(saveAllMusicsFile)
        for (element in allSongs){
            // Comparons avec quelque chose qui ne peut pas changer et qui soit unique :
            if (element.path == currentSong.path){
                element.favorite = currentSong.favorite
                writeAllMusicsToFile(saveAllMusicsFile, allSongs)
                break
            }
        }
        // Ensuite, mettons à jour nos playlists :
        val playlists = readAllPlaylistsFromFile(savePlaylistsFile)
        for (playlist in playlists){
            for (element in playlist.musicList){
                if (element.path == currentSong.path){
                    element.favorite = currentSong.favorite
                    break
                }
            }
        }
        // Mettons à jour la playlist favoris :
        val favoritePlaylist = playlists[0]
        var shouldBeInFavoriteList = true
        for (element in favoritePlaylist.musicList){
            if (element.path == currentSong.path){
                favoritePlaylist.musicList.remove(element)
                shouldBeInFavoriteList = false
                break
            }
        }
        if (shouldBeInFavoriteList){
            favoritePlaylist.musicList.add(currentSong)
        }
        writePlaylistsToFile(savePlaylistsFile, playlists)
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

    class FunctionalSeekBar(private var musicPlayerActivity: MusicPlayerActivity) : Runnable{

        override fun run() {
            musicPlayerActivity.seekBar.progress = musicPlayerActivity.mediaPlayer.currentPosition
            musicPlayerActivity.currentTimeTv.text = musicPlayerActivity.convertDuration(musicPlayerActivity.mediaPlayer.currentPosition.toLong())

            Handler(Looper.getMainLooper()).postDelayed(this,1000)

        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "RESUME MUSIC")
    }
}
