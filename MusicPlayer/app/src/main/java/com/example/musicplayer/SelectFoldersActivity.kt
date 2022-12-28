package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.musicplayer.classes.MyMediaPlayer
import java.io.File

class SelectFoldersActivity : AppCompatActivity() {
    private lateinit var folderList : ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_folders)

        retrieveAllFoldersUsed()

    }

    private fun retrieveAllFoldersUsed(){
        for (music in MyMediaPlayer.allMusics) {
            val musicFolder = File(MyMediaPlayer.allMusics[0].path).parent
            if (musicFolder as String !in folderList) {
                folderList.add(musicFolder)
            }
        }
    }
}