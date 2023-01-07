package com.example.musicplayer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.ShortcutList
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Shortcuts

class ShortcutsActivity : AppCompatActivity() {
    private lateinit var adapter : ShortcutList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shorcuts)
        val shortcutPanel = findViewById<LinearLayout>(R.id.shortcuts)

        val recyclerView = findViewById<RecyclerView>(R.id.shortcut_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = ShortcutList(MyMediaPlayer.allShortcuts,this)
        recyclerView.adapter = adapter

        val switch : SwitchCompat = findViewById(R.id.enable_shortcuts)
        switch.setOnCheckedChangeListener { _, boolean ->
            if (boolean) {
                shortcutPanel.visibility = View.VISIBLE
            } else {
                shortcutPanel.visibility = View.GONE
            }
        }

        findViewById<ImageView>(R.id.add_shortcut).setOnClickListener { launchAddShortcutActivity() }
        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun launchAddShortcutActivity() {
        val intent = Intent(this,CreateShortcutActivity::class.java)
        startActivity(intent)
    }
}