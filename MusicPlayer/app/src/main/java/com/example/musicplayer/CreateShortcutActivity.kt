package com.example.musicplayer

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.example.musicplayer.adapters.VpAdapter
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class CreateShortcutActivity : Tools() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_shortcut)

        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)

        viewPager.adapter = VpAdapter(this, true)

        TabLayoutMediator(tabLayout, viewPager){tab, index ->
            tab.text = when(index){
                0 -> {resources.getString(R.string.musics)}
                1 -> {resources.getString(R.string.playlists)}
                2 -> {resources.getString(R.string.albums)}
                3 -> {resources.getString(R.string.artists)}
                else -> { throw Resources.NotFoundException("Position not found")}
            }
        }.attach()
    }

    fun addSelectedShortcut(element : Any) {
        MyMediaPlayer.allShortcuts.shortcutsList.add(element)
        finish()
    }
}