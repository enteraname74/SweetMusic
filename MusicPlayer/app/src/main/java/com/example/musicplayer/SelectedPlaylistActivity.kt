package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectedPlaylistActivity : Tools(), MusicList.OnMusicListener {
    private lateinit var playlist : Playlist
    private var playlistPosition : Int = 0
    private lateinit var adapter : MusicList
    private var musics = ArrayList<Music>()
    private var menuRecyclerView : RecyclerView? = null
    private var mediaPlayer = MyMediaPlayer.getInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_playlist)

        menuRecyclerView = findViewById(R.id.menu_playlist_recycler_view)
        val allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
        playlistPosition = intent.getSerializableExtra("POSITION") as Int
        playlist = allPlaylists[playlistPosition]
        musics = playlist.musicList

        adapter = MusicList(musics,playlist.listName,applicationContext,this)

        menuRecyclerView?.layoutManager = LinearLayoutManager(this)
        menuRecyclerView?.adapter = adapter

        val playlistName = findViewById<TextView>(R.id.playlist_name)
        playlistName?.text = playlist.listName

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

            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedPlaylistActivity) }
            songTitleInfo?.setSelected(true)
        }

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong() }
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            val playlists = readAllPlaylistsFromFile(savePlaylistsFile)
            playlist = playlists[playlistPosition]
            adapter.musics = playlists[playlistPosition].musicList
            adapter.notifyItemRangeChanged(0, adapter.getItemCount())

            val noSongPlaying = findViewById<TextView>(R.id.no_song_playing)
            val infoSongPlaying = findViewById<RelativeLayout>(R.id.info_song_playing)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)
            val pausePlay = findViewById<ImageView>(R.id.pause_play)
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

                pausePlay?.setOnClickListener{ pausePlay() }
                nextBtn?.setOnClickListener{ playNextSong() }
                previousBtn?.setOnClickListener{ playPreviousSong() }
                bottomInfos.setOnClickListener{ onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedPlaylistActivity) }
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
        addSongs.setOnClickListener{ onAddSongsClick() }
    }

    private fun onAddSongsClick(){
        val intent = Intent(this@SelectedPlaylistActivity,MusicSelectionActivity::class.java)
        resultLauncher.launch(intent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getSerializableExtra("addedSongs") as ArrayList<Int>
            val allMusics = readAllMusicsFromFile(saveAllMusicsFile)
            for (position in data){
                if (allMusics[position] !in musics){
                    musics.add(allMusics[position])
                }
            }
            playlist.musicList = musics
            adapter.musics = musics
            menuRecyclerView?.adapter = adapter

            val playlists = readAllPlaylistsFromFile(savePlaylistsFile)
            playlists[playlistPosition].musicList = musics
            writePlaylistsToFile(savePlaylistsFile, playlists)
        }
    }

    override fun onMusicClick(position: Int) {
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        // Vérifions si on change de playlist :
        if (musics != MyMediaPlayer.currentPlaylist) {
            Log.d("CHANGEMENT PLAYLIST","")
            MyMediaPlayer.currentPlaylist = musics
            MyMediaPlayer.playlistName = playlist.listName
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(this@SelectedPlaylistActivity,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    private fun playNextSong(){
        if(MyMediaPlayer.currentIndex==(MyMediaPlayer.currentPlaylist.size)-1){
            MyMediaPlayer.currentIndex = 0
        } else {
            MyMediaPlayer.currentIndex+=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    private fun playPreviousSong(){
        if(MyMediaPlayer.currentIndex==0){
            MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size)-1
        } else {
            MyMediaPlayer.currentIndex-=1
        }
        adapter.notifyDataSetChanged()
        playMusic()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                Toast.makeText(this,"Ajout dans une playlist",Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                musics.removeAt(item.groupId)
                adapter.musics.removeAt(item.groupId)
                adapter.notifyDataSetChanged()

                // Mettons à jour l'état des playlists :
                val playlists = readAllPlaylistsFromFile(savePlaylistsFile)
                playlists[playlistPosition].musicList = musics
                writePlaylistsToFile(savePlaylistsFile, playlists)

                Toast.makeText(this,"Suppressions de la musique dans la playlist",Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                Log.d("GROUPID", item.groupId.toString())
                val intent = Intent(this@SelectedPlaylistActivity,ModifyMusicInfoActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", playlist.listName)
                intent.putExtra("POSITION",item.groupId)
                resultModifyMusic.launch(intent)
                true
            }
            else -> {
                onContextItemSelected(item)
            }
        }
    }

    private var resultModifyMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // On récupère les musiques avec la modification effectuée :
            musics = readAllMusicsFromFile(saveAllMusicsFile)
            adapter.musics = musics
        }
    }
}