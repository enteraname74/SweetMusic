package com.example.musicplayer.adapters

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musicplayer.fragments.PlaylistsFragment
import com.example.musicplayer.fragments.AlbumsFragment
import com.example.musicplayer.fragments.ArtistsFragment
import com.example.musicplayer.fragments.MusicsFragment

class VpAdapter(fragmentActivity: FragmentActivity, private val isForShortcut : Boolean) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> { MusicsFragment.newInstance(isForShortcut) }
            1 -> { PlaylistsFragment.newInstance(isForShortcut) }
            2 -> { AlbumsFragment.newInstance(isForShortcut) }
            3 -> { ArtistsFragment.newInstance(isForShortcut) }
            else -> { throw Resources.NotFoundException("Position not found")}
        }
    }
}