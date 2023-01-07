package com.example.musicplayer.classes

import com.example.musicplayer.Music
import java.io.Serializable

class Artist(
    var artistName : String,
    var musicList : ArrayList<Music>,
    var artistCover : ByteArray?) : Serializable