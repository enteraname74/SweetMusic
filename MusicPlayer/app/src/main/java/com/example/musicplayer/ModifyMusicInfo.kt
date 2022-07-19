package com.example.musicplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.EditText
import android.widget.ImageView

class ModifyMusicInfo : AppCompatActivity() {
    private lateinit var musicFile : Music
    private lateinit var albumCoverField : ImageView
    private lateinit var musicNameField : EditText
    private lateinit var albumNameField : EditText
    private lateinit var artistNameField : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_music_info)

        // On récupère notre musique à modifier :
        musicFile = intent.getSerializableExtra("MUSIC") as Music

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
    }
}