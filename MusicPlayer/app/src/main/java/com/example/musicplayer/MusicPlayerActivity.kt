package com.example.musicplayer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import com.example.musicplayer.notification.MusicNotificationService
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

// Classe représentant la lecture d'une musique :
class MusicPlayerActivity : Tools(), MediaPlayer.OnPreparedListener, MusicList.OnMusicListener {

    private lateinit var titleTv : TextView
    lateinit var currentTimeTv : TextView
    private lateinit var totalTimeTv : TextView
    lateinit var seekBar : SeekBar
    private lateinit var pausePlayButton : ImageView
    private lateinit var nextBtn : ImageView
    private lateinit var previousBtn : ImageView
    private lateinit var musicIcon : ImageView
    private lateinit var favoriteBtn : ImageView
    private lateinit var currentSong : Music
    private lateinit var sort : ImageView

    private lateinit var bottomSheetLayout: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var headerCurrentListButton: TextView
    private lateinit var currentListRecyclerView: RecyclerView
    private lateinit var adapter : MusicList
    private var changingFavouriteState = false

    private var myThread = Thread(FunctionalSeekBar(this))

    private var sameMusic = false

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            if (intent.extras?.getBoolean("STOP") != null && intent.extras?.getBoolean("STOP") as Boolean) {
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else if (intent.extras?.getBoolean("STOP") != null && !(intent.extras?.getBoolean("STOP") as Boolean)){
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            if (intent.extras?.getBoolean("FAVORITE_CHANGED") != null && (intent.extras?.getBoolean("FAVORITE_CHANGED") as Boolean)){
                Log.d("MPA", "favorite changed"+currentSong.favorite)
                if (currentSong.favorite) {
                    favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
                } else {
                    favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                }
            }
            titleTv.text = currentSong.name
            CoroutineScope(Dispatchers.Main).launch { setColor() }
        }
    }

    private var newPrimaryColor = R.color.primary_color
    private var newSecondaryColor = R.color.secondary_color
    private var newTextColor = R.color.text_color

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("SAME MUSIC", true)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        sameMusic = intent.getSerializableExtra("SAME MUSIC") as Boolean
        if (savedInstanceState != null) {
            sameMusic = savedInstanceState.getBoolean("SAME MUSIC")
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        titleTv = findViewById(R.id.song_title)
        currentTimeTv = findViewById(R.id.current_time)
        totalTimeTv = findViewById(R.id.total_time)
        seekBar = findViewById(R.id.seek_bar)
        pausePlayButton = findViewById(R.id.pause_play)
        nextBtn = findViewById(R.id.next)
        previousBtn = findViewById(R.id.previous)
        musicIcon = findViewById(R.id.album_cover_big)
        favoriteBtn = findViewById(R.id.favorite)
        sort = findViewById(R.id.sort)
        sort.setImageResource(MyMediaPlayer.iconsList[MyMediaPlayer.iconIndex])

        titleTv.isSelected = true

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        CoroutineScope(Dispatchers.Main).launch { mediaPlayer.setOnCompletionListener { playNextSong() } }

        currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

        pausePlayButton.setOnClickListener{ pausePlay(pausePlayButton) }
        nextBtn.setOnClickListener{ playNextSong() }
        previousBtn.setOnClickListener{ playPreviousSong() }
        favoriteBtn.setOnClickListener{ setFavorite() }
        sort.setOnClickListener{ changeSorting() }

        musicIcon.setOnLongClickListener { showBottomSheet() }

        CoroutineScope(Dispatchers.Main).launch { playMusic() }

        this@MusicPlayerActivity.runOnUiThread(myThread)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    updateMusicNotification(!mediaPlayer.isPlaying)
                    currentTimeTv.text = convertDuration(mediaPlayer.currentPosition.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        CoroutineScope(Dispatchers.Main).launch { findViewById<ImageView>(R.id.quit_activity).setOnClickListener{ finish() }}

        registerReceiver(broadcastReceiver, IntentFilter("BROADCAST"))

        bottomSheetLayout = findViewById(R.id.bottom_sheet_dialog_song_list)
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        headerCurrentListButton = findViewById(R.id.bottom_sheet_current_playlist_text_view)
        currentListRecyclerView = findViewById(R.id.bottom_sheet_recycler_view)

        CoroutineScope(Dispatchers.IO).launch{
            adapter = MusicList(MyMediaPlayer.currentPlaylist, "currentList",this@MusicPlayerActivity,this@MusicPlayerActivity)
            currentListRecyclerView.layoutManager = LinearLayoutManager(this@MusicPlayerActivity)
            currentListRecyclerView.adapter = adapter
            setSorting()
        }

        CoroutineScope(Dispatchers.Main).launch { headerCurrentListButton.setOnClickListener { openCloseBottomSheet() }}

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_SETTLING) {
                    currentListRecyclerView.layoutManager?.scrollToPosition(MyMediaPlayer.currentIndex)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Si on a plus de musiques dans la playlist à jouer, il faut quitter cette activité
        if(MyMediaPlayer.currentIndex == -1) {
            Log.d("MUSIC PLAYER ACTIVITY", "CURRENT INDEX == -1")
            finish()
        } else {
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

            if (!mediaPlayer.isPlaying){
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }

            CoroutineScope(Dispatchers.Main).launch { setColor() }

            // Vérifions si la musique est en favoris :
            CoroutineScope(Dispatchers.Main).launch { getFavoriteState() }

            titleTv.text = currentSong.name
            songTitleInfo?.text = currentSong.name
            totalTimeTv.text = convertDuration(currentSong.duration)
        }
    }

    private fun setResourcesWithMusic(){
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)

        currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

        CoroutineScope(Dispatchers.Main).launch { setColor() }
        // Vérifions si la musique est en favoris :
        getFavoriteState()

        titleTv.text = currentSong.name
        songTitleInfo?.text = currentSong.name
        totalTimeTv.text = convertDuration(currentSong.duration)

        playMusic()
    }

    override fun playMusic(){
        /*
        Si la musique est la même, alors on ne met à jour que la seekBar (elle se remettra au bon niveau automatiquement)
         */
        if (!sameMusic) {
            mediaPlayer.reset()
            try {
                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(PlaybackService.audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
                    .build()

                when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                        Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
                    }

                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        mediaPlayer.setDataSource(currentSong.path)
                        mediaPlayer.setOnPreparedListener(this)
                        mediaPlayer.prepareAsync()
                    }
                    else -> {
                        Toast.makeText(this,"AN unknown error has come up", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                Log.e("ERROR MEDIA PLAYER", e.toString())
                e.printStackTrace()
            }
        } else {
            seekBar.progress = 0
            seekBar.max = mediaPlayer.duration

            if (!mediaPlayer.isPlaying){
                pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            }
        }
    }

    override fun onPrepared(p0: MediaPlayer?) {
        mediaPlayer.start()
        updateMusicNotification(false)
        seekBar.progress = 0
        seekBar.max = mediaPlayer.duration
        pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Si il n'y a pas de notifications, on l'affiche
        if(notificationManager.activeNotifications.isEmpty()) {
            val service = MusicNotificationService(applicationContext as Context)
            if (mediaPlayer.isPlaying){
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                service.showNotification(R.drawable.ic_baseline_pause_24)
            } else {
                pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                service.showNotification(R.drawable.ic_baseline_play_arrow_24)
            }
        }

        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
        if (mediaPlayer.isPlaying){
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            intentForNotification.putExtra("STOP", false)
        } else {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            intentForNotification.putExtra("STOP", true)
        }
        applicationContext.sendBroadcast(intentForNotification)
    }

    private fun openCloseBottomSheet() {
        if(sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED){
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    private fun changeSorting() {
        if(MyMediaPlayer.iconIndex == (MyMediaPlayer.iconsList.size)-1){
            MyMediaPlayer.iconIndex = 0
        } else {
            MyMediaPlayer.iconIndex += 1
        }
        setSorting()
    }

    private fun setSorting(){
        when (MyMediaPlayer.iconIndex){
            0 -> {
                MyMediaPlayer.currentPlaylist = ArrayList(MyMediaPlayer.initialPlaylist.map { it.copy() })
                MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(MyMediaPlayer.currentPlaylist.find { it.path == currentSong.path })
            }
            1 -> {
                MyMediaPlayer.currentPlaylist.shuffle()
                MyMediaPlayer.currentPlaylist.remove(currentSong)
                MyMediaPlayer.currentPlaylist.add(0,currentSong)
                MyMediaPlayer.currentIndex = 0
            }
            2 -> {
                // On choisit la fonction de replay de la meme musique, on supprime d'abord toute la playlist actuelle :
                MyMediaPlayer.currentPlaylist.clear()
                MyMediaPlayer.currentPlaylist.add(currentSong)
                MyMediaPlayer.currentIndex = 0
            }
        }
        sort.setImageResource(MyMediaPlayer.iconsList[MyMediaPlayer.iconIndex])
        adapter.musics = MyMediaPlayer.currentPlaylist
        adapter.notifyDataSetChanged()
    }

    private fun playNextSong(){
        sameMusic = false
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        mediaPlayer.reset()
        setResourcesWithMusic()
    }

    private fun playPreviousSong(){
        sameMusic = false
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        mediaPlayer.reset()
        setResourcesWithMusic()
    }

    // Permet de savoir si une chanson est en favoris :
    private fun getFavoriteState(){
        if(MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite){
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
        } else {
            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }
    }

    // Permet de changer le statut favoris de la chanson :
    private fun setFavorite(){
        CoroutineScope(Dispatchers.Main).launch {
            if (!changingFavouriteState) {
                changingFavouriteState = true
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if(currentSong.favorite){
                            MyMediaPlayer.initialPlaylist.find { it.path == currentSong.path }?.favorite = false
                            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = false
                            currentSong.favorite = false
                            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                        } else {
                            MyMediaPlayer.initialPlaylist.find { it.path == currentSong.path }?.favorite = true
                            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = true
                            currentSong.favorite = true
                            favoriteBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
                        }

                        // il faut maintenant sauvegardé l'état de la musique dans TOUTES les playlists :
                        // Commencons par la playlist principale :
                        MyMediaPlayer.allMusics.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                        // Ensuite, mettons à jour nos playlists :
                        val allPlaylists = MyMediaPlayer.allPlaylists
                        for (playlist in allPlaylists){
                            playlist.musicList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                        }
                        // Mettons à jour la playlist Favorites :
                        val favoritePlaylist = allPlaylists[0]
                        var shouldBeInFavoriteList = true
                        for (element in favoritePlaylist.musicList){
                            if (element.path == currentSong.path){
                                favoritePlaylist.musicList.remove(element)
                                shouldBeInFavoriteList = false
                                break
                            }
                        }
                        if (shouldBeInFavoriteList){
                            favoritePlaylist.musicList.add(currentSong)
                        }

                        val posInShortcuts = MyMediaPlayer.allShortcuts.positionInList(currentSong)
                        if (posInShortcuts != -1) {
                            (MyMediaPlayer.allShortcuts.shortcutsList[posInShortcuts] as Music).favorite = currentSong.favorite
                            CoroutineScope(Dispatchers.IO).launch {
                                writeAllShortcuts()
                            }
                        }

                        // Mettons à jour les albums et les artistes :
                        for (album in MyMediaPlayer.allAlbums) {
                            if (album.albumList.find { it.path == currentSong.path } != null) {
                                album.albumList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                            }
                        }

                        for (artist in MyMediaPlayer.allArtists) {
                            if (artist.musicList.find { it.path == currentSong.path } != null) {
                                artist.musicList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                            }
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            writeAllMusics()
                            writeAllPlaylists()
                            changingFavouriteState = false
                        }

                        updateMusicNotification(!mediaPlayer.isPlaying)
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        Log.e("error", e.toString())
                    }
                }
            }
        }
    }

    fun convertDuration(duration: Long): String {
        val minutes: Float = duration.toFloat() / 1000 / 60
        val seconds: Float = duration.toFloat() / 1000 % 60

        val strMinutes: String = minutes.toString().split(".")[0]

        val strSeconds = if (seconds < 10.0) {
            "0" + seconds.toString().split(".")[0]
        } else {
            seconds.toString().split(".")[0]
        }

        return "$strMinutes:$strSeconds"
    }

    class FunctionalSeekBar(private var musicPlayerActivity: MusicPlayerActivity) : Runnable{

        override fun run() {
            musicPlayerActivity.seekBar.progress = musicPlayerActivity.mediaPlayer.currentPosition
            musicPlayerActivity.currentTimeTv.text = musicPlayerActivity.convertDuration(musicPlayerActivity.mediaPlayer.currentPosition.toLong())

            Handler(Looper.getMainLooper()).postDelayed(this,1000)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun setColor(){
        var bitmap : Bitmap? = null
        if (currentSong.albumCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentSong.albumCover
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            musicIcon.setImageBitmap(bitmap)
        } else {
            musicIcon.setImageResource(R.drawable.saxophone)
            bitmap = (ResourcesCompat.getDrawable(this.resources, R.drawable.saxophone, null) as BitmapDrawable).bitmap
        }
        val dominantColor: Palette.Swatch? =
            if (Palette.from(bitmap as Bitmap).generate().lightVibrantSwatch == null) {
                Palette.from(bitmap).generate().dominantSwatch
            } else {
                Palette.from(bitmap).generate().lightVibrantSwatch
            }
        //val dominantColor: Palette.Swatch? = Palette.from(bitmap as Bitmap).generate().dominantSwatch
        newPrimaryColor = ColorUtils.blendARGB(getColor(R.color.primary_color),dominantColor?.rgb as Int,0.1f)
        newSecondaryColor = ColorUtils.blendARGB(getColor(R.color.secondary_color),dominantColor.rgb,0.2f)
        newTextColor = ColorUtils.blendARGB(getColor(R.color.text_color),dominantColor.rgb,0.1f)

        findViewById<LinearLayout>(R.id.music_player).setBackgroundColor(newPrimaryColor)
        titleTv.setTextColor(newTextColor)
        currentTimeTv.setTextColor(newTextColor)
        totalTimeTv.setTextColor(newTextColor)

        seekBar.progressDrawable.setTint(newTextColor)
        seekBar.thumb.setTint(newTextColor)

        pausePlayButton.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        nextBtn.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        previousBtn.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        favoriteBtn.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        findViewById<ImageView>(R.id.quit_activity).setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        sort.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)

        bottomSheetLayout.background.colorFilter = BlendModeColorFilter(newSecondaryColor, BlendMode.SRC_ATOP)
        adapter.backgroundColor = newSecondaryColor
        adapter.notifyDataSetChanged()

        window.navigationBarColor = newSecondaryColor
        window.statusBarColor = newPrimaryColor
    }

    override fun onDestroy() {
        Log.d("MP","ON DESTROY CALLED")
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onMusicClick(position: Int) {
        if (MyMediaPlayer.currentIndex != position) {
            sameMusic = false
            MyMediaPlayer.currentIndex = position
            setResourcesWithMusic()
        }
    }

    @SuppressLint("ResourceAsColor")
    override fun onLongMusicClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_menu)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<LinearLayout>(R.id.bottom_sheet)?.setBackgroundColor(newPrimaryColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.add_to_a_playlist_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.add_to_a_playlist_text)?.setTextColor(newTextColor)
        bottomSheetDialog.findViewById<TextView>(R.id.delete_music)?.apply {
            setTextColor(newTextColor)
            text = getString(R.string.remove_from_played_list)
        }
        bottomSheetDialog.findViewById<ImageView>(R.id.remove_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<ImageView>(R.id.modify_music_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.modify_music_text)?.setTextColor(newTextColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.play_next_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.play_next_text)?.setTextColor(newTextColor)
        bottomSheetDialog.window?.navigationBarColor = newPrimaryColor

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_a_playlist)?.setOnClickListener {
            bottomSheetAddTo(position, this, adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_shortcuts)?.setOnClickListener {
            addSelectedShortcut(adapter.musics[position])
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.remove)?.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            builder.setTitle(getString(R.string.remove_from_played_list))

            builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                val currentMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
                // Si on supprime la musique que l'on joue actuellement, on passe si possible à la suivante :
                if (MyMediaPlayer.currentIndex == position) {
                    if (MyMediaPlayer.currentPlaylist.size > 1) {
                        playNextSong()
                        MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentMusic)
                    } else {
                        stopMusic()
                        Toast.makeText(
                            this,
                            resources.getString(R.string.no_songs_left_in_the_current_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    MyMediaPlayer.currentPlaylist.removeAt(position)
                } else {
                    MyMediaPlayer.currentPlaylist.removeAt(position)
                    MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentMusic)
                }
                adapter.notifyItemRemoved(position)

                Toast.makeText(
                    this,
                    resources.getString(R.string.removed_from_played_list),
                    Toast.LENGTH_SHORT
                ).show()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                dialogInterface.cancel()
            }

            builder.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_music)?.setOnClickListener {
            bottomSheetModifyMusic(this,position,adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.play_next)?.setOnClickListener {
            bottomSheetPlayNextInCurrentPlaylist(adapter,position)
            bottomSheetDialog.dismiss()
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun showBottomSheet() : Boolean {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_music_player_activity)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<LinearLayout>(R.id.bottom_sheet)?.setBackgroundColor(newPrimaryColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.add_to_a_playlist_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.add_to_a_playlist_text)?.setTextColor(newTextColor)
        bottomSheetDialog.findViewById<ImageView>(R.id.modify_music_img)?.setColorFilter(newTextColor, PorterDuff.Mode.MULTIPLY)
        bottomSheetDialog.findViewById<TextView>(R.id.modify_music_text)?.setTextColor(newTextColor)
        bottomSheetDialog.window?.navigationBarColor = newPrimaryColor

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_a_playlist)?.setOnClickListener {
            bottomSheetAddTo(MyMediaPlayer.currentIndex, this, adapter)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.add_to_shortcuts)?.setOnClickListener {
            addSelectedShortcut(currentSong)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.findViewById<LinearLayout>(R.id.modify_music)?.setOnClickListener {
            bottomSheetModifyMusic(this,MyMediaPlayer.currentIndex,adapter)
            bottomSheetDialog.dismiss()
        }

        return true
    }

    override fun onBackPressed() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
}