package com.example.smartmusicplayer

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listViewSongs: ListView
    private lateinit var txtSelectedSong: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val songs = mutableListOf<Song>()
    private var selectedSong: Song? = null

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                loadSongs()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listViewSongs = findViewById(R.id.listViewSongs)
        txtSelectedSong = findViewById(R.id.txtSelectedSong)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        checkPermissions()

        // Select song
        listViewSongs.setOnItemClickListener { _, _, position, _ ->
            selectedSong = songs[position]
            txtSelectedSong.text = "Selected: ${selectedSong?.title}"
        }

        // Start music
        btnStart.setOnClickListener {
            selectedSong?.let {
                val intent = Intent(this, MusicService::class.java).apply {
                    action = "START"
                    putExtra("URI", it.uri.toString())
                    putExtra("TITLE", it.title)
                }
                ContextCompat.startForegroundService(this, intent)
            }
        }

        // Stop music
        btnStop.setOnClickListener {
            val intent = Intent(this, MusicService::class.java).apply {
                action = "STOP"
            }
            startService(intent)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Media access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            // Notification permission (Android 13+)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            loadSongs()
        } else {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // 🔥 CONTENT PROVIDER PART
    private fun loadSongs() {
        songs.clear()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val cursor = contentResolver.query(
            uri,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)

                val songUri = ContentUris.withAppendedId(uri, id)

                songs.add(Song(title, artist, songUri))
            }
        }

        val displayList = songs.map { "${it.title} - ${it.artist}" }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayList
        )

        listViewSongs.adapter = adapter
    }
}