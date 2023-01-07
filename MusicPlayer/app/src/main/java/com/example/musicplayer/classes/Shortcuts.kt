package com.example.musicplayer.classes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.res.ResourcesCompat
import com.example.musicplayer.*
import java.io.Serializable

class Shortcuts(
    val shortcutsList : ArrayList<Any>
) : Serializable {

    fun getIntentFromElement(context : Context, elementInfos : Any) : Intent? {
        return when(elementInfos) {
            is Music -> {
                MyMediaPlayer.initialPlaylist = arrayListOf(elementInfos)
                MyMediaPlayer.currentPlaylist = arrayListOf(elementInfos)
                MyMediaPlayer.playlistName = ""
                MyMediaPlayer.currentIndex = 0
                Intent(context, MusicPlayerActivity::class.java).apply {
                    putExtra("SAME MUSIC", false)
                }
            }
            is Playlist -> Intent(context, SelectedPlaylistActivity::class.java).apply {
                putExtra("POSITION", MyMediaPlayer.allPlaylists.indexOf(elementInfos))
            }
            is Album -> Intent(context, SelectedAlbumActivity::class.java).apply {
                putExtra("POSITION", MyMediaPlayer.allAlbums.indexOf(elementInfos))
            }
            is Artist -> Intent(context, SelectedArtistActivity::class.java).apply {
                putExtra("POSITION", MyMediaPlayer.allArtists.indexOf(elementInfos))
            }
            else -> null
        }
    }

    fun getNameOfShortcut(elementInfos : Any) : String? {
        return when(elementInfos) {
            is Music -> elementInfos.name
            is Playlist -> elementInfos.listName
            is Album -> elementInfos.albumName
            is Artist -> elementInfos.artistName
            else -> null
        }
    }

    fun getCoverOfShortcut(elementInfos : Any) : ByteArray? {
        return when(elementInfos) {
            is Music -> elementInfos.albumCover
            is Playlist -> elementInfos.playlistCover
            is Album -> elementInfos.albumCover
            is Artist -> elementInfos.artistCover
            else -> null
        }
    }
}