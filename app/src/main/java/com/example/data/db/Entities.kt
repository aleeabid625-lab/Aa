package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val iconColorHex: String = "#A855F7"
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val songTitle: String,
    val songArtist: String,
    val songAlbum: String,
    val songUri: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: Long,
    val songUri: String,
    val songTitle: String,
    val songArtist: String,
    val songAlbum: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentPlayEntity(
    @PrimaryKey
    val songId: Long,
    val songUri: String,
    val songTitle: String,
    val songArtist: String,
    val songAlbum: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val playedAt: Long = System.currentTimeMillis()
)
