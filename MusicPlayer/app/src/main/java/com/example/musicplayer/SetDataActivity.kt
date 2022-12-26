package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.IOException
import java.io.ObjectInputStream

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
            CoroutineScope(Dispatchers.IO).launch {
                //writeAllAsync(allMusics,allPlaylists)
            }
            MyMediaPlayer.dataWasChanged = true
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this,"Missing correct files !", Toast.LENGTH_SHORT).show()
        }
    }
}