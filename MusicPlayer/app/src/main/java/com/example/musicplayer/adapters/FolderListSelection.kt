package com.example.musicplayer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.classes.Folder
import java.io.Serializable

data class FolderListSelection(
    var folders : ArrayList<Folder>,
    private val context : Context,
    private val mOnFolderListener : OnFolderListener
) : RecyclerView.Adapter<FolderListSelection.FolderListViewHolder>(), Serializable {

    class FolderListViewHolder(itemView : View, private var onFolderListener : OnFolderListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        Serializable {

        var folder : TextView
        var fullPath : TextView
        var isUsedInApp : CheckBox

        init{
            super.itemView
            folder = itemView.findViewById(R.id.folder_name)
            fullPath = itemView.findViewById(R.id.full_path)
            isUsedInApp = itemView.findViewById(R.id.checkbox_folder)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            this.onFolderListener.onFolderClick(bindingAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderListViewHolder {
        return FolderListViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.folder_file_selection,
                parent,
                false
            ), mOnFolderListener
        )
    }

    override fun onBindViewHolder(holder: FolderListViewHolder, position: Int) {
        holder.folder.text = folders[position].path.split("/").toTypedArray().last()
        holder.fullPath.text = folders[position].path
        holder.isUsedInApp.isChecked = folders[position].isUsedInApp
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    interface OnFolderListener {
        fun onFolderClick(position : Int)
    }
}