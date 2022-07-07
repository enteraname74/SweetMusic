package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class SelectedPlaylistActivity : AppCompatActivity(), MusicList.OnMusicListener {
    private lateinit var playlist : Playlist
    private lateinit var adapter : MusicList
    private var musics = ArrayList<Music>()
    private var allMusics = ArrayList<Music>()
    private var menuRecyclerView : RecyclerView? = null
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_playlist)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        playlist = intent.getSerializableExtra("LIST") as Playlist
        allMusics = intent.getSerializableExtra("MAIN") as ArrayList<Music>
        musics = playlist.musicList

        adapter = MusicList(musics,applicationContext,this)

        menuRecyclerView?.layoutManager = LinearLayoutManager(this)
        menuRecyclerView?.adapter = adapter

        val playlistName = findViewById<TextView>(R.id.playlist_name)
        playlistName?.text = playlist.listName

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
            bottomInfos.setOnClickListener(View.OnClickListener {onBottomMenuClick(MyMediaPlayer.currentIndex) })
            songTitleInfo?.setSelected(true)
        }

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());

            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val nextBtn = findViewById<ImageView>(R.id.next)
            val previousBtn = findViewById<ImageView>(R.id.previous)
            val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)
            noSongPlaying.visibility = View.VISIBLE

            if (MyMediaPlayer.currentIndex != -1){
                noSongPlaying.visibility = View.GONE
                infoSongPlaying.visibility = View.VISIBLE
                songTitleInfo.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name

                pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
                nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
                previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
                bottomInfos.setOnClickListener(View.OnClickListener {onBottomMenuClick(MyMediaPlayer.currentIndex) })
                songTitleInfo?.setSelected(true)
            }

            if (!mediaPlayer.isPlaying){
                pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
            } else {
                pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            }
            mediaPlayer.setOnCompletionListener { playNextSong() }
            Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())
            Log.d("RESUME","resume")
        }

        val addSongs = findViewById<ImageView>(R.id.add_songs)
        addSongs.setOnClickListener(View.OnClickListener { onAddSongsClick() })
    }

    fun onAddSongsClick(){
        val intent = Intent(this@SelectedPlaylistActivity,MusicSelectionActivity::class.java)

        intent.putExtra("MAIN", allMusics)
        resultLauncher.launch(intent)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getSerializableExtra("addedSongs") as ArrayList<Music>
            for (song in data){
                if (song !in musics){
                    musics.add(song)
                }
            }
            playlist.musicList = musics
            adapter.musics = musics
            menuRecyclerView?.adapter = adapter
        }
    }

    override fun onMusicClick(position: Int) {
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(this@SelectedPlaylistActivity,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("LIST",musics)
        //flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun onBottomMenuClick(position : Int){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(this@SelectedPlaylistActivity,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        // Si on joue actuellement une autre playlist que celle du menu dans laquelle on est, on passe uniquement la playlist qui se jour actuellement :
        intent.putExtra("LIST",MyMediaPlayer.currentPlaylist)
        //flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun playMusic(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun playNextSong(){
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        playMusic()
    }

    private fun pausePlay(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
    }
}