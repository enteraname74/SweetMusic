package com.example.musicplayer.classes

import java.io.Serializable

data class Folder(
    val path : String,
    var isUsedInApp : Boolean = true
): Serializable