package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SeeMusicListActivity : AppCompatActivity(),MusicList.OnMusicListener {
    private var list = ArrayList<Music>()
    private lateinit var adapter : MusicList
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var listName : TextView
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_see_music_list)

        menuRecyclerView = findViewById(R.id.songs_list)
        listName = findViewById(R.id.list_name)

        val listType = intent.getSerializableExtra("LIST-TYPE")

        if (listType == "initialList"){
            listName.text = "Initial List"
            list = MyMediaPlayer.initialPlaylist
        } else {
            listName.text = "Current List"
            list = MyMediaPlayer.currentPlaylist
        }

        adapter = MusicList(list, listName.text as String,applicationContext,this)
        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuRecyclerView.adapter = adapter

        /*
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
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
            pausePlay?.setOnClickListener{ pausePlay() }
            nextBtn?.setOnClickListener{ playNextSong() }
            previousBtn?.setOnClickListener{ playPreviousSong() }
            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@SeeMusicListActivity) }
            songTitleInfo.isSelected = true
        }
        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }

         */
    }

    override fun onMusicClick(position: Int) {
        TODO("Not yet implemented")
    }
}