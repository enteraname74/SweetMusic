package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.FileUtils
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.ObjectInputStream

class SetDataActivity : Tools() {
    private lateinit var editTextMusics: EditText
    private lateinit var editTextPlaylists: EditText
    private lateinit var filePathMusics : Uri
    private lateinit var filePathPlaylists : Uri
    private var validMusicsFile = false
    private var validPlaylistsFile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_data)

        editTextMusics = findViewById(R.id.infos_selected_musics_file)
        editTextPlaylists = findViewById(R.id.infos_selected_playlists_file)

        val musicsButton = findViewById<Button>(R.id.select_musics_button)
        musicsButton.setOnClickListener { selectMusicsFile() }

        val playlistsButton = findViewById<Button>(R.id.select_playlists_button)
        playlistsButton.setOnClickListener { selectPlaylistsFile() }

        val validateButton = findViewById<Button>(R.id.validate_button)
        validateButton.setOnClickListener { onValidateButtonClick() }

        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener { onCancelButtonClick() }

    }

    private fun selectMusicsFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultMusicsLauncher.launch(intent)
    }

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    private var resultMusicsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            val path = uri?.path as String
            val pathName = path.substring(path.lastIndexOf("/")+1)
            Log.d("path", uri.toString())
            Log.d("pathName", pathName.toString())

            if (pathName == "allMusics.musics"){
                validMusicsFile = true
                filePathMusics = uri
                editTextMusics.text = pathName.toEditable()
            } else {
                val wrongFile = "Wrong file !"
                editTextMusics.text = wrongFile.toEditable()
            }
        }
    }

    private fun selectPlaylistsFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultPlaylistsLauncher.launch(intent)
    }

    private var resultPlaylistsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            val path = uri?.path as String
            val pathName = path.substring(path.lastIndexOf("/")+1)

            if (pathName == "allPlaylists.playlists"){
                validPlaylistsFile = true
                filePathPlaylists = uri
                editTextPlaylists.text = pathName.toEditable()

            } else {
                val wrongFile = "Wrong file !"
                editTextPlaylists.text = wrongFile.toEditable()
            }
        }
    }

    private fun onValidateButtonClick(){
        if (validMusicsFile && validPlaylistsFile){
            val allMusics = readAllMusicsFromUri(filePathMusics)
            val allPlaylists = readAllPlaylistsFromUri(filePathPlaylists)

            CoroutineScope(Dispatchers.IO).launch {
                writeAllAsync(allMusics,allPlaylists)
            }
            Log.d("after","")
            MyMediaPlayer.dataWasChanged = true

            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this,"Missing correct files !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCancelButtonClick(){
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun readAllMusicsFromUri(uri : Uri) : ArrayList<Music> {
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(contentResolver.openInputStream(uri))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
        } catch (error : IOException){
            Log.d("Error read musics",error.toString())
        }
        MyMediaPlayer.allMusics = content
        return content
    }

    private fun readAllPlaylistsFromUri(uri : Uri) : ArrayList<Playlist> {
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(contentResolver.openInputStream(uri))
            content = ois.readObject() as ArrayList<Playlist>
            ois.close()
        } catch (error : IOException){
            Log.d("Error read playlists",error.toString())
        }
        MyMediaPlayer.allPlaylists = content
        return content
    }
}