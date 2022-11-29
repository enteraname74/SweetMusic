package com.example.musicplayer.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.classes.Album
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

class Albums (
    var allAlbums : ArrayList<Album>,
    private val context : Context,
    private val mOnAlbumListener : OnAlbumsListener
) : RecyclerView.Adapter<Albums.AlbumsViewHolder>(), Serializable {

    class AlbumsViewHolder(itemView : View, private var onAlbumsListener : OnAlbumsListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,Serializable {

        var albumName : TextView
        var albumCover : ShapeableImageView
        var artistName : TextView

        init{
            super.itemView

            albumName = itemView.findViewById(R.id.album_name)
            albumCover = itemView.findViewById(R.id.album_cover)
            artistName = itemView.findViewById(R.id.artist_name)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onAlbumsListener.onAlbumClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumsViewHolder {
        return AlbumsViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.album_file,
                parent,
                false
            ), mOnAlbumListener
        )
    }

    override fun onBindViewHolder(holder: AlbumsViewHolder, position: Int) {
        val currentAlbum = allAlbums[position]


        holder.albumName.text = currentAlbum.albumName
        holder.artistName.text = currentAlbum.artist

        if (currentAlbum.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentAlbum.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.albumCover.setImageBitmap(bitmap)
        } else {
            holder.albumCover.setImageResource(R.drawable.michael)
        }
    }

    override fun getItemCount(): Int {
        return allAlbums.size
    }

    interface OnAlbumsListener {
        fun onAlbumClick(position : Int)
    }
}