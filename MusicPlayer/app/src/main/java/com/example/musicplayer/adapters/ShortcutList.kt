package com.example.musicplayer.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.classes.Shortcuts
import java.io.Serializable

class ShortcutList(
    var shortcuts: Shortcuts,
    private val context: Context,
    private val onShortcutListener : ShortcutList.OnShortcutListener,
) : RecyclerView.Adapter<ShortcutList.ShortcutListViewHolder>(){

    class ShortcutListViewHolder(itemView : View, val context: Context, val shortcuts: Shortcuts, private val onShortcutListener: OnShortcutListener) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener,
        View.OnLongClickListener,
        Serializable {

        var cover : ImageView
        var name : TextView

        init{
            super.itemView
            cover = itemView.findViewById(R.id.cover)
            name = itemView.findViewById(R.id.name)
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            context.startActivity(shortcuts.getIntentFromElement(context, shortcuts.shortcutsList[bindingAdapterPosition]))
        }

        override fun onLongClick(p0: View?): Boolean {
            this.onShortcutListener.onLongShortcutClick(bindingAdapterPosition)
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutListViewHolder {
        return ShortcutListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.shorcut_file,
                parent,
                false
            ),
            context,
            shortcuts,
            onShortcutListener
        )
    }

    override fun onBindViewHolder(holder: ShortcutListViewHolder, position: Int) {
        val element = shortcuts.shortcutsList[position]
        val bytes = shortcuts.getCoverOfShortcut(element)
        if(bytes != null){
            // Passons d'abord notre byteArray en bitmap :
            var bitmap: Bitmap? = null
            if (bytes.isNotEmpty()) {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            holder.cover.setImageBitmap(bitmap)
        } else {
            holder.cover.setImageResource(R.drawable.saxophone)
        }

        holder.name.text = shortcuts.getNameOfShortcut(element)
    }

    override fun getItemCount(): Int {
        return shortcuts.shortcutsList.size
    }

    interface OnShortcutListener {
        fun onLongShortcutClick(position: Int)
    }
}