package com.example.musicplayer

import java.io.Serializable

data class Music (
    val name : String,
    val artist : String,
    val album : String,
    val albumCoverPath : String,
    val duration : Long,
    val path : String
        ) : Serializable{}