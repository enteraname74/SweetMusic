package com.example.musicplayer

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musicplayer.fragments.PlaylistsFragment
import com.example.musicplayer.fragments.AlbumsFragment
import com.example.musicplayer.fragments.ArtistsFragment
import com.example.musicplayer.fragments.MusicsFragment

class VpAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> { MusicsFragment() }
            1 -> { PlaylistsFragment() }
            2 -> { AlbumsFragment() }
            3 -> { ArtistsFragment() }
            else -> { throw Resources.NotFoundException("Position not found")}
        }
    }
}