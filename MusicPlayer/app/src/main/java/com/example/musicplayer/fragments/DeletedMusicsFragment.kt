package com.example.musicplayer.fragments

import android.content.Context
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
import com.example.musicplayer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class DeletedMusicsFragment : Fragment() {
    private lateinit var adapter : DeletedMusicList
    private lateinit  var menuRecyclerView : RecyclerView
    private val saveAllDeletedFiles = "allDeleted.musics"
    private val saveAllMusicsFile = "allMusics.musics"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = DeletedMusicList(MyMediaPlayer.allDeletedMusics,"deletedSongs", activity?.applicationContext as Context)
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

    override fun onResume() {
        super.onResume()
        adapter.musics = MyMediaPlayer.allDeletedMusics
        adapter.notifyDataSetChanged()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d("deleted fragment",item.itemId.toString())

        return when (item.itemId) {
            20 -> {
                val musicToRetrieve = adapter.musics[item.groupId]
                adapter.musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)

                MyMediaPlayer.allMusics.add(0, musicToRetrieve)
                CoroutineScope(Dispatchers.IO).launch { writeAllDeletedSong() }
                CoroutineScope(Dispatchers.IO).launch { writeAllMusicsToFile() }

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

    private fun writeAllDeletedSong(){
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllDeletedFiles)))
            oos.writeObject(MyMediaPlayer.allDeletedMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write deleted",error.toString())
        }
    }

    private fun writeAllMusicsToFile(){
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllMusicsFile)))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write musics",error.toString())
        }
    }
}