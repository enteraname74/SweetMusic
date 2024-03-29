package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
    private lateinit var selectedAlbum : Album
    private lateinit var playlistCoverField : ImageView
    private lateinit var playlistNameField : EditText
    private var positionInShortcuts = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_playlist_info)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        albumPos = intent.getSerializableExtra("POS") as Int
        selectedAlbum = MyMediaPlayer.allAlbums[albumPos]

        positionInShortcuts = MyMediaPlayer.allShortcuts.positionInList(selectedAlbum)

        playlistCoverField = findViewById(R.id.playlist_cover)
        playlistNameField = findViewById(R.id.edit_playlist_name)

        findViewById<TextView>(R.id.title_activity).text = getString(R.string.modify_album)
        findViewById<TextView>(R.id.playlist_name).text = getString(R.string.name_of_the_album)

        if (selectedAlbum.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = selectedAlbum.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            playlistCoverField.setImageBitmap(bitmap)
        } else {
            playlistCoverField.setImageResource(R.drawable.ic_saxophone_svg)
        }

        playlistNameField.setText(selectedAlbum.albumName)

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
            selectedAlbum.albumName = playlistNameField.text.toString().trim()
            val drawable = playlistCoverField.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            val bitmap = bitmapDrawable.bitmap

            val byteArray = bitmapToByteArray(bitmap)

            selectedAlbum.albumCover = byteArray

            MyMediaPlayer.allAlbums[albumPos] = selectedAlbum

            // Mettons à jour toutes les musiques de l'album :
            for (music in selectedAlbum.albumList) {
                music.album = selectedAlbum.albumName
                music.albumCover = selectedAlbum.albumCover

                // Mise à jour dans la playlist principale :
                MyMediaPlayer.allMusics.find { it.path == music.path }?.apply {
                    album = selectedAlbum.albumName
                    albumCover = selectedAlbum.albumCover
                }

                // Mise à jour dans les palylists :
                for (playlist in MyMediaPlayer.allPlaylists) {
                    playlist.musicList.find { it.path == music.path }?.apply {
                        album = selectedAlbum.albumName
                        albumCover = selectedAlbum.albumCover
                    }
                }

                // Mise à jour des playlists de lecture :

                MyMediaPlayer.initialPlaylist.find { it.path == music.path }?.apply {
                    album = selectedAlbum.albumName
                    albumCover = selectedAlbum.albumCover
                }

                MyMediaPlayer.currentPlaylist.find { it.path == music.path }?.apply {
                    album = selectedAlbum.albumName
                    albumCover = selectedAlbum.albumCover
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                writeAllPlaylists()
                writeAllMusics()
            }

            CoroutineScope(Dispatchers.IO).launch {
                if (positionInShortcuts != -1) {
                    MyMediaPlayer.allShortcuts.shortcutsList[positionInShortcuts] = selectedAlbum
                    writeAllShortcuts()
                }
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