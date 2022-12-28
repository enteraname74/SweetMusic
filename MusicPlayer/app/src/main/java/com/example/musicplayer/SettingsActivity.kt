package com.example.musicplayer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.folders).setOnClickListener { launchFolderActivity() }
    }

    private fun launchFolderActivity() {
        val intent = Intent(this, SelectFoldersActivity::class.java)
        startActivity(intent)
    }
}