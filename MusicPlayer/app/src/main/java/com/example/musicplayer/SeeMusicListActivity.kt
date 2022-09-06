package com.example.musicplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class SeeMusicListActivity : Tools(),MusicList.OnMusicListener {
    private var list = ArrayList<Music>()
    private lateinit var adapter : MusicList
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var listName : TextView
    private lateinit var listType : String

    private lateinit var onAudioFocusChange : AudioManager.OnAudioFocusChangeListener
    private lateinit var audioAttributes : AudioAttributes
    private lateinit var audioManager : AudioManager

    private lateinit var pausePlayButton : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_see_music_list)

        menuRecyclerView = findViewById(R.id.songs_list)
        listName = findViewById(R.id.list_name)

        listType = intent.getSerializableExtra("LIST-TYPE") as String

        if (listType == "initialList"){
            listName.text = "Initial List"
            list = MyMediaPlayer.initialPlaylist
        } else {
            listName.text = "Current List"
            list = MyMediaPlayer.currentPlaylist
        }

        CoroutineScope(Dispatchers.IO).launch{
            adapter = MusicList(list, listName.text as String,applicationContext,this@SeeMusicListActivity)
            menuRecyclerView.layoutManager = LinearLayoutManager(this@SeeMusicListActivity)
            menuRecyclerView.adapter = adapter
        }

        pausePlayButton = findViewById(R.id.pause_play)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)

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
            pausePlayButton.setOnClickListener{ pausePlay() }
            nextBtn?.setOnClickListener{ playNextSong(adapter) }
            previousBtn?.setOnClickListener{ playPreviousSong(adapter) }
            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@SeeMusicListActivity) }
            songTitleInfo.isSelected = true
        }

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
        if (MyMediaPlayer.modifiedSong) {
            GlobalScope.launch(Dispatchers.IO) {
                launch {
                    writeAllAsync(
                        MyMediaPlayer.allMusics,
                        MyMediaPlayer.allPlaylists
                    )
                }
            }
            adapter.musics = list
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            MyMediaPlayer.modifiedSong = false
        }

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        noSongPlaying.visibility = View.VISIBLE

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

            pausePlayButton.setOnClickListener { pausePlay() }
            nextBtn?.setOnClickListener { playNextSong(adapter) }
            previousBtn?.setOnClickListener { playPreviousSong(adapter) }
            bottomInfos.setOnClickListener {
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()
            }
            songTitleInfo?.isSelected = true
        }

        if (!mediaPlayer.isPlaying) {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }

        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
    }

    override fun onMusicClick(position: Int) {
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        Log.d("POSITION", position.toString())

        val returnIntent = Intent()
        returnIntent.putExtra("SAME MUSIC", sameMusic)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                Toast.makeText(this, "Ajout dans une playlist", Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                if (listType == "initialList"){
                    if (MyMediaPlayer.initialPlaylist.equals(MyMediaPlayer.currentPlaylist)){
                        MyMediaPlayer.currentPlaylist.remove(list[item.groupId])
                    }
                    MyMediaPlayer.initialPlaylist.remove(list[item.groupId])
                } else {
                    MyMediaPlayer.currentPlaylist.remove(list[item.groupId])
                }
                adapter.notifyItemRemoved(item.groupId)

                Toast.makeText(
                    this,
                    "Suppressions de la musique dans la playlist",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            2 -> {
                val intent = Intent(this@SeeMusicListActivity, ModifyMusicInfoActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", "Main")
                val positionMainList = MyMediaPlayer.allMusics.indexOf(list[item.groupId])
                intent.putExtra("POSITION", positionMainList)
                resultLauncher.launch(intent)
                true
            }
            3 -> {
                // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                val song = list[item.groupId]

                if (listType == "initialList"){
                    if (MyMediaPlayer.initialPlaylist.equals(MyMediaPlayer.currentPlaylist)){
                        MyMediaPlayer.currentPlaylist.remove(list[item.groupId])
                        MyMediaPlayer.currentPlaylist.add(
                            MyMediaPlayer.currentIndex + 1,
                            song
                        )
                    }
                    MyMediaPlayer.initialPlaylist.remove(list[item.groupId])
                    MyMediaPlayer.initialPlaylist.add(
                        MyMediaPlayer.currentIndex + 1,
                        song
                    )
                } else {
                    MyMediaPlayer.currentPlaylist.remove(list[item.groupId])
                    MyMediaPlayer.currentPlaylist.add(
                        MyMediaPlayer.currentIndex + 1,
                        song
                    )
                }
                adapter.notifyItemRemoved(item.groupId)
                adapter.notifyItemInserted(MyMediaPlayer.currentIndex + 1)
                true
            }
            else -> {
                onContextItemSelected(item)
            }
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK){
            list = if (listType == "initialList"){
                MyMediaPlayer.initialPlaylist
            } else {
                MyMediaPlayer.currentPlaylist
            }
            adapter.musics = list
        }
    }

    private fun pausePlay() {
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(onAudioFocusChange)
            .build()

        when (audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this,"AN unknown error has come up", Toast.LENGTH_SHORT).show()
            }
        }
    }
}