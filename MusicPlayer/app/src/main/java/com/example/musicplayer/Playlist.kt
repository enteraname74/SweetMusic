package com.example.musicplayer

import java.io.Serializable

data class Playlist (
    val listName : String,
    var musicList : ArrayList<Music>,
    var playlistCover : ByteArray?,
    val isFavoriteList : Boolean = false
        ): Serializable {}