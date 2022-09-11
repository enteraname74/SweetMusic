package com.example.musicplayer.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.Music
import com.example.musicplayer.MusicList
import kotlinx.coroutines.*
import java.io.*

class MusicsFragment : Fragment(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {

    private val saveAllMusicsFile = "allMusics.musics"
    private val saveAllDeletedFiles = "allDeleted.musics"
    private val savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var adapter : MusicList
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var searchView : SearchView
    private var searchIsOn = false
    private val mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = MusicList(ArrayList<Music>(), "Main",activity?.applicationContext as Context, this)
        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
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

        val nextButton : ImageView = activity?.findViewById(R.id.next) as ImageView
        val previousButton : ImageView = activity?.findViewById(R.id.previous) as ImageView

        nextButton.setOnClickListener { playNextSong(adapter) }
        previousButton.setOnClickListener { playPreviousSong(adapter) }

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
        if (MyMediaPlayer.dataWasChanged){
            // Si on a mis à jour toutes nos données, il faut qu'on change nos musiques :
            adapter.musics = MyMediaPlayer.allMusics
            MyMediaPlayer.dataWasChanged = false
        }
        adapter.notifyDataSetChanged()

        val nextButton : ImageView = activity?.findViewById(R.id.next) as ImageView
        val previousButton : ImageView = activity?.findViewById(R.id.previous) as ImageView

        nextButton.setOnClickListener { playNextSong(adapter) }
        previousButton.setOnClickListener { playPreviousSong(adapter) }
        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
    }

    override fun onMusicClick(position: Int) {
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            println("not the same song. Selected : $position, Normal : ${MyMediaPlayer.currentIndex}")
            sameMusic = false
        }
        // Vérifions si on change de playlist :
        if (adapter.musics != MyMediaPlayer.initialPlaylist) {
            println("changement playlist")
            MyMediaPlayer.currentPlaylist = ArrayList(adapter.musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(adapter.musics.map { it.copy() })
            MyMediaPlayer.playlistName = "Main"
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position

        val intent = Intent(context, MusicPlayerActivity::class.java)
        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d("music fragment",item.itemId.toString())

        return when (item.itemId) {
            0 -> {
                // ADD TO PLAYLIST
                Toast.makeText(context, resources.getString(R.string.added_in_the_playlist), Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                // DELETE FROM APP
                val musicToRemove = adapter.musics[item.groupId]
                adapter.musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)
                MyMediaPlayer.allMusics.remove(musicToRemove)

                // Enlevons la musique de nos playlists :
                for(playlist in MyMediaPlayer.allPlaylists) {
                    if (playlist.musicList.contains(musicToRemove)){
                        playlist.musicList.remove(musicToRemove)
                    }
                }

                // Si la musique était en favoris, on lui enlève ce statut :
                musicToRemove.favorite = false

                CoroutineScope(Dispatchers.IO).launch {
                    MyMediaPlayer.allDeletedMusics.add(0,musicToRemove)
                    writeAllDeletedSong()
                    writeAllMusicsToFile(MyMediaPlayer.allMusics)
                    writePlaylistsToFile()
                }

                Toast.makeText(
                    context,
                    resources.getString(R.string.deleted_from_app),
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            2 -> {
                // MODIFY INFOS :
                val intent = Intent(context, ModifyMusicInfoActivity::class.java)
                intent.putExtra("PATH", adapter.musics[item.groupId].path)
                resultLauncher.launch(intent)
                true
            }
            3 -> {
                if (MyMediaPlayer.currentPlaylist.size > 0) {
                    // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                    val currentMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
                    val songToPlayNext = adapter.musics[item.groupId]

                    // On empêche de pouvoir ajouter la même musique pour éviter des problèmes de position négatif :
                    if (currentMusic != songToPlayNext) {
                        MyMediaPlayer.initialPlaylist.remove(songToPlayNext)
                        MyMediaPlayer.currentPlaylist.remove(songToPlayNext)

                        // Assurons nous de récupérer la bonne position de la musique qui se joue actuellement :
                        MyMediaPlayer.currentIndex =
                            MyMediaPlayer.currentPlaylist.indexOf(currentMusic)

                        MyMediaPlayer.initialPlaylist.add(
                            MyMediaPlayer.currentIndex + 1,
                            songToPlayNext
                        )
                        MyMediaPlayer.currentPlaylist.add(
                            MyMediaPlayer.currentIndex + 1,
                            songToPlayNext
                        )
                        Toast.makeText(
                            context,
                            resources.getString(R.string.music_will_be_played_next),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private fun writeAllMusicsToFile(content : ArrayList<Music>){
        MyMediaPlayer.allMusics = content
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllMusicsFile)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write musics",error.toString())
        }
    }

    private fun playNextSong(adapter : MusicList){
        if(MyMediaPlayer.currentIndex ==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex +=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    private fun playPreviousSong(adapter : MusicList){
        if(MyMediaPlayer.currentIndex ==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex -=1
        }
        adapter.notifyDataSetChanged()
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

    fun writePlaylistsToFile(){
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, savePlaylistsFile)))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write playlists",error.toString())
        }
    }
}