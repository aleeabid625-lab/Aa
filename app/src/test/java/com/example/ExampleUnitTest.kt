package com.example

import com.example.model.EqualizerState
import com.example.model.RepeatMode
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `song duration formatted properly`() {
        val song = Song(
            id = 1L,
            title = "Midnight Groove",
            artist = "Abid",
            album = "Neon Nights",
            durationMs = 215000L, // 3m 35s
            contentUri = "content://test/1"
        )
        assertEquals("3:35", song.formattedDuration)
        assertEquals("Abid", song.displayArtist)
        assertEquals("Neon Nights", song.displayAlbum)
        assertFalse(song.isDemo)
    }

    @Test
    fun `song default artist and album fallback`() {
        val song = Song(
            id = 2L,
            title = "Track 2",
            artist = "<unknown>",
            album = "",
            durationMs = 60000L,
            contentUri = "content://test/2"
        )
        assertEquals("Unknown Artist", song.displayArtist)
        assertEquals("Unknown Album", song.displayAlbum)
    }

    @Test
    fun `equalizer state defaults to enabled with bass boost`() {
        val eq = EqualizerState()
        assertTrue(eq.isEnabled)
        assertEquals("Rock", eq.currentPreset)
        assertEquals(5, eq.bands.size)
        assertTrue(eq.bassBoostStrength > 0)
    }

    @Test
    fun `repeat mode enum values exist`() {
        assertEquals(RepeatMode.OFF, RepeatMode.valueOf("OFF"))
        assertEquals(RepeatMode.ALL, RepeatMode.valueOf("ALL"))
        assertEquals(RepeatMode.ONE, RepeatMode.valueOf("ONE"))
    }
}
