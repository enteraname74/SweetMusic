package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicplayer.classes.Album
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModifyAlbumInfoActivity : Tools() {
    private var albumPos : Int = -1
    private lateinit var album : Album
    private lateinit var playlistCoverField : ImageView
    private lateinit var playlistNameField : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_playlist_info)

        albumPos = intent.getSerializableExtra("POS") as Int
        album = MyMediaPlayer.allAlbums[albumPos]

        playlistCoverField = findViewById(R.id.playlist_cover)
        playlistNameField = findViewById(R.id.edit_playlist_name)

        findViewById<TextView>(R.id.title_activity).text = getString(R.string.modify_album_informations)
        findViewById<TextView>(R.id.playlist_name).text = getString(R.string.name_of_the_album)

        if (album.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = album.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            playlistCoverField.setImageBitmap(bitmap)
        } else {
            playlistCoverField.setImageResource(R.drawable.ic_saxophone_svg)
        }

        playlistNameField.setText(album.albumName)

        playlistCoverField.setOnClickListener{ selectImage() }
        val validateButton = findViewById<Button>(R.id.validate_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)

        CoroutineScope(Dispatchers.Main).launch {
            validateButton.setOnClickListener{ onValidateButtonClick() }
            cancelButton.setOnClickListener{ onCancelButtonClick() }
            findViewById<ImageView>(R.id.quit_activity).setOnClickListener { finish() }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultImageLauncher.launch(intent)
    }

    private var resultImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            val bitmap = contentResolver.loadThumbnail(
                uri as Uri,
                Size(400, 400),
                null
            )
            playlistCoverField.setImageBitmap(bitmap)
        }
    }

    private fun onValidateButtonClick(){
        // On modifie les éléments du fichier :
        // Si le nom est déjà prit ou si le nom reste le même, on peut enregistrer les changements
        CoroutineScope(Dispatchers.IO).launch {
            album.albumName = playlistNameField.text.toString()
            val drawable = playlistCoverField.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            val bitmap = bitmapDrawable.bitmap

            val byteArray = bitmapToByteArray(bitmap)

            album.albumCover = byteArray

            MyMediaPlayer.allAlbums[albumPos] = album

            // Mettons à jour toutes les musiques de l'album :
            for (music in album.albumList) {
                music.album = album.albumName
                music.albumCover = album.albumCover

                // Mise à jour dans la playlist principale :
                MyMediaPlayer.allMusics.find { it.path == music.path }?.album = album.albumName
                MyMediaPlayer.allMusics.find { it.path == music.path }?.albumCover = album.albumCover

                // Mise à jour dans les palylists :
                for (playlist in MyMediaPlayer.allPlaylists) {
                    playlist.musicList.find { it.path == music.path }?.album = album.albumName
                    playlist.musicList.find { it.path == music.path }?.albumCover = album.albumCover
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                writePlaylistsToFile()
                writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)
            }

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun onCancelButtonClick(){
        setResult(RESULT_CANCELED)
        finish()
    }
}