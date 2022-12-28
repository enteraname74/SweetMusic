package com.example.musicplayer

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.FolderListSelection
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SelectFoldersActivity : Tools(), FolderListSelection.OnFolderListener {
    private lateinit var adapter : FolderListSelection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_folders)

        val recyclerView = findViewById<RecyclerView>(R.id.menu_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FolderListSelection(MyMediaPlayer.allFolders, this, this)
        println(MyMediaPlayer.allFolders.size)
        recyclerView.adapter = adapter

        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
        findViewById<Button>(R.id.validate_selection).setOnClickListener { validateSelectionAndQuit() }
    }

    override fun onFolderClick(position: Int) {
        MyMediaPlayer.allFolders[position].isUsedInApp = !MyMediaPlayer.allFolders[position].isUsedInApp
        adapter.notifyItemChanged(position)
    }

    private fun validateSelectionAndQuit() {
        val musicsToDelete = ArrayList<Music>()

        for (music in MyMediaPlayer.allMusics) {
            if (MyMediaPlayer.allFolders.find { it.path == File(music.path).parent }?.isUsedInApp == false) {
                musicsToDelete.add(music)
            }
        }

        for (music in MyMediaPlayer.allDeletedMusics) {
            if (MyMediaPlayer.allFolders.find { it.path == File(music.path).parent }?.isUsedInApp == false) {
                musicsToDelete.add(music)
            }
        }

        for (music in musicsToDelete) {
            definitelyRemoveMusicFromApp(music)
        }

        CoroutineScope(Dispatchers.IO).launch {
            writePlaylistsToFile()
            writeAllMusics()
            writeAllDeletedSong()
            writeAllFolders()
        }
        finish()
    }
}