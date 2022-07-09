package com.example.musicplayer

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.*

class PlaylistsMenuActivity : AppCompatActivity(), Playlists.OnPlaylistsListener {
    private var menuRecyclerView : RecyclerView? = null
    private lateinit var adapter : Playlists
    private lateinit var noPlaylistsFound : TextView
    private var playlists = ArrayList<Playlist>()
    private val playlistsNames = ArrayList<String>()
    private var mediaPlayer = MyMediaPlayer.getInstance
    private var saveFile = "allPlaylists.playlists"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists_menu)

        //writeObjectToFile("allPlaylists.playlists", playlists)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        noPlaylistsFound = findViewById<TextView>(R.id.no_playlists_found)

        // Si on a déjà enregistré des playlists, on va les chercher dans notre fichier :
        if (File(applicationContext.filesDir, saveFile).exists()){
            playlists = readAllPlaylistsFromFile(saveFile)
        }

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

        // On met en place les données du menu situé tout en bas de l'écran :
        if (MyMediaPlayer.currentIndex == -1){
            noSongPlaying.visibility = View.VISIBLE
            infoSongPlaying.visibility = View.GONE
        } else {
            // Changement de la vue :
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name

            // Mise en places des boutons :
            pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
            nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
            previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
            bottomInfos.setOnClickListener(View.OnClickListener {onBottomMenuClick(MyMediaPlayer.currentIndex) })
            songTitleInfo?.setSelected(true)
        }

        // Mise en place du bouton de création de playlist :
        val addPlaylist = findViewById<ImageView>(R.id.add_playlist)
        addPlaylist?.setOnClickListener(View.OnClickListener { addPlaylist() })

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            Log.d("MAJ","")
            playlists = readAllPlaylistsFromFile(saveFile)
            adapter.allPlaylists = playlists
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());

            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val nextBtn = findViewById<ImageView>(R.id.next)
            val previousBtn = findViewById<ImageView>(R.id.previous)

            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)

            noSongPlaying.visibility = View.VISIBLE

            if (MyMediaPlayer.currentIndex != -1){
                noSongPlaying.visibility = View.GONE
                infoSongPlaying.visibility = View.VISIBLE
                songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name

                pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
                nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
                previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
                bottomInfos.setOnClickListener(View.OnClickListener {onBottomMenuClick(MyMediaPlayer.currentIndex) })
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

        val currentPlaylist = playlists[position]
        val intent = Intent(this@PlaylistsMenuActivity,SelectedPlaylistActivity::class.java)
        intent.putExtra("LIST",currentPlaylist)
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

    private fun playMusic(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            e.printStackTrace()
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

    private fun pausePlay(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
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
             */

            if (inputText.text.toString() != "" && !(inputText.text.toString().startsWith(" ")) && !(playlistsNames.contains(inputText.text.toString()))) {
                val newPlaylist = Playlist(inputText.text.toString(), ArrayList<Music>())
                playlists.add(newPlaylist)
                writeObjectToFile(saveFile, playlists)

                menuRecyclerView?.visibility = View.VISIBLE
                noPlaylistsFound.visibility = View.GONE

                //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
                menuRecyclerView?.layoutManager = LinearLayoutManager(this)
                menuRecyclerView?.adapter = Playlists(playlists, applicationContext, this)
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

    private fun writeObjectToFile(filename : String, content : ArrayList<Playlist>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error","")
        }
    }

    private fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
        val path = applicationContext.filesDir
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)));
            content = ois.readObject() as ArrayList<Playlist>
            ois.close();
        } catch (error : IOException){
            Log.d("Error","")
        }

        return content
    }
}
