package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.model.EqualizerBand
import com.example.model.EqualizerState
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class MusicPlayerManager(private val context: Context) {
    companion object {
        private const val TAG = "MusicPlayerManager"
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Audio effects
    private var hardwareEqualizer: Equalizer? = null
    private var hardwareBassBoost: BassBoost? = null

    // Playback StateFlows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Equalizer State
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // Sleep Timer (remaining seconds, 0 = off)
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    private var progressJob: Job? = null
    private val random = Random()

    // Callbacks for service integration
    var onStateChanged: (() -> Unit)? = null
    var onSongStarted: ((Song) -> Unit)? = null

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (_isPlaying.value) {
                            val pos = player.currentPosition.toLong().coerceAtLeast(0)
                            _currentPositionMs.value = pos
                            val dur = player.duration.toLong().coerceAtLeast(0)
                            if (dur > 0) _durationMs.value = dur
                        }
                    }
                } catch (e: Exception) {
                    // Ignore state race during track transition
                }
                delay(300)
            }
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        _currentIndex.value = validIndex
        val song = songs[validIndex]
        loadAndPlay(song, playWhenReady = autoPlay)
    }

    fun playSong(song: Song, queueContext: List<Song>? = null) {
        val q = queueContext ?: listOf(song)
        val index = q.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        _queue.value = q
        _currentIndex.value = index
        loadAndPlay(song, playWhenReady = true)
    }

    private fun loadAndPlay(song: Song, playWhenReady: Boolean) {
        _currentSong.value = song
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs

        releasePlayer()

        try {
            val player = MediaPlayer().apply {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(song.contentUri))
                prepare()
            }
            mediaPlayer = player

            _durationMs.value = player.duration.toLong().coerceAtLeast(song.durationMs)

            // Setup equalizer on the new audio session
            setupAudioEffects(player.audioSessionId)

            player.setOnCompletionListener {
                handleTrackCompletion()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                _isPlaying.value = false
                onStateChanged?.invoke()
                true
            }

            if (playWhenReady) {
                resume()
                onSongStarted?.invoke(song)
            } else {
                _isPlaying.value = false
                onStateChanged?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load song: ${song.title}", e)
            _isPlaying.value = false
            onStateChanged?.invoke()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun resume() {
        if (requestAudioFocus()) {
            try {
                mediaPlayer?.start()
                _isPlaying.value = true
                onStateChanged?.invoke()
                _currentSong.value?.let { onSongStarted?.invoke(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Resume failed", e)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            onStateChanged?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Pause failed", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            val clamped = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
            mediaPlayer?.seekTo(clamped.toInt())
            _currentPositionMs.value = clamped
        } catch (e: Exception) {
            Log.e(TAG, "Seek failed", e)
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = when {
            _isShuffle.value -> {
                if (q.size > 1) {
                    var idx: Int
                    do {
                        idx = random.nextInt(q.size)
                    } while (idx == _currentIndex.value)
                    idx
                } else 0
            }
            else -> (_currentIndex.value + 1) % q.size
        }
        _currentIndex.value = nextIndex
        loadAndPlay(q[nextIndex], playWhenReady = true)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, restart current song
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = when {
            _isShuffle.value -> random.nextInt(q.size)
            else -> if (_currentIndex.value - 1 < 0) q.size - 1 else _currentIndex.value - 1
        }
        _currentIndex.value = prevIndex
        loadAndPlay(q[prevIndex], playWhenReady = true)
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.size - 1) {
                    playNext()
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    // --- Equalizer & Bass Boost ---

    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            releaseAudioEffects()
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            hardwareEqualizer = eq

            val bass = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.bassBoostStrength.toShort())
                }
            }
            hardwareBassBoost = bass

            // Apply current band settings to hardware if supported
            applyStoredBandsToHardware()
        } catch (e: Exception) {
            Log.w(TAG, "Audio effects not supported on this device/session", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        try {
            hardwareEqualizer?.enabled = enabled
            hardwareBassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle hardware equalizer", e)
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = clamped)
        try {
            if (hardwareBassBoost?.strengthSupported == true) {
                hardwareBassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set bass boost", e)
        }
    }

    fun setBandLevel(bandId: Int, level: Int) {
        val updatedBands = _equalizerState.value.bands.map {
            if (it.id == bandId) it.copy(currentLevel = level.coerceIn(it.minLevel, it.maxLevel)) else it
        }
        _equalizerState.value = _equalizerState.value.copy(bands = updatedBands, currentPreset = "Custom")
        try {
            hardwareEqualizer?.setBandLevel(bandId.toShort(), level.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set band level", e)
        }
    }

    fun applyPreset(presetName: String) {
        val bands = when (presetName) {
            "Bass Boosted" -> listOf(
                EqualizerBand(0, 60, currentLevel = 700),
                EqualizerBand(1, 230, currentLevel = 450),
                EqualizerBand(2, 910, currentLevel = 0),
                EqualizerBand(3, 3600, currentLevel = 150),
                EqualizerBand(4, 14000, currentLevel = 300)
            )
            "Rock" -> listOf(
                EqualizerBand(0, 60, currentLevel = 450),
                EqualizerBand(1, 230, currentLevel = 250),
                EqualizerBand(2, 910, currentLevel = -100),
                EqualizerBand(3, 3600, currentLevel = 350),
                EqualizerBand(4, 14000, currentLevel = 500)
            )
            "Pop" -> listOf(
                EqualizerBand(0, 60, currentLevel = -100),
                EqualizerBand(1, 230, currentLevel = 200),
                EqualizerBand(2, 910, currentLevel = 500),
                EqualizerBand(3, 3600, currentLevel = 250),
                EqualizerBand(4, 14000, currentLevel = -50)
            )
            "Jazz" -> listOf(
                EqualizerBand(0, 60, currentLevel = 300),
                EqualizerBand(1, 230, currentLevel = 100),
                EqualizerBand(2, 910, currentLevel = -150),
                EqualizerBand(3, 3600, currentLevel = 200),
                EqualizerBand(4, 14000, currentLevel = 350)
            )
            "Electronic" -> listOf(
                EqualizerBand(0, 60, currentLevel = 550),
                EqualizerBand(1, 230, currentLevel = 350),
                EqualizerBand(2, 910, currentLevel = 0),
                EqualizerBand(3, 3600, currentLevel = 200),
                EqualizerBand(4, 14000, currentLevel = 450)
            )
            "Vocal" -> listOf(
                EqualizerBand(0, 60, currentLevel = -200),
                EqualizerBand(1, 230, currentLevel = 100),
                EqualizerBand(2, 910, currentLevel = 600),
                EqualizerBand(3, 3600, currentLevel = 400),
                EqualizerBand(4, 14000, currentLevel = 100)
            )
            else -> listOf(
                EqualizerBand(0, 60, currentLevel = 0),
                EqualizerBand(1, 230, currentLevel = 0),
                EqualizerBand(2, 910, currentLevel = 0),
                EqualizerBand(3, 3600, currentLevel = 0),
                EqualizerBand(4, 14000, currentLevel = 0)
            )
        }
        val bassBoostVal = if (presetName == "Bass Boosted") 750 else if (presetName == "Rock" || presetName == "Electronic") 450 else 250
        _equalizerState.value = _equalizerState.value.copy(
            currentPreset = presetName,
            bands = bands,
            bassBoostStrength = bassBoostVal
        )
        setBassBoost(bassBoostVal)
        applyStoredBandsToHardware()
    }

    private fun applyStoredBandsToHardware() {
        hardwareEqualizer?.let { eq ->
            try {
                _equalizerState.value.bands.forEach { band ->
                    eq.setBandLevel(band.id.toShort(), band.currentLevel.toShort())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed applying bands to hardware equalizer", e)
            }
        }
    }

    // --- Sleep Timer ---

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = 0
            return
        }

        _sleepTimerSeconds.value = minutes * 60
        sleepTimerJob = scope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value -= 1
            }
            pause()
        }
    }

    // --- Audio Focus ---

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> resume()
                    }
                }
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> resume()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releaseAudioEffects() {
        try {
            hardwareEqualizer?.release()
            hardwareEqualizer = null
            hardwareBassBoost?.release()
            hardwareBassBoost = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio effects", e)
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media player", e)
        }
    }
}
