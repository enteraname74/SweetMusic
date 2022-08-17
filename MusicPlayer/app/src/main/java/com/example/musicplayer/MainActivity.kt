package com.example.musicplayer

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.car.ui.toolbar.TabLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.system.measureTimeMillis

class MainActivity : MusicList.OnMusicListener, Tools(),AudioManager.OnAudioFocusChangeListener, NavigationView.OnNavigationItemSelectedListener  {

    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var adapter : MusicList

    private lateinit var audioManager : AudioManager
    private lateinit var audioAttributes : AudioAttributes
    private lateinit var audioFocusRequest : AudioFocusRequest
    private lateinit var onAudioFocusChange: AudioManager.OnAudioFocusChangeListener

    private lateinit var tabLayout : com.google.android.material.tabs.TabLayout
    private lateinit var viewPager : ViewPager2

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

        if (!checkPermission()){
            requestPermission()
        }

        if (File(applicationContext.filesDir, saveAllMusicsFile).exists()){
            musics = readAllMusicsFromFile(saveAllMusicsFile)
            allMusicsBackup = ArrayList(musics.map { it.copy() })
            adapter = MusicList(musics, "Main",applicationContext, this)

            //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
            /*
            menuRecyclerView?.layoutManager = LinearLayoutManager(this)
            menuRecyclerView?.adapter = adapter
             */
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
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

                    Log.d("GET ALL MUSICS","")
                    adapter = MusicList(musics,"Main", applicationContext, this)

                    //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
                    /*
                    menuRecyclerView?.layoutManager = LinearLayoutManager(this)
                    menuRecyclerView?.adapter = adapter

                     */
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                }
            }
        }
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = VpAdapter(this)

        TabLayoutMediator(tabLayout, viewPager){tab, index ->
            tab.text = when(index){
                0 -> {"Musics"}
                1 -> {"Playlists"}
                2 -> {"Albums"}
                3 -> {"Artists"}
                else -> { throw Resources.NotFoundException("Position not found")}
            }
        }.attach()

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
        val shuffleButton = findViewById<Button>(R.id.shuffle_button)

        playlistsButton.setOnClickListener{ playlistButton() }
        shuffleButton.setOnClickListener { playRandom(musics, this@MainActivity) }

        // On ajoute nos musiques et playlists dans notre mediaplayer :

        GlobalScope.launch(Dispatchers.IO){
            launch{readPlaylistsAsync()}
        }
        println("end")

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val openMenu = findViewById<ImageView>(R.id.open_menu)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        openMenu.setOnClickListener { openNavigationMenu(drawerLayout) }
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

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        noSongPlaying.visibility = View.VISIBLE

        val time = measureTimeMillis {

            if (MyMediaPlayer.currentIndex != -1) {
                GlobalScope.launch(Dispatchers.IO) {
                    launch {
                        withContext(Dispatchers.Main) {
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
                                withContext(Dispatchers.Main) {
                                    albumCoverInfo.setImageBitmap(bitmap)
                                }
                            } else {
                                albumCoverInfo.setImageResource(R.drawable.michael)
                            }
                        }
                    }
                }

                pausePlay?.setOnClickListener { pausePlay() }
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

    private fun playlistButton() {
        val intent = Intent(this@MainActivity,PlaylistsMenuActivity::class.java)
        startActivity(intent)
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.download_data -> {
                GlobalScope.launch(Dispatchers.IO){
                    launch{
                        retrieveAllMusicsFromApp()
                    }
                }
                Toast.makeText(this, "Data retrieved", Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                true
            }
        }
    }
}
