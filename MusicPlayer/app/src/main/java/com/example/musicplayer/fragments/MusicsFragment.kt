package com.example.musicplayer.fragments

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        mediaPlayer.setOnCompletionListener { (activity as MainActivity).playNextSong(adapter) }
    }

    override fun onMusicClick(position: Int) {
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

            if(mediaPlayer.isPlaying){
                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", false)
                context?.applicationContext?.sendBroadcast(intentForNotification)
            } else {
                val intentForNotification = Intent("BROADCAST_NOTIFICATION")
                intentForNotification.putExtra("STOP", true)
                context?.applicationContext?.sendBroadcast(intentForNotification)
            }
        }

        val intent = Intent(context, MusicPlayerActivity::class.java)
        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onLongMusicClick(positon: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_menu)
        bottomSheetDialog.show()
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

                // Enlevons la musique des playlists utilisées par le mediaplayer si possible :
                if (MyMediaPlayer.currentIndex != -1) {
                    val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
                    if (MyMediaPlayer.initialPlaylist.contains(musicToRemove)) {
                        MyMediaPlayer.initialPlaylist.remove(musicToRemove)
                    }
                    if (MyMediaPlayer.currentPlaylist.contains(musicToRemove)) {
                        // Si c'est la chanson qu'on joue actuellement, alors on passe si possible à la suivante :
                        Log.d("CONTAINS","")
                        if (musicToRemove.path == currentSong.path) {
                            Log.d("SAME","")
                            // Si on peut passer à la musique suivante, on le fait :
                            if (MyMediaPlayer.currentPlaylist.size > 1) {
                                Log.d("PLAY NEXT","")
                                (activity as MainActivity).playNextSong(adapter)
                                MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                            } else {
                                Log.d("REPLACE STATUS","")
                                // Sinon on enlève la musique en spécifiant qu'aucune musique ne peut être lancer (playlist avec 0 musiques)
                                val noSongPlaying = activity?.findViewById<TextView>(R.id.no_song_playing)
                                val infoSongPlaying = activity?.findViewById<RelativeLayout>(R.id.info_song_playing)
                                val albumCoverInfo = activity?.findViewById<ImageView>(R.id.album_cover_info)
                                val bottomInfos = activity?.findViewById<LinearLayout>(R.id.bottom_infos)

                                noSongPlaying?.visibility = View.VISIBLE
                                infoSongPlaying?.visibility = View.GONE
                                albumCoverInfo?.setImageResource(R.drawable.icone_musique)
                                bottomInfos?.setOnClickListener(null)
                                MyMediaPlayer.currentIndex = -1

                                mediaPlayer.pause()
                            }
                            MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                        } else {
                            Log.d("JUST DELETE","")
                            MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                            // Vu qu'on change les positions des musiques, on récupère la position de la musique chargée dans le mediaplayer pour bien pouvoir jouer celle d'après / avant :
                            MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                        }
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