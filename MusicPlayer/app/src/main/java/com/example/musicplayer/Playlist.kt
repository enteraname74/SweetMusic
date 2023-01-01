package com.example.musicplayer

import com.example.musicplayer.Music
import java.io.Serializable

data class Playlist (
    var listName : String,
    var musicList : ArrayList<Music>,
    var playlistCover : ByteArray?,
    val isFavoriteList : Boolean = false
        ): Serializable {}