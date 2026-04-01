package com.example.smartmusicplayer

import android.net.Uri

data class Song(
    val title: String,
    val artist: String,
    val uri: Uri
)