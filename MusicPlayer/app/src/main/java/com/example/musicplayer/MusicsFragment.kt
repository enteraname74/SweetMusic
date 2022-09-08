package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
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
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

class MusicsFragment : Fragment(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {

    private val saveAllMusicsFile = "allMusics.musics"
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
            sameMusic = false
        }
        // Vérifions si on change de playlist :
        if (adapter.musics != MyMediaPlayer.initialPlaylist) {
            println("there")
            MyMediaPlayer.currentPlaylist = ArrayList(adapter.musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(adapter.musics.map { it.copy() })
            MyMediaPlayer.playlistName = "Main"
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position

        val intent = Intent(context,MusicPlayerActivity::class.java)
        Log.d("SAME MUSIC", sameMusic.toString())
        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        Log.d("music fragment",item.itemId.toString())

        return when (item.itemId) {
            0 -> {
                Toast.makeText(context, resources.getString(R.string.added_in_the_playlist), Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                val musicToRemove = adapter.musics[item.groupId]
                adapter.musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)
                MyMediaPlayer.allMusics.remove(musicToRemove)

                CoroutineScope(Dispatchers.IO).launch { writeAllMusicsToFile(MyMediaPlayer.allMusics) }

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
                // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                MyMediaPlayer.initialPlaylist.remove(adapter.musics[item.groupId])
                MyMediaPlayer.currentPlaylist.remove(adapter.musics[item.groupId])

                MyMediaPlayer.initialPlaylist.add(
                    MyMediaPlayer.currentIndex + 1,
                    adapter.musics[item.groupId]
                )
                MyMediaPlayer.currentPlaylist.add(
                    MyMediaPlayer.currentIndex + 1,
                    adapter.musics[item.groupId]
                )
                Toast.makeText(context,resources.getString(R.string.music_will_be_played_next), Toast.LENGTH_SHORT).show()
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
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    private fun playPreviousSong(adapter : MusicList){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
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
}