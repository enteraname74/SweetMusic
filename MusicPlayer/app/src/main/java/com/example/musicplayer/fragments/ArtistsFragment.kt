package com.example.musicplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.classes.Artist
import com.example.musicplayer.adapters.Artists
import com.example.musicplayer.Music
import com.example.musicplayer.classes.MyMediaPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistsFragment : Fragment(), Artists.OnArtistsListener, SearchView.OnQueryTextListener {
    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var adapter: Artists
    private val mediaPlayer = MyMediaPlayer.getInstance
    private lateinit var searchView : SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_artists, container, false)
        adapter = Artists(MyMediaPlayer.allArtists, requireContext(), this)

        searchView = view.findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        menuRecyclerView = view.findViewById(R.id.menu_artist_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(context)
        menuRecyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
        searchView.setQuery("",false)

        CoroutineScope(Dispatchers.Main).launch {
            (activity as MainActivity).generateArtists(adapter)
        }
        mediaPlayer.setOnCompletionListener { (activity as MainActivity).playNextSong() }
        (activity?.findViewById(R.id.next) as ImageView).setOnClickListener { (activity as MainActivity).playNextSong() }
        (activity?.findViewById(R.id.previous) as ImageView).setOnClickListener { (activity as MainActivity).playPreviousSong() }
    }

    override fun onArtistClick(position: Int) {
        val intent = Intent(context, SelectedArtistActivity::class.java)
        val artist = adapter.allArtists[position]
        val globalPosition = MyMediaPlayer.allArtists.indexOf(artist)

        intent.putExtra("POSITION", globalPosition)

        startActivity(intent)
    }

    override fun onArtistLongClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_artist_menu)

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_shortcuts)?.setOnClickListener {
            (activity as MainActivity).addSelectedShortcut(adapter.allArtists[position], (activity as MainActivity).shortcutAdapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
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
                val list = ArrayList<Artist>()

                if(p0 == ""){
                    adapter.allArtists = MyMediaPlayer.allArtists
                } else {
                    for (artist: Artist in MyMediaPlayer.allArtists) {
                        if (artist.artistName.lowercase().contains(p0.lowercase())){
                            list.add(artist)
                        }
                    }
                    if (list.size > 0) {
                        adapter.allArtists = list
                    } else {
                        adapter.allArtists = ArrayList<Artist>()
                    }
                }
                adapter.notifyDataSetChanged()
            }
        } catch (error : Error){
            Log.d("ERROR",error.toString())
        }
        return true
    }
}