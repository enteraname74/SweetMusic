package com.example.musicplayer.classes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.musicplayer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import java.lang.reflect.AnnotatedElement

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
                putExtra("POSITION", MyMediaPlayer.allPlaylists.indexOf(
                    MyMediaPlayer.allPlaylists.find { it.listName == elementInfos.listName }
                ))
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

    fun positionInList(elementInfos: Any) : Int {
        var res = -1
        for (element in shortcutsList) {
            if (elementInfos is Music && element is Music) {
                if (elementInfos.path == element.path) {
                    res = shortcutsList.indexOf(element)
                    break
                }
            } else if (elementInfos is Playlist && element is Playlist) {
                if (elementInfos.listName == element.listName) {
                    res = shortcutsList.indexOf(element)
                    break
                }
            } else if (elementInfos is Album && element is Album) {
                if ((elementInfos.albumName == element.albumName) && (elementInfos.artist == element.artist)) {
                    res = shortcutsList.indexOf(element)
                    break
                }
            } else if (elementInfos is Artist && element is Artist) {
                if (elementInfos.artistName == element.artistName) {
                    res = shortcutsList.indexOf(element)
                    break
                }
            }
        }

        Log.d("SHORTCUTS", "POS $res")
        return res
    }
}