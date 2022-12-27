package com.example.musicplayer.fragments

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.*
import com.example.musicplayer.Music
import com.example.musicplayer.adapters.NewMusicsList
import com.example.musicplayer.classes.MyMediaPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import java.io.*

class FoundMusicsFragment : Fragment(), NewMusicsList.OnMusicListener {
    private lateinit var adapter : NewMusicsList
    private lateinit  var menuRecyclerView : RecyclerView
    private lateinit var fetchingSongs : LinearLayout
    private val saveAllMusicsFile = "allMusics.musics"
    private val saveAllDeletedFiles = "allDeleted.musics"
    private lateinit var fetchingState : TextView
    private lateinit var determinateProgressBar : ProgressBar
    private lateinit var indeterminateProgressBar : ProgressBar
    private lateinit var fetchingJob : Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NewMusicsList(ArrayList<Music>(), this, requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_found_musics, container, false)

        menuRecyclerView = view.findViewById(R.id.menu_recycler_view)
        menuRecyclerView.layoutManager = LinearLayoutManager(view.context)
        menuRecyclerView.adapter = adapter

        fetchingSongs = view.findViewById(R.id.fetching_songs)

        val addNewSongs = view.findViewById<Button>(R.id.add_songs)
        addNewSongs.setOnClickListener { addSongsToAllMusics() }

        fetchingState = view.findViewById(R.id.fetching_state)
        determinateProgressBar = view.findViewById(R.id.determinate_bar)
        indeterminateProgressBar = view.findViewById(R.id.indeterminate_bar)

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchingJob = CoroutineScope(Dispatchers.IO).launch { fetchMusics() }
    }

    private fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
        return byteStream.toByteArray()
    }

    private fun writeAllMusicsToFile(){
        val path = activity?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllMusicsFile)))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write musics",error.toString())
        }
    }

    private suspend fun fetchMusics() {
        val musics = ArrayList<Music>()

        // A "projection" defines the columns that will be returned for each row
        val projection: Array<String> = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Albums.ALBUM_ID
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        // Does a query against the table and returns a Cursor object
        val cursor = activity?.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,    // The content URI of the words table
            projection,                                     // The columns to return for each row
            selection,                                      // Either null, or the boolean that specifies the rows to retrieve
            null,
            null // The sort order for the returned rows
        )

        when (cursor?.count) {
            null -> {
                Toast.makeText(activity, resources.getString(R.string.cannot_retrieve_files), Toast.LENGTH_SHORT).show()
            }
            else -> {
                var count = 0
                withContext(Dispatchers.Main) {
                    fetchingState.text = getString(R.string.fetching_found_songs)
                    indeterminateProgressBar.visibility = View.GONE
                    determinateProgressBar.visibility = View.VISIBLE
                    determinateProgressBar.max = cursor.count
                }
                while (cursor.moveToNext()) {
                    // Si la musique n'est pas présente dans notre liste, alors on ajoute la musique dans la liste des musiques trouvées :
                    if ((MyMediaPlayer.allMusics.find { it.path == cursor.getString(4) } == null) &&
                        (MyMediaPlayer.allDeletedMusics.find { it.path == cursor.getString(4) } == null)) {
                        val albumId = cursor.getLong(5)
                        val albumUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
                        )

                        val albumCover: ByteArray? = try {
                            withContext(Dispatchers.IO) {
                                /*
                                val bitmap = activity?.contentResolver?.loadThumbnail(
                                    albumUri,
                                    Size(400, 400),
                                    null
                                )
                                 */
                                val bitmap = ThumbnailUtils.createAudioThumbnail(File(cursor.getString(4)),Size(350,350),null)
                                bitmapToByteArray(bitmap)
                            }
                        } catch (error: IOException) {
                            null
                        }
                        val music = Music(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            albumCover,
                            cursor.getLong(3),
                            cursor.getString(4)
                        )
                        if (File(music.path).exists()) {
                            musics.add(music)
                        }
                    }
                    withContext(Dispatchers.Main){
                        count+=1
                        determinateProgressBar.setProgress(count,true)
                    }
                }
                cursor.close()

                withContext(Dispatchers.Main){
                    adapter.musics = musics
                    adapter.notifyDataSetChanged()
                    fetchingSongs.visibility = View.GONE
                    menuRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addSongsToAllMusics(){
        for (music in adapter.musics.asReversed()){
            MyMediaPlayer.allMusics.add(0, music)
        }
        CoroutineScope(Dispatchers.IO).launch { writeAllMusicsToFile() }
        Toast.makeText(
            context,
            resources.getString(R.string.retrieved_all_new_musics),
            Toast.LENGTH_SHORT
        ).show()
        activity?.finish()
    }

    private fun writeAllDeletedSong(){
        val path = context?.applicationContext?.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllDeletedFiles)))
            oos.writeObject(MyMediaPlayer.allDeletedMusics)
            oos.close()
        } catch (error : IOException){
            Log.d("Error write deleted",error.toString())
        }
    }

    override fun onMusicClick(position: Int) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_find_new_songs)
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<ImageView>(R.id.action_img)?.setImageResource(R.drawable.ic_baseline_delete_24)
        bottomSheetDialog.findViewById<TextView>(R.id.action_text)?.text = getString(R.string.delete_music)

        bottomSheetDialog.findViewById<LinearLayout>(R.id.action)?.setOnClickListener {
            val musicToRemove = adapter.musics[position]
            adapter.musics.removeAt(position)
            adapter.notifyItemRemoved(position)

            MyMediaPlayer.allDeletedMusics.add(0, musicToRemove)
            CoroutineScope(Dispatchers.IO).launch { writeAllDeletedSong() }

            Toast.makeText(
                context,
                resources.getString(R.string.retrieved_music),
                Toast.LENGTH_SHORT
            ).show()
            bottomSheetDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchingJob.cancel()
    }

}