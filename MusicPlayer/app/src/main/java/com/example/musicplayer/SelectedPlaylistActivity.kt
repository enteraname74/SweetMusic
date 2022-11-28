package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelectedPlaylistActivity : Tools(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {
    private lateinit var playlist : Playlist
    private var playlistPosition : Int = 0
    private lateinit var adapter : MusicList
    private var allPlaylists = ArrayList<Playlist>()
    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private var searchIsOn = false
    private lateinit var searchView : SearchView
    private lateinit var menuRecyclerView : RecyclerView

    private lateinit var pausePlayButton : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_playlist)

        pausePlayButton = findViewById(R.id.pause_play)
        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)

        allPlaylists = MyMediaPlayer.allPlaylists
        playlistPosition = intent.getSerializableExtra("POSITION") as Int
        playlist = allPlaylists[playlistPosition]
        musics = playlist.musicList
        allMusicsBackup = ArrayList(musics.map { it.copy() })

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        adapter = MusicList(musics, playlist.listName, applicationContext, this@SelectedPlaylistActivity)
        menuRecyclerView.layoutManager = LinearLayoutManager(this@SelectedPlaylistActivity)
        menuRecyclerView.adapter = adapter

        val playlistName = findViewById<TextView>(R.id.playlist_name)
        playlistName?.text = playlist.listName

        val quitActivity = findViewById<ImageView>(R.id.quit_activity)
        quitActivity.setOnClickListener{ finish() }

        val modifyPlaylistInfos = findViewById<ImageView>(R.id.modify_playlist)
        modifyPlaylistInfos.setOnClickListener { modifyPlaylist() }

        val addSongs = findViewById<ImageView>(R.id.add_songs)
        addSongs.setOnClickListener{ onAddSongsClick() }

        val shuffleButton = findViewById<ImageView>(R.id.shuffle)
        shuffleButton.setOnClickListener { playRandom(musics, this@SelectedPlaylistActivity) }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()

        if(!searchIsOn){
            adapter.musics = MyMediaPlayer.allPlaylists[playlistPosition].musicList
        }
        adapter.notifyItemRangeChanged(0, adapter.itemCount)

        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        if (MyMediaPlayer.currentIndex != -1){
            CoroutineScope(Dispatchers.Main).launch {
                bottomInfos.visibility = View.VISIBLE
                songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
                if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
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

                pausePlayButton.setOnClickListener { pausePlay(pausePlayButton) }
                nextBtn?.setOnClickListener { playNextSong(adapter) }
                previousBtn?.setOnClickListener { playPreviousSong(adapter) }
                bottomInfos.setOnClickListener {
                    onBottomMenuClick(
                        MyMediaPlayer.currentIndex,
                        this@SelectedPlaylistActivity
                    )
                }
                songTitleInfo?.isSelected = true
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                bottomInfos.visibility = View.GONE
                albumCoverInfo?.setImageResource(R.drawable.icone_musique)
                bottomInfos?.setOnClickListener(null)
            }
        }

        if (!mediaPlayer.isPlaying){
            pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }


        CoroutineScope(Dispatchers.Main).launch { setColorTheme() }
    }

    private fun onAddSongsClick(){
        val intent = Intent(this@SelectedPlaylistActivity,MusicSelectionActivity::class.java)
        resultLauncher.launch(intent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getSerializableExtra("addedSongs") as ArrayList<Int>
            val allMusics = readAllMusicsFromFile(saveAllMusicsFile)
            for (position in data){
                if (musics.find { it.path == allMusics[position].path } == null){
                    musics.add(allMusics[position])
                }
            }
            playlist.musicList = musics
            adapter.musics = musics
            menuRecyclerView.adapter = adapter

            val playlists = readAllPlaylistsFromFile(savePlaylistsFile)
            playlists[playlistPosition].musicList = musics
            writePlaylistsToFile(savePlaylistsFile, playlists)
        }
    }

    override fun onMusicClick(position: Int) {
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            sameMusic = false
        }
        // Vérifions si on change de playlist : (on le fait aussi obligatoirement si la playlist jouée est vide)
        if (musics != MyMediaPlayer.initialPlaylist || MyMediaPlayer.currentPlaylist.size == 0) {
            MyMediaPlayer.currentPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.playlistName = playlist.listName
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        val intent = Intent(this@SelectedPlaylistActivity,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", allMusicsBackup.indexOf(musics[position]))

        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                Toast.makeText(this,resources.getString(R.string.added_in_the_playlist),Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                val musicToRemove = adapter.musics[item.groupId]
                adapter.musics.remove(musicToRemove)
                adapter.notifyItemRemoved(item.groupId)
                MyMediaPlayer.allPlaylists[playlistPosition].musicList.remove(musicToRemove)

                // Si on enlève une musique de la playlist des favoris, on enlève son statut de favoris :
                if (playlist.isFavoriteList){
                    val globalPosition = MyMediaPlayer.allMusics.indexOf(musicToRemove)
                    val positionInInitialList = MyMediaPlayer.initialPlaylist.indexOf(musicToRemove)
                    val positionInCurrentList = MyMediaPlayer.currentPlaylist.indexOf(musicToRemove)

                    if(globalPosition != -1)  {
                        MyMediaPlayer.allMusics[globalPosition].favorite = false
                    }

                    if(positionInInitialList != -1)  {
                        MyMediaPlayer.initialPlaylist[positionInInitialList].favorite = false
                    }

                    if(positionInCurrentList != -1)  {
                        MyMediaPlayer.currentPlaylist[positionInCurrentList].favorite = false
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        launch{writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)}
                    }

                    for (playlist in MyMediaPlayer.allPlaylists){
                        if (playlist.musicList.contains(musicToRemove)){
                            val position = playlist.musicList.indexOf(musicToRemove)
                            playlist.musicList[position].favorite = false
                        }
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    launch{writePlaylistsToFile(savePlaylistsFile, MyMediaPlayer.allPlaylists)}
                }

                Toast.makeText(this,resources.getString(R.string.deleted_from_playlist),Toast.LENGTH_SHORT).show()

                true
            }
            2 -> {
                // MODIFY INFOS :
                val intent = Intent(this@SelectedPlaylistActivity,ModifyMusicInfoActivity::class.java)
                intent.putExtra("PATH", adapter.musics[item.groupId].path)
                resultModifyMusic.launch(intent)
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
                            this,
                            resources.getString(R.string.music_will_be_played_next),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }
            else -> {
                onContextItemSelected(item)
            }
        }
    }

    private var resultModifyMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // On récupère les musiques avec la modification effectuée :
            allMusicsBackup = MyMediaPlayer.allPlaylists[playlistPosition].musicList
            adapter.notifyDataSetChanged()
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
                    adapter.musics = playlist.musicList
                } else {
                    searchIsOn = true
                    for (music: Music in playlist.musicList) {
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

    private fun setColorTheme(){
        var bitmap: Bitmap? = null
        val playlistCover = findViewById<ImageView>(R.id.cover)

        if (playlist.playlistCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = playlist.playlistCover
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            playlistCover.setImageBitmap(bitmap)
        } else {
            playlistCover.setImageResource(R.drawable.michael)
            val drawable = playlistCover.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            bitmap = bitmapDrawable.bitmap
        }

        val dominantColor: Palette.Swatch? = Palette.from(bitmap as Bitmap).generate().dominantSwatch
        val newPrimaryColor = ColorUtils.blendARGB(getColor(R.color.primary_color),dominantColor?.rgb as Int,0.1f)
        val newSecondaryColor = ColorUtils.blendARGB(getColor(R.color.secondary_color),dominantColor.rgb,0.1f)
        val newTextColor = ColorUtils.blendARGB(getColor(R.color.text_color),dominantColor.rgb,0.1f)

        searchView.background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        menuRecyclerView.background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        adapter.backgroundColor = newSecondaryColor
        adapter.notifyDataSetChanged()


        findViewById<TextView>(R.id.playlist_name).setBackgroundColor(ColorUtils.setAlphaComponent(newPrimaryColor,150))
        findViewById<TextView>(R.id.playlist_name).setTextColor(newTextColor)
        findViewById<TextView>(R.id.song_title_info).setTextColor(newTextColor)

        findViewById<LinearLayout>(R.id.quit_activity_panel).setBackgroundColor(ColorUtils.setAlphaComponent(newPrimaryColor,150))
        findViewById<ImageView>(R.id.quit_activity).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.add_songs).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.shuffle).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.previous).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.next).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.pause_play).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.modify_playlist).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)

        findViewById<LinearLayout>(R.id.bottom_infos).setBackgroundColor(newPrimaryColor)
        findViewById<LinearLayout>(R.id.buttons_panel).background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        findViewById<LinearLayout>(R.id.background).setBackgroundColor(newPrimaryColor)

        window.navigationBarColor = newPrimaryColor
        window.statusBarColor = newPrimaryColor
    }

    private fun modifyPlaylist() {
        val intent = Intent(this,ModifyPlaylistInfoActivity::class.java)
        intent.putExtra("POSITION",playlistPosition)
        modifyPlaylistLauncher.launch(intent)
    }

    private var modifyPlaylistLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val playlistName = findViewById<TextView>(R.id.playlist_name)
            playlistName.text = playlist.listName
        }
    }
}