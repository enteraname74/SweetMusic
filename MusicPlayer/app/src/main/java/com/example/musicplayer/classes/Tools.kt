package com.example.musicplayer.classes

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.musicplayer.*
import com.example.musicplayer.adapters.MusicList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

open class Tools : AppCompatActivity() {
    val saveAllMusicsFile = "allMusics.musics"
    val savePlaylistsFile = "allPlaylists.playlists"
    val saveAllDeletedFiles = "allDeleted.musics"

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
        if (list.size > 0) {
            val shuffledList = ArrayList(list.map { it.copy() })
            shuffledList.shuffle()
            MyMediaPlayer.currentPlaylist = ArrayList(shuffledList.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(shuffledList.map { it.copy() })
            val sameMusic = false

            MyMediaPlayer.currentIndex = 0

            val intent = Intent(context, MusicPlayerActivity::class.java)

            intent.putExtra("SAME MUSIC", sameMusic)

            startActivity(intent)
        }
    }

    fun onBottomMenuClick(position : Int, context : Context){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(context, MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    open fun playNextSong(adapter : MusicList){
        if (requestFocus()) {
            if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
                MyMediaPlayer.currentIndex = 0
            } else {
                MyMediaPlayer.currentIndex +=1
            }
            adapter.notifyDataSetChanged()
            playMusic()
        }
    }

    open fun playPreviousSong(adapter : MusicList){
        if (requestFocus()){
            if(MyMediaPlayer.currentIndex ==0){
                MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
            } else {
                MyMediaPlayer.currentIndex -=1
            }
            adapter.notifyDataSetChanged()
            playMusic()
        }
    }

    open fun requestFocus() : Boolean{
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        return when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
                false
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                true
            }
            else -> {
                Toast.makeText(this,"An unknown error has come up", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    fun stopMusic(){
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        mediaPlayer.stop()
        MyMediaPlayer.currentIndex = -1
        MyMediaPlayer.currentPlaylist = ArrayList<Music>()
        MyMediaPlayer.initialPlaylist = ArrayList<Music>()
        PlaybackService.audioManager.abandonAudioFocusRequest(audioFocusRequest)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    open fun pausePlay(pausePlayButton : ImageView){
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        if(PlaybackService.audioManager.isMusicActive){
            if(!(mediaPlayer.isPlaying)){
                when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                        Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
                    }

                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        mediaPlayer.start()
                        pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)

                        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                        intentForNotification.putExtra("STOP", false)
                        applicationContext.sendBroadcast(intentForNotification)
                    }
                    else -> {
                        Toast.makeText(this,"An unknown error has come up", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                mediaPlayer.pause()
                PlaybackService.audioManager.abandonAudioFocusRequest(audioFocusRequest)
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)

                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", true)
                applicationContext.sendBroadcast(intentForNotification)
            }
        } else {
            if (mediaPlayer.isPlaying){
                mediaPlayer.pause()
                PlaybackService.audioManager.abandonAudioFocusRequest(audioFocusRequest)
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)

                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", true)
                applicationContext.sendBroadcast(intentForNotification)
            } else {
                mediaPlayer.start()
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)

                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", false)
                applicationContext.sendBroadcast(intentForNotification)
            }
        }
    }

    fun updateBottomPanel(songTitle : TextView, albumCover : ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                songTitle.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            }

            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
                // Passons d'abord notre byteArray en bitmap :
                val bytes =
                    MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                withContext(Dispatchers.Main) {
                    albumCover.setImageBitmap(bitmap)
                }
            } else {
                albumCover.setImageResource(R.drawable.michael)
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
            Toast.makeText(applicationContext, "Couldn't retrieve musics", Toast.LENGTH_LONG).show()

        }
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, "allPlaylists.playlists")))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.d("Error retrieving musics",error.toString())
            Toast.makeText(applicationContext, "Couldn't retrieve playlists", Toast.LENGTH_LONG).show()
        }
        Toast.makeText(applicationContext, "Data retrieved in your Download Folder", Toast.LENGTH_LONG).show()
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

    open fun readAllMusicsFromFile(filename : String) : ArrayList<Music> {
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

    open fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
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
            writePlaylistsToFile(savePlaylistsFile, MyMediaPlayer.allPlaylists)
        }
    }

    open fun readAllDeletedMusicsFromFile() {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, saveAllDeletedFiles)))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
        } catch (error : IOException){
            Log.d("Error read deleted",error.toString())
        }
        MyMediaPlayer.allDeletedMusics = content
    }

    open fun writeAllDeletedSong(){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllDeletedFiles)))
            oos.writeObject(MyMediaPlayer.allDeletedMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write deleted",error.toString())
        }
    }

    /****************************** OTHERS : **********************************/

    fun openNavigationMenu(drawerLayout : DrawerLayout){
        drawerLayout.openDrawer(GravityCompat.START)
    }
}