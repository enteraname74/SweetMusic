package com.example.musicplayer

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.musicplayer.adapters.VpAdapter
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.*

class MainActivity : Tools(), NavigationView.OnNavigationItemSelectedListener  {

    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var tabLayout : com.google.android.material.tabs.TabLayout
    private lateinit var fetchingSongs : LinearLayout
    private lateinit var viewPager : ViewPager2

    private lateinit var pausePlayButton : ImageView

    private lateinit var bottomSheetLayout: LinearLayout
    lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

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
        setContentView(R.layout.activity_main)

        pausePlayButton = findViewById(R.id.pause_play)
        fetchingSongs = findViewById(R.id.fetching_songs)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        if (SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                requestPermissionToWrite()
            }
        }

        if (!checkPermission()){
            requestPermission()
        }

        if (File(applicationContext.filesDir, saveAllMusicsFile).exists()){
            CoroutineScope(Dispatchers.Main).launch {
                MyMediaPlayer.allMusics = readAllMusicsFromFile(saveAllMusicsFile)
                allMusicsBackup = MyMediaPlayer.allMusics.map{ it.copy() } as ArrayList<Music> /* = java.util.ArrayList<com.example.musicplayer.Music> */
                fetchingSongs.visibility = View.GONE
                viewPager.visibility = View.VISIBLE
            }
        }

        viewPager.adapter = VpAdapter(this)

        TabLayoutMediator(tabLayout, viewPager){tab, index ->
            tab.text = when(index){
                0 -> {resources.getString(R.string.musics)}
                1 -> {resources.getString(R.string.playlists)}
                2 -> {resources.getString(R.string.albums)}
                3 -> {resources.getString(R.string.artists)}
                else -> { throw Resources.NotFoundException("Position not found")}
            }
        }.attach()


        val shuffleButton = findViewById<Button>(R.id.shuffle_button)
        shuffleButton.setOnClickListener { playRandom(MyMediaPlayer.allMusics, this) }

        CoroutineScope(Dispatchers.IO).launch{ readPlaylistsAsync() }
        CoroutineScope(Dispatchers.IO).launch { readAllDeletedMusicsFromFile() }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val openMenu = findViewById<ImageView>(R.id.open_menu)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        openMenu.setOnClickListener { openNavigationMenu(drawerLayout) }

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
                this@MainActivity
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Si nous rentrons dans cette condition, c'est que l'utilisateur ouvre l'application pour la première fois
        // Si on a la permission et qu'on a pas encore de fichiers avec des musiques, alors on va chercher nos musiques :
        if (checkPermission() && !File(applicationContext.filesDir, saveAllMusicsFile).exists()){
            // Créons d'abord la playlist des favoris :
            CoroutineScope(Dispatchers.IO).launch {
                val favoritePlaylist = Playlist("Favorites",ArrayList(),null, true)
                MyMediaPlayer.allPlaylists = ArrayList<Playlist>()
                MyMediaPlayer.allPlaylists.add(favoritePlaylist)
                writePlaylistsToFile()
            }

            //CoroutineScope(Dispatchers.IO).launch { fetchMusics() }
        }

        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        if (MyMediaPlayer.currentIndex != -1) {
            CoroutineScope(Dispatchers.Main).launch {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
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

                pausePlayButton.setOnClickListener { pausePlay(pausePlayButton) }
                songTitleInfo?.isSelected = true
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        if (!mediaPlayer.isPlaying) {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }

        registerReceiver(broadcastReceiver, IntentFilter("BROADCAST"))

        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.find_new_songs -> {
                val intent = Intent(this@MainActivity,FindNewSongsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.download_data -> {
                CoroutineScope(Dispatchers.Main).launch { retrieveAllMusicsFromApp() }
                true
            }
            R.id.set_data -> {
                val intent = Intent(this@MainActivity,SetDataActivity::class.java)
                setDataResult.launch(intent)
                true
            }
            else -> {
                true
            }
        }
    }

    private var setDataResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private suspend fun fetchMusics() {
        // Pour éviter de potentiels crash de l'app :
        val shuffleButton = findViewById<Button>(R.id.shuffle_button)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val openMenu = findViewById<ImageView>(R.id.open_menu)

        withContext(Dispatchers.Main) {
            shuffleButton.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            openMenu.visibility = View.GONE
        }

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
                Toast.makeText(this, resources.getString(R.string.cannot_retrieve_files), Toast.LENGTH_SHORT).show()
            }
            else -> {
                while (cursor.moveToNext()) {
                    val albumId = cursor.getLong(5)
                    val albumUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
                    )

                    val albumCover : ByteArray? = try {
                        val bitmap = ThumbnailUtils.createAudioThumbnail(File(cursor.getString(4)),Size(350,350),null)
                        bitmapToByteArray(bitmap)
                    } catch (error : IOException){
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
                        MyMediaPlayer.allMusics.add(music)
                    }
                }
                cursor.close()
                MyMediaPlayer.allMusics

                writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)

                val openMenu = findViewById<ImageView>(R.id.open_menu)
                withContext(Dispatchers.Main){
                    fetchingSongs.visibility = View.GONE
                    viewPager.visibility = View.VISIBLE
                    shuffleButton.visibility = View.VISIBLE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    openMenu.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkPermission() : Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            69
        )
    }

    private fun requestPermissionToWrite(){
        val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

        if (SDK_INT >= 30) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}
