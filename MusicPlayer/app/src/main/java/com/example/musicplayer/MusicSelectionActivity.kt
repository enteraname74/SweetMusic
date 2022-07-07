package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException


class MusicSelectionActivity : AppCompatActivity(), MusicListSelection.OnMusicListener {
    private var musics = ArrayList<Music>()
    private lateinit var adapter : MusicListSelection
    private var selectedMusics = ArrayList<Music>()
    private var selectedMusicsPositions = ArrayList<Int>()
    private var menuRecyclerView : RecyclerView? = null
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_selection)

        menuRecyclerView = findViewById(R.id.all_songs_list)
        musics = intent.getSerializableExtra("MAIN") as ArrayList<Music>

        adapter = MusicListSelection(musics,selectedMusicsPositions,applicationContext,this)

        menuRecyclerView?.layoutManager = LinearLayoutManager(this)
        menuRecyclerView?.adapter = adapter

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
            pausePlay?.setOnClickListener(View.OnClickListener{pausePlay()})
            nextBtn?.setOnClickListener(View.OnClickListener { playNextSong() })
            previousBtn?.setOnClickListener(View.OnClickListener { playPreviousSong() })
            bottomInfos.setOnClickListener(View.OnClickListener {onBottomMenuClick(MyMediaPlayer.currentIndex) })
            songTitleInfo?.setSelected(true)
        }
        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }

        val validateButton = findViewById<Button>(R.id.validate)
        validateButton.setOnClickListener(View.OnClickListener { onValidateButtonClick() })
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
        val intent = Intent(this@MusicSelectionActivity,MusicPlayerActivity::class.java)

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

    override fun onMusicClick(position: Int) {
        val selectedMusic = musics[position]

        if (selectedMusic in selectedMusics){
            selectedMusics.remove(selectedMusic)
            selectedMusicsPositions.remove(position)
        } else {
            selectedMusics.add(selectedMusic)
            selectedMusicsPositions.add(position)
        }

        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    private fun onValidateButtonClick(){
        val returnIntent = Intent()
        returnIntent.putExtra("addedSongs", selectedMusics)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}