package com.example.musicplayer.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.text.Editable
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
import androidx.core.widget.ImageViewCompat
import com.example.musicplayer.Music
import com.example.musicplayer.R
import com.example.musicplayer.classes.MyMediaPlayer
import kotlinx.android.synthetic.main.fragment_set_musics.*
import java.io.IOException
import java.io.ObjectInputStream

class SetMusicsFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_set_musics, container, false)

        infosText = view.findViewById(R.id.infos_text)
        infosImg = view.findViewById(R.id.infos_img)

        view.findViewById<Button>(R.id.select_musics_button).setOnClickListener {
            selectMusicsFile()
        }

        return view
    }

    private fun selectMusicsFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        resultMusicsLauncher.launch(intent)
    }

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    private var resultMusicsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri : Uri? = result.data?.data
            allMusics = readAllMusicsFromUri(uri as Uri)
        }
    }

    private fun readAllMusicsFromUri(uri : Uri) : ArrayList<Music> {
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(requireContext().contentResolver.openInputStream(uri))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
            setValidationOfInformations(true)
        } catch (error : IOException){
            Log.d("Error read musics",error.toString())
            setValidationOfInformations(false)
        }
        return content
    }

    private fun setValidationOfInformations(isValid : Boolean) {
        if (isValid) {
            correctMusicFileSelected = true
            infosText.apply {
                text = getString(R.string.correct_file)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.valid_color))
            }
            infosImg.apply {
                setImageResource(R.drawable.ic_baseline_check_24)
                visibility = View.VISIBLE
            }
            ImageViewCompat.setImageTintList(infosImg,ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.valid_color)))
        } else {
            correctMusicFileSelected = false
            infosText.apply{
                text = getString(R.string.wrong_file)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color))
            }

            infosImg.apply {
                setImageResource(R.drawable.ic_baseline_close_24)
                infosImg.visibility = View.VISIBLE
            }
            ImageViewCompat.setImageTintList(infosImg,ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_color)))
        }
    }

    companion object {
        var correctMusicFileSelected = false
        var allMusics = ArrayList<Music>()
    }
}