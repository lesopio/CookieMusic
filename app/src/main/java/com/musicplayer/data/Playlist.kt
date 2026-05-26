package com.musicplayer.data

data class Playlist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
