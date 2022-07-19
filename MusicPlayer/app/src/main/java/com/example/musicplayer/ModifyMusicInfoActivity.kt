package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.*


class ModifyMusicInfoActivity : AppCompatActivity() {
    private lateinit var musicFile : Music
    private lateinit var albumCoverField : ImageView
    private lateinit var musicNameField : EditText
    private lateinit var albumNameField : EditText
    private lateinit var artistNameField : EditText
    private val saveMusicsFile = "allMusics.musics"
    private var savePlaylistsFile = "allPlaylists.playlists"
    private lateinit var currentPlaylist : ArrayList<Music>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_music_info)
        val playlistName = intent.getSerializableExtra("PLAYLIST_NAME") as String
        if (playlistName == "Main"){
            currentPlaylist = readAllMusicsFromFile(saveMusicsFile)
        } else {
            val allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
            currentPlaylist = allPlaylists.first{it.listName == playlistName}.musicList
        }

        // On récupère notre musique à modifier :
        val position = intent.getSerializableExtra("POSITION") as Int
        musicFile = currentPlaylist[position]

        // On récupère les différents champs modifiable :
        albumCoverField = findViewById(R.id.album_image)
        musicNameField = findViewById(R.id.edit_music_name)
        albumNameField = findViewById(R.id.edit_album_name)
        artistNameField = findViewById(R.id.edit_artist_name)

        // On indique les infos actuelles de la musique dans nos champs :
        if (musicFile.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = musicFile.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            albumCoverField.setImageBitmap(bitmap)
        } else {
            albumCoverField.setImageResource(R.drawable.michael)
        }

        musicNameField.setText(musicFile.name)
        albumNameField.setText(musicFile.album)
        artistNameField.setText(musicFile.artist)

        albumCoverField.setOnClickListener(View.OnClickListener { selectImage() })
        val validateButton = findViewById<Button>(R.id.validate_button)
        validateButton.setOnClickListener(View.OnClickListener { onValidateButtonClick() })
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultImageLauncher.launch(intent)
    }

    var resultImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.getData()?.data
            val inputStream = contentResolver.openInputStream(uri as Uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            albumCoverField.setImageBitmap(bitmap)
        }
    }

    private fun onValidateButtonClick(){
        // On modifie les éléments du fichier :
        musicFile.name = musicNameField.text.toString()
        musicFile.album = albumNameField.text.toString()
        musicFile.artist = artistNameField.text.toString()

        val drawable = albumCoverField.drawable
        val bitmapDrawable = drawable as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap

        val byteArray = bitmapToByteArray(bitmap)

        musicFile.albumCover = byteArray

        // On ne peut pas renvoyer le fichier car l'image de l'album est trop lourde. On écrase donc directement la musique dans le fichier de sauvegarde :

        val allMusics = readAllMusicsFromFile(saveMusicsFile)
        var position = allMusics.indexOf(allMusics.first{it.path == musicFile.path})
        allMusics[position] = musicFile
        writeObjectToFile(saveMusicsFile,allMusics)

        // Ensuite, mettons à jour nos playlists :
        val playlists = readAllPlaylistsFromFile(savePlaylistsFile)

        for (playlist in playlists){
            for (music in playlist.musicList){
                if (music.path == musicFile.path){
                    position = playlist.musicList.indexOf(music)
                    playlist.musicList[position] = musicFile
                    break
                }
            }
        }
        writePlaylistToFile(savePlaylistsFile, playlists)

        setResult(RESULT_OK)
        finish()
    }

    private fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
        return byteStream.toByteArray()
    }

    private fun writeObjectToFile(filename : String, content : ArrayList<Music>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("ErrorWRITE",error.toString())
        }
        Toast.makeText(this,"ALL SONGS WRITE", Toast.LENGTH_SHORT).show()
    }

    private fun writePlaylistToFile(filename : String, content : ArrayList<Playlist>){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.d("Error","")
        }
    }


    private fun readAllMusicsFromFile(filename : String) : ArrayList<Music> {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)));
            content = ois.readObject() as ArrayList<Music>
            ois.close();
        } catch (error : IOException){
            Log.d("Error",error.toString())
        }
        Toast.makeText(this,"ALL SONGS FETCHED", Toast.LENGTH_SHORT).show()
        return content
    }

    private fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
        val path = applicationContext.filesDir
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)));
            content = ois.readObject() as ArrayList<Playlist>
            ois.close();
        } catch (error : IOException){
            Log.d("Error","")
        }

        return content
    }
}