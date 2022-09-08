package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectedArtistActivity : Tools(), MusicList.OnMusicListener, SearchView.OnQueryTextListener {
    private lateinit var artist : Artist
    private var artistPosition : Int = 0
    private lateinit var adapter : MusicList
    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var searchView : SearchView
    private lateinit var menuRecyclerView : RecyclerView

    private lateinit var onAudioFocusChange : AudioManager.OnAudioFocusChangeListener
    private lateinit var audioAttributes : AudioAttributes
    private lateinit var audioManager : AudioManager

    private lateinit var pausePlayButton : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.album_and_artist_layout)

        pausePlayButton = findViewById(R.id.pause_play)
        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        artistPosition = intent.getSerializableExtra("POSITION") as Int

        artist = MyMediaPlayer.allArtists[artistPosition]
        musics = artist.musicList
        allMusicsBackup = ArrayList(musics.map { it.copy() })

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)

        adapter = MusicList(musics, artist.artistName, applicationContext, this@SelectedArtistActivity)
        menuRecyclerView.layoutManager = LinearLayoutManager(this@SelectedArtistActivity)
        menuRecyclerView.adapter = adapter

        val artistName = findViewById<TextView>(R.id.playlist_name)
        artistName?.text = artist.artistName

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        if (MyMediaPlayer.currentIndex == -1){
            noSongPlaying.visibility = View.VISIBLE
            infoSongPlaying.visibility = View.GONE
        } else {
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null){
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

            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedArtistActivity) }
            songTitleInfo?.isSelected = true
        }

        val shuffleButton = findViewById<ImageView>(R.id.shuffle)
        shuffleButton.setOnClickListener { playRandom(musics, this@SelectedArtistActivity) }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        onAudioFocusChange = AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d("focusChange", focusChange.toString())
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> println("gain")
                else -> {
                    if (mediaPlayer.isPlaying) {
                        println("loss focus")
                        mediaPlayer.pause()
                        pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                    }
                    Log.d("change does..", "")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()

        allMusicsBackup = MyMediaPlayer.allArtists[artistPosition].musicList
        adapter.musics = MyMediaPlayer.allArtists[artistPosition].musicList
        adapter.notifyDataSetChanged()

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        noSongPlaying.visibility = View.VISIBLE

        if (MyMediaPlayer.currentIndex != -1){
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null){
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

            pausePlayButton.setOnClickListener{ pausePlay() }
            nextBtn?.setOnClickListener{ playNextSong(adapter) }
            previousBtn?.setOnClickListener{ playPreviousSong(adapter) }
            bottomInfos.setOnClickListener{ onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedArtistActivity) }
            songTitleInfo?.isSelected = true
        }

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
        // Vérifions si on change de playlist :
        if (musics != MyMediaPlayer.initialPlaylist) {
            MyMediaPlayer.currentPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.playlistName = artist.artistName
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        val intent = Intent(this@SelectedArtistActivity,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("ARTIST POSITION", artistPosition)

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
                musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)
                MyMediaPlayer.allMusics.remove(musicToRemove)

                GlobalScope.launch(Dispatchers.IO){
                    launch{writeAllMusicsToFile(saveAllMusicsFile,MyMediaPlayer.allMusics)}
                }

                Toast.makeText(this,resources.getString(R.string.deleted_from_playlist),Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                // MODIFY INFOS :
                val intent = Intent(this@SelectedArtistActivity,ModifyMusicInfoActivity::class.java)
                intent.putExtra("PATH", musics[item.groupId].path)
                resultModifyMusic.launch(intent)
                true
            }
            3 -> {
                // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                MyMediaPlayer.initialPlaylist.remove((musics[item.groupId]))
                MyMediaPlayer.currentPlaylist.remove((musics[item.groupId]))

                MyMediaPlayer.currentPlaylist.add(MyMediaPlayer.currentIndex+1, musics[item.groupId])
                Toast.makeText(this,resources.getString(R.string.music_will_be_played_next),Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                onContextItemSelected(item)
            }
        }
    }

    private var resultModifyMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

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
                    } else {
                        musics = ArrayList<Music>()
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
                    } else {
                        musics = ArrayList<Music>()
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

    private fun pausePlay() {
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(onAudioFocusChange)
            .build()

        when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,resources.getString(R.string.cannot_launch_song), Toast.LENGTH_SHORT).show()
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                if(mediaPlayer.isPlaying){
                    mediaPlayer.pause()
                    pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                } else {
                    mediaPlayer.start()
                    pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                }
            }
            else -> {
                Toast.makeText(this,resources.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            playlistCover.setImageResource(R.drawable.michael)
            val drawable = playlistCover.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            bitmap = bitmapDrawable.bitmap
        }

        val backgroundColor: Palette.Swatch? =
            if (Palette.from(bitmap as Bitmap).generate().darkVibrantSwatch == null) {
                Palette.from(bitmap as Bitmap).generate().swatches[0]
            } else {
                Palette.from(bitmap as Bitmap).generate().darkVibrantSwatch
            }

        findViewById<LinearLayout>(R.id.background).setBackgroundColor(backgroundColor?.rgb as Int)

        val lighterColorTheme = ColorUtils.blendARGB(backgroundColor.rgb, Color.WHITE,0.1f)
        val buttonPanel = findViewById<LinearLayout>(R.id.buttons_panel)

        searchView.background.colorFilter = BlendModeColorFilter(lighterColorTheme, BlendMode.SRC_ATOP)
        buttonPanel.background.colorFilter = BlendModeColorFilter(lighterColorTheme, BlendMode.SRC_ATOP)
        menuRecyclerView.background.colorFilter = BlendModeColorFilter(lighterColorTheme, BlendMode.SRC_ATOP)
        adapter.backgroundColor = lighterColorTheme
        adapter.colorsForText = backgroundColor
        adapter.notifyDataSetChanged()

        val shuffleButton = findViewById<ImageView>(R.id.shuffle)
        val playlistName = findViewById<TextView>(R.id.playlist_name)
        playlistName.setBackgroundColor(ColorUtils.setAlphaComponent(backgroundColor.rgb as Int,150))
        playlistName.setTextColor(backgroundColor.titleTextColor)

        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        findViewById<ImageView>(R.id.modify_playlist).setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        shuffleButton.setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.previous).setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.next).setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.pause_play).setColorFilter(backgroundColor.titleTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<TextView>(R.id.song_title_info).setTextColor(backgroundColor.titleTextColor)
        bottomInfos.setBackgroundColor(lighterColorTheme)
        noSongPlaying.setTextColor(backgroundColor.titleTextColor)

        window.navigationBarColor = lighterColorTheme
        window.statusBarColor = lighterColorTheme
    }
}
