package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.MusicListSelection
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools


class MusicSelectionActivity : Tools(), MusicListSelection.OnMusicListener, SearchView.OnQueryTextListener {
    private lateinit var adapter : MusicListSelection
    private lateinit var searchView : SearchView
    private var searchIsOn = false
    private var selectedMusicsPositions = ArrayList<Int>()
    private lateinit var menuRecyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_selection)

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        menuRecyclerView = findViewById(R.id.all_songs_list)

        adapter = MusicListSelection(MyMediaPlayer.allMusics,selectedMusicsPositions,applicationContext,this)

        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuRecyclerView.adapter = adapter

        val validateButton = findViewById<Button>(R.id.validate)
        val cancelButton = findViewById<Button>(R.id.cancel)
        validateButton.setOnClickListener{ onValidateButtonClick() }
        cancelButton.setOnClickListener{ onCancelButtonClick() }
    }

    override fun onMusicClick(position: Int) {

        val selectedMusic = adapter.musics[position]
        val globalPosition = MyMediaPlayer.allMusics.indexOf(selectedMusic)

        if (globalPosition in selectedMusicsPositions){
            selectedMusicsPositions.remove(globalPosition)
        } else {
            selectedMusicsPositions.add(globalPosition)
        }

        adapter.notifyDataSetChanged()
    }

    private fun onValidateButtonClick(){
        val returnIntent = Intent()
        returnIntent.putExtra("addedSongs", selectedMusicsPositions)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun onCancelButtonClick(){
        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
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
                val list = ArrayList<Music>()

                if(p0 == ""){
                    searchIsOn = false
                    adapter.musics = MyMediaPlayer.allMusics
                } else {
                    searchIsOn = true
                    for (music: Music in MyMediaPlayer.allMusics) {
                        if ((music.name.lowercase().contains(p0.lowercase())) || (music.album.lowercase().contains(p0.lowercase())) || (music.artist.lowercase().contains(p0.lowercase()))){
                            list.add(music)
                        }
                    }

                    if (list.size > 0) {
                        adapter.musics = list
                    } else {
                        adapter.musics = ArrayList<Music>()
                    }
                }
                adapter.notifyDataSetChanged()
            }
        } catch (error : Error){
            Log.d("ERROR",error.toString())
        }
        return true
    }
}