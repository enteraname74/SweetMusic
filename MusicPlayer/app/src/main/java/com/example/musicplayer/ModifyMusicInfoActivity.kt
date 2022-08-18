package com.example.musicplayer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ModifyMusicInfoActivity : Tools() {
    private lateinit var musicFile : Music
    private lateinit var albumCoverField : ImageView
    private lateinit var musicNameField : EditText
    private lateinit var albumNameField : EditText
    private lateinit var artistNameField : EditText
    private var givenPosition = -1
    private var indexCurrentPlaylist = -1
    private var indexInitialPlaylist = -1
    private var indexCurrentAlbum = -1
    private var indexCurrentArtist = -1
    private lateinit var currentPlaylist : ArrayList<Music>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_music_info)

        val playlistName = intent.getSerializableExtra("PLAYLIST_NAME") as String

        currentPlaylist = if (playlistName == "Main") {
            MyMediaPlayer.allMusics
        } else if(playlistName == "Album") {
            indexCurrentAlbum = intent.getSerializableExtra("ALBUM POSITION") as Int
            MyMediaPlayer.allAlbums[indexCurrentAlbum].albumList
        } else if(playlistName == "Artist") {
            indexCurrentArtist= intent.getSerializableExtra("ARTIST POSITION") as Int
            MyMediaPlayer.allArtists[indexCurrentArtist].musicList
        } else {
            val allPlaylists = MyMediaPlayer.allPlaylists
            allPlaylists.first{it.listName == playlistName}.musicList
        }

        // On récupère notre musique à modifier :
        givenPosition = intent.getSerializableExtra("POSITION") as Int
        musicFile = currentPlaylist[givenPosition]

        indexCurrentPlaylist = MyMediaPlayer.currentPlaylist.indexOf(musicFile)
        indexInitialPlaylist = MyMediaPlayer.initialPlaylist.indexOf(musicFile)

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
            val uri : Uri? = result.getData()?.data
            val inputStream = contentResolver.openInputStream(uri as Uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            albumCoverField.setImageBitmap(bitmap)
        }
    }

    private fun onValidateButtonClick(){
        val returnIntent = Intent()
        // On modifie les éléments du fichier :

        // On enlève les potentiels espaces en trop :
        musicFile.name = musicNameField.text.toString().trim()
        musicFile.album = albumNameField.text.toString().trim()
        musicFile.artist = artistNameField.text.toString().trim()

        val drawable = albumCoverField.drawable
        val bitmapDrawable = drawable as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap

        val byteArray = bitmapToByteArray(bitmap)

        musicFile.albumCover = byteArray

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
        Log.d("index",MyMediaPlayer.currentPlaylist.indexOf(musicFile).toString())
        if (MyMediaPlayer.currentPlaylist.size != 0 && indexCurrentPlaylist != -1){
            MyMediaPlayer.currentPlaylist[indexCurrentPlaylist] = musicFile
        }

        if (MyMediaPlayer.initialPlaylist.size != 0 && indexInitialPlaylist != -1){
            MyMediaPlayer.initialPlaylist[indexInitialPlaylist] = musicFile
        }
        // Si nous venons d'un album ou d'un artiste, changeons aussi les données la bas :
        if (indexCurrentAlbum != -1){
            MyMediaPlayer.allAlbums[indexCurrentAlbum].albumList[givenPosition] = musicFile
        } else if (indexCurrentArtist != -1){
            MyMediaPlayer.allArtists[indexCurrentArtist].musicList[givenPosition] = musicFile
        }
        MyMediaPlayer.modifiedSong = true

        returnIntent.putExtra("POSITION", givenPosition)
        setResult(RESULT_OK,returnIntent)

        GlobalScope.launch(Dispatchers.IO) {
            launch {
                writeAllAsync(
                    MyMediaPlayer.allMusics,
                    MyMediaPlayer.allPlaylists
                )
            }
        }
        finish()
    }

    private fun onCancelButtonClick(){
        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }
}