package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class PlaylistsMenuActivity : AppCompatActivity(), Playlists.OnPlaylistsListener {
    private var menuRecyclerView : RecyclerView? = null
    private var playlists = ArrayList<Playlist>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists_menu)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        val noPlaylistsFound = findViewById<TextView>(R.id.no_playlists_found)

        val playlist = intent.getSerializableExtra("MAIN") as Playlist
        playlists.add(playlist)

        if (playlists.size != 0){
            menuRecyclerView?.visibility = View.VISIBLE
            noPlaylistsFound.visibility = View.GONE

            //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
            menuRecyclerView?.layoutManager = LinearLayoutManager(this)
            menuRecyclerView?.adapter = Playlists(playlists,applicationContext,this)
        } else {
            menuRecyclerView?.visibility = View.GONE
            noPlaylistsFound.visibility = View.VISIBLE
        }

    }

    override fun onPlaylistClick(position: Int) {
        Log.d("PLAYLIST POSITION", position.toString())

        val currentPlaylist = playlists[position]
        val intent = Intent(this@PlaylistsMenuActivity,SelectedPlaylistActivity::class.java)
        intent.putExtra("LIST",currentPlaylist)

        startActivity(intent)
    }
}
