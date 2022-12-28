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
import androidx.core.content.ContextCompat
import com.example.musicplayer.Playlist
import com.example.musicplayer.R
import com.example.musicplayer.SetDataActivity
import com.example.musicplayer.classes.MyMediaPlayer
import kotlinx.android.synthetic.main.fragment_set_musics.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            CoroutineScope(Dispatchers.IO).launch { selectPlaylistsFile() }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as SetDataActivity).currentFragmentPos = 1
    }

    private fun selectPlaylistsFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultPlaylistsLauncher.launch(intent)
    }

    private var resultPlaylistsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            CoroutineScope(Dispatchers.IO).launch {
                allPlaylists = readAllPlaylistsFromUri(uri as Uri)
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private suspend fun setValidationOfInformations(isValid : Boolean) {
        withContext(Dispatchers.Main) {
            if (isValid) {
                correctPlaylistFileSelected = true
                infos_text.apply {
                    text = getString(R.string.correct_file)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.valid_color))
                }
                infosImg.apply {
                    setImageResource(R.drawable.ic_baseline_check_24)
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.valid_color), PorterDuff.Mode.MULTIPLY)
                    visibility = View.VISIBLE
                }
            } else {
                correctPlaylistFileSelected= false
                infos_text.apply {
                    text = getString(R.string.wrong_file)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color))
                }
                infosImg.apply {
                    setImageResource(R.drawable.ic_baseline_close_24)
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_color), PorterDuff.Mode.MULTIPLY)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private suspend fun readAllPlaylistsFromUri(uri : Uri) : ArrayList<Playlist> {
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(requireContext().contentResolver.openInputStream(uri))
            content = ois.readObject() as ArrayList<Playlist>
            if (content.size > 0) {
                try {
                    content[0].listName
                    setValidationOfInformations(true)
                } catch (e: ClassCastException) {
                    setValidationOfInformations(false)
                }
            } else {
                setValidationOfInformations(false)
            }
            ois.close()
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