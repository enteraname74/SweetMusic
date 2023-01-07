package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.classes.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModifyMusicInfoActivity : Tools() {
    private lateinit var musicFile : Music
    private lateinit var albumCoverField : ImageView
    private lateinit var musicNameField : EditText
    private lateinit var albumNameField : EditText
    private lateinit var artistNameField : EditText
    private var indexCurrentPlaylist = -1
    private var indexInitialPlaylist = -1

    private var albumPosition = -1
    private var positionInAlbum = -1
    private var artistPosition = -1
    private var positionInArtist = -1
    private var positionInShortcut = -1

    private lateinit var path : String

    private var hasAlbumCoverChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_music_info)

        if(savedInstanceState != null) {
            updateMusicNotification(!mediaPlayer.isPlaying)
        }

        // On récupère notre musique à modifier :
        path = intent.getSerializableExtra("PATH") as String
        musicFile = MyMediaPlayer.allMusics.first{it.path == path}

        // On récupère la position de notre musique dans les playlists utilisés par le lecteur :
        indexCurrentPlaylist = MyMediaPlayer.currentPlaylist.indexOf(musicFile)
        indexInitialPlaylist = MyMediaPlayer.initialPlaylist.indexOf(musicFile)

        // Si on a déjà initialisé la liste d'albums/artistes (par ex : si on est dans un album/artiste), on récupère l'album et la position dans l'album :
        if (MyMediaPlayer.allAlbums.size > 0){
            for (position in 0 until MyMediaPlayer.allAlbums.size){
                if (MyMediaPlayer.allAlbums[position].albumList.contains(musicFile)){
                    albumPosition = position
                    positionInAlbum = MyMediaPlayer.allAlbums[position].albumList.indexOf(musicFile)
                    break
                }
            }
        }

        if (MyMediaPlayer.allArtists.size > 0){
            for (position in 0 until MyMediaPlayer.allArtists.size){
                if (MyMediaPlayer.allArtists[position].musicList.contains(musicFile)){
                    artistPosition = position
                    positionInArtist = MyMediaPlayer.allArtists[position].musicList.indexOf(musicFile)
                    break
                }
            }
        }

        positionInShortcut = MyMediaPlayer.allShortcuts.positionInList(musicFile)

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
            albumCoverField.setImageResource(R.drawable.ic_saxophone_svg)
        }

        musicNameField.setText(musicFile.name)
        albumNameField.setText(musicFile.album)
        artistNameField.setText(musicFile.artist)

        albumCoverField.setOnClickListener{selectImage()}
        val validateButton = findViewById<Button>(R.id.validate_button)
        val cancelButton = findViewById<Button>(R.id.cancel_button)

        validateButton.setOnClickListener{onValidateButtonClick()}
        cancelButton.setOnClickListener{ onCancelButtonClick() }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
            albumCoverField.setImageBitmap(bitmap)
            hasAlbumCoverChanged = true
        }
    }

    private fun onValidateButtonClick(){
        val returnIntent = Intent()
        // On modifie les éléments du fichier :
        // On enlève les potentiels espaces en trop :
        musicFile.name = musicNameField.text.toString().trim()
        musicFile.album = albumNameField.text.toString().trim()
        musicFile.artist = artistNameField.text.toString().trim()

        if (hasAlbumCoverChanged) {
            val drawable = albumCoverField.drawable
            val bitmapDrawable = drawable as BitmapDrawable
            val bitmap = bitmapDrawable.bitmap

            val byteArray = bitmapToByteArray(bitmap)

            musicFile.albumCover = byteArray
        } else {
            musicFile.albumCover = MyMediaPlayer.allMusics.find { it.album == musicFile.album && it.artist == musicFile.artist && it.albumCover != null }?.albumCover
        }

        // On ne peut pas renvoyer le fichier car l'image de l'album est trop lourde. On écrase donc directement la musique dans le fichier de sauvegarde :
        val allMusics = MyMediaPlayer.allMusics
        var position = allMusics.indexOf(allMusics.first{it.path == musicFile.path})
        allMusics[position] = musicFile
        MyMediaPlayer.allMusics = allMusics

        // Ensuite, mettons à jour nos playlists :
        val playlists = MyMediaPlayer.allPlaylists

        for (playlist in playlists){
            for (music in playlist.musicList){
                if (music.path == musicFile.path){
                    position = playlist.musicList.indexOf(music)
                    playlist.musicList[position] = musicFile
                    break
                }
            }
        }
        MyMediaPlayer.allPlaylists = playlists

        // Modifions les infos de la musique dans nos deux autres playlists :
        if (MyMediaPlayer.currentPlaylist.size != 0 && indexCurrentPlaylist != -1){
            MyMediaPlayer.currentPlaylist[indexCurrentPlaylist] = musicFile
        }

        if (MyMediaPlayer.initialPlaylist.size != 0 && indexInitialPlaylist != -1){
            MyMediaPlayer.initialPlaylist[indexInitialPlaylist] = musicFile
        }
        // Si nous venons d'un album ou d'un artiste, changeons aussi les données la bas :
        if (albumPosition != -1){
            val album = MyMediaPlayer.allAlbums[albumPosition]
            if (musicFile.album != album.albumName || musicFile.artist != album.artist){
                album.albumList.removeAt(positionInAlbum)
            } else {
                MyMediaPlayer.allAlbums[albumPosition].albumList[positionInAlbum] = musicFile
            }
        }
        if (artistPosition != -1){
            val artist = MyMediaPlayer.allArtists[artistPosition]
            if (musicFile.artist != artist.artistName){
                artist.musicList.removeAt(positionInArtist)
            } else {
                MyMediaPlayer.allArtists[artistPosition].musicList[positionInArtist] = musicFile
            }
        }
        MyMediaPlayer.modifiedSong = true

        setResult(RESULT_OK,returnIntent)

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            launch {
                writeAllAsync(
                    MyMediaPlayer.allMusics,
                    MyMediaPlayer.allPlaylists
                )
            }
        }
        // Si une musique se joue, on vérifie si celle jouée actuellement est celle que l'on modifie :
        if (MyMediaPlayer.currentIndex != -1) {
            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].path == musicFile.path) {
                updateMusicNotification(!mediaPlayer.isPlaying)
            }
        }

        if (positionInShortcut != -1) {
            MyMediaPlayer.allShortcuts.shortcutsList[positionInShortcut] = musicFile
            CoroutineScope(Dispatchers.IO).launch { writeAllShortcuts() }
        }

        returnIntent.putExtra("modifiedSongPath", path)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun onCancelButtonClick(){
        setResult(RESULT_CANCELED)
        finish()
    }
}