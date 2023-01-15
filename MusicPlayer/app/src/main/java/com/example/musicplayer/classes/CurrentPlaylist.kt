package com.example.musicplayer.classes

import com.example.musicplayer.Music
import java.io.Serializable

class CurrentPlaylist(
    var initialPlaylist : ArrayList<Music>,
    var currentPlaylist : ArrayList<Music>,
    var currentMusicPos : Int) : Serializable