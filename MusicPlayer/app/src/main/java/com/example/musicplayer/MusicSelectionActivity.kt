package com.example.musicplayer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MusicSelectionActivity : Tools(), MusicListSelection.OnMusicListener {
    private var musics = ArrayList<Music>()
    private lateinit var adapter : MusicListSelection
    private var selectedMusicsPositions = ArrayList<Int>()
    private lateinit var menuRecyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_selection)

        menuRecyclerView = findViewById(R.id.all_songs_list)
        musics = MyMediaPlayer.allMusics

        adapter = MusicListSelection(musics,selectedMusicsPositions,applicationContext,this)

        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuRecyclerView.adapter = adapter

        val validateButton = findViewById<Button>(R.id.validate)
        val cancelButton = findViewById<Button>(R.id.cancel)
        validateButton.setOnClickListener{ onValidateButtonClick() }
        cancelButton.setOnClickListener{ onCancelButtonClick() }
    }

    override fun onResume() {
        super.onResume()

        val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
        val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
        val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)

        if (MyMediaPlayer.currentIndex != -1){
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

            songTitleInfo.isSelected = true
        }
    }

    override fun onMusicClick(position: Int) {

        if (position in selectedMusicsPositions){
            selectedMusicsPositions.remove(position)
        } else {
            selectedMusicsPositions.add(position)
        }

        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    private fun onValidateButtonClick(){
        val returnIntent = Intent()
        returnIntent.putExtra("addedSongs", selectedMusicsPositions)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun onCancelButtonClick(){
        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }
}