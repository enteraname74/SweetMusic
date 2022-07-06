package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class PlaylistsMenuActivity : AppCompatActivity(), Playlists.OnPlaylistsListener, MusicList.OnMusicListener {
    private var menuRecyclerView : RecyclerView? = null
    private var playlists = ArrayList<Playlist>()
    private var musics = ArrayList<Music>()
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists_menu)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        val noPlaylistsFound = findViewById<TextView>(R.id.no_playlists_found)

        musics = intent.getSerializableExtra("MAIN") as ArrayList<Music>
        val playlist = intent.getSerializableExtra("testPlaylist") as Playlist
        playlists.add(playlist)

        if (playlists.size != 0){
            menuRecyclerView?.visibility = View.VISIBLE
            noPlaylistsFound.visibility = View.GONE

            //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
            menuRecyclerView?.layoutManager = LinearLayoutManager(this)
            menuRecyclerView?.adapter = Playlists(playlists,applicationContext,this)
        } else {
            menuRecyclerView?.visibility = View.GONE
            noPlaylistsFound.visibility = View.VISIBLE
        }

        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val nextBtn = findViewById<ImageView>(R.id.next)
        val previousBtn = findViewById<ImageView>(R.id.previous)

        val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)

        // On met en place les données du menu situé tout en bas de l'écran :
        if (MyMediaPlayer.currentIndex == -1){
            noSongPlaying.visibility = View.VISIBLE
            infoSongPlaying.visibility = View.GONE
        } else {
            // Changement de la vue :
            noSongPlaying.visibility = View.GONE
            infoSongPlaying.visibility = View.VISIBLE
            songTitleInfo?.text = musics[MyMediaPlayer.currentIndex].name

            // Mise en places des boutons :
            pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
            nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
            previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
            bottomInfos.setOnClickListener(View.OnClickListener {onMusicClick(MyMediaPlayer.currentIndex) })
            songTitleInfo?.setSelected(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            menuRecyclerView?.adapter = Playlists(playlists,applicationContext,this)

            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val nextBtn = findViewById<ImageView>(R.id.next)
            val previousBtn = findViewById<ImageView>(R.id.previous)

            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val bottomInfos = findViewById<LinearLayout>(R.id.bottom_infos)

            noSongPlaying.visibility = View.VISIBLE

            if (MyMediaPlayer.currentIndex != -1){
                noSongPlaying.visibility = View.GONE
                infoSongPlaying.visibility = View.VISIBLE
                songTitleInfo.text = musics[MyMediaPlayer.currentIndex].name

                pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
                nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
                previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
                bottomInfos.setOnClickListener(View.OnClickListener {onMusicClick(MyMediaPlayer.currentIndex) })
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
    }

    override fun onPlaylistClick(position: Int) {
        Log.d("PLAYLIST POSITION", position.toString())

        val currentPlaylist = playlists[position]
        val intent = Intent(this@PlaylistsMenuActivity,SelectedPlaylistActivity::class.java)
        intent.putExtra("LIST",currentPlaylist)

        startActivity(intent)
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
        val intent = Intent(this@PlaylistsMenuActivity,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("LIST",musics)
        //flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun playMusic(){
        val pausePlay = findViewById<ImageView>(R.id.pause_play)
        val currentSong = musics.get(MyMediaPlayer.currentIndex)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = musics[MyMediaPlayer.currentIndex].name
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun playNextSong(){
        if(MyMediaPlayer.currentIndex==(musics.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (musics.size)-1
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
