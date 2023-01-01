package com.example.musicplayer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.musicplayer.classes.Tools

class SettingsActivity : Tools() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.folders).setOnClickListener { launchFolderActivity() }
    }

    private fun launchFolderActivity() {
        val intent = Intent(this, SelectFoldersActivity::class.java)
        startActivity(intent)
    }
}