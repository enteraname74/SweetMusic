package com.example.musicplayer


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.musicplayer.classes.Album
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelectedAlbumActivity : Tools(), MusicList.OnMusicListener, SearchView.OnQueryTextListener{
    private lateinit var album : Album
    private var albumPosition : Int = 0
    private lateinit var adapter : MusicList
    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var searchView : SearchView
    private var searchIsOn = false
    private lateinit var menuRecyclerView : RecyclerView

    private lateinit var pausePlayButton : ImageView

    private lateinit var bottomSheetLayout: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.extras?.getBoolean("STOP") != null && intent.extras?.getBoolean("STOP") as Boolean) {
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            updateBottomPanel(findViewById(R.id.song_title_info),findViewById(R.id.album_cover_info))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_playlist)

        pausePlayButton = findViewById(R.id.pause_play)
        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        albumPosition = intent.getSerializableExtra("POSITION") as Int

        findViewById<ImageView>(R.id.add_songs).visibility = View.GONE
        CoroutineScope(Dispatchers.Main).launch { findViewById<ImageView>(R.id.modify_playlist).setOnClickListener { modifyAlbum() } }

        album = MyMediaPlayer.allAlbums[albumPosition]
        musics = album.albumList
        allMusicsBackup = ArrayList(musics.map { it.copy() })

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        adapter = MusicList(musics, album.albumName, applicationContext, this@SelectedAlbumActivity)
        menuRecyclerView.layoutManager = LinearLayoutManager(this@SelectedAlbumActivity)
        menuRecyclerView.adapter = adapter

        val albumName = findViewById<TextView>(R.id.playlist_name)
        albumName?.text = album.albumName

        val quitActivity = findViewById<ImageView>(R.id.quit_activity)
        quitActivity.setOnClickListener{ finish() }

        val shuffleButton = findViewById<ImageView>(R.id.shuffle)
        shuffleButton.setOnClickListener { playRandom(musics, this@SelectedAlbumActivity) }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }

        bottomSheetLayout = findViewById(R.id.bottom_infos)
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    stopMusic()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        findViewById<LinearLayout>(R.id.bottom_infos).setOnClickListener {
            onBottomMenuClick(
                MyMediaPlayer.currentIndex,
                this
            )
        }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()

        allMusicsBackup = MyMediaPlayer.allAlbums[albumPosition].albumList
        if(!searchIsOn){
            musics = MyMediaPlayer.allAlbums[albumPosition].albumList
            adapter.musics = musics
        }

        adapter.notifyDataSetChanged()

        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)


        if (MyMediaPlayer.currentIndex != -1){
            CoroutineScope(Dispatchers.Main).launch {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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
                songTitleInfo?.isSelected = true
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                albumCoverInfo?.setImageResource(R.drawable.icone_musique)
            }
        }

        registerReceiver(broadcastReceiver, IntentFilter("BROADCAST"))

        if (!mediaPlayer.isPlaying){
            pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }

        CoroutineScope(Dispatchers.Main).launch { setColorTheme() }
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
            MyMediaPlayer.playlistName = "Album"
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        val intent = Intent(this@SelectedAlbumActivity,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("ALBUM POSITION", albumPosition)
        println("start mp activity")

        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                // ADD TO PLAYLIST
                Toast.makeText(this,resources.getString(R.string.added_in_the_playlist),Toast.LENGTH_SHORT).show()
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
                        if (musicToRemove.path == currentSong.path) {
                            // Si on peut passer à la musique suivante, on le fait :
                            if (MyMediaPlayer.currentPlaylist.size > 1) {
                                playNextSong(adapter)
                                MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                            } else {
                                // Sinon on enlève la musique en spécifiant qu'aucune musique ne peut être lancer (playlist avec 0 musiques)
                                val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
                                val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)
                                val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)

                                infoSongPlaying?.visibility = View.GONE
                                albumCoverInfo?.setImageResource(R.drawable.icone_musique)
                                bottomInfos?.setOnClickListener(null)
                                MyMediaPlayer.currentIndex = -1

                                mediaPlayer.pause()
                            }
                            MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                        } else {
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
                    writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)
                    writePlaylistsToFile(savePlaylistsFile, MyMediaPlayer.allPlaylists)
                }

                Toast.makeText(
                    this,
                    resources.getString(R.string.deleted_from_app),
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            2 -> {
                // MODIFY INFOS :
                val intent = Intent(this@SelectedAlbumActivity,ModifyMusicInfoActivity::class.java)
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

    private var resultModifyMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

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
                    adapter.musics = allMusicsBackup
                } else {
                    searchIsOn = true
                    for (music: Music in allMusicsBackup) {
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

        if (album.albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = album.albumCover
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
        window.statusBarColor = newSecondaryColor
    }

    private fun modifyAlbum() {
        val intent = Intent(this,ModifyAlbumInfoActivity::class.java)
        intent.putExtra("POS",albumPosition)
        modifyAlbumLauncher.launch(intent)
    }

    private var modifyAlbumLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            findViewById<TextView>(R.id.playlist_name).text = album.albumName
        }
    }
}