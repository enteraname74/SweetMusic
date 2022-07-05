package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectedPlaylistActivity : AppCompatActivity(), MusicList.OnMusicListener {
    private var menuRecyclerView : RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_playlist)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        val playlist = intent.getSerializableExtra("LIST") as Playlist

        menuRecyclerView?.layoutManager = LinearLayoutManager(this)
        menuRecyclerView?.adapter = MusicList(playlist.musicList,applicationContext,this)

        val playlistName = findViewById<TextView>(R.id.playlist_name)
        playlistName?.text = playlist.listName
    }

    override fun onMusicClick(position: Int) {
        TODO("Not yet implemented")
    }
}