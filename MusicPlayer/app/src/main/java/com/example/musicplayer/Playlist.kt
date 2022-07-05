package com.example.musicplayer

import java.io.Serializable

data class Playlist (
    val listName : String,
    val musicList : ArrayList<Music>,
    val isFavoriteList : Boolean
        ): Serializable {}