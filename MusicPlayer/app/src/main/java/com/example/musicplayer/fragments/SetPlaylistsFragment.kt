package com.example.musicplayer.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicplayer.Playlist
import com.example.musicplayer.R
import com.example.musicplayer.classes.MyMediaPlayer
import kotlinx.android.synthetic.main.fragment_set_musics.*
import java.io.IOException
import java.io.ObjectInputStream

class SetPlaylistsFragment : Fragment() {
    private lateinit var infosText : TextView
    private lateinit var infosImg : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_set_playlists, container, false)

        infosText = view.findViewById(R.id.infos_text)
        infosImg = view.findViewById(R.id.infos_img)

        view.findViewById<Button>(R.id.select_playlists_button).setOnClickListener {
            selectPlaylistsFile()
        }
        return view
    }

    private fun selectPlaylistsFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultPlaylistsLauncher.launch(intent)
    }

    private var resultPlaylistsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            readAllPlaylistsFromUri(uri as Uri)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun setValidationOfInformations(isValid : Boolean) {
        if (isValid) {
            SetMusicsFragment.correctMusicFileSelected = true
            infos_text.apply {
                text = getString(R.string.correct_file)
                setTextColor(R.color.valid_color)
            }
            infosImg.apply {
                setImageResource(R.drawable.ic_baseline_check_24)
                setColorFilter(R.color.valid_color, PorterDuff.Mode.MULTIPLY)
                visibility = View.VISIBLE
            }
        } else {
            SetMusicsFragment.correctMusicFileSelected = false
            infos_text.apply {
                text = getString(R.string.wrong_file)
                setTextColor(R.color.error_color)
            }
            infosImg.apply {
                setImageResource(R.drawable.ic_baseline_close_24)
                setColorFilter(R.color.valid_color, PorterDuff.Mode.MULTIPLY)
                visibility = View.VISIBLE
            }
        }
    }

    private fun readAllPlaylistsFromUri(uri : Uri) : ArrayList<Playlist> {
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(requireContext().contentResolver.openInputStream(uri))
            content = ois.readObject() as ArrayList<Playlist>
            ois.close()
            setValidationOfInformations(true)
        } catch (error : IOException){
            Log.d("Error read playlists",error.toString())
            setValidationOfInformations(false)
        }
        return content
    }

    companion object {
        var correctPlaylistFileSelected = false
        var allPlaylists = ArrayList<Playlist>()
    }
}