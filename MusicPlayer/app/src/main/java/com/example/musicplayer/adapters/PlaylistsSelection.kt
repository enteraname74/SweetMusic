package com.example.musicplayer.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Playlist
import com.example.musicplayer.R
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

class PlaylistsSelection(
    var playlists : ArrayList<Playlist>,
    var selectedPlaylists : HashMap<Int, String>,
    private val context : Context,
    private val mOnPlaylistListener : OnPlaylistListener
    ) : RecyclerView.Adapter<PlaylistsSelection.PlaylistsSelectionViewHolder>(), Serializable {

    class PlaylistsSelectionViewHolder(itemView : View, private var onPlaylistsSelectionListener : OnPlaylistListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,Serializable {

        var playlistName : TextView
        var playlistCover : ShapeableImageView
        var songNumber : TextView
        var checkBox : CheckBox

        init{
            super.itemView

            playlistName = itemView.findViewById(R.id.playlist_name)
            playlistCover = itemView.findViewById(R.id.playlist_cover)
            songNumber = itemView.findViewById(R.id.number_of_songs)
            checkBox = itemView.findViewById(R.id.checkbox_playlist)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onPlaylistsSelectionListener.onPlaylistClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsSelectionViewHolder {
        return PlaylistsSelectionViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.playlist_file_selection,
                parent,
                false
            ), mOnPlaylistListener
        )
    }

    override fun onBindViewHolder(holder: PlaylistsSelectionViewHolder, position: Int) {
        val currentPlaylist = playlists[position]

        if (currentPlaylist.playlistCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentPlaylist.playlistCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.playlistCover.setImageBitmap(bitmap)
        } else {
            holder.playlistCover.setImageResource(R.drawable.ic_saxophone_svg)
        }

        holder.playlistName.text = currentPlaylist.listName
        holder.songNumber.text = context.getString(R.string.songs_number, currentPlaylist.musicList.size)

        holder.checkBox.isChecked = currentPlaylist.listName in selectedPlaylists.values
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    interface OnPlaylistListener {
        fun onPlaylistClick(position : Int)
    }
}