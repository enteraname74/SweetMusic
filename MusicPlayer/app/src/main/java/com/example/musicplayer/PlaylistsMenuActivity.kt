package com.example.musicplayer

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

class PlaylistsMenuActivity : Tools(), Playlists.OnPlaylistsListener {
    private var menuRecyclerView : RecyclerView? = null
    private lateinit var adapter : Playlists
    private lateinit var noPlaylistsFound : TextView
    private var playlists = ArrayList<Playlist>()
    private val playlistsNames = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists_menu)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        noPlaylistsFound = findViewById<TextView>(R.id.no_playlists_found)

        playlists = MyMediaPlayer.allPlaylists

        // On récupère une liste des noms des playlists :
        for (element in playlists){
            playlistsNames.add(element.listName)
        }

        adapter = Playlists(playlists,applicationContext,this)

        if (playlists.size != 0){
            menuRecyclerView?.visibility = View.VISIBLE
            noPlaylistsFound.visibility = View.GONE

            //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
            menuRecyclerView?.layoutManager = GridLayoutManager(this,2)
            menuRecyclerView?.adapter = adapter
        } else {
            menuRecyclerView?.visibility = View.GONE
            noPlaylistsFound.visibility = View.VISIBLE
        }

        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        // On met en place les données du menu situé tout en bas de l'écran :
        if (MyMediaPlayer.currentIndex == -1){
            noSongPlaying.visibility = View.VISIBLE
            infoSongPlaying.visibility = View.GONE
        } else {
            // Changement de la vue :
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null){
                // Passons d'abord notre byteArray en bitmap :
                val bytes = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                albumCoverInfo.setImageBitmap(bitmap)
            } else {
                albumCoverInfo.setImageResource(R.drawable.michael)
            }

            // Mise en places des boutons :
            pausePlay?.setOnClickListener{pausePlay()}
            nextBtn?.setOnClickListener{playNextSong()}
            previousBtn?.setOnClickListener{playPreviousSong()}
            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex)}
            songTitleInfo?.setSelected(true)
        }

        // Mise en place du bouton de création de playlist :
        val addPlaylist = findViewById<ImageView>(R.id.add_playlist)
        addPlaylist?.setOnClickListener{ addPlaylist() }

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            Log.d("MAJ","")
            playlists = readAllPlaylistsFromFile(savePlaylistsFile)
            adapter.allPlaylists = playlists
            adapter.notifyItemRangeChanged(0, adapter.getItemCount())

            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val nextBtn = findViewById<ImageView>(R.id.next)
            val previousBtn = findViewById<ImageView>(R.id.previous)

            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
            val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

            noSongPlaying.visibility = View.VISIBLE

            if (MyMediaPlayer.currentIndex != -1){
                noSongPlaying.visibility = View.GONE
                infoSongPlaying.visibility = View.VISIBLE
                songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
                if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null){
                    // Passons d'abord notre byteArray en bitmap :
                    val bytes = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
                    var bitmap: Bitmap? = null
                    if (bytes != null && bytes.isNotEmpty()) {
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    albumCoverInfo.setImageBitmap(bitmap)
                } else {
                    albumCoverInfo.setImageResource(R.drawable.michael)
                }

                pausePlay?.setOnClickListener{ pausePlay() }
                nextBtn?.setOnClickListener{ playNextSong() }
                previousBtn?.setOnClickListener{ playPreviousSong() }
                bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex) }
                songTitleInfo?.setSelected(true)
            }

            if (!mediaPlayer.isPlaying){
                pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            mediaPlayer.setOnCompletionListener { playNextSong() }
            Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())
            Log.d("RESUME","resume")
        }
    }

    override fun onPlaylistClick(position: Int) {
        Log.d("PLAYLIST POSITION", position.toString())

        val intent = Intent(this@PlaylistsMenuActivity,SelectedPlaylistActivity::class.java)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun onBottomMenuClick(position : Int){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(this@PlaylistsMenuActivity,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
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

    private fun addPlaylist(){
        val builder = AlertDialog.Builder(this@PlaylistsMenuActivity)
        builder.setTitle("Create playlist")
        // L'entrée :
        val inputText = EditText(this@PlaylistsMenuActivity)
        // Le type d'entrée :
        inputText.setInputType(InputType.TYPE_CLASS_TEXT)
        builder.setView(inputText)
        // Les boutons :
        // Si on valide la création, on crée notre playlist :
        builder.setPositiveButton("OK",DialogInterface.OnClickListener{ _, _ ->
            /* Afin de créer une playlist, nous devons vérifier les critères suivants :
                - Le nom n'est pas vide ou ne commence pas avec un espace (au cas où on a qu'un espace en guise de nom
                - Le nom n'est pas déjà prit
                - Le nom n'est pas celui de la playlist principale ("Main")
             */

            if (inputText.text.toString() != "" && !(inputText.text.toString().startsWith(" ")) && !(playlistsNames.contains(inputText.text.toString())) && (inputText.text.toString() != "Main")) {
                playlistsNames.add(inputText.text.toString())
                val newPlaylist = Playlist(inputText.text.toString(), ArrayList(),null)
                playlists.add(newPlaylist)
                adapter.allPlaylists = playlists
                GlobalScope.launch(Dispatchers.IO){
                    launch{writePlaylistsToFile(savePlaylistsFile, playlists)}
                }

                menuRecyclerView?.visibility = View.VISIBLE
                noPlaylistsFound.visibility = View.GONE

                menuRecyclerView?.layoutManager = GridLayoutManager(this,2)
                menuRecyclerView?.adapter = adapter
            } else {
                Toast.makeText(this,"A title must be set correctly !",Toast.LENGTH_SHORT).show()
            }
        })
        // Si on annule la création de la playlist, on quitte la fenêtre
        builder.setNegativeButton("CANCEL", DialogInterface.OnClickListener{dialogInterface, i ->
            dialogInterface.cancel()
        })

        builder.show()

        Log.d("playlist ajouté","")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                if (playlists[item.groupId].isFavoriteList){
                    Toast.makeText(this,"You can't delete the Favorites playlist",Toast.LENGTH_SHORT).show()
                } else {
                    playlists.removeAt(item.groupId)
                    adapter.allPlaylists = playlists
                    adapter.notifyItemRemoved(item.groupId)

                    writePlaylistsToFile(savePlaylistsFile,playlists)
                    Toast.makeText(this,"Suppressions de la playlist",Toast.LENGTH_SHORT).show()
                }
                true
            }
            1 -> {
                val intent = Intent(this@PlaylistsMenuActivity,ModifyPlaylistInfoActivity::class.java)
                intent.putExtra("POSITION",item.groupId)
                resultLauncher.launch(intent)
                true
            }
            else -> {
                onContextItemSelected(item)
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
