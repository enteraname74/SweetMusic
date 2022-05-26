package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity(), MusicList.OnMusicListener {


    private var musics = ArrayList<Music>()
    var menuRecyclerView : RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("Test","TEST")

        if (!checkPermission()){
            requestPermission()
        }

        // A "projection" defines the columns that will be returned for each row
        val projection: Array<String> = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        // Does a query against the table and returns a Cursor object
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
            projection,                                     // The columns to return for each row
            selection,                                      // Either null, or the boolean that specifies the rows to retrieve
            null,
            null // The sort order for the returned rows
        )

        menuRecyclerView = findViewById<RecyclerView>(R.id.menu_recycler_view)
        val noSongsFound = findViewById<TextView>(R.id.no_songs_found)

        when (cursor?.count){
            null -> {
                Toast.makeText(this,"Couldn't retrieve music files",Toast.LENGTH_SHORT).show()
                menuRecyclerView?.visibility = View.GONE;
                noSongsFound.visibility = View.VISIBLE;
            }
            0 -> {
                menuRecyclerView?.visibility = View.GONE;
                noSongsFound.visibility = View.VISIBLE
            }
            else -> {
                while(cursor.moveToNext()){
                    val music = Music(cursor.getString(0),cursor.getString(1),cursor.getString(2),"",cursor.getLong(3),cursor.getString(4))
                    if(File(music.path).exists()) {
                        musics.add(music)
                    }
                }

                musics.reverse()

                menuRecyclerView?.visibility = View.VISIBLE;
                noSongsFound.visibility = View.GONE;

                //layoutManager permet de gérer la facon dont on affiche nos elements dans le recyclerView
                menuRecyclerView?.layoutManager = LinearLayoutManager(this)
                menuRecyclerView?.adapter = MusicList(musics, applicationContext,this)
            }
        }


    }

    private fun checkPermission() : Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(this,"PERMISSION IS REQUIRED FOR THIS APP TO FUNCTION. PLEASE ALLOW PERMISSIONS FROM SETTINGS",Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                 arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                69
            )
        }
    }
    override fun onMusicClick(position: Int) {
        Log.d("ACTIVITE", "APPUIE"+position)

        MyMediaPlayer.getInstance.reset()
        MyMediaPlayer.currentIndex = position

        val intent = Intent(this@MainActivity,MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("LIST",musics)
        //flags = Intent.FLAG_ACTIVITY_NEW_TASK

        Log.d("ACTIVITE", musics.toString())
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if(menuRecyclerView!=null){
            menuRecyclerView?.adapter = MusicList(musics, applicationContext,this)
            Log.d("CURRENT SONG",MyMediaPlayer.currentIndex.toString())
            Log.d("RESUME","resume")
        }
    }

}
