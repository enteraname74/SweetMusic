package com.example.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class AlbumFragment : Fragment(), Albums.OnAlbumsListener {
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Albums
    private var albums = ArrayList<Album>()
    private val mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val copiedMusics = ArrayList(MyMediaPlayer.allMusics.map { it.copy() })

        var currentAlbum : Album
        // Trions d'abord notre liste par album et artiste :
        copiedMusics.sortWith(compareBy<Music> {it.album}.thenBy { it.artist })
        currentAlbum = Album(copiedMusics[0].album,ArrayList<Music>(),copiedMusics[0].albumCover,copiedMusics[0].artist)

        for(music in copiedMusics){
            if (music.album == currentAlbum.albumName && music.artist == currentAlbum.artist){
                currentAlbum.albumList.add(music)
            } else {
                // On passe à un autre album :
                // On ajoute d'abord notre album à notre liste :
                albums.add(currentAlbum)
                // On change ensuite l'album actuelle
                currentAlbum = Album(music.album,ArrayList<Music>(),music.albumCover,music.artist)
            }
        }

        adapter = Albums(albums,context as Context,this)

        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)

        menuRecyclerView = view.findViewById(R.id.menu_playlist_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(context)
        menuRecyclerView.adapter = adapter

        val nextButton : ImageView = activity?.findViewById(R.id.next) as ImageView
        val previousButton : ImageView = activity?.findViewById(R.id.previous) as ImageView

        nextButton.setOnClickListener { playNextSong() }
        previousButton.setOnClickListener { playPreviousSong() }

        return view
    }

    override fun onResume() {
        super.onResume()
    }

    private fun playNextSong(){
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        playMusic()
    }

    private fun playMusic(){
        mediaPlayer.reset()
        try {
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()

            val pausePlay = activity?.findViewById<ImageView>(R.id.pause_play)
            val songTitleInfo = activity?.findViewById<TextView>(R.id.song_title_info)
            val albumCoverInfo = activity?.findViewById<ImageView>(R.id.album_cover_info)

            if (currentSong.albumCover != null){
                // Passons d'abord notre byteArray en bitmap :
                val bytes = currentSong.albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                albumCoverInfo?.setImageBitmap(bitmap)
            } else {
                albumCoverInfo?.setImageResource(R.drawable.michael)
            }

            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            Log.d("ERROR","")
            e.printStackTrace()
        }
    }

    override fun onAlbumClick(position: Int) {
        TODO("Not yet implemented")
    }
}