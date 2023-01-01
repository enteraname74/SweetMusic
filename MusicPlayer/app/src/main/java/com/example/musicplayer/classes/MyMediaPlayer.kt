package com.example.musicplayer.classes

import android.content.Context
import android.media.MediaPlayer
import com.example.musicplayer.Music
import com.example.musicplayer.Playlist
import com.example.musicplayer.R

class MyMediaPlayer {
    /*Permet de créer un équivalent de "static" en Java
    Toutes les valeurs présentes dans companion object sont en static
     */
    companion object {
        private var instance : MediaPlayer? = null

        val getInstance : MediaPlayer
        get(){
            if (instance == null){
                instance = MediaPlayer()
            }
            return instance as MediaPlayer
        }

        var currentIndex : Int = -1
        var initialPlaylist = ArrayList<Music>()
        var currentPlaylist = ArrayList<Music>()
        var playlistName = ""

        // Afin de gagner en performances, nous utiliserons la classe  MyMediaPlayer pour faire passer certains infos :
        // Permet de savoir si on va devoir rafraichir la playlist principale si une musique a été modifiée :
        var modifiedSong = false


        var dataWasChanged = false
        //Permet d'éviter de chercher à chaque fois dans les fichiers toutes les musiques/playlists :

        var allMusics = ArrayList<Music>()
        var allPlaylists = ArrayList<Playlist>()
        var allDeletedMusics = ArrayList<Music>()
        var allFolders = ArrayList<Folder>()

        val iconsList = listOf(
            R.drawable.ic_baseline_sync_24,
            R.drawable.ic_baseline_shuffle_24,
            R.drawable.ic_baseline_replay_24
        )
        var iconIndex = 0

        var allAlbums = ArrayList<Album>()
        var allArtists = ArrayList<Artist>()
    }
}