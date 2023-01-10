package com.example.musicplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.MainActivity
import com.example.musicplayer.ModifyAlbumInfoActivity
import com.example.musicplayer.R
import com.example.musicplayer.SelectedAlbumActivity
import com.example.musicplayer.adapters.Albums
import com.example.musicplayer.classes.Album
import com.example.musicplayer.classes.MyMediaPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumsFragment : Fragment(), Albums.OnAlbumsListener, SearchView.OnQueryTextListener {
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Albums
    private lateinit var searchView : SearchView
    private val mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_albums, container, false)
        adapter = Albums(MyMediaPlayer.allAlbums,requireContext(),this)

        searchView = view.findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        menuRecyclerView = view.findViewById(R.id.menu_album_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(context)
        menuRecyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
        searchView.setQuery("",false)

        CoroutineScope(Dispatchers.Main).launch {
            (activity as MainActivity).generateAlbums(adapter)
        }
        mediaPlayer.setOnCompletionListener { (activity as MainActivity).playNextSong() }
        (activity?.findViewById(R.id.next) as ImageView).setOnClickListener { (activity as MainActivity).playNextSong() }
        (activity?.findViewById(R.id.previous) as ImageView).setOnClickListener { (activity as MainActivity).playPreviousSong() }
    }

    override fun onAlbumClick(position: Int) {
        val intent = Intent(context, SelectedAlbumActivity::class.java)
        val album = adapter.allAlbums[position]
        val globalPosition = MyMediaPlayer.allAlbums.indexOf(album)

        intent.putExtra("POSITION", globalPosition)

        startActivity(intent)
    }

    override fun onAlbumLongClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_album_menu)

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_shortcuts)?.setOnClickListener {
            (activity as MainActivity).addSelectedShortcut(adapter.allAlbums[position], (activity as MainActivity).shortcutAdapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_album)?.setOnClickListener{
            val intent = Intent(requireContext(),ModifyAlbumInfoActivity::class.java)
            val globalPosition = MyMediaPlayer.allAlbums.indexOf(adapter.allAlbums[position])
            intent.putExtra("POS",globalPosition)
            startActivity(intent)
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
                val list = ArrayList<Album>()

                if(p0 == ""){
                    adapter.allAlbums = MyMediaPlayer.allAlbums
                } else {
                    for (album: Album in MyMediaPlayer.allAlbums) {
                        if ((album.albumName.lowercase().contains(p0.lowercase())) || (album.artist.lowercase().contains(p0.lowercase()))) {
                            list.add(album)
                        }
                    }
                    if (list.size > 0) {
                        adapter.allAlbums = list
                    } else {
                        adapter.allAlbums = ArrayList<Album>()
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