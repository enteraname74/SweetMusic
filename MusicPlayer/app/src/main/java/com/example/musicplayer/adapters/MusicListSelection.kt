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
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class MusicListSelection(
    var musics : ArrayList<Music>,
    val selectedMusicsPositions : ArrayList<Int>,
    private val context : Context,
    private val mOnMusicListener : OnMusicListener
) : RecyclerView.Adapter<MusicListSelection.MusicListViewHolder>(), Serializable {

    class MusicListViewHolder(itemView : View, private var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, Serializable {

        var albumCover : ImageView? = null
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
        Log.d("POSITION", position.toString())
        val currentMusic = musics[position]

        if(currentMusic.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentMusic.albumCover
            var bitmap: Bitmap? = null
            if (bytes != null && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.albumCover?.setImageBitmap(bitmap)
        } else {
            holder.albumCover?.setImageResource(R.drawable.michael)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album

        if (position in selectedMusicsPositions) {
            if (holder.checkbox?.isChecked == true){
                holder.checkbox?.setChecked(false)
            } else {
                holder.checkbox?.setChecked(true)
            }
        } else {
            holder.checkbox?.setChecked(false)
        }
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}