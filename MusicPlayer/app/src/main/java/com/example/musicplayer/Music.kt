package com.example.musicplayer

import java.io.Serializable

// Classe permettant de représenter une musique :
data class Music(
    var name: String,
    var artist: String,
    var album: String,
    var albumCover: ByteArray?,
    var duration: Long,
    var path: String,
    var favorite: Boolean = false,
    var soundLevel : Float = 0.5F
) : Serializable