package com.example.musicplayer
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class MusicList(
    var musics : ArrayList<Music>,
    var listName : String,
    private val context : Context,
    private val mOnMusicListener : OnMusicListener,
    var backgroundColor: Int = -1,
    var colorsForText : Palette.Swatch? = null) : RecyclerView.Adapter<MusicList.MusicListViewHolder>(), Serializable {

    class MusicListViewHolder(itemView : View, private var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener,Serializable, View.OnCreateContextMenuListener{

        var albumCover : ImageView? = null
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
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onClick(v: View?) {
            this.onMusicListener.onMusicClick(bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            Log.d("LONG PRESS","")
            itemView.showContextMenu()
            return true
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.add(this.bindingAdapterPosition, 0, 0, itemView.resources.getString(R.string.add_to))
            menu?.add(this.bindingAdapterPosition, 1, 0, itemView.resources.getString(R.string.remove))
            menu?.add(this.bindingAdapterPosition, 2, 0, itemView.resources.getString(R.string.modify))
            menu?.add(this.bindingAdapterPosition, 3, 0, itemView.resources.getString(R.string.play_next))
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
            currentPlayedMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        } catch (error : ArrayIndexOutOfBoundsException){
        }


        if(currentMusic.albumCover != null){
            // Passons d'abord notre byteArray en bitmap :
            val bytes = currentMusic.albumCover
            var bitmap: Bitmap? = null
            if ((bytes != null) && bytes.isNotEmpty()) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            holder.albumCover?.setImageBitmap(bitmap)
        } else {
            holder.albumCover?.setImageResource(R.drawable.michael)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album
        if (backgroundColor != -1){
            holder.background?.setBackgroundColor(backgroundColor)

            if(currentMusic == currentPlayedMusic && MyMediaPlayer.playlistName == listName){
                holder.songName?.setTextColor(colorsForText?.bodyTextColor as Int)
                holder.albumName?.setTextColor(colorsForText?.bodyTextColor as Int)
                holder.separator?.setTextColor(colorsForText?.bodyTextColor as Int)
                holder.artist?.setTextColor(colorsForText?.bodyTextColor as Int)
            } else {
                holder.songName?.setTextColor(colorsForText?.titleTextColor as Int)
                holder.albumName?.setTextColor(colorsForText?.titleTextColor as Int)
                holder.separator?.setTextColor(colorsForText?.titleTextColor as Int)
                holder.artist?.setTextColor(colorsForText?.titleTextColor as Int)
            }
        } else {
            if(currentMusic == currentPlayedMusic && MyMediaPlayer.playlistName == listName){
                holder.songName?.setTextColor(Color.parseColor(context.resources.getString(R.color.selected_music_color)))
                holder.albumName?.setTextColor(Color.parseColor(context.resources.getString(R.color.selected_music_color)))
                holder.separator?.setTextColor(Color.parseColor(context.resources.getString(R.color.selected_music_color)))
                holder.artist?.setTextColor(Color.parseColor(context.resources.getString(R.color.selected_music_color)))
            } else {
                holder.songName?.setTextColor(Color.parseColor(context.resources.getString(R.color.third_color)))
                holder.albumName?.setTextColor(Color.parseColor(context.resources.getString(R.color.third_color)))
                holder.separator?.setTextColor(Color.parseColor(context.resources.getString(R.color.third_color)))
                holder.artist?.setTextColor(Color.parseColor(context.resources.getString(R.color.third_color)))
            }
        }
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}