package com.example.musicplayer

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

class PlaylistsFragment : Fragment(), Playlists.OnPlaylistsListener {

    private val savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var adapter : Playlists
    private var playlists = ArrayList<Playlist>()
    private val mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlists = MyMediaPlayer.allPlaylists

        adapter = Playlists(playlists,context as Context,this,R.layout.playlist_file_linear)
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)

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
        playlists = MyMediaPlayer.allPlaylists
        adapter.allPlaylists = playlists
        if (MyMediaPlayer.dataWasChanged){
            // Si on a mis à jour toutes nos données, il faut qu'on change nos musiques :
            playlists = MyMediaPlayer.allPlaylists
            MyMediaPlayer.dataWasChanged = false
        }
        adapter.notifyDataSetChanged()
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onPlaylistClick(position: Int) {
        Log.d("PLAYLIST POSITION", position.toString())

        val intent = Intent(context,SelectedPlaylistActivity::class.java)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun addPlaylist(){
        val builder = AlertDialog.Builder(context as Context)
        builder.setTitle("Create playlist")
        // L'entrée :
        val inputText = EditText(context)
        // Le type d'entrée :
        inputText.setInputType(InputType.TYPE_CLASS_TEXT)
        builder.setView(inputText)
        // Les boutons :
        // Si on valide la création, on crée notre playlist :
        builder.setPositiveButton("OK", DialogInterface.OnClickListener{ _, _ ->
            /* Afin de créer une playlist, nous devons vérifier les critères suivants :
                - Le nom n'est pas vide ou ne commence pas avec un espace (au cas où on a qu'un espace en guise de nom
                - Le nom n'est pas déjà prit
                - Le nom n'est pas celui de la playlist principale ("Main")
             */

            if (inputText.text.toString() != "" && !(inputText.text.toString().startsWith(" ")) && (playlists.find { it.listName == inputText.text.toString().trim() } == null) && (inputText.text.toString() != "Main")) {
                val newPlaylist = Playlist(inputText.text.toString(), ArrayList(),null)
                playlists.add(newPlaylist)
                adapter.allPlaylists = playlists
                GlobalScope.launch(Dispatchers.IO){
                    launch{writePlaylistsToFile(savePlaylistsFile, playlists)}
                }

                menuRecyclerView.layoutManager = LinearLayoutManager(context)
                menuRecyclerView.adapter = adapter
            } else {
                Toast.makeText(context,"A title must be set correctly !", Toast.LENGTH_SHORT).show()
            }
        })
        // Si on annule la création de la playlist, on quitte la fenêtre
        builder.setNegativeButton("CANCEL", DialogInterface.OnClickListener{ dialogInterface, i ->
            dialogInterface.cancel()
        })

        builder.show()

        Log.d("playlist ajouté","")
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
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        playMusic()
    }

    private fun playMusic(){
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
                albumCoverInfo?.setImageResource(R.drawable.michael)
            }

            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            Log.d("ERROR","")
            e.printStackTrace()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d("Inside Playlist","")
        return when(item.itemId){
            10 -> {
                if (playlists[item.groupId].isFavoriteList){
                    Toast.makeText(context,"You can't delete the Favorites playlist",Toast.LENGTH_SHORT).show()
                } else {
                    playlists.removeAt(item.groupId)
                    adapter.allPlaylists = playlists
                    adapter.notifyItemRemoved(item.groupId)

                    writePlaylistsToFile(savePlaylistsFile,playlists)
                    Toast.makeText(context,"Suppressions de la playlist",Toast.LENGTH_SHORT).show()
                }
                true
            }
            11 -> {
                val intent = Intent(context,ModifyPlaylistInfoActivity::class.java)
                intent.putExtra("POSITION",item.groupId)
                resultLauncher.launch(intent)
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // On récupère les playlists avec la modification effectuée :
            playlists = MyMediaPlayer.allPlaylists
            adapter.allPlaylists = playlists
        }
    }
}