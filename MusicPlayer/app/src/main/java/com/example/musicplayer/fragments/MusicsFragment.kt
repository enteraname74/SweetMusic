package com.example.musicplayer.fragments

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.Music
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.notification.MusicNotificationService
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = MusicList(ArrayList<Music>(), "Main",requireContext(), this)
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
        if (MyMediaPlayer.dataWasChanged){
            // Si on a mis à jour toutes nos données, il faut qu'on change nos musiques :
            adapter.musics = MyMediaPlayer.allMusics
            MyMediaPlayer.dataWasChanged = false
        }
        adapter.notifyDataSetChanged()

        val nextButton : ImageView = activity?.findViewById(R.id.next) as ImageView
        val previousButton : ImageView = activity?.findViewById(R.id.previous) as ImageView

        nextButton.setOnClickListener { (activity as MainActivity).playNextSong(adapter) }
        previousButton.setOnClickListener { (activity as MainActivity).playPreviousSong(adapter) }
        mediaPlayer.setOnCompletionListener {
            Log.d("MUSIC FRAGMENT", "WILL PLAY NEXT")
            (activity as MainActivity).playNextSong(adapter) }

        CoroutineScope(Dispatchers.Main).launch { (activity as MainActivity).verifiyAllMusics(adapter) }
    }

    override fun onMusicClick(position: Int) {
        Log.d("MUSIC FRAGMENT", "START CLICK")
        mediaPlayer.setOnCompletionListener(null)
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            sameMusic = false
        }
        // Vérifions si on change de playlist : (on le fait aussi obligatoirement si la playlist jouée est vide)
        if (adapter.musics != MyMediaPlayer.currentPlaylist || MyMediaPlayer.currentPlaylist.size == 0) {
            CoroutineScope(Dispatchers.Main).launch {
                MyMediaPlayer.initialPlaylist = ArrayList(adapter.musics.map { it.copy() })
            }
            MyMediaPlayer.currentPlaylist = ArrayList(adapter.musics.map { it.copy() })
            MyMediaPlayer.playlistName = "Main"
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position

        CoroutineScope(Dispatchers.Default).launch {
            val notificationManager = context?.applicationContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Si il n'y a pas de notifications, on l'affiche
            if(notificationManager.activeNotifications.isEmpty()) {
                val service = MusicNotificationService(context?.applicationContext as Context)
                if (mediaPlayer.isPlaying){
                    service.showNotification(R.drawable.ic_baseline_pause_circle_outline_24)
                } else {
                    service.showNotification(R.drawable.ic_baseline_play_circle_outline_24)
                }
            }

            (activity as MainActivity).updateMusicNotification(!mediaPlayer.isPlaying)
        }
        Log.d("MUSIC FRAGMENT", "END CLICK")
        val intent = Intent(context, MusicPlayerActivity::class.java)
        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onLongMusicClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_menu)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_a_playlist)?.setOnClickListener {
            (activity as MainActivity).bottomSheetAddTo(position, requireContext(), adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
            (activity as MainActivity).bottomSheetRemoveFromApp(adapter,position,(activity as MainActivity).sheetBehavior, requireContext())
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_music)?.setOnClickListener {
            (activity as MainActivity).bottomSheetModifyMusic(requireContext(),position,adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.play_next)?.setOnClickListener {
            (activity as MainActivity).bottomSheetPlayNext(adapter,position)
            bottomSheetDialog.dismiss()
        }
    }

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