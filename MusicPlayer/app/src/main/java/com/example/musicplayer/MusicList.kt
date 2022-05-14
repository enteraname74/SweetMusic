package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.car.ui.utils.CarUiUtils.getActivity
import kotlinx.android.synthetic.main.music_file.view.*


class MusicList(private val musics : ArrayList<Music>, private val context : Context, private val mOnMusicListener : OnMusicListener) : RecyclerView.Adapter<MusicList.MusicListViewHolder>() {

    class MusicListViewHolder(var itemView : View, var onMusicListener : OnMusicListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

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

    private fun onListItemClick(position : Int){
        var currentMusic = musics[position]

        MyMediaPlayer.getInstance.reset()
        MyMediaPlayer.currentIndex = position
        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */
        val intent = Intent(context,MusicPlayerActivity::class.java).apply{
            putExtra("LIST",musics)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    override fun onBindViewHolder(holder: MusicListViewHolder, position: Int) {
        var currentMusic = musics[position]

        holder.albumCover?.setImageResource(R.drawable.icone_musique)
        holder.songName?.text = currentMusic.name
        holder.artist?.text = currentMusic.artist
        holder.albumName?.text = currentMusic.album
        if(MyMediaPlayer.currentIndex == position){
            holder.background?.setBackgroundColor(Color.parseColor("#161925"))
        }
    }

    override fun getItemCount(): Int {
        return musics.size
    }

    interface OnMusicListener {
        fun onMusicClick(position : Int)
    }
}