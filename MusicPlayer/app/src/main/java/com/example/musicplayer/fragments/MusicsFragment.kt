package com.example.musicplayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.Music
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.classes.MyMediaPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import java.io.*


private const val SHORTCUT_PARA = "shortcut"

class MusicsFragment : Fragment(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {
    private val saveAllDeletedFiles = "allDeleted.musics"
    private val savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var adapter : MusicList
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var searchView : SearchView
    private var searchIsOn = false
    private val mediaPlayer = MyMediaPlayer.getInstance

    private var shortcutUsage: Boolean? = null

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            shortcutUsage = it.getBoolean(SHORTCUT_PARA)
        }

        adapter = MusicList(ArrayList<Music>(), "",requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_musics, container, false)

        searchView = view.findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        menuRecyclerView = view.findViewById(R.id.menu_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(view.context)
        menuRecyclerView.adapter = adapter

        context?.registerReceiver(broadcastReceiver, IntentFilter("BROADCAST"))

        return view
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
        Log.d("RESUME FRAG","")
        // Si on est dans la barre e recherche, on ne met pas tout de suite à jour les musiques pour rester dans la barre :
        if(!searchIsOn){
            adapter.musics = MyMediaPlayer.allMusics
        }
        if (shortcutUsage == false) {
            if (MyMediaPlayer.dataWasChanged) {
                // Si on a mis à jour toutes nos données, il faut qu'on change nos musiques :
                adapter.musics = MyMediaPlayer.allMusics
                MyMediaPlayer.dataWasChanged = false
            }
            adapter.notifyDataSetChanged()

            (activity?.findViewById(R.id.next) as ImageView).setOnClickListener { (activity as MainActivity).playNextSong() }
            (activity?.findViewById(R.id.previous) as ImageView).setOnClickListener { (activity as MainActivity).playPreviousSong() }

            mediaPlayer.setOnCompletionListener {
                Log.d("MUSIC FRAGMENT", "WILL PLAY NEXT")
                (activity as MainActivity).playNextSong(adapter)
            }

            CoroutineScope(Dispatchers.Main).launch { (activity as MainActivity).verifiyAllMusics(adapter) }
        } else {
            CoroutineScope(Dispatchers.Main).launch { (activity as CreateShortcutActivity).verifiyAllMusics(adapter) }
        }
    }

    override fun onMusicClick(position: Int) {
        if (shortcutUsage == false) {
            (activity as MainActivity).musicClicked(requireContext(), adapter, position, "Main")
        } else {
            (activity as CreateShortcutActivity).addSelectedShortcut(adapter.musics[position])
        }
    }

    override fun onLongMusicClick(position: Int) {
        if (shortcutUsage == false) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_menu)
            bottomSheetDialog.show()

            bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_a_playlist)
                ?.setOnClickListener {
                    (activity as MainActivity).bottomSheetAddTo(position, requireContext(), adapter)
                    bottomSheetDialog.dismiss()
                }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
                (activity as MainActivity).bottomSheetRemoveFromApp(
                    adapter,
                    position,
                    (activity as MainActivity).sheetBehavior,
                    requireContext()
                )
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_music)?.setOnClickListener {
                (activity as MainActivity).bottomSheetModifyMusic(
                    requireContext(),
                    position,
                    adapter
                )
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.play_next)?.setOnClickListener {
                (activity as MainActivity).bottomSheetPlayNext(adapter, position)
                bottomSheetDialog.dismiss()
            }
        }
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return manageSearchBarEvents(p0)
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        return manageSearchBarEvents(p0)
    }

    private fun manageSearchBarEvents(p0 : String?) : Boolean {
        try {
            if (p0 != null) {
                val list = ArrayList<Music>()

                if(p0 == ""){
                    searchIsOn = false
                    adapter.musics = MyMediaPlayer.allMusics
                } else {
                    searchIsOn = true
                    for (music: Music in MyMediaPlayer.allMusics) {
                        if ((music.name.lowercase().contains(p0.lowercase())) || (music.album.lowercase().contains(p0.lowercase())) || (music.artist.lowercase().contains(p0.lowercase()))){
                            list.add(music)
                        }
                    }

                    if (list.size > 0) {
                        adapter.musics = list
                    } else {
                        adapter.musics = ArrayList<Music>()
                    }
                }
                adapter.notifyDataSetChanged()
            }
        } catch (error : Error){
            Log.d("ERROR",error.toString())
        }
        return true
    }

    companion object {
        @JvmStatic
        fun newInstance(isForShortcut: Boolean) =
            MusicsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(SHORTCUT_PARA, isForShortcut)
                }
            }
    }
}