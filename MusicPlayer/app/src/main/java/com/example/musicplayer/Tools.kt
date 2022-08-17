package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.io.*

open class Tools : AppCompatActivity() {
    val saveAllMusicsFile = "allMusics.musics"
    val savePlaylistsFile = "allPlaylists.playlists"

    var mediaPlayer = MyMediaPlayer.getInstance

    /************************ USES THE MEDIAPLAYER : ***************************/

    open fun playMusic(){
        mediaPlayer.reset()
        try {
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)

            val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)
            if (currentSong.albumCover != null){
                // Passons d'abord notre byteArray en bitmap :
                val bytes = currentSong.albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                albumCoverInfo.setImageBitmap(bitmap)
            } else {
                albumCoverInfo.setImageResource(R.drawable.michael)
            }

            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            Log.d("ERROR","")
            e.printStackTrace()
        }
    }

    fun playRandom(list : ArrayList<Music>, context : Context) {
        MyMediaPlayer.getInstance.reset()
        val shuffledList = ArrayList(list.map { it.copy() })
        shuffledList.shuffle()
        MyMediaPlayer.currentPlaylist = ArrayList(shuffledList.map { it.copy() })
        MyMediaPlayer.initialPlaylist = ArrayList(shuffledList.map { it.copy() })
        val sameMusic = false

        MyMediaPlayer.currentIndex = 0

        val intent = Intent(context,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    fun onBottomMenuClick(position : Int, context : Context){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true
        MyMediaPlayer.doesASongWillBePlaying = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
            MyMediaPlayer.doesASongWillBePlaying = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(context,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    open fun playNextSong(adapter : MusicList){
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    open fun playPreviousSong(adapter : MusicList){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    open fun pausePlay(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("doesA..", MyMediaPlayer.doesASongWillBePlaying.toString())
            Log.d("MediaPlayer state", mediaPlayer.isPlaying.toString())
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> println("gain")
                else -> {
                    if (mediaPlayer.isPlaying && !MyMediaPlayer.doesASongWillBePlaying) {
                        println("loss focus")
                        mediaPlayer.pause()
                        val pausePlay: ImageView = findViewById(R.id.pause_play)
                        pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                    }
                    Log.d("change does..", "")
                    MyMediaPlayer.doesASongWillBePlaying = false
                }
            }
        }


        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(onAudioFocusChange)
            .build()

        when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                MyMediaPlayer.doesASongWillBePlaying = true
                if(mediaPlayer.isPlaying){
                    mediaPlayer.pause()
                    pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                } else {
                    mediaPlayer.start()
                    pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                }
            }
            else -> {
                Toast.makeText(this,"AN unknown error has come up", Toast.LENGTH_SHORT).show()
            }
        }
    }



    /************************ WRITE OR READ INTO FILES : ***************************/

    fun retrieveAllMusicsFromApp(){
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, "allMusics.musics")))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error retrieving musics",error.toString())
        }
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, "allPlaylists.playlists")))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.d("Error retrieving musics",error.toString())
        }
    }

    fun writeAllMusicsToFile(filename : String, content : ArrayList<Music>){
        MyMediaPlayer.allMusics = content
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write musics",error.toString())
        }
    }

    fun readAllMusicsFromFile(filename : String) : ArrayList<Music> {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
        } catch (error : IOException){
            Log.d("Error read musics",error.toString())
        }
        MyMediaPlayer.allMusics = content
        return content
    }

    fun writePlaylistsToFile(filename : String, content : ArrayList<Playlist>){
        MyMediaPlayer.allPlaylists = content
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write playlists",error.toString())
        }
    }

    fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
        val path = applicationContext.filesDir
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Playlist>
            ois.close()
        } catch (error : IOException){
            Log.d("Error read playlists",error.toString())
        }
        MyMediaPlayer.allPlaylists = content
        return content
    }

    fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
        return byteStream.toByteArray()
    }

    fun writeAllAsync(musics : ArrayList<Music>, playlists : ArrayList<Playlist>){
        writeAllMusicsToFile(saveAllMusicsFile, musics)
        writePlaylistsToFile(savePlaylistsFile, playlists)
        println("reussie")
    }

    fun readPlaylistsAsync(){
        MyMediaPlayer.allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
        if (MyMediaPlayer.allPlaylists.size == 0){
            MyMediaPlayer.allPlaylists.add(Playlist("Favorites",ArrayList(),null,true))
            writePlaylistsToFile(savePlaylistsFile,MyMediaPlayer.allPlaylists)
        }
    }

    /****************************** OTHERS : **********************************/

    fun openNavigationMenu(drawerLayout : DrawerLayout){
        drawerLayout.openDrawer(GravityCompat.START)
    }
}