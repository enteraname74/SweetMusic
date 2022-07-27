package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.*

open class Tools : AppCompatActivity() {
    val saveAllMusicsFile = "allMusics.musics"
    val savePlaylistsFile = "allPlaylists.playlists"
    private var mediaPlayer = MyMediaPlayer.getInstance

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

    fun onBottomMenuClick(position : Int, context : Context){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
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
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }



    /************************ WRITE OR READ INTO FILES : ***************************/



    fun writeAllMusicsToFile(filename : String, content : ArrayList<Music>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
            Toast.makeText(this,"ALL SONGS WROTE", Toast.LENGTH_SHORT).show()
        } catch (error : IOException){
            Log.d("ErrorWRITE",error.toString())
        }
    }

    fun readAllMusicsFromFile(filename : String) : ArrayList<Music> {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
            Toast.makeText(this,"ALL SONGS FETCHED", Toast.LENGTH_SHORT).show()
        } catch (error : IOException){
            Log.d("Error",error.toString())
        }
        return content
    }

    fun writePlaylistsToFile(filename : String, content : ArrayList<Playlist>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
            Toast.makeText(this,"ALL PLAYLISTS WROTE", Toast.LENGTH_SHORT).show()
        } catch (error : IOException){
            Log.d("Error","")
        }
    }

    fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
        val path = applicationContext.filesDir
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Playlist>
            ois.close()
            Toast.makeText(this,"ALL PLAYLISTS FETCHED", Toast.LENGTH_SHORT).show()
        } catch (error : IOException){
            Log.d("Error","")
        }

        return content
    }

    fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
        return byteStream.toByteArray()
    }

    fun readPlaylistsAsync() = runBlocking { // this: CoroutineScope
        launch { // launch a new coroutine and continue
            MyMediaPlayer.allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
            println("Work done")
        }
    }
}