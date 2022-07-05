package com.example.musicplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PlaylistsMenuActivity : AppCompatActivity(), Playlists.OnPlaylistsListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPlaylistClick(position: Int) {
        TODO("Not yet implemented")
    }
}
