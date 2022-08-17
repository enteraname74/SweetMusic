package com.example.musicplayer

import android.content.res.Resources
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class VpAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        Log.d("position",position.toString())
        return when(position){
            0 -> { MusicsFragment() }
            1 -> { PlaylistsFragment() }
            2 -> { AlbumFragment() }
            3 -> { ArtistsFragment() }
            else -> { throw Resources.NotFoundException("Position not found")}
        }
    }
}