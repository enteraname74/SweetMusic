package com.example.musicplayer.adapters
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Music
import com.example.musicplayer.R
import java.io.Serializable

// Classe permettant de représenter une liste de musiques :
data class DeletedMusicList(
    var musics : ArrayList<Music>,
    var listName : String,
    private val context : Context) : RecyclerView.Adapter<DeletedMusicList.DeletedMusicListViewHolder>(), Serializable {

    class DeletedMusicListViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,Serializable, View.OnCreateContextMenuListener{

        var albumCover : ImageView? = null
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
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onClick(v: View?) {
            itemView.showContextMenu()
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.add(this.bindingAdapterPosition, 20, 0, itemView.resources.getString(R.string.retrieve_music))
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedMusicListViewHolder {
        return DeletedMusicListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.music_file,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DeletedMusicListViewHolder, position: Int) {
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
            holder.albumCover?.setImageResource(R.drawable.michael)
        }

        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album

    }

    override fun getItemCount(): Int {
        return musics.size
    }
}