package com.example.musicplayer

import android.content.res.Resources
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.musicplayer.adapters.SetDataVpAdapter
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.example.musicplayer.fragments.SetMusicsFragment
import com.example.musicplayer.fragments.SetPlaylistsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SetDataActivity : Tools() {
    private lateinit var tabLayout: TabLayout

    private val fragmentList = ArrayList<Fragment>(
        arrayListOf(
            SetMusicsFragment(),
            SetPlaylistsFragment(),
        )
    )

    var currentFragmentPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_data)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager.adapter = SetDataVpAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, index ->
            tab.text = when (index) {
                0 -> {
                    resources.getString(R.string.musics)
                }
                1 -> {
                    resources.getString(R.string.playlists)
                }
                else -> {
                    throw Resources.NotFoundException("Position not found")
                }
            }
        }.attach()

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener { goToPreviousStep() }
        findViewById<ImageView>(R.id.forward_arrow).setOnClickListener { goToNextStep() }

        findViewById<Button>(R.id.validate_selection).setOnClickListener { onValidateButtonClick() }
        findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
    }

    private fun goToPreviousStep() {
        if (currentFragmentPos != 0) {
            tabLayout.selectTab(tabLayout.getTabAt(currentFragmentPos - 1))
        }
    }

    private fun goToNextStep() {
        if (currentFragmentPos != (fragmentList.size - 1)) {
            tabLayout.selectTab(tabLayout.getTabAt(currentFragmentPos + 1))
        }
    }

    private fun onValidateButtonClick(){
        if (SetMusicsFragment.correctMusicFileSelected && SetPlaylistsFragment.correctPlaylistFileSelected){
            // On change d'abord nos musiques :
            for (music in SetMusicsFragment.allMusics) {
                MyMediaPlayer.allMusics.find { File(it.path).name == File(music.path).name }?.apply {
                    name = music.name
                    albumCover = music.albumCover
                    album = music.album
                    artist = music.artist
                }
            }
            MyMediaPlayer.allPlaylists = SetPlaylistsFragment.allPlaylists
            val songsToDelete = ArrayList<Music>()
            for (playlist in MyMediaPlayer.allPlaylists) {
                for (music in playlist.musicList) {
                    val correspondingSong = MyMediaPlayer.allMusics.find { File(it.path).name == File(music.path).name }
                    if (correspondingSong == null) {
                        songsToDelete.add(music)
                    } else {
                        music.apply {
                            name = correspondingSong.name
                            albumCover = correspondingSong.albumCover
                            album = correspondingSong.album
                            artist = correspondingSong.artist
                        }
                    }
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                writeAllPlaylists()
                writeAllMusics()
            }
            MyMediaPlayer.dataWasChanged = true
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this,getString(R.string.missing_correct_files), Toast.LENGTH_SHORT).show()
        }
    }
}