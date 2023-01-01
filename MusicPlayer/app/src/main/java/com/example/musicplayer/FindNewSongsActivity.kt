package com.example.musicplayer

import android.content.res.Resources
import android.os.Bundle
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.example.musicplayer.adapters.FindNewSongsAdapter
import com.example.musicplayer.classes.Tools
import com.google.android.material.tabs.TabLayoutMediator

class FindNewSongsActivity : Tools() {
    private lateinit var tabLayout : com.google.android.material.tabs.TabLayout
    private lateinit var viewPager : ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_new_songs)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        viewPager.adapter = FindNewSongsAdapter(this)

        TabLayoutMediator(tabLayout, viewPager){tab, index ->
            tab.text = when(index){
                0 -> {resources.getString(R.string.new_songs)}
                1 -> {resources.getString(R.string.deleted_songs)}
                else -> { throw Resources.NotFoundException("Position not found")}
            }
        }.attach()

        val quitActivityButton = findViewById<ImageView>(R.id.quit_activity)
        quitActivityButton.setOnClickListener { finish() }
    }
}