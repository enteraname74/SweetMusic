package com.example.musicplayer.classes

import com.example.musicplayer.Music
import java.io.Serializable

class Album(
    var albumName : String,
    var albumList : ArrayList<Music>,
    var albumCover : ByteArray?,
    var artist : String) : Serializable