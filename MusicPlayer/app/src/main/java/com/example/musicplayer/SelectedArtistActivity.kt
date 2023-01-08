package com.example.musicplayer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.classes.Artist
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelectedArtistActivity : Tools(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {
    private lateinit var artist : Artist
    private var artistPosition : Int = 0
    private lateinit var adapter : MusicList
    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var searchView : SearchView
    private lateinit var menuRecyclerView : RecyclerView
    private var searchIsOn = false

    private lateinit var pausePlayButton : ImageView

    private lateinit var bottomSheetLayout: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

    private var newPrimaryColor = R.color.primary_color
    private var newSecondaryColor = R.color.secondary_color
    private var newTextColor = R.color.text_color

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

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        pausePlayButton = findViewById(R.id.pause_play)
        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        artistPosition = intent.getSerializableExtra("POSITION") as Int

        findViewById<ImageView>(R.id.modify_playlist).visibility = View.GONE
        findViewById<ImageView>(R.id.add_songs).visibility = View.GONE

        artist = MyMediaPlayer.allArtists[artistPosition]
        musics = artist.musicList
        allMusicsBackup = ArrayList(musics.map { it.copy() })

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        adapter = MusicList(musics, artist.artistName, this, this)
        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuRecyclerView.adapter = adapter

        val artistName = findViewById<TextView>(R.id.playlist_name)
        artistName?.text = artist.artistName

        val quitActivity = findViewById<ImageView>(R.id.quit_activity)
        quitActivity.setOnClickListener{ finish() }

        val shuffleButton = findViewById<ImageView>(R.id.shuffle)
        shuffleButton.setOnClickListener { playRandom(musics, this, artist.artistName) }

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

        allMusicsBackup = MyMediaPlayer.allArtists[artistPosition].musicList
        adapter.musics = MyMediaPlayer.allArtists[artistPosition].musicList
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
                    albumCoverInfo.setImageResource(R.drawable.ic_saxophone_svg)
                }

                pausePlayButton.setOnClickListener { pausePlay(pausePlayButton) }
                nextBtn?.setOnClickListener { playNextSong(adapter) }
                previousBtn?.setOnClickListener { playPreviousSong(adapter) }
                songTitleInfo?.isSelected = true
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
        musicClicked(this, adapter, position, artist.artistName)
    }

    @SuppressLint("ResourceAsColor")
    override fun onLongMusicClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_menu)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<LinearLayout>(R.id.bottom_sheet)?.setBackgroundColor(newPrimaryColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.add_to_a_playlist_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.add_to_a_playlist_text)?.setTextColor(newTextColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.remove_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<ImageView>(R.id.modify_music_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.modify_music_text)?.setTextColor(newTextColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.play_next_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.play_next_text)?.setTextColor(newTextColor)
        bottomSheetDialog.window?.navigationBarColor = newPrimaryColor

        bottomSheetDialog.findViewById<TextView>(R.id.delete_music)?.apply {
            text = getString(R.string.remove_from_playlist)
            setTextColor(newTextColor)
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_a_playlist)?.setOnClickListener {
            bottomSheetAddTo(position, this, adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_shortcuts)?.setOnClickListener {
            addSelectedShortcut(adapter.musics[position])
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
            bottomSheetRemoveFromApp(adapter,position, sheetBehavior, this)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_music)?.setOnClickListener {
            bottomSheetModifyMusic(this,position,adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.play_next)?.setOnClickListener {
            bottomSheetPlayNext(adapter,position)
            bottomSheetDialog.dismiss()
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

    @SuppressLint("ResourceAsColor")
    private fun setColorTheme(){
        var bitmap: Bitmap? = null
        val playlistCover = findViewById<ImageView>(R.id.cover)

        if (artist.artistCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = artist.artistCover
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            playlistCover.setImageBitmap(bitmap)
        } else {
            playlistCover.setImageResource(R.drawable.ic_saxophone_svg)
            val drawable = playlistCover.drawable
            bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }

        val dominantColor: Palette.Swatch? =
            if (Palette.from(bitmap as Bitmap).generate().lightVibrantSwatch == null) {
                Palette.from(bitmap).generate().dominantSwatch
            } else {
                Palette.from(bitmap).generate().lightVibrantSwatch
            }
        newPrimaryColor = ColorUtils.blendARGB(getColor(R.color.primary_color),dominantColor?.rgb as Int,0.1f)
        newSecondaryColor = ColorUtils.blendARGB(getColor(R.color.secondary_color),dominantColor.rgb,0.1f)
        newTextColor = ColorUtils.blendARGB(getColor(R.color.text_color),dominantColor.rgb,0.1f)

        searchView.background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        findViewById<LinearLayout>(R.id.playlists).background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        adapter.backgroundColor = newSecondaryColor
        adapter.notifyDataSetChanged()

        findViewById<CoordinatorLayout>(R.id.playlist_activity).setBackgroundColor(newPrimaryColor)
        findViewById<TextView>(R.id.playlist_name).setBackgroundColor(ColorUtils.setAlphaComponent(newPrimaryColor,150))
        findViewById<TextView>(R.id.playlist_name).setTextColor(newTextColor)
        findViewById<TextView>(R.id.song_title_info).setTextColor(newTextColor)

        findViewById<ImageView>(R.id.quit_activity).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.quit_activity).background.colorFilter = BlendModeColorFilter(newPrimaryColor, BlendMode.SRC_ATOP)
        findViewById<ImageView>(R.id.shuffle).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.previous).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.next).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.pause_play).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)

        findViewById<LinearLayout>(R.id.bottom_infos).setBackgroundColor(newPrimaryColor)
        findViewById<LinearLayout>(R.id.buttons_panel).background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        findViewById<LinearLayout>(R.id.background).setBackgroundColor(newPrimaryColor)

        window.navigationBarColor = newPrimaryColor
        window.statusBarColor = newSecondaryColor
    }
}