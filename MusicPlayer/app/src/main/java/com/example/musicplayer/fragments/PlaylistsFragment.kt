package com.example.musicplayer.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.adapters.Playlists
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.Playlist
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

private const val SHORTCUT_PARA = "shortcut"

class PlaylistsFragment : Fragment(), Playlists.OnPlaylistsListener {
    private val savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Playlists
    private val mediaPlayer = MyMediaPlayer.getInstance

    private var shortcutUsage: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        adapter = Playlists(
            MyMediaPlayer.allPlaylists,requireContext(),this,
            R.layout.playlist_file_linear
        )

        // Mise en place du bouton de création de playlist :
        val addPlaylist = view.findViewById<ImageView>(R.id.add_playlist)
        if (shortcutUsage == false) {
            addPlaylist?.setOnClickListener{ addPlaylist() }
        } else {
            addPlaylist.visibility = View.GONE
        }

        menuRecyclerView = view.findViewById(R.id.menu_playlist_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(context)
        menuRecyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()

        adapter.allPlaylists = MyMediaPlayer.allPlaylists
        adapter.notifyDataSetChanged()
        if (shortcutUsage == false) {
            mediaPlayer.setOnCompletionListener { (activity as MainActivity).playNextSong() }
            (activity?.findViewById(R.id.next) as ImageView).setOnClickListener { (activity as MainActivity).playNextSong() }
            (activity?.findViewById(R.id.previous) as ImageView).setOnClickListener { (activity as MainActivity).playPreviousSong() }
        }
    }

    override fun onPlaylistClick(position: Int) {
        if (shortcutUsage == false) {
            val intent = Intent(context, SelectedPlaylistActivity::class.java)
            intent.putExtra("POSITION", position)
            startActivity(intent)
        } else {
            (activity as CreateShortcutActivity).addSelectedShortcut(adapter.allPlaylists[position])
        }
    }

    override fun onPlayListLongClick(position: Int) {
        if (shortcutUsage == false) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_playlist_menu)
            bottomSheetDialog.show()

            if (MyMediaPlayer.allPlaylists[position].isFavoriteList) {
                bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.visibility = View.GONE
            } else {
                bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
                    (activity as MainActivity).bottomSheetRemovePlaylist(
                        position,
                        adapter,
                        requireContext()
                    )
                    bottomSheetDialog.dismiss()
                }
            }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_playlist)?.setOnClickListener {
                (activity as MainActivity).bottomSheetModifyPlaylist(requireContext(), position)
                bottomSheetDialog.dismiss()
            }
        }
    }

    private fun addPlaylist(){
        val builder = AlertDialog.Builder(context as Context, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.create_playlist))
        // L'entrée :
        val inputText = EditText(context)
        // Le type d'entrée :
        inputText.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(inputText)
        // Les boutons :
        // Si on valide la création, on crée notre playlist :
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            /* Afin de créer une playlist, nous devons vérifier les critères suivants :
                - Le nom n'est pas vide ou ne commence pas avec un espace (au cas où on a qu'un espace en guise de nom
                - Le nom n'est pas déjà prit
                - Le nom n'est pas celui de la playlist principale ("Main")
             */

            if (inputText.text.toString() != "" && !(inputText.text.toString().startsWith(" ")) && (MyMediaPlayer.allPlaylists.find { it.listName == inputText.text.toString().trim() } == null) && (inputText.text.toString() != "Main")) {
                val newPlaylist = Playlist(inputText.text.toString(), ArrayList(), null)
                MyMediaPlayer.allPlaylists.add(newPlaylist)
                adapter.allPlaylists = MyMediaPlayer.allPlaylists
                CoroutineScope(Dispatchers.IO).launch {
                    (activity as MainActivity).writePlaylistsToFile()
                }

                menuRecyclerView.layoutManager = LinearLayoutManager(context)
                menuRecyclerView.adapter = adapter
            } else {
                Toast.makeText(context, getString(R.string.a_title_must_be_set_correctly), Toast.LENGTH_SHORT)
                    .show()
            }
        }
        // Si on annule la création de la playlist, on quitte la fenêtre
        builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        builder.show()
    }

    companion object {
        @JvmStatic
        fun newInstance(isForShortcut: Boolean) =
            PlaylistsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(SHORTCUT_PARA, isForShortcut)
                }
            }
    }
}