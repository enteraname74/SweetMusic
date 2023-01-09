package com.example.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.musicplayer.Music
import com.example.musicplayer.classes.MyMediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream

class ChangeFavoriteStateNotificationReceiver : BroadcastReceiver() {

    private val saveAllMusicsFile = "allMusics.musics"
    private val savePlaylistsFile = "allPlaylists.playlists"
    private val saveAllShortcuts = "allShortcuts.shortcuts"

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("FAVORITE STATE LISTENER", "CHANGED STATE")
            setFavorite(MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex], context)

            val intentForNotification = Intent("BROADCAST_NOTIFICATION")
            intentForNotification.putExtra("FAVORITE_CHANGED", true)
            intentForNotification.putExtra("STOP", !(MyMediaPlayer.getInstance.isPlaying))
            context.sendBroadcast(intentForNotification)

            val intentForActivity = Intent("BROADCAST")
            intentForActivity.putExtra("FAVORITE_CHANGED", true)
            context.sendBroadcast(intentForActivity)
        }

    }

    private fun setFavorite(currentSong : Music, context: Context){
        CoroutineScope(Dispatchers.Main).launch {
            if (!changingFavouriteState) {
                changingFavouriteState = true
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if(currentSong.favorite){
                            MyMediaPlayer.initialPlaylist.find { it.path == currentSong.path }?.favorite = false
                            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = false
                            currentSong.favorite = false
                        } else {
                            MyMediaPlayer.initialPlaylist.find { it.path == currentSong.path }?.favorite = true
                            MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].favorite = true
                            currentSong.favorite = true
                        }

                        // il faut maintenant sauvegardé l'état de la musique dans TOUTES les playlists :
                        // Commencons par la playlist principale :
                        MyMediaPlayer.allMusics.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                        // Ensuite, mettons à jour nos playlists :
                        val allPlaylists = MyMediaPlayer.allPlaylists
                        for (playlist in allPlaylists){
                            playlist.musicList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                        }
                        // Mettons à jour la playlist Favorites :
                        val favoritePlaylist = allPlaylists[0]
                        var shouldBeInFavoriteList = true
                        for (element in favoritePlaylist.musicList){
                            if (element.path == currentSong.path){
                                favoritePlaylist.musicList.remove(element)
                                shouldBeInFavoriteList = false
                                break
                            }
                        }
                        if (shouldBeInFavoriteList){
                            favoritePlaylist.musicList.add(currentSong)
                        }

                        val posInShortcuts = MyMediaPlayer.allShortcuts.positionInList(currentSong)
                        if(posInShortcuts != -1) {
                            (MyMediaPlayer.allShortcuts.shortcutsList[posInShortcuts] as Music).favorite = currentSong.favorite
                            CoroutineScope(Dispatchers.IO).launch {
                                writeAllShortcuts(context)
                            }
                        }

                        // Mettons à jour les albums et les artistes :
                        for (album in MyMediaPlayer.allAlbums) {
                            if (album.albumList.find { it.path == currentSong.path } != null) {
                                album.albumList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                            }
                        }

                        for (artist in MyMediaPlayer.allArtists) {
                            if (artist.musicList.find { it.path == currentSong.path } != null) {
                                artist.musicList.find { it.path == currentSong.path }?.favorite = currentSong.favorite
                            }
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            writeAllMusics(context)
                            writeAllPlaylists(context)
                            changingFavouriteState = false
                        }

                        Log.d("AFTER CHANGE STATE", "POS : "+MyMediaPlayer.currentIndex)
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        Log.e("error", e.toString())
                    }
                }
            }
        }
    }

    private fun writeAllMusics(context: Context) {
        val path = context.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllMusicsFile)))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write musics",error.toString())
        }
    }

    private fun writeAllShortcuts(context: Context){
        val path = context.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllShortcuts)))
            oos.writeObject(MyMediaPlayer.allShortcuts)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write shortcuts",error.toString())
        }
    }

    private fun writeAllPlaylists(context: Context){
        val path = context.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, savePlaylistsFile)))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write playlists",error.toString())
        }
    }

    companion object {
        private var changingFavouriteState = false
    }
}