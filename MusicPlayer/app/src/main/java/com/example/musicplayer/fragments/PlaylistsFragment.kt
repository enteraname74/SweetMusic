package com.example.musicplayer.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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

class PlaylistsFragment : Fragment(), Playlists.OnPlaylistsListener {

    private val savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Playlists
    private val mediaPlayer = MyMediaPlayer.getInstance

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
        addPlaylist?.setOnClickListener{ addPlaylist() }

        menuRecyclerView = view.findViewById(R.id.menu_playlist_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(context)
        menuRecyclerView.adapter = adapter

        val nextButton : ImageView = activity?.findViewById(R.id.next) as ImageView
        val previousButton : ImageView = activity?.findViewById(R.id.previous) as ImageView

        nextButton.setOnClickListener { playNextSong() }
        previousButton.setOnClickListener { playPreviousSong() }

        return view
    }

    override fun onResume() {
        super.onResume()

        adapter.allPlaylists = MyMediaPlayer.allPlaylists
        adapter.notifyDataSetChanged()
        mediaPlayer.setOnCompletionListener { (activity as MainActivity).playNextSong() }
    }

    override fun onPlaylistClick(position: Int) {
        Log.d("PLAYLIST POSITION", position.toString())

        val intent = Intent(context, SelectedPlaylistActivity::class.java)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    override fun onPlayListLongClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_playlist_menu)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
            (activity as MainActivity).bottomSheetRemovePlaylist(position,adapter, requireContext())
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_playlist)?.setOnClickListener {
            (activity as MainActivity).bottomSheetModifyPlaylist(requireContext(),position)
            bottomSheetDialog.dismiss()
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
                    writePlaylistsToFile(
                        savePlaylistsFile,
                        MyMediaPlayer.allPlaylists
                    )
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

    private fun writePlaylistsToFile(filename : String, content : ArrayList<Playlist>){
        MyMediaPlayer.allPlaylists = content
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write playlists",error.toString())
        }
    }

    private fun playNextSong(){
        if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex +=1
        }
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex ==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex -=1
        }
        playMusic()
    }

    private fun playMusic(){
        if (MyMediaPlayer.currentIndex != -1 && MyMediaPlayer.currentPlaylist.size != 0) {
            mediaPlayer.reset()
            try {
                val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
                mediaPlayer.setDataSource(currentSong.path)
                mediaPlayer.prepare()
                mediaPlayer.start()

                val pausePlay = activity?.findViewById<ImageView>(R.id.pause_play)
                val songTitleInfo = activity?.findViewById<TextView>(R.id.song_title_info)
                val albumCoverInfo = activity?.findViewById<ImageView>(R.id.album_cover_info)

                if (currentSong.albumCover != null){
                    // Passons d'abord notre byteArray en bitmap :
                    val bytes = currentSong.albumCover
                    var bitmap: Bitmap? = null
                    if (bytes != null && bytes.isNotEmpty()) {
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    albumCoverInfo?.setImageBitmap(bitmap)
                } else {
                    albumCoverInfo?.setImageResource(R.drawable.ic_saxophone_svg)
                }

                pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            } catch (e: IndexOutOfBoundsException) {
                Log.d("ERROR","")
                e.printStackTrace()
            }
        }
    }
}