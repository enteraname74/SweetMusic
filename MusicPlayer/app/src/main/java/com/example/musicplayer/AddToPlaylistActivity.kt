package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.PlaylistsSelection
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddToPlaylistActivity : Tools(), PlaylistsSelection.OnPlaylistListener {
    private var playlists = ArrayList<Playlist>()
    private lateinit var adapter : PlaylistsSelection
    private lateinit var selectedMusic : Music
    private var isFavoritePlaylistSelected = false
    private var selectedPlaylists = HashMap<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_to_playlist)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        val position = intent.getSerializableExtra("POSITION") as Int
        selectedMusic = MyMediaPlayer.allMusics[position]

        for (playlist in MyMediaPlayer.allPlaylists){
            if (playlist.musicList.find { it.path == selectedMusic.path } == null) {
                playlists.add(playlist)
            }
        }
        Log.d("ADD TO", playlists.size.toString())

        adapter = PlaylistsSelection(playlists, selectedPlaylists, this, this)
        val menuRecyclerView = findViewById<RecyclerView>(R.id.menu_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuRecyclerView.adapter = adapter

        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
        findViewById<Button>(R.id.validate_selection).setOnClickListener { validateSelection() }
    }

    override fun onPlaylistClick(position: Int) {
        val selectedPlaylist = playlists[position]

        if (selectedPlaylist.isFavoriteList) {
            isFavoritePlaylistSelected = isFavoritePlaylistSelected != true
        }

        if (position in selectedPlaylists.keys){
            selectedPlaylists.remove(position)
        } else {
            selectedPlaylists[position] = selectedPlaylist.listName
        }
        Log.d("ADD TO", selectedPlaylists.toString())
        adapter.notifyDataSetChanged()
    }

    private fun validateSelection() {
        if (isFavoritePlaylistSelected) {
            if (MyMediaPlayer.currentIndex != -1){
                MyMediaPlayer.currentPlaylist.find { it.path == selectedMusic.path }?.favorite = true
            }
            selectedMusic.favorite = true
            MyMediaPlayer.allMusics.find { it.path == selectedMusic.path }?.favorite = true
        }
        for (playlistPosition in selectedPlaylists.keys) {
            playlists[playlistPosition].musicList.add(selectedMusic)
        }
        CoroutineScope(Dispatchers.IO).launch { writeAllPlaylists() }
        finish()
    }
}