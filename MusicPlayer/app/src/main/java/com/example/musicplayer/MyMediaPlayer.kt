package com.example.musicplayer

import android.media.MediaPlayer

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
        var currentPlaylist = ArrayList<Music>()
        var playlistName = ""

        // Afin de gagner en performances, nous utiliserons la classe  MyMediaPlayer pour faire passer certains infos :
        // Permet de savoir si on va devoir rafraichir la playlist principale si une musique a été modifiée :
        var modifiedSong = false

        //Permet d'éviter de chercher à chaque fois dans les fichiers toutes les musiques/playlists :

        var allMusics = ArrayList<Music>()
        var allPlaylists = ArrayList<Playlist>()

        var doesASongWillBePlaying = true
    }


}