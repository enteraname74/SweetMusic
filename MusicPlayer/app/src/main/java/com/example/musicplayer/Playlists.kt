package com.example.musicplayer

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

class Playlists (
    var allPlaylists : ArrayList<Playlist>,
    private val context : Context,
    private val mOnPlaylistListener : Playlists.OnPlaylistsListener ) : RecyclerView.Adapter<Playlists.PlaylistsViewHolder>(), Serializable {

    class PlaylistsViewHolder(itemView : View, private var onPlaylistsListener : OnPlaylistsListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var albumCover : ImageView? = null
        var playlistName : TextView? = null


        init{
            super.itemView

            albumCover = itemView.findViewById(R.id.playlist_cover)
            playlistName = itemView.findViewById(R.id.playlist_name)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onPlaylistsListener.onPlaylistClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        return PlaylistsViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.playlist_file,
                parent,
                false
            ), mOnPlaylistListener
        )
    }

    override fun onBindViewHolder(holder: Playlists.PlaylistsViewHolder, position: Int) {
        val currentPlaylist = allPlaylists[position]

        holder.albumCover?.setImageResource(R.drawable.michael)
        holder.playlistName?.text = currentPlaylist.listName
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