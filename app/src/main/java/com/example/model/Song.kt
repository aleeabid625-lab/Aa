package com.example.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val path: String = "",
    val folderName: String = "Internal",
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val isDemo: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val displayArtist: String
        get() = if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) "Unknown Artist" else artist

    val displayAlbum: String
        get() = if (album.isBlank() || album.equals("<unknown>", ignoreCase = true)) "Unknown Album" else album
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class EqualizerBand(
    val id: Int,
    val centerFreqHz: Int,
    val minLevel: Int = -1000,
    val maxLevel: Int = 1000,
    val currentLevel: Int = 0
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bassBoostStrength: Int = 350, // 0 to 1000 (35%)
    val currentPreset: String = "Rock",
    val presets: List<String> = listOf("Normal", "Rock", "Pop", "Jazz", "Electronic", "Bass Boosted", "Vocal", "Flat"),
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, currentLevel = 300),
        EqualizerBand(1, 230, currentLevel = 150),
        EqualizerBand(2, 910, currentLevel = -50),
        EqualizerBand(3, 3600, currentLevel = 200),
        EqualizerBand(4, 14000, currentLevel = 350)
    )
)
