package com.example.musicplayer.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.DeletedMusicList
import com.example.musicplayer.ModifyMusicInfoActivity
import com.example.musicplayer.MyMediaPlayer
import com.example.musicplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class DeletedMusicsFragment : Fragment(), DeletedMusicList.OnMusicListener {
    private lateinit var adapter : DeletedMusicList
    private lateinit  var menuRecyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = DeletedMusicList(MyMediaPlayer.allDeletedMusics,"deletedSongs", activity?.applicationContext as Context, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_deleted_musics, container, false)

        menuRecyclerView = view.findViewById(R.id.menu_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(view.context)
        menuRecyclerView.adapter = adapter

        return view
    }

    override fun onMusicClick(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d("music fragment",item.itemId.toString())

        return when (item.itemId) {
            0 -> {
                val musicToRetrieve = adapter.musics[item.groupId]
                adapter.musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)

                MyMediaPlayer.allMusics.add(0, musicToRetrieve)

                Toast.makeText(
                    context,
                    resources.getString(R.string.retrieved_music),
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
}