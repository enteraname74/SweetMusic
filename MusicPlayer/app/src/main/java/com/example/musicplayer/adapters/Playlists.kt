package com.example.musicplayer.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.Playlist
import java.io.Serializable
import java.lang.Error

class Playlists (
    var allPlaylists : ArrayList<Playlist>,
    private val context : Context,
    private val mOnPlaylistListener : OnPlaylistsListener,
    private val resourceName : Int) : RecyclerView.Adapter<Playlists.PlaylistsViewHolder>(), Serializable {

    class PlaylistsViewHolder(itemView : View, private var onPlaylistsListener : OnPlaylistsListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener,Serializable, View.OnCreateContextMenuListener {

        var playlistName : TextView
        var playlistCover : ImageView
        var songNumber : TextView? = null

        init{
            super.itemView

            playlistName = itemView.findViewById(R.id.playlist_name)
            playlistCover = itemView.findViewById(R.id.playlist_cover)
            try {
                songNumber = itemView.findViewById(R.id.number_of_songs)
            } catch (error : Error){

            }

            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onClick(v: View?) {
            this.onPlaylistsListener.onPlaylistClick(bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            Log.d("LONG PRESS","")
            itemView.showContextMenu()
            return true
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.add(this.bindingAdapterPosition, 10, 0, itemView.resources.getString(R.string.remove))
            menu?.add(this.bindingAdapterPosition, 11, 0, itemView.resources.getString(R.string.modify))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        return PlaylistsViewHolder(
            LayoutInflater.from(context).inflate(
                resourceName,
                parent,
                false
            ), mOnPlaylistListener
        )
    }

    override fun onBindViewHolder(holder: PlaylistsViewHolder, position: Int) {
        val currentPlaylist = allPlaylists[position]


        holder.playlistName.text = currentPlaylist.listName
        if (resourceName == R.layout.playlist_file_linear){
            val text = currentPlaylist.musicList.size.toString() + " songs"
            holder.songNumber?.text = text
        }

        if (currentPlaylist.playlistCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentPlaylist.playlistCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.playlistCover.setImageBitmap(bitmap)
        } else {
            holder.playlistCover.setImageResource(R.drawable.michael)
        }
    }

    override fun getItemCount(): Int {
        return allPlaylists.size
    }

    interface OnPlaylistsListener {
        fun onPlaylistClick(position : Int)
    }

    fun addList(list : Playlist) {
        allPlaylists.add(list)
    }
}