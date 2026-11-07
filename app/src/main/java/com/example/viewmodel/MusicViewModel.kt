package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioScanner
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongEntity
import com.example.data.db.RecentPlayEntity
import com.example.model.EqualizerState
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.service.MusicPlaybackService
import com.example.service.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeSection(val title: String) {
    ALL_SONGS("All Songs"),
    FOLDERS("Folders"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favorites")
}

sealed class SubDetailScreen {
    object None : SubDetailScreen()
    data class FolderDetail(val folderName: String, val songs: List<Song>) : SubDetailScreen()
    data class AlbumDetail(val albumName: String, val artist: String, val songs: List<Song>) : SubDetailScreen()
    data class ArtistDetail(val artistName: String, val songs: List<Song>) : SubDetailScreen()
    data class PlaylistDetail(val playlistId: Long, val playlistName: String) : SubDetailScreen()
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = MusicPlayerManager.getInstance(application)
    private val db = AppDatabase.getDatabase(application)
    private val musicDao = db.musicDao()

    // Player State delegations
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val queue: StateFlow<List<Song>> = playerManager.queue
    val equalizerState: StateFlow<EqualizerState> = playerManager.equalizerState
    val sleepTimerSeconds: StateFlow<Int> = playerManager.sleepTimerSeconds

    // UI state
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _selectedSection = MutableStateFlow(HomeSection.ALL_SONGS)
    val selectedSection: StateFlow<HomeSection> = _selectedSection.asStateFlow()

    private val _subDetailScreen = MutableStateFlow<SubDetailScreen>(SubDetailScreen.None)
    val subDetailScreen: StateFlow<SubDetailScreen> = _subDetailScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())

    // Filtered songs according to search query
    val songs: StateFlow<List<Song>> = combine(_rawSongs, _searchQuery) { all, query ->
        if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Database flows
    val playlists: StateFlow<List<PlaylistEntity>> = musicDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = musicDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentPlays: StateFlow<List<RecentPlayEntity>> = musicDao.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Favorite IDs set for fast O(1) checks
    val favoriteIds: StateFlow<Set<Long>> = favorites.combine(_rawSongs) { favs, _ ->
        favs.map { it.songId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    init {
        scanSongs()
        observeStorageChanges()
    }

    fun scanSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val scanned = AudioScanner.scanDeviceSongs(getApplication())
                _rawSongs.value = scanned
            } catch (e: Exception) {
                // handle safely
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun observeStorageChanges() {
        viewModelScope.launch {
            AudioScanner.mediaStoreUpdatesFlow(getApplication()).collect {
                // Refresh list on media change
                val scanned = AudioScanner.scanDeviceSongs(getApplication())
                _rawSongs.value = scanned
            }
        }
    }

    // Playback control
    fun playSong(song: Song, contextList: List<Song>? = null) {
        val list = contextList ?: _rawSongs.value
        playerManager.playSong(song, list)
        MusicPlaybackService.startService(getApplication())
    }

    fun playQueueIndex(index: Int) {
        val q = playerManager.queue.value
        if (q.isNotEmpty()) {
            playerManager.setQueue(q, startIndex = index, autoPlay = true)
            MusicPlaybackService.startService(getApplication())
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
        MusicPlaybackService.startService(getApplication())
    }

    fun playNext() {
        playerManager.playNext()
        MusicPlaybackService.startService(getApplication())
    }

    fun playPrevious() {
        playerManager.playPrevious()
        MusicPlaybackService.startService(getApplication())
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    // Equalizer
    fun setEqualizerEnabled(enabled: Boolean) = playerManager.setEqualizerEnabled(enabled)
    fun setBassBoost(strength: Int) = playerManager.setBassBoost(strength)
    fun setBandLevel(bandId: Int, level: Int) = playerManager.setBandLevel(bandId, level)
    fun applyPreset(presetName: String) = playerManager.applyPreset(presetName)

    // Sleep timer
    fun setSleepTimer(minutes: Int) = playerManager.setSleepTimer(minutes)

    // Navigation & Filtering
    fun selectSection(section: HomeSection) {
        _selectedSection.value = section
        _subDetailScreen.value = SubDetailScreen.None
    }

    fun openSubDetail(detail: SubDetailScreen) {
        _subDetailScreen.value = detail
    }

    fun closeSubDetail() {
        _subDetailScreen.value = SubDetailScreen.None
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Favorites
    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteIds.value.contains(song.id)
            if (isFav) {
                musicDao.removeFavorite(song.id)
            } else {
                musicDao.addFavorite(
                    FavoriteEntity(
                        songId = song.id,
                        songUri = song.contentUri,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songAlbum = song.album,
                        durationMs = song.durationMs,
                        albumArtUri = song.albumArtUri
                    )
                )
            }
        }
    }

    // Playlists
    fun createPlaylist(name: String, colorHex: String = "#A855F7") {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            musicDao.insertPlaylist(PlaylistEntity(name = name.trim(), iconColorHex = colorHex))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            musicDao.deletePlaylist(playlistId)
            musicDao.deletePlaylistSongs(playlistId)
            if (_subDetailScreen.value is SubDetailScreen.PlaylistDetail) {
                _subDetailScreen.value = SubDetailScreen.None
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            musicDao.addSongToPlaylist(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = song.id,
                    songTitle = song.title,
                    songArtist = song.artist,
                    songAlbum = song.album,
                    songUri = song.contentUri,
                    durationMs = song.durationMs,
                    albumArtUri = song.albumArtUri
                )
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            musicDao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getPlaylistSongsFlow(playlistId: Long) = musicDao.getSongsForPlaylist(playlistId)
}
