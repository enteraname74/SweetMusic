package com.example.musicplayer.classes

import com.example.musicplayer.Music

class Album(
    var albumName : String,
    var albumList : ArrayList<Music>,
    var albumCover : ByteArray?,
    var artist : String)