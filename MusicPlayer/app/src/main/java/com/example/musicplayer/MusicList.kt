package com.example.musicplayer
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class MusicList(
    var musics : ArrayList<Music>,
    private val context : Context,
    private val mOnMusicListener : OnMusicListener) : RecyclerView.Adapter<MusicList.MusicListViewHolder>(), Serializable {

    class MusicListViewHolder(itemView : View, private var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, Serializable {

        var albumCover : ImageView? = null
        var songName : TextView? = null
        var artist : TextView? = null
        var albumName : TextView? = null
        var background : LinearLayout? = null

        init{
            super.itemView

            albumCover = itemView.findViewById(R.id.album_cover)
            songName = itemView.findViewById(R.id.songs_name)
            artist = itemView.findViewById(R.id.artist)
            albumName = itemView.findViewById(R.id.album_name)
            background = itemView.findViewById(R.id.background)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onMusicListener.onMusicClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicListViewHolder {
        return MusicListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.music_file,
                parent,
                false
            ), mOnMusicListener
        )
    }

    override fun onBindViewHolder(holder: MusicListViewHolder, position: Int) {
        Log.d("POSITION", position.toString())
        val currentMusic = musics[position]

        holder.albumCover?.setImageResource(R.drawable.michael)
        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album

        if(MyMediaPlayer.currentIndex == position && MyMediaPlayer.currentPlaylist == musics){
            Log.d("CHANGE COLOR", MyMediaPlayer.currentIndex.toString())
            holder.songName?.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            holder.songName?.setTextColor(Color.parseColor("#7da7c5"))
        }
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}