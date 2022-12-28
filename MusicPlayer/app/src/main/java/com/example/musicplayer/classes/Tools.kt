package com.example.musicplayer.classes

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Environment
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.musicplayer.*
import com.example.musicplayer.adapters.MusicList
import com.example.musicplayer.adapters.Playlists
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

open class Tools : AppCompatActivity() {
    val saveAllMusicsFile = "allMusics.musics"
    val savePlaylistsFile = "allPlaylists.playlists"
    val saveAllDeletedFiles = "allDeleted.musics"
    val saveAllFolders = "allFolders.folders"

    var mediaPlayer = MyMediaPlayer.getInstance

    /************************ USES THE MEDIAPLAYER : ***************************/

    fun verifiyAllMusics(adapter : MusicList) {
        var count = 0

        for (music in MyMediaPlayer.allMusics) {
            if (!File(music.path).exists()) {

                val position = adapter.musics.indexOf(music)
                adapter.musics.removeAt(position)
                adapter.notifyItemRemoved(position)
                MyMediaPlayer.allMusics.remove(music)

                // Enlevons la musique de nos playlists :
                for (playlist in MyMediaPlayer.allPlaylists) {
                    if (playlist.musicList.contains(music)) {
                        playlist.musicList.remove(music)
                    }
                }

                // Enlevons la musique des playlists utilisées par le mediaplayer si possible :
                if (MyMediaPlayer.currentIndex != -1) {
                    val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
                    if (MyMediaPlayer.initialPlaylist.contains(music)) {
                        MyMediaPlayer.initialPlaylist.remove(music)
                    }
                    if (MyMediaPlayer.currentPlaylist.contains(music)) {
                        // Si c'est la chanson qu'on joue actuellement, alors on passe si possible à la suivante :
                        Log.d("CONTAINS", "")
                        if (music.path == currentSong.path) {
                            Log.d("SAME", "")
                            // Si on peut passer à la musique suivante, on le fait :
                            if (MyMediaPlayer.currentPlaylist.size > 1) {
                                Log.d("PLAY NEXT", "")
                                playNextSong(adapter)
                                MyMediaPlayer.currentIndex =
                                    MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                            } else {
                                MyMediaPlayer.currentIndex = -1
                                mediaPlayer.pause()
                            }
                            MyMediaPlayer.currentPlaylist.remove(music)
                        } else {
                            Log.d("JUST DELETE", "")
                            MyMediaPlayer.currentPlaylist.remove(music)
                            // Vu qu'on change les positions des musiques, on récupère la position de la musique chargée dans le mediaplayer pour bien pouvoir jouer celle d'après / avant :
                            MyMediaPlayer.currentIndex =
                                MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                        }
                    }
                }
                count+=1
            }
        }

        if (count > 0) {
            Log.d("delete songs","")
            CoroutineScope(Dispatchers.IO).launch {
                writeAllDeletedSong()
                writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)
                writePlaylistsToFile()
            }

            Toast.makeText(
                applicationContext,
                resources.getString(R.string.x_musics_automatically_removed, count),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    open fun playMusic(){
        mediaPlayer.reset()
        try {
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            mediaPlayer.setDataSource(currentSong.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            val pausePlay = findViewById<ImageView>(R.id.pause_play)
            val songTitleInfo = findViewById<TextView>(R.id.song_title_info)

            val albumCoverInfo = findViewById<ImageView>(R.id.album_cover_info)
            if (currentSong.albumCover != null){
                // Passons d'abord notre byteArray en bitmap :
                val bytes = currentSong.albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                albumCoverInfo.setImageBitmap(bitmap)
            } else {
                albumCoverInfo.setImageResource(R.drawable.ic_saxophone_svg)
            }

            pausePlay?.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
            songTitleInfo?.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
        } catch (e: IndexOutOfBoundsException) {
            Log.d("ERROR","")
            e.printStackTrace()
        }
    }

    fun playRandom(list : ArrayList<Music>, context : Context) {
        if (list.size > 0) {
            val shuffledList = ArrayList(list.map { it.copy() })
            shuffledList.shuffle()
            MyMediaPlayer.currentPlaylist = ArrayList(shuffledList.map { it.copy() })
            MyMediaPlayer.initialPlaylist = ArrayList(shuffledList.map { it.copy() })
            val sameMusic = false

            MyMediaPlayer.currentIndex = 0

            val intent = Intent(context, MusicPlayerActivity::class.java)

            intent.putExtra("SAME MUSIC", sameMusic)

            startActivity(intent)
        }
    }

    fun onBottomMenuClick(position : Int, context : Context){
        Log.d("MUSIC POSITION", position.toString())
        var sameMusic = true

        if (position != MyMediaPlayer.currentIndex) {
            MyMediaPlayer.getInstance.reset()
            sameMusic = false
        }
        MyMediaPlayer.currentIndex = position
        Log.d("MEDIA POSITION", MyMediaPlayer.currentIndex.toString())
        val intent = Intent(context, MusicPlayerActivity::class.java)

        /*On fait passer notre liste de musiques dans notre nouvelle activité pour
        récupérer les données des musiques
         */

        intent.putExtra("SAME MUSIC", sameMusic)
        intent.putExtra("POSITION", position)

        startActivity(intent)
    }

    open fun playNextSong(adapter : MusicList){
        if(MyMediaPlayer.currentPlaylist.size != 0) {
            if (requestFocus()) {
                if (MyMediaPlayer.currentIndex == (MyMediaPlayer.currentPlaylist.size) - 1) {
                    MyMediaPlayer.currentIndex = 0
                } else {
                    MyMediaPlayer.currentIndex += 1
                }
                adapter.notifyDataSetChanged()
                playMusic()
            }
        }
    }

    open fun playNextSong(){
        if(MyMediaPlayer.currentPlaylist.size != 0) {
            if (requestFocus()) {
                if (MyMediaPlayer.currentIndex == (MyMediaPlayer.currentPlaylist.size) - 1) {
                    MyMediaPlayer.currentIndex = 0
                } else {
                    MyMediaPlayer.currentIndex += 1
                }
                playMusic()
            }
        }
    }

    open fun playPreviousSong(adapter : MusicList){
        if(MyMediaPlayer.currentPlaylist.size != 0) {
            if (requestFocus()) {
                if (MyMediaPlayer.currentIndex == 0) {
                    MyMediaPlayer.currentIndex = (MyMediaPlayer.currentPlaylist.size) - 1
                } else {
                    MyMediaPlayer.currentIndex -= 1
                }
                adapter.notifyDataSetChanged()
                playMusic()
            }
        }
    }

    open fun requestFocus() : Boolean{
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        return when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
                false
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                true
            }
            else -> {
                Toast.makeText(this,"An unknown error has come up", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    fun stopMusic(){
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        mediaPlayer.stop()
        MyMediaPlayer.currentIndex = -1
        MyMediaPlayer.currentPlaylist = ArrayList<Music>()
        MyMediaPlayer.initialPlaylist = ArrayList<Music>()
        PlaybackService.audioManager.abandonAudioFocusRequest(audioFocusRequest)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    open fun pausePlay(pausePlayButton : ImageView){
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(PlaybackService.audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(PlaybackService.onAudioFocusChange)
            .build()

        if(!(mediaPlayer.isPlaying)){
            when (PlaybackService.audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    Toast.makeText(this,"Cannot launch the music", Toast.LENGTH_SHORT).show()
                }

                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    mediaPlayer.start()
                    pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                    updateMusicNotification(false)
                }
                else -> {
                    Toast.makeText(this,"An unknown error has come up", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            mediaPlayer.pause()
            PlaybackService.audioManager.abandonAudioFocusRequest(audioFocusRequest)
            pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)

            updateMusicNotification(true)
        }
    }

    fun updateBottomPanel(songTitle : TextView, albumCover : ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                songTitle.text = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].name
            }

            if (MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover != null) {
                // Passons d'abord notre byteArray en bitmap :
                val bytes =
                    MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex].albumCover
                var bitmap: Bitmap? = null
                if (bytes != null && bytes.isNotEmpty()) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                withContext(Dispatchers.Main) {
                    albumCover.setImageBitmap(bitmap)
                }
            } else {
                albumCover.setImageResource(R.drawable.ic_saxophone_svg)
            }
        }
    }

    fun updateMusicNotification(isMusicPaused : Boolean) {
        val intentForNotification = Intent("BROADCAST_NOTIFICATION")
        intentForNotification.putExtra("STOP", isMusicPaused)
        applicationContext.sendBroadcast(intentForNotification)
    }

    /************************ WRITE OR READ INTO FILES : ***************************/

    fun retrieveAllMusicsFromApp(){
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, "allMusics.musics")))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.e("Error retrieving musics",error.toString())
            Toast.makeText(applicationContext, "Couldn't retrieve musics", Toast.LENGTH_LONG).show()

        }
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, "allPlaylists.playlists")))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.e("Error retrieving playlists",error.toString())
            Toast.makeText(applicationContext, "Couldn't retrieve playlists", Toast.LENGTH_LONG).show()
        }
        Toast.makeText(applicationContext, "Data retrieved in your Download Folder", Toast.LENGTH_LONG).show()
    }

    fun writeAllMusicsToFile(filename : String, content : ArrayList<Music>){
        MyMediaPlayer.allMusics = content
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, filename)))
            oos.writeObject(content)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write musics",error.toString())
        }
    }

    fun writeAllMusics() {
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllMusicsFile)))
            oos.writeObject(MyMediaPlayer.allMusics)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write musics",error.toString())
        }
    }

    open fun readAllMusicsFromFile(filename : String) : ArrayList<Music> {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
        } catch (error : IOException){
            Log.e("Error read musics",error.toString())
        }
        MyMediaPlayer.allMusics = content
        return content
    }

    fun writePlaylistsToFile(){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, savePlaylistsFile)))
            oos.writeObject(MyMediaPlayer.allPlaylists)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write playlists",error.toString())
        }
    }

    open fun readAllPlaylistsFromFile(filename : String) : ArrayList<Playlist> {
        val path = applicationContext.filesDir
        var content = ArrayList<Playlist>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, filename)))
            content = ois.readObject() as ArrayList<Playlist>
            ois.close()
        } catch (error : IOException){
            Log.e("Error read playlists",error.toString())
        }
        MyMediaPlayer.allPlaylists = content
        return content
    }

    fun bitmapToByteArray(bitmap: Bitmap) : ByteArray {
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
        return byteStream.toByteArray()
    }

    fun writeAllAsync(musics : ArrayList<Music>, playlists : ArrayList<Playlist>){
        writeAllMusicsToFile(saveAllMusicsFile, musics)
        writePlaylistsToFile()
    }

    fun readPlaylistsAsync(){
        MyMediaPlayer.allPlaylists = readAllPlaylistsFromFile(savePlaylistsFile)
        if (MyMediaPlayer.allPlaylists.size == 0){
            MyMediaPlayer.allPlaylists.add(Playlist("Favorites",ArrayList(),null,true))
            writePlaylistsToFile()
        }
    }

    open fun readAllDeletedMusicsFromFile() {
        val path = applicationContext.filesDir
        var content = ArrayList<Music>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, saveAllDeletedFiles)))
            content = ois.readObject() as ArrayList<Music>
            ois.close()
        } catch (error : IOException){
            Log.e("Error read deleted",error.toString())
        }
        MyMediaPlayer.allDeletedMusics = content
    }

    open fun writeAllDeletedSong(){
        val path = applicationContext.filesDir
        try {
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllDeletedFiles)))
            oos.writeObject(MyMediaPlayer.allDeletedMusics)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write deleted",error.toString())
        }
    }

    open fun readAllFoldersFromFile() {
        val path = applicationContext.filesDir
        var content = ArrayList<Folder>()
        try {
            val ois = ObjectInputStream(FileInputStream(File(path, saveAllFolders)))
            content = ois.readObject() as ArrayList<Folder>
            ois.close()
        } catch (error : IOException){
            Log.e("Error read folders",error.toString())
        }
        MyMediaPlayer.allFolders = content
        Log.d("FOLDERS", MyMediaPlayer.allFolders.toString())
    }

    open fun writeAllFolders(){
        val path = applicationContext.filesDir
        try {
            Log.d("FOLDERS", MyMediaPlayer.allFolders.toString())
            val oos = ObjectOutputStream(FileOutputStream(File(path, saveAllFolders)))
            oos.writeObject(MyMediaPlayer.allFolders)
            oos.close()
        } catch (error : IOException){
            Log.e("Error write folders",error.toString())
        }
    }

    fun retrieveAllFoldersUsed(){
        for (music in MyMediaPlayer.allMusics) {
            val musicFolder = File(music.path).parent
            if (MyMediaPlayer.allFolders.find { it.path == musicFolder } == null) {
                MyMediaPlayer.allFolders.add(Folder(musicFolder as String))
            }
        }
    }

    /****************************** OTHERS : **********************************/

    fun openNavigationMenu(drawerLayout : DrawerLayout){
        drawerLayout.openDrawer(GravityCompat.START)
    }

    /***************************** BOTTOM SHEET DIALOG : **********************/

    fun bottomSheetAddTo(position: Int, context : Context, adapter : MusicList){
        val selectedMusic = adapter.musics[position]
        val globalPosition = MyMediaPlayer.allMusics.indexOf(selectedMusic)
        val intent = Intent(context, AddToPlaylistActivity::class.java)
        intent.putExtra("POSITION", globalPosition)
        startActivity(intent)
    }

    fun bottomSheetRemoveFromApp(adapter : MusicList, position : Int, sheetBehavior : BottomSheetBehavior<LinearLayout>) {
        val musicToRemove = adapter.musics[position]
        adapter.musics.removeAt(position)
        adapter.notifyItemRemoved(position)
        MyMediaPlayer.allMusics.remove(musicToRemove)

        // Enlevons la musique de nos playlists :
        for(playlist in MyMediaPlayer.allPlaylists) {
            if (playlist.musicList.contains(musicToRemove)){
                playlist.musicList.remove(musicToRemove)
            }
        }

        // Enlevons la musique des playlists utilisées par le mediaplayer si possible :
        if (MyMediaPlayer.currentIndex != -1) {
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            if (MyMediaPlayer.initialPlaylist.contains(musicToRemove)) {
                MyMediaPlayer.initialPlaylist.remove(musicToRemove)
            }
            if (MyMediaPlayer.currentPlaylist.contains(musicToRemove)) {
                // Si c'est la chanson qu'on joue actuellement, alors on passe si possible à la suivante :
                Log.d("CONTAINS","")
                if (musicToRemove.path == currentSong.path) {
                    Log.d("SAME","")
                    // Si on peut passer à la musique suivante, on le fait :
                    if (MyMediaPlayer.currentPlaylist.size > 1) {
                        Log.d("PLAY NEXT","")
                        playNextSong(adapter)
                        MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                    } else {
                        // Sinon on enlève la musique en spécifiant qu'aucune musique ne peut être lancer (playlist avec 0 musiques)
                        CoroutineScope(Dispatchers.Main).launch {
                            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                        stopMusic()
                    }
                    MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                } else {
                    Log.d("JUST DELETE","")
                    MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                    // Vu qu'on change les positions des musiques, on récupère la position de la musique chargée dans le mediaplayer pour bien pouvoir jouer celle d'après / avant :
                    MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                }
            }
        }

        // Si la musique était en favoris, on lui enlève ce statut :
        musicToRemove.favorite = false

        CoroutineScope(Dispatchers.IO).launch {
            MyMediaPlayer.allDeletedMusics.add(0,musicToRemove)
            writeAllDeletedSong()
            writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)
            writePlaylistsToFile()
        }

        Toast.makeText(
            applicationContext,
            resources.getString(R.string.deleted_from_app),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun bottomSheetRemoveFromPlaylist(adapter : MusicList, position : Int, playlistPosition : Int, playlist : Playlist) {
        val musicToRemove = adapter.musics[position]
        adapter.musics.remove(musicToRemove)
        adapter.notifyItemRemoved(position)
        MyMediaPlayer.allPlaylists[playlistPosition].musicList.remove(musicToRemove)

        // Si on enlève une musique de la playlist des favoris, on enlève son statut de favoris :
        if (playlist.isFavoriteList){
            val globalPosition = MyMediaPlayer.allMusics.indexOf(musicToRemove)
            val positionInInitialList = MyMediaPlayer.initialPlaylist.indexOf(musicToRemove)
            val positionInCurrentList = MyMediaPlayer.currentPlaylist.indexOf(musicToRemove)

            if(globalPosition != -1)  {
                MyMediaPlayer.allMusics[globalPosition].favorite = false
            }

            if(positionInInitialList != -1)  {
                MyMediaPlayer.initialPlaylist[positionInInitialList].favorite = false
            }

            if(positionInCurrentList != -1)  {
                MyMediaPlayer.currentPlaylist[positionInCurrentList].favorite = false
            }

            CoroutineScope(Dispatchers.IO).launch {
                launch{writeAllMusicsToFile(saveAllMusicsFile, MyMediaPlayer.allMusics)}
            }

            for (playlist in MyMediaPlayer.allPlaylists){
                if (playlist.musicList.contains(musicToRemove)){
                    val position = playlist.musicList.indexOf(musicToRemove)
                    playlist.musicList[position].favorite = false
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            launch{writePlaylistsToFile()}
        }

        Toast.makeText(applicationContext,resources.getString(R.string.deleted_from_playlist),Toast.LENGTH_SHORT).show()

    }

    fun bottomSheetModifyMusic(context : Context, position: Int, adapter: MusicList) {
        val intent = Intent(context, ModifyMusicInfoActivity::class.java)
        intent.putExtra("PATH", adapter.musics[position].path)
        startActivity(intent)
    }

    fun bottomSheetPlayNext(adapter: MusicList, position: Int) {
        if (MyMediaPlayer.currentPlaylist.size > 0) {
            // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
            val currentMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            val songToPlayNext = adapter.musics[position]

            // On empêche de pouvoir ajouter la même musique pour éviter des problèmes de position négatif :
            if (currentMusic != songToPlayNext) {
                MyMediaPlayer.initialPlaylist.remove(songToPlayNext)
                MyMediaPlayer.currentPlaylist.remove(songToPlayNext)

                // Assurons nous de récupérer la bonne position de la musique qui se joue actuellement :
                MyMediaPlayer.currentIndex =
                    MyMediaPlayer.currentPlaylist.indexOf(currentMusic)

                MyMediaPlayer.initialPlaylist.add(
                    MyMediaPlayer.currentIndex + 1,
                    songToPlayNext
                )
                MyMediaPlayer.currentPlaylist.add(
                    MyMediaPlayer.currentIndex + 1,
                    songToPlayNext
                )
                Toast.makeText(
                    applicationContext,
                    resources.getString(R.string.music_will_be_played_next),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun bottomSheetPlayNextInCurrentPlaylist(adapter: MusicList, position : Int) {
        // Lorsque l'on veut jouer une musique après celle qui ce joue actuellement, on supprime d'abord la musique de la playlist :
        val currentMusic = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        val songToPlayNext = adapter.musics[position]

        // On empêche de pouvoir ajouter la même musique pour éviter des problèmes de position négatif :
        if (currentMusic != songToPlayNext) {
            MyMediaPlayer.currentPlaylist.remove(songToPlayNext)

            // Assurons nous de récupérer la bonne position de la musique qui se joue actuellement :
            MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentMusic)

            MyMediaPlayer.currentPlaylist.add(
                MyMediaPlayer.currentIndex + 1,
                songToPlayNext
            )

            adapter.notifyItemRemoved(position)
            adapter.notifyItemInserted(MyMediaPlayer.currentIndex + 1)
            Toast.makeText(this,resources.getString(R.string.music_will_be_played_next), Toast.LENGTH_SHORT).show()
        }
    }

    fun bottomSheetRemovePlaylist(position: Int, adapter : Playlists) {
        if (MyMediaPlayer.allPlaylists[position].isFavoriteList){
            Toast.makeText(applicationContext,resources.getString(R.string.cannot_delete_favorite_playlist),Toast.LENGTH_SHORT).show()
        } else {
            MyMediaPlayer.allPlaylists.removeAt(position)
            adapter.allPlaylists = MyMediaPlayer.allPlaylists
            adapter.notifyItemRemoved(position)

            writePlaylistsToFile()
            Toast.makeText(applicationContext,resources.getString(R.string.playlist_deleted),Toast.LENGTH_SHORT).show()
        }
    }

    fun bottomSheetModifyPlaylist(context : Context, position: Int) {
        val intent = Intent(context, ModifyPlaylistInfoActivity::class.java)
        intent.putExtra("POSITION",position)
        startActivity(intent)
    }

    fun definitelyRemoveMusicFromApp(musicToRemove : Music) {
        MyMediaPlayer.allMusics.remove(musicToRemove)

        if (MyMediaPlayer.allDeletedMusics.contains(musicToRemove)){
            MyMediaPlayer.allDeletedMusics.remove(musicToRemove)
        }

        // Enlevons la musique de nos playlists :
        for(playlist in MyMediaPlayer.allPlaylists) {
            if (playlist.musicList.contains(musicToRemove)){
                playlist.musicList.remove(musicToRemove)
            }
        }

        // Enlevons la musique des playlists utilisées par le mediaplayer si possible :
        if (MyMediaPlayer.currentIndex != -1) {
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
            if (MyMediaPlayer.initialPlaylist.contains(musicToRemove)) {
                MyMediaPlayer.initialPlaylist.remove(musicToRemove)
            }
            if (MyMediaPlayer.currentPlaylist.contains(musicToRemove)) {
                // Si c'est la chanson qu'on joue actuellement, alors on passe si possible à la suivante :
                Log.d("CONTAINS","")
                if (musicToRemove.path == currentSong.path) {
                    Log.d("SAME","")
                    // Si on peut passer à la musique suivante, on le fait :
                    if (MyMediaPlayer.currentPlaylist.size > 1) {
                        Log.d("PLAY NEXT","")
                        playNextSong()
                        MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                    } else {
                        // Sinon on enlève la musique en spécifiant qu'aucune musique ne peut être lancer (playlist avec 0 musiques)
                        stopMusic()
                    }
                    MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                } else {
                    Log.d("JUST DELETE","")
                    MyMediaPlayer.currentPlaylist.remove(musicToRemove)
                    // Vu qu'on change les positions des musiques, on récupère la position de la musique chargée dans le mediaplayer pour bien pouvoir jouer celle d'après / avant :
                    MyMediaPlayer.currentIndex = MyMediaPlayer.currentPlaylist.indexOf(currentSong)
                }
            }
        }
    }
}