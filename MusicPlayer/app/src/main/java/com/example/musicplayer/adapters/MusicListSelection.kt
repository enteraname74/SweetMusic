package com.example.musicplayer.adapters
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Music
import com.example.musicplayer.R
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class MusicListSelection(
    var musics : ArrayList<Music>,
    val selectedMusicsInfos : HashMap<Int, String>,
    private val context : Context,
    private val mOnMusicListener : OnMusicListener
) : RecyclerView.Adapter<MusicListSelection.MusicListViewHolder>(), Serializable {

    class MusicListViewHolder(itemView : View, private var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, Serializable {

        var albumCover : ShapeableImageView? = null
        var songName : TextView? = null
        var artist : TextView? = null
        var albumName : TextView? = null
        var background : LinearLayout? = null
        var checkbox : CheckBox? = null

        init{
            super.itemView

            albumCover = itemView.findViewById(R.id.album_cover)
            songName = itemView.findViewById(R.id.songs_name)
            artist = itemView.findViewById(R.id.artist)
            albumName = itemView.findViewById(R.id.album_name)
            background = itemView.findViewById(R.id.background)
            checkbox = itemView.findViewById(R.id.checkbox_music)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onMusicListener.onMusicClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicListViewHolder {
        return MusicListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.music_file_selection,
                parent,
                false
            ), mOnMusicListener
        )
    }

    override fun onBindViewHolder(holder: MusicListViewHolder, position: Int) {
        val currentMusic = musics[position]

        if(currentMusic.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentMusic.albumCover
            var bitmap: Bitmap? = null
            if (bytes != null && bytes.isNotEmpty()) {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            holder.albumCover?.setImageBitmap(bitmap)
        } else {
            holder.albumCover?.setImageResource(R.drawable.ic_saxophone_svg)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album

        holder.checkbox?.isChecked = currentMusic.path in selectedMusicsInfos.values
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}