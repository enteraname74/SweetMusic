package com.example.musicplayer

import java.io.Serializable

data class Playlist (
    val listName : String,
    var musicList : ArrayList<Music>,
    val isFavoriteList : Boolean = false
        ): Serializable {}