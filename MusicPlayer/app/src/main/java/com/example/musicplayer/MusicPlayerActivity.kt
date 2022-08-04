package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRouter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException


// Classe représentant la lecture d'une musique :
class MusicPlayerActivity : Tools() {

    private lateinit var titleTv : TextView
    lateinit var currentTimeTv : TextView
    private lateinit var totalTimeTv : TextView
    lateinit var seekBar : SeekBar
    private lateinit var initialList : ImageView
    private lateinit var currentList : ImageView
    private lateinit var pausePlay : ImageView
    private lateinit var nextBtn : ImageView
    private lateinit var previousBtn : ImageView
    private lateinit var musicIcon : ImageView
    private lateinit var favoriteBtn : ImageView
    private lateinit var currentSong : Music
    private lateinit var sort : ImageView
    private lateinit var audioManager : AudioManager
    private lateinit var audioAttributes : AudioAttributes
    private lateinit var audioFocusRequest : AudioFocusRequest
    private lateinit var onAudioFocusChange: AudioManager.OnAudioFocusChangeListener
    private var myThread = Thread(FunctionalSeekBar(this))

    private var sameMusic = false
    var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        sameMusic = intent.getSerializableExtra("SAME MUSIC") as Boolean
        val position = intent.getSerializableExtra("POSITION") as Int

        MyMediaPlayer.currentIndex = position

        titleTv = findViewById(R.id.song_title)
        currentTimeTv = findViewById(R.id.current_time)
        totalTimeTv = findViewById(R.id.total_time)
        seekBar = findViewById(R.id.seek_bar)
        initialList = findViewById(R.id.initial_playlist)
        currentList = findViewById(R.id.current_playlist)
        pausePlay = findViewById(R.id.pause_play)
        nextBtn = findViewById(R.id.next)
        previousBtn = findViewById(R.id.previous)
        musicIcon = findViewById(R.id.album_cover_big)
        favoriteBtn = findViewById(R.id.favorite)
        sort = findViewById(R.id.sort)
        sort.setImageResource(MyMediaPlayer.iconsList[MyMediaPlayer.iconIndex])

        titleTv.isSelected = true

        /*
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("doesA..",MyMediaPlayer.doesASongWillBePlaying.toString())
            Log.d("MediaPlayer state", mediaPlayer.isPlaying.toString())
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> println("gain")
                else -> {
                    if (mediaPlayer.isPlaying && !MyMediaPlayer.doesASongWillBePlaying) {
                        println("loss focus")
                        mediaPlayer.pause()
                        pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                    }
                    Log.d("change does..","")
                    MyMediaPlayer.doesASongWillBePlaying = false
                }
            }
        }

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(onAudioFocusChange)
            .build()

        when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this, "Cannot launch the music", Toast.LENGTH_SHORT).show()
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Log.d("start music", "")
                setRessourcesWithMusic()

                this@MusicPlayerActivity.runOnUiThread(myThread)

                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
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

            else -> {
                Toast.makeText(this, "AN unknown error has come up", Toast.LENGTH_SHORT).show()
            }
        }

         */
        setRessourcesWithMusic()

        this@MusicPlayerActivity.runOnUiThread(myThread)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
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
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val background = findViewById<RelativeLayout>(R.id.music_player)
        var bitmap : Bitmap? = null

        currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

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

        Log.d("palette",Palette.from(bitmap as Bitmap).generate().swatches[0].toString())
        val backgroundColor : Palette.Swatch? = if (Palette.from(bitmap).generate().darkVibrantSwatch == null){
            Log.d("here","")
            Palette.from(bitmap).generate().swatches[0]
        } else {
            Palette.from(bitmap).generate().darkVibrantSwatch
        }

        background.setBackgroundColor(backgroundColor?.rgb as Int)
        titleTv.setTextColor(backgroundColor.titleTextColor)
        currentTimeTv.setTextColor(backgroundColor.titleTextColor)
        totalTimeTv.setTextColor(backgroundColor.titleTextColor)
        seekBar.progressDrawable.setTint(backgroundColor.titleTextColor)

        initialList.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        currentList.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        pausePlay.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        nextBtn.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        previousBtn.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        favoriteBtn.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        sort.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)

        titleTv.text = currentSong.name
        songTitleInfo?.text = currentSong.name
        totalTimeTv.text = convertDuration(currentSong.duration)

        // Vérifions si la musique est en favoris :
        getFavoriteState()

        initialList.setOnClickListener{ seeList("initialList") }
        currentList.setOnClickListener{ seeList("currentList") }
        pausePlay.setOnClickListener{ pausePlay() }
        nextBtn.setOnClickListener{ playNextSong() }
        previousBtn.setOnClickListener{ playPreviousSong() }
        favoriteBtn.setOnClickListener{ setFavorite() }
        sort.setOnClickListener{ changeSorting() }

        playMusic()
    }

    private fun seeList(listType: String) {
        val intent = Intent(this@MusicPlayerActivity, SeeMusicListActivity::class.java)
        intent.putExtra("LIST-TYPE", listType)
        startActivity(intent)
    }

    private fun changeSorting() {
        if(MyMediaPlayer.iconIndex == (MyMediaPlayer.iconsList.size)-1){
            MyMediaPlayer.iconIndex = 0
        } else {
            MyMediaPlayer.iconIndex += 1
        }
        setSorting()
    }

    private fun setSorting(){
        when (MyMediaPlayer.iconIndex){
            0 -> {
                MyMediaPlayer.currentPlaylist = ArrayList(MyMediaPlayer.initialPlaylist.map { it.copy() })
                MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
            }
            1 -> {
                println("shuffle")
                MyMediaPlayer.currentPlaylist.shuffle()
                MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
            }
            2 -> {
                // On choisit la fonction de replay de la meme musique, on supprime d'abord toute la playlist actuelle :
                MyMediaPlayer.currentPlaylist.clear()
                MyMediaPlayer.currentPlaylist.add(currentSong)
                MyMediaPlayer.currentIndex = 0
            }
        }
        sort.setImageResource(MyMediaPlayer.iconsList[MyMediaPlayer.iconIndex])
    }

    override fun playMusic(){
        /*
        Si la musique est la même, alors on ne met à jour que la seekBar (elle se remettra au bon niveau automatiquement)
         */
        var am = getSystemService(AUDIO_SERVICE) as AudioManager
        var volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d("current level", volume_level.toString())

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
        am = getSystemService(AUDIO_SERVICE) as AudioManager
        volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        val mr = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
        val ri = mr.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        Log.d("infos", ri.toString())

        Log.d("current level", volume_level.toString())
        mediaPlayer.setVolume(0.5F, 0.5F)

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
        /*
        Log.d("ML!,ZF",audioManager.requestAudioFocus(audioFocusRequest).toString())
        when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {

            }
            else -> {
                Toast.makeText(this,"AN unknown error has come up", Toast.LENGTH_SHORT).show()
            }
        }

         */
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
            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = false
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        } else {
            currentSong.favorite = true
            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = true
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
        }

        // il faut maintenant sauvegardé l'état de la musique dans TOUTES les playlists :
        // Commencons par la playlist principale :
        val allMusics  = MyMediaPlayer.allMusics
        for (element in allMusics){
            // Comparons avec quelque chose qui ne peut pas changer et qui soit unique :
            if (element.path == currentSong.path){
                element.favorite = currentSong.favorite
                break
            }
        }
        // Ensuite, mettons à jour nos playlists :
        val allPlaylists = MyMediaPlayer.allPlaylists
        for (playlist in allPlaylists){
            for (element in playlist.musicList){
                if (element.path == currentSong.path){
                    element.favorite = currentSong.favorite
                    break
                }
            }
        }
        // Mettons à jour la playlist favoris :
        val favoritePlaylist = allPlaylists[0]
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

        GlobalScope.launch(Dispatchers.IO){
            launch{writeAllAsync(allMusics,allPlaylists)}
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
