package com.example.musicplayer

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.*
import kotlin.system.measureTimeMillis

class MainActivity :MusicList.OnMusicListener, Tools(),AudioManager.OnAudioFocusChangeListener  {

    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var adapter : MusicList
    private var menuRecyclerView : RecyclerView? = null
    private lateinit var searchView : SearchView
    private var mediaPlayer = MyMediaPlayer.getInstance
    private lateinit var audioManager : AudioManager
    private lateinit var audioAttributes : AudioAttributes
    private lateinit var audioFocusRequest : AudioFocusRequest
    private lateinit var onAudioFocusChange: AudioManager.OnAudioFocusChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("DOWNLOAD", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString())
        Log.d("DOWNLOAD", applicationContext.filesDir.toString())

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("doesA..",MyMediaPlayer.doesASongWillBePlaying.toString())
            Log.d("MediaPlayer state", mediaPlayer.isPlaying.toString())
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> println("gain")
                else -> {
                    if (mediaPlayer.isPlaying && !MyMediaPlayer.doesASongWillBePlaying) {
                        println("loss focus")
                        mediaPlayer.pause()
                        val pausePlay = findViewById<ImageView>(R.id.pause_play)
                        pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                    }
                    Log.d("change does..","")
                    MyMediaPlayer.doesASongWillBePlaying = false
                }
            }
        }

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(this)
            .build()

        val noSongsFound = findViewById<TextView>(R.id.no_songs_found)
        noSongsFound.visibility = View.VISIBLE

        if (!checkPermission()){
            requestPermission()
        }

        if (File(applicationContext.filesDir, saveAllMusicsFile).exists()){
            musics = readAllMusicsFromFile(saveAllMusicsFile)
            allMusicsBackup = ArrayList(musics.map { it.copy() })

            menuRecyclerView = findViewById(R.id.menu_recycler_view)
            menuRecyclerView?.visibility = View.VISIBLE
            noSongsFound.visibility = View.GONE

            adapter = MusicList(musics, "Main",applicationContext, this)

            //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
            menuRecyclerView?.layoutManager = LinearLayoutManager(this)
            menuRecyclerView?.adapter = adapter
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            searchView = findViewById(R.id.search_view)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(p0: String?): Boolean {
                    try {
                        if (p0 != null) {
                            Log.d("TEXTE", p0.toString())
                            val list = ArrayList<Music>()

                            if(p0 == ""){
                                musics = allMusicsBackup
                                adapter.musics = musics
                                adapter.notifyDataSetChanged()
                            } else {
                                for (music: Music in allMusicsBackup) {
                                    if ((music.name.lowercase().contains(p0.lowercase())) || (music.album.lowercase().contains(p0.lowercase())) || (music.artist.lowercase().contains(p0.lowercase()))){
                                        list.add(music)
                                    }
                                }

                                if (list.size > 0) {
                                    musics = list
                                    adapter.musics = musics
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    } catch (error : Error){
                        Log.d("ERROR",error.toString())
                    }
                    return true
                }

                override fun onQueryTextChange(p0: String?): Boolean {
                    try {
                        if (p0 != null) {
                            Log.d("TEXTE", p0.toString())
                            val list = ArrayList<Music>()

                            if(p0 == ""){
                                musics = allMusicsBackup
                                adapter.musics = musics
                                adapter.notifyDataSetChanged()
                            } else {
                                for (music: Music in allMusicsBackup) {
                                    if ((music.name.lowercase().contains(p0.lowercase())) || (music.album.lowercase().contains(p0.lowercase())) || (music.artist.lowercase().contains(p0.lowercase()))){
                                        list.add(music)
                                    }
                                }

                                if (list.size > 0) {
                                    musics = list
                                    adapter.musics = musics
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    } catch (error : Error){
                        Log.d("ERROR",error.toString())
                    }
                    return true
                }
            })
        } else {
            // Si nous rentrons dans cette condition, c'est que l'utilisateur ouvre l'application pour la première fois

            // Créons d'abord la playlist des favoris :
            val favoritePlaylist = Playlist("Favorites",ArrayList(),null)
            val playlists = ArrayList<Playlist>()
            playlists.add(favoritePlaylist)
            writePlaylistsToFile(savePlaylistsFile,playlists)

            // A "projection" defines the columns that will be returned for each row
            val projection: Array<String> = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Albums.ALBUM_ID
            )

            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

            // Does a query against the table and returns a Cursor object
            val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
                projection,                                     // The columns to return for each row
                selection,                                      // Either null, or the boolean that specifies the rows to retrieve
                null,
                null // The sort order for the returned rows
            )

            when (cursor?.count) {
                null -> {
                    Toast.makeText(this, "Couldn't retrieve music files", Toast.LENGTH_SHORT).show()
                    menuRecyclerView?.visibility = View.GONE
                    noSongsFound.visibility = View.VISIBLE
                }
                0 -> {
                    menuRecyclerView?.visibility = View.GONE
                    noSongsFound.visibility = View.VISIBLE
                }
                else -> {
                    while (cursor.moveToNext()) {
                        val albumId = cursor.getLong(5)
                        val albumUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
                        )
                        Log.d("URI", albumUri.toString())

                        val albumCover : ByteArray? = try {
                            val bitmap = contentResolver.loadThumbnail(
                                albumUri,
                                Size(500, 500),
                                null
                            )
                            bitmapToByteArray(bitmap)
                        } catch (error : FileNotFoundException){
                            null
                        }
                        val music = Music(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            albumCover,
                            cursor.getLong(3),
                            cursor.getString(4)
                        )
                        if (File(music.path).exists()) {
                            musics.add(music)
                        }
                    }

                    cursor.close()
                    musics.reverse()

                    writeAllMusicsToFile(saveAllMusicsFile, musics)
                    menuRecyclerView = findViewById(R.id.menu_recycler_view)
                    menuRecyclerView?.visibility = View.VISIBLE
                    noSongsFound.visibility = View.GONE
                    Log.d("GET ALL MUSICS","")
                    adapter = MusicList(musics,"Main", applicationContext, this)

                    //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
                    menuRecyclerView?.layoutManager = LinearLayoutManager(this)
                    menuRecyclerView?.adapter = adapter
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                }
            }
        }

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)

        if (MyMediaPlayer.currentIndex == -1){
            noSongPlaying.visibility = View.VISIBLE
            infoSongPlaying.visibility = View.GONE
        } else {
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@MainActivity)}
            songTitleInfo?.isSelected = true
        }

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
        MyMediaPlayer.allMusics = musics

        val playlistsButton = findViewById<Button>(R.id.playlists)
        val downloadButton = findViewById<ImageButton>(R.id.download)
        val shuffleButton = findViewById<Button>(R.id.shuffle_button)

        playlistsButton.setOnClickListener{ playlistButton() }
        downloadButton.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                launch{
                    retrieveAllMusicsFromApp()
                }
            }
            Toast.makeText(this, "retrieve data", Toast.LENGTH_SHORT).show()
        }
        shuffleButton.setOnClickListener { playRandom(musics, this@MainActivity) }

        // On ajoute nos musiques et playlists dans notre mediaplayer :

        GlobalScope.launch(Dispatchers.IO){
            launch{readPlaylistsAsync()}
        }
        println("end")
    }

    private fun checkPermission() : Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(this,"PERMISSION IS REQUIRED FOR THIS APP TO FUNCTION. PLEASE ALLOW PERMISSIONS FROM SETTINGS",Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                69
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                 arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                69
            )
        }
    }

    override fun onMusicClick(position: Int) {
        var sameMusic = true
        MyMediaPlayer.doesASongWillBePlaying = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        // Vérifions si on change de playlist :
        if (musics != MyMediaPlayer.initialPlaylist) {
            MyMediaPlayer.currentPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.playlistName = "Main"
            MyMediaPlayer.doesASongWillBePlaying = false
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position

        val intent = Intent(this@MainActivity,MusicPlayerActivity::class.java)
        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
        if (menuRecyclerView != null) {
            if (MyMediaPlayer.modifiedSong) {
                GlobalScope.launch(Dispatchers.IO) {
                    launch {
                        writeAllAsync(
                            MyMediaPlayer.allMusics,
                            MyMediaPlayer.allPlaylists
                        )
                    }
                }
                println("test")
                MyMediaPlayer.modifiedSong = false
            }
            adapter.musics = musics
            adapter.notifyItemRangeChanged(0, adapter.itemCount)


            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val nextBtn = findViewById<ImageView>(R.id.next)
            val previousBtn = findViewById<ImageView>(R.id.previous)
            val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
            val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

            noSongPlaying.visibility = View.VISIBLE

            val time = measureTimeMillis {

                if (MyMediaPlayer.currentIndex != -1) {
                    GlobalScope.launch(Dispatchers.IO) {
                        launch {
                            noSongPlaying.visibility = View.GONE
                            infoSongPlaying.visibility = View.VISIBLE
                            songTitleInfo.text =
                                MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
                            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
                                // Passons d'abord notre byteArray en bitmap :
                                val bytes =
                                    MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
                                var bitmap: Bitmap? = null
                                if (bytes != null && bytes.isNotEmpty()) {
                                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                                albumCoverInfo.setImageBitmap(bitmap)
                            } else {
                                albumCoverInfo.setImageResource(R.drawable.michael)
                            }
                        }
                    }

                    pausePlay?.setOnClickListener { pausePlay() }
                    nextBtn?.setOnClickListener { playNextSong(adapter) }
                    previousBtn?.setOnClickListener { playPreviousSong(adapter) }
                    bottomInfos.setOnClickListener {
                        onBottomMenuClick(
                            MyMediaPlayer.currentIndex,
                            this@MainActivity
                        )
                    }
                    songTitleInfo?.isSelected = true
                }
            }

            if (!mediaPlayer.isPlaying) {
                pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            mediaPlayer.setOnCompletionListener { playNextSong(adapter) }

            Log.d("TIME", time.toString())
        }
    }

    private fun playlistButton() {
        val intent = Intent(this@MainActivity,PlaylistsMenuActivity::class.java)
        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                Toast.makeText(this,"Ajout dans une playlist",Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)

                GlobalScope.launch(Dispatchers.IO){
                    launch{writeAllMusicsToFile(saveAllMusicsFile, musics)}
                }

                Toast.makeText(this,"Suppressions de la musique dans la playlist",Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                // MODIFY INFOS :
                // On s'assure de séléctionner la bonne position au cas où on utilise la barre de recherche :
                val position = allMusicsBackup.indexOf(musics[item.groupId])
                val intent = Intent(this@MainActivity,ModifyMusicInfoActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", "Main")
                intent.putExtra("POSITION",position)
                resultLauncher.launch(intent)
                true
            }
            3 -> {
                // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                MyMediaPlayer.initialPlaylist.remove(musics[item.groupId])
                MyMediaPlayer.currentPlaylist.remove(musics[item.groupId])

                MyMediaPlayer.initialPlaylist.add(MyMediaPlayer.currentIndex+1, musics[item.groupId])
                MyMediaPlayer.currentPlaylist.add(MyMediaPlayer.currentIndex+1, musics[item.groupId])
                Toast.makeText(this,"Musique ajoutée à la file d'attente",Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                onContextItemSelected(item)
            }
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // On récupère les musiques avec la modification effectuée :
            allMusicsBackup = MyMediaPlayer.allMusics
            adapter.notifyDataSetChanged()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d("doesA..",MyMediaPlayer.doesASongWillBePlaying.toString())
        Log.d("MediaPlayer state", mediaPlayer.isPlaying.toString())
        Log.d("focusChange", focusChange.toString())
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> println("gain")
            else -> {
                if (mediaPlayer.isPlaying && !MyMediaPlayer.doesASongWillBePlaying) {
                    println("loss focus")
                    mediaPlayer.pause()
                    val pausePlay = findViewById<ImageView>(R.id.pause_play)
                    pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                }
                Log.d("change does..","")
                MyMediaPlayer.doesASongWillBePlaying = false
            }
        }
    }
}
