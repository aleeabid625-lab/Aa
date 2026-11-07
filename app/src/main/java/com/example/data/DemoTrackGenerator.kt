package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.Song
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object DemoTrackGenerator {

    private data class TrackSpec(
        val title: String,
        val artist: String,
        val album: String,
        val baseFreq: Double,
        val chords: List<Double>,
        val bpm: Int,
        val durationSec: Int
    )

    private val DEMO_SPECS = listOf(
        TrackSpec(
            title = "Midnight Cyber Groove",
            artist = "Abid Neon Trio",
            album = "Purple Skyline",
            baseFreq = 220.0,
            chords = listOf(220.0, 277.18, 329.63, 440.0),
            bpm = 110,
            durationSec = 45
        ),
        TrackSpec(
            title = "Violet Waves & Bass",
            artist = "Abid & The Synth",
            album = "Neon Horizon",
            baseFreq = 146.83,
            chords = listOf(146.83, 174.61, 220.0, 293.66),
            bpm = 120,
            durationSec = 50
        ),
        TrackSpec(
            title = "Velvet Dreams Ambient",
            artist = "Cosmic Abid",
            album = "Starlight Odyssey",
            baseFreq = 196.0,
            chords = listOf(196.0, 246.94, 293.66, 392.0),
            bpm = 90,
            durationSec = 60
        ),
        TrackSpec(
            title = "Electric Nightfall",
            artist = "Abid Soundlab",
            album = "Frequency 2026",
            baseFreq = 261.63,
            chords = listOf(261.63, 329.63, 392.0, 523.25),
            bpm = 128,
            durationSec = 42
        ),
        TrackSpec(
            title = "Lo-Fi Midnight Chill",
            artist = "Abid Beats",
            album = "Purple Coffee",
            baseFreq = 174.61,
            chords = listOf(174.61, 220.0, 261.63, 349.23),
            bpm = 85,
            durationSec = 55
        )
    )

    fun getOrGenerateDemoSongs(context: Context): List<Song> {
        val demoDir = File(context.filesDir, "demo_music").apply { if (!exists()) mkdirs() }
        val songs = mutableListOf<Song>()

        DEMO_SPECS.forEachIndexed { index, spec ->
            val fileName = "demo_track_${index + 1}.wav"
            val file = File(demoDir, fileName)
            if (!file.exists() || file.length() < 1024) {
                generateWavFile(file, spec)
            }

            val uri = Uri.fromFile(file).toString()
            songs.add(
                Song(
                    id = -1000L - index,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    albumId = -1L,
                    durationMs = spec.durationSec * 1000L,
                    contentUri = uri,
                    albumArtUri = null,
                    path = file.absolutePath,
                    folderName = "Abid Music Specials",
                    sizeBytes = file.length(),
                    dateAdded = System.currentTimeMillis() / 1000,
                    mimeType = "audio/wav",
                    isDemo = true
                )
            )
        }
        return songs
    }

    private fun generateWavFile(file: File, spec: TrackSpec) {
        val sampleRate = 22050
        val numChannels = 2
        val bitsPerSample = 16
        val numSamples = sampleRate * spec.durationSec
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val dataSize = numSamples * numChannels * (bitsPerSample / 8)

        FileOutputStream(file).use { out ->
            // WAV Header
            out.write("RIFF".toByteArray())
            out.write(intToByteArrayLE(36 + dataSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToByteArrayLE(16)) // subchunk1 size (16 for PCM)
            out.write(shortToByteArrayLE(1.toShort())) // AudioFormat 1 = PCM
            out.write(shortToByteArrayLE(numChannels.toShort()))
            out.write(intToByteArrayLE(sampleRate))
            out.write(intToByteArrayLE(byteRate))
            out.write(shortToByteArrayLE((numChannels * bitsPerSample / 8).toShort())) // block align
            out.write(shortToByteArrayLE(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intToByteArrayLE(dataSize))

            // Audio synthesis buffer
            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            val beatSamples = (sampleRate * 60) / spec.bpm

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val chordIndex = ((i / (beatSamples * 4)) % spec.chords.size)
                val chordFreq = spec.chords[chordIndex]

                // Bass tone
                val bass = sin(2.0 * Math.PI * spec.baseFreq * time) * 0.35
                // Melody tone
                val melody = sin(2.0 * Math.PI * chordFreq * time) * 0.25
                // Harmonic overtone
                val overtone = sin(2.0 * Math.PI * (chordFreq * 1.5) * time) * 0.1
                // Rhythm pulse
                val beatProgress = (i % beatSamples).toDouble() / beatSamples
                val kick = if (beatProgress < 0.15) (1.0 - beatProgress / 0.15) * 0.3 * sin(2.0 * Math.PI * 65.0 * time) else 0.0

                val sampleVal = ((bass + melody + overtone + kick).coerceIn(-1.0, 1.0) * 32000).toInt().toShort()

                if (!buffer.hasRemaining()) {
                    out.write(buffer.array())
                    buffer.clear()
                }
                buffer.putShort(sampleVal) // Left
                buffer.putShort(sampleVal) // Right
            }
            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArrayLE(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
