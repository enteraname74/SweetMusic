package com.example.musicplayer.adapters

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musicplayer.fragments.SetMusicsFragment
import com.example.musicplayer.fragments.SetPlaylistsFragment

class SetDataVpAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> { SetMusicsFragment() }
            1 -> { SetPlaylistsFragment() }
            else -> { throw Resources.NotFoundException("Position not found")}
        }
    }
}