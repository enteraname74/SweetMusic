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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectedAlbumActivity : Tools(), MusicList.OnMusicListener {
    private lateinit var album : Album
    private var albumPosition : Int = 0
    private lateinit var adapter : MusicList
    private var allPlaylists = ArrayList<Playlist>()
    private var musics = ArrayList<Music>()
    private var allMusicsBackup = ArrayList<Music>()
    private lateinit var searchView : SearchView
    private lateinit var menuRecyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_album)

        menuRecyclerView = findViewById(R.id.menu_album_recycler_view)
        albumPosition = intent.getSerializableExtra("POSITION") as Int

        album = MyMediaPlayer.allAlbums[albumPosition]
        musics = album.albumList
        allMusicsBackup = ArrayList(musics.map { it.copy() })

        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
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
                            }
                        }
                    }
                } catch (error : Error){
                    Log.d("ERROR",error.toString())
                }
                return true
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            launch {
                adapter = MusicList(musics, album.albumName, applicationContext, this@SelectedAlbumActivity)
                menuRecyclerView.layoutManager = LinearLayoutManager(this@SelectedAlbumActivity)
                menuRecyclerView.adapter = adapter
            }
        }

        val albumName = findViewById<TextView>(R.id.album_name)
        albumName?.text = album.albumName

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

            bottomInfos.setOnClickListener{onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedAlbumActivity) }
            songTitleInfo?.isSelected = true
        }

        val shuffleButton = findViewById<Button>(R.id.shuffle_button)
        shuffleButton.setOnClickListener { playRandom(musics, this@SelectedAlbumActivity) }

        // Lorsqu'une musique se finit, on passe à la suivante automatiquement :
        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
    }

    override fun onResume() {
        super.onResume()
        searchView.clearFocus()
        if (MyMediaPlayer.modifiedSong){
            GlobalScope.launch(Dispatchers.IO){
                launch{writeAllAsync(MyMediaPlayer.allMusics, MyMediaPlayer.allPlaylists)}
            }
            println("test")
            MyMediaPlayer.modifiedSong = false
        }
        adapter.musics = MyMediaPlayer.allAlbums[albumPosition].albumList
        adapter.notifyItemRangeChanged(0, adapter.itemCount)

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
            nextBtn?.setOnClickListener{ playNextSong(adapter) }
            previousBtn?.setOnClickListener{ playPreviousSong(adapter) }
            bottomInfos.setOnClickListener{ onBottomMenuClick(MyMediaPlayer.currentIndex, this@SelectedAlbumActivity) }
            songTitleInfo?.isSelected = true
        }

        if (!mediaPlayer.isPlaying){
            pausePlay?.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        } else {
            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        }
        mediaPlayer.setOnCompletionListener { playNextSong(adapter) }
        Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())
        Log.d("RESUME","resume")
    }

    override fun onMusicClick(position: Int) {
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        // Vérifions si on change de playlist :
        if (musics != MyMediaPlayer.initialPlaylist) {
            MyMediaPlayer.currentPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(musics.map { it.copy() })
            MyMediaPlayer.playlistName = album.albumName
            sameMusic = false
        }

        MyMediaPlayer.currentIndex = position
        val intent = Intent(this@SelectedAlbumActivity,MusicPlayerActivity::class.java)

        intent.putExtra("SAME MUSIC", sameMusic)

        startActivity(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            0 -> {
                Toast.makeText(this,"Ajout dans une playlist",Toast.LENGTH_SHORT).show()
                true
            }
            1 -> {
                musics.removeAt(item.groupId)
                adapter.notifyItemRemoved(item.groupId)

                GlobalScope.launch(Dispatchers.IO){
                    launch{writePlaylistsToFile(savePlaylistsFile,allPlaylists)}
                }

                Toast.makeText(this,"Suppressions de la musique dans la playlist",Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                // MODIFY INFOS :
                // On s'assure de séléctionner la bonne position au cas où on utilise la barre de recherche :
                val position = allMusicsBackup.indexOf(musics[item.groupId])
                val intent = Intent(this@SelectedAlbumActivity,ModifyMusicInfoActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", album.albumName)
                intent.putExtra("POSITION",position)
                resultModifyMusic.launch(intent)
                true
            }
            3 -> {
                // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
                MyMediaPlayer.initialPlaylist.remove((musics[item.groupId]))
                MyMediaPlayer.currentPlaylist.remove((musics[item.groupId]))

                MyMediaPlayer.currentPlaylist.add(MyMediaPlayer.currentIndex+1, musics[item.groupId])
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
            allMusicsBackup = MyMediaPlayer.allAlbums[albumPosition].albumList
            adapter.notifyDataSetChanged()
        }
    }
}