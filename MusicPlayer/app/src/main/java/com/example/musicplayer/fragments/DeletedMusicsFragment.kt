package com.example.musicplayer.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.adapters.NewMusicsList
import com.example.musicplayer.classes.MyMediaPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class DeletedMusicsFragment : Fragment(), NewMusicsList.OnMusicListener {
    private lateinit var adapter : NewMusicsList
    private lateinit  var menuRecyclerView : RecyclerView
    private val saveAllDeletedFiles = "allDeleted.musics"
    private val saveAllMusicsFile = "allMusics.musics"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NewMusicsList(MyMediaPlayer.allDeletedMusics,this, requireContext())
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

    override fun onMusicClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_find_new_songs)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<ImageView>(R.id.action_img)?.setImageResource(R.drawable.ic_baseline_add_24)
        bottomSheetDialog.findViewById<TextView>(R.id.action_text)?.text = getString(R.string.retrieve_music)

        bottomSheetDialog.findViewById<LinearLayout>(R.id.action)?.setOnClickListener {
            val musicToRetrieve = adapter.musics[position]
            adapter.musics.removeAt(position)
            adapter.notifyItemRemoved(position)

            MyMediaPlayer.allMusics.add(0, musicToRetrieve)
            CoroutineScope(Dispatchers.IO).launch { writeAllDeletedSong() }
            CoroutineScope(Dispatchers.IO).launch { writeAllMusicsToFile() }

            Toast.makeText(
                context,
                resources.getString(R.string.retrieved_music),
                Toast.LENGTH_SHORT
            ).show()
            bottomSheetDialog.dismiss()
        }
    }
}