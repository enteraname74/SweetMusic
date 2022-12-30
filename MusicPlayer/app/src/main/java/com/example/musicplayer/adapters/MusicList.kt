package com.example.musicplayer.adapters
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Music
import com.example.musicplayer.classes.MyMediaPlayer
import com.example.musicplayer.R
import com.google.android.material.imageview.ShapeableImageView
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class MusicList(
    var musics : ArrayList<Music>,
    var listName : String,
    private val context : Context,
    private val mOnMusicListener : OnMusicListener,
    var backgroundColor: Int = -1,
    var colorsForText : Palette.Swatch? = null) : RecyclerView.Adapter<MusicList.MusicListViewHolder>(), Serializable {

    class MusicListViewHolder(itemView : View, private var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener,Serializable {

        var albumCover : ShapeableImageView? = null
        var songName : TextView? = null
        var artist : TextView? = null
        var albumName : TextView? = null
        var separator : TextView? = null
        var background : LinearLayout? = null

        init{
            super.itemView

            albumCover = itemView.findViewById(R.id.album_cover)
            songName = itemView.findViewById(R.id.songs_name)
            artist = itemView.findViewById(R.id.artist)
            albumName = itemView.findViewById(R.id.album_name)
            separator = itemView.findViewById(R.id.separator)
            background = itemView.findViewById(R.id.background)

            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onMusicListener.onMusicClick(bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            this.onMusicListener.onLongMusicClick(bindingAdapterPosition)
            return true
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

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MusicListViewHolder, position: Int) {
        val currentMusic = musics[position]
        var currentPlayedMusic: Music? = null
        try {
            if (MyMediaPlayer.currentPlaylist.size != 0 && MyMediaPlayer.currentIndex != -1) {
                currentPlayedMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            }
        } catch (error : ArrayIndexOutOfBoundsException){
        }

        if(currentMusic.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentMusic.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            holder.albumCover?.setImageBitmap(bitmap)
        } else {
            holder.albumCover?.setImageResource(R.drawable.saxophone)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album
        if (backgroundColor != -1) {
            holder.background?.setBackgroundColor(backgroundColor)
        }
        var textStyle = Typeface.DEFAULT
        var textColor = Color.parseColor(context.resources.getString(R.color.text_color))

        if(currentMusic == currentPlayedMusic && (MyMediaPlayer.playlistName == listName || MyMediaPlayer.currentPlaylist == musics)){
            textStyle = Typeface.DEFAULT_BOLD
            textColor = Color.parseColor(context.resources.getString(R.color.selected_music_color))
        }
        holder.songName?.apply {
            setTextColor(textColor)
            typeface = textStyle
        }
        holder.albumName?.apply {
            setTextColor(textColor)
            typeface = textStyle
        }
        holder.separator?.apply {
            setTextColor(textColor)
            typeface = textStyle
        }
        holder.artist?.apply {
            setTextColor(textColor)
            typeface = textStyle
        }
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)

        fun onLongMusicClick(position : Int)
    }
}