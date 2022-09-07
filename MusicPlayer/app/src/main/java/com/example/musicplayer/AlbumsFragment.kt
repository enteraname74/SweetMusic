package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class AlbumsFragment : Fragment(), Albums.OnAlbumsListener, SearchView.OnQueryTextListener {
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Albums
    private lateinit var searchView : SearchView
    private val mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_albums, container, false)
        adapter = Albums(MyMediaPlayer.allAlbums,context as Context,this@AlbumsFragment)

        searchView = view.findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        menuRecyclerView = view.findViewById(R.id.menu_album_recycler_view)
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
        searchView.clearFocus()
        Log.d("FRAG ALBUM RES","")

        CoroutineScope(Dispatchers.Main).launch {
            if (MyMediaPlayer.allMusics.size > 0) {
                val copiedMusics = ArrayList(MyMediaPlayer.allMusics.map { it.copy() })
                var currentAlbum: Album
                // Trions d'abord notre liste par album et artiste :
                copiedMusics.sortWith(compareBy<Music> { it.album }.thenBy { it.artist })
                currentAlbum = Album(
                    copiedMusics[0].album,
                    ArrayList<Music>(),
                    copiedMusics[0].albumCover,
                    copiedMusics[0].artist
                )
                // On vide nos albums pour mettre à jour ensuite ces derniers :
                MyMediaPlayer.allAlbums.clear()
                for (music in copiedMusics) {
                    if (music.album == currentAlbum.albumName && music.artist == currentAlbum.artist) {
                        currentAlbum.albumList.add(music)
                    } else {
                        // On passe à un autre album :
                        // On ajoute d'abord notre album à notre liste :
                        MyMediaPlayer.allAlbums.add(currentAlbum)
                        // On change ensuite l'album actuelle
                        currentAlbum =
                            Album(music.album, ArrayList<Music>(), music.albumCover, music.artist)
                        currentAlbum.albumList.add(music)
                    }
                }
                adapter.allAlbums = MyMediaPlayer.allAlbums
            }
            adapter.notifyDataSetChanged()
        }
        mediaPlayer.setOnCompletionListener { playNextSong() }
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
        val intent = Intent(context,SelectedAlbumActivity::class.java)
        val album = MyMediaPlayer.allAlbums[position]
        val globalPosition = MyMediaPlayer.allAlbums.indexOf(album)

        intent.putExtra("POSITION", globalPosition)

        startActivity(intent)
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return manageSearchBarEvents(p0)
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        return manageSearchBarEvents(p0)
    }

    private fun manageSearchBarEvents(p0 : String?) : Boolean {
        try {
            if (p0 != null) {
                val list = ArrayList<Album>()

                MyMediaPlayer.allAlbums = if(p0 == ""){
                    MyMediaPlayer.allAlbums
                } else {
                    for (album: Album in MyMediaPlayer.allAlbums) {
                        if ((album.albumName.lowercase().contains(p0.lowercase())) || (album.artist.lowercase().contains(p0.lowercase()))){
                            list.add(album)
                        }
                    }

                    if (list.size > 0) {
                        list
                    } else {
                        ArrayList<Album>()
                    }
                }
                adapter.allAlbums = MyMediaPlayer.allAlbums
                adapter.notifyDataSetChanged()
            }
        } catch (error : Error){
            Log.d("ERROR",error.toString())
        }
        return true
    }
}