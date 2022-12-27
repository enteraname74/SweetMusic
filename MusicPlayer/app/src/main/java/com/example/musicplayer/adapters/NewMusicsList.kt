package com.example.musicplayer.adapters
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Music
import com.example.musicplayer.R
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class NewMusicsList(
    var musics : ArrayList<Music>,
    private val mOnMusicListener : NewMusicsList.OnMusicListener,
    private val context : Context) : RecyclerView.Adapter<NewMusicsList.NewMusicListViewHolder>(), Serializable {

    class NewMusicListViewHolder(itemView : View, private var onMusicListener : NewMusicsList.OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,Serializable {

        var albumCover : ShapeableImageView? = null
        var songName : TextView? = null
        var artist : TextView? = null
        var albumName : TextView? = null

        init{
            super.itemView

            albumCover = itemView.findViewById(R.id.album_cover)
            songName = itemView.findViewById(R.id.songs_name)
            artist = itemView.findViewById(R.id.artist)
            albumName = itemView.findViewById(R.id.album_name)

            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            this.onMusicListener.onMusicClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewMusicListViewHolder {
        return NewMusicListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.music_file,
                parent,
                false
            ), mOnMusicListener
        )
    }

    override fun onBindViewHolder(holder: NewMusicListViewHolder, position: Int) {
        val currentMusic = musics[position]

        if(currentMusic.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentMusic.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.albumCover?.setImageBitmap(bitmap)
        } else {
            holder.albumCover?.setImageResource(R.drawable.ic_saxophone_svg)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album

    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}