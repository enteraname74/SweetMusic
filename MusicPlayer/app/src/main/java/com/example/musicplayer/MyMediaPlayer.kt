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
    }


}