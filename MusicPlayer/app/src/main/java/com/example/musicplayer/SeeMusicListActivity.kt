package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_music_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SeeMusicListActivity : Tools(),MusicList.OnMusicListener {
    private var list = ArrayList<Music>()
    private lateinit var adapter : MusicList
    private lateinit var menuRecyclerView : RecyclerView
    private lateinit var listName : TextView
    private lateinit var listType : String

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

        GlobalScope.launch(Dispatchers.IO){
            launch{
                adapter = MusicList(list, listName.text as String,applicationContext,this@SeeMusicListActivity)
                menuRecyclerView.layoutManager = LinearLayoutManager(this@SeeMusicListActivity)
                menuRecyclerView.adapter = adapter


            }
        }

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
                intent.putExtra("POSITION", item.groupId)
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
}