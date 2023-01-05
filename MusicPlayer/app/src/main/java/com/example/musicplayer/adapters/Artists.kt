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
import com.example.musicplayer.classes.Artist
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

class Artists (
    var allArtists : ArrayList<Artist>,
    private val context : Context,
    private val mOnArtistListener : OnArtistsListener
) : RecyclerView.Adapter<Artists.ArtistsViewHolder>(), Serializable {

    class ArtistsViewHolder(itemView: View, private var onArtistsListener: OnArtistsListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, Serializable {

        var artistName: TextView
        var artistsCover: ShapeableImageView
        var songsNumber: TextView

        init {
            super.itemView

            artistName = itemView.findViewById(R.id.artist_name)
            artistsCover = itemView.findViewById(R.id.artist_cover)
            songsNumber = itemView.findViewById(R.id.number_of_songs)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onArtistsListener.onArtistClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsViewHolder {
        return ArtistsViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.artist_file,
                parent,
                false
            ), mOnArtistListener
        )
    }

    override fun onBindViewHolder(holder: ArtistsViewHolder, position: Int) {
        val currentArtist = allArtists[position]


        holder.artistName.text = currentArtist.artistName
        val numberText = if (currentArtist.musicList.size > 1) {
            context.getString(R.string.x_musics, currentArtist.musicList.size)
        } else {
            context.getString(R.string.one_music)
        }
        holder.songsNumber.text = numberText

        if (currentArtist.artistCover != null) {
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentArtist.artistCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            holder.artistsCover.setImageBitmap(bitmap)
        } else {
            holder.artistsCover.setImageResource(R.drawable.ic_saxophone_svg)
        }
    }

    override fun getItemCount(): Int {
        return allArtists.size
    }

    interface OnArtistsListener {
        fun onArtistClick(position: Int)
    }
}
