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
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


class ModifyPlaylistInfoActivity : AppCompatActivity() {
    private lateinit var playlist : Playlist
    private var position : Int = 0
    private lateinit var allPlaylists : ArrayList<Playlist>
    private lateinit var playlistCoverField : ImageView
    private lateinit var playlistNameField : EditText
    private var savePlaylistsFile = "allPlaylists.playlists"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_playlist_info)

        // On récupère notre playlist à modifier :
        position = intent.getSerializableExtra("POSITION") as Int

        allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
        playlist = allPlaylists[position]

        // On récupère les différents champs modifiable :
        playlistCoverField = findViewById(R.id.playlist_cover)
        playlistNameField = findViewById(R.id.edit_playlist_name)

        // On indique les infos actuelles de la musique dans nos champs :
        if (playlist.playlistCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = playlist.playlistCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            playlistCoverField.setImageBitmap(bitmap)
        } else {
            playlistCoverField.setImageResource(R.drawable.michael)
        }

        playlistNameField.setText(playlist.listName)

        playlistCoverField.setOnClickListener(View.OnClickListener { selectImage() })
        val validateButton = findViewById<Button>(R.id.validate_button)
        validateButton.setOnClickListener(View.OnClickListener { onValidateButtonClick() })
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultImageLauncher.launch(intent)
    }

    private var resultImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            val inputStream = contentResolver.openInputStream(uri as Uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            playlistCoverField.setImageBitmap(bitmap)
        }
    }

    private fun onValidateButtonClick(){
        // On modifie les éléments du fichier :
        playlist.listName = playlistNameField.text.toString()

        val drawable = playlistCoverField.drawable
        val bitmapDrawable = drawable as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap

        val byteArray = bitmapToByteArray(bitmap)

        playlist.playlistCover = byteArray

        // On ne peut pas renvoyer le fichier car l'image de l'album est trop lourde. On écrase donc directement la musique dans le fichier de sauvegarde :

        // Mettons à jour nos playlists :
        allPlaylists[position] = playlist
        writePlaylistToFile(savePlaylistsFile, allPlaylists)

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