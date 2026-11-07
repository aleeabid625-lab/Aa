package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.PlaylistEntity
import com.example.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongDetailDialog
import com.example.ui.components.SongItem
import com.example.ui.theme.DarkPurpleBorder
import com.example.ui.theme.DarkPurpleCard
import com.example.ui.theme.DarkPurpleSurface
import com.example.ui.theme.ElectricPink
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SoftPurple
import com.example.viewmodel.HomeSection
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.SubDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onRequestStoragePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlays by viewModel.recentPlays.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val subDetailScreen by viewModel.subDetailScreen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // Dialog & overlay states
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToShowDetail by remember { mutableStateOf<Song?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Intercept back button when sub-screen or player expanded
    if (isNowPlayingExpanded) {
        BackHandler { isNowPlayingExpanded = false }
    } else if (subDetailScreen !is SubDetailScreen.None) {
        BackHandler { viewModel.closeSubDetail() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            HeaderBar(
                isDarkTheme = isDarkTheme,
                isScanning = isScanning,
                onToggleTheme = { viewModel.toggleTheme() },
                onRefreshScan = {
                    onRequestStoragePermission()
                    viewModel.scanSongs()
                },
                onOpenEqualizer = { showEqualizerDialog = true }
            )

            // Search Input
            SearchInputField(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // Main Content: Sub Detail Screen or Main Tabs
            Box(modifier = Modifier.weight(1f)) {
                when (val detail = subDetailScreen) {
                    is SubDetailScreen.None -> {
                        MainTabsContent(
                            selectedSection = selectedSection,
                            songs = songs,
                            playlists = playlists,
                            recentPlays = recentPlays,
                            favoriteIds = favoriteIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onSelectSection = { viewModel.selectSection(it) },
                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onShowDetails = { songToShowDetail = it },
                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                            onOpenFolder = { name, list ->
                                viewModel.openSubDetail(SubDetailScreen.FolderDetail(name, list))
                            },
                            onOpenAlbum = { album, artist, list ->
                                viewModel.openSubDetail(SubDetailScreen.AlbumDetail(album, artist, list))
                            },
                            onOpenArtist = { artist, list ->
                                viewModel.openSubDetail(SubDetailScreen.ArtistDetail(artist, list))
                            },
                            onOpenPlaylist = { id, name ->
                                viewModel.openSubDetail(SubDetailScreen.PlaylistDetail(id, name))
                            },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onRequestPermission = onRequestStoragePermission
                        )
                    }

                    is SubDetailScreen.FolderDetail -> {
                        DetailSongListView(
                            title = detail.folderName,
                            subtitle = "Folder • ${detail.songs.size} tracks",
                            songs = detail.songs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onBack = { viewModel.closeSubDetail() },
                            onPlayAll = {
                                if (detail.songs.isNotEmpty()) viewModel.playSong(detail.songs.first(), detail.songs)
                            },
                            onShuffleAll = {
                                if (detail.songs.isNotEmpty()) {
                                    val shuffled = detail.songs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song -> viewModel.playSong(song, detail.songs) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onShowDetails = { songToShowDetail = it }
                        )
                    }

                    is SubDetailScreen.AlbumDetail -> {
                        DetailSongListView(
                            title = detail.albumName,
                            subtitle = "Album by ${detail.artist} • ${detail.songs.size} tracks",
                            songs = detail.songs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onBack = { viewModel.closeSubDetail() },
                            onPlayAll = {
                                if (detail.songs.isNotEmpty()) viewModel.playSong(detail.songs.first(), detail.songs)
                            },
                            onShuffleAll = {
                                if (detail.songs.isNotEmpty()) {
                                    val shuffled = detail.songs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song -> viewModel.playSong(song, detail.songs) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onShowDetails = { songToShowDetail = it }
                        )
                    }

                    is SubDetailScreen.ArtistDetail -> {
                        DetailSongListView(
                            title = detail.artistName,
                            subtitle = "Artist • ${detail.songs.size} tracks",
                            songs = detail.songs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onBack = { viewModel.closeSubDetail() },
                            onPlayAll = {
                                if (detail.songs.isNotEmpty()) viewModel.playSong(detail.songs.first(), detail.songs)
                            },
                            onShuffleAll = {
                                if (detail.songs.isNotEmpty()) {
                                    val shuffled = detail.songs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song -> viewModel.playSong(song, detail.songs) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onShowDetails = { songToShowDetail = it }
                        )
                    }

                    is SubDetailScreen.PlaylistDetail -> {
                        val playlistSongs by viewModel.getPlaylistSongsFlow(detail.playlistId).collectAsState(initial = emptyList())
                        val mappedSongs = playlistSongs.map { ps ->
                            Song(
                                id = ps.songId,
                                title = ps.songTitle,
                                artist = ps.songArtist,
                                album = ps.songAlbum,
                                durationMs = ps.durationMs,
                                contentUri = ps.songUri,
                                albumArtUri = ps.albumArtUri
                            )
                        }

                        DetailSongListView(
                            title = detail.playlistName,
                            subtitle = "Custom Playlist • ${mappedSongs.size} tracks",
                            songs = mappedSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onBack = { viewModel.closeSubDetail() },
                            onPlayAll = {
                                if (mappedSongs.isNotEmpty()) viewModel.playSong(mappedSongs.first(), mappedSongs)
                            },
                            onShuffleAll = {
                                if (mappedSongs.isNotEmpty()) {
                                    val shuffled = mappedSongs.shuffled()
                                    viewModel.playSong(shuffled.first(), shuffled)
                                }
                            },
                            onSongClick = { song -> viewModel.playSong(song, mappedSongs) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onShowDetails = { songToShowDetail = it }
                        )
                    }
                }
            }

            // Docked MiniPlayer
            if (currentSong != null) {
                val isFav = favoriteIds.contains(currentSong!!.id)
                MiniPlayer(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    progressMs = progressMs,
                    durationMs = durationMs,
                    isFavorite = isFav,
                    onExpandPlayer = { isNowPlayingExpanded = true },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onToggleFavorite = { viewModel.toggleFavorite(currentSong!!) }
                )
            }
        }

        // Full Screen Animated Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentSong?.let { song ->
                NowPlayingScreen(
                    song = song,
                    isPlaying = isPlaying,
                    progressMs = progressMs,
                    durationMs = durationMs,
                    isFavorite = favoriteIds.contains(song.id),
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    queue = queue,
                    onCollapse = { isNowPlayingExpanded = false },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onPlayPrevious = { viewModel.playPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onOpenEqualizer = { showEqualizerDialog = true },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onSelectQueueIndex = { viewModel.playQueueIndex(it) }
                )
            }
        }

        // Dialogs
        if (showEqualizerDialog) {
            EqualizerDialog(
                equalizerState = equalizerState,
                onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                onBassBoostChange = { viewModel.setBassBoost(it) },
                onBandLevelChange = { id, lvl -> viewModel.setBandLevel(id, lvl) },
                onSelectPreset = { viewModel.applyPreset(it) },
                onDismiss = { showEqualizerDialog = false }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name, color -> viewModel.createPlaylist(name, color) }
            )
        }

        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onPlaylistSelected = { pid -> viewModel.addSongToPlaylist(pid, song) },
                onCreateNewPlaylist = { showCreatePlaylistDialog = true },
                onDismiss = { songToAddToPlaylist = null }
            )
        }

        songToShowDetail?.let { song ->
            SongDetailDialog(song = song, onDismiss = { songToShowDetail = null })
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                activeRemainingSec = sleepTimerSec,
                onSetTimerMinutes = { viewModel.setSleepTimer(it) },
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

@Composable
private fun HeaderBar(
    isDarkTheme: Boolean,
    isScanning: Boolean,
    onToggleTheme: () -> Unit,
    onRefreshScan: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonPurple, NeonFuchsia))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Abid Music Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Abid Music",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "High Fidelity Player",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 6.dp),
                    color = NeonPurple,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.testTag("refresh_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Storage",
                        tint = SoftPurple
                    )
                }
            }

            IconButton(
                onClick = onOpenEqualizer,
                modifier = Modifier.testTag("header_eq_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = SoftPurple
                )
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = SoftPurple
                )
            }
        }
    }
}

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search songs, artists, albums, or folders...", color = SoftPurple.copy(alpha = 0.6f), fontSize = 13.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = SoftPurple)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SoftPurple)
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurple,
            unfocusedBorderColor = DarkPurpleBorder,
            focusedContainerColor = DarkPurpleCard,
            unfocusedContainerColor = DarkPurpleCard,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("search_input_field")
    )
}

@Composable
private fun MainTabsContent(
    selectedSection: HomeSection,
    songs: List<Song>,
    playlists: List<PlaylistEntity>,
    recentPlays: List<com.example.data.db.RecentPlayEntity>,
    favoriteIds: Set<Long>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSelectSection: (HomeSection) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowDetails: (Song) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenFolder: (String, List<Song>) -> Unit,
    onOpenAlbum: (String, String, List<Song>) -> Unit,
    onOpenArtist: (String, List<Song>) -> Unit,
    onOpenPlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRequestPermission: () -> Unit
) {
    val sections = HomeSection.values()

    Column(modifier = Modifier.fillMaxSize()) {
        // Recently Played Carousel (if has items)
        if (recentPlays.isNotEmpty()) {
            RecentlyPlayedCarousel(
                recentPlays = recentPlays,
                allSongs = songs,
                onSongClick = onSongClick
            )
        }

        // Section Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSection.ordinal,
            containerColor = Color.Transparent,
            contentColor = NeonPurple,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection.ordinal]),
                    color = NeonPurple,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            sections.forEach { section ->
                val isSelected = selectedSection == section
                Tab(
                    selected = isSelected,
                    onClick = { onSelectSection(section) },
                    text = {
                        Text(
                            text = section.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else SoftPurple.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_${section.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content of Selected Tab
        when (selectedSection) {
            HomeSection.ALL_SONGS -> {
                AllSongsSection(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    favoriteIds = favoriteIds,
                    onSongClick = onSongClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowDetails = onShowDetails,
                    onRequestPermission = onRequestPermission
                )
            }

            HomeSection.FOLDERS -> {
                FoldersSection(songs = songs, onOpenFolder = onOpenFolder)
            }

            HomeSection.ALBUMS -> {
                AlbumsSection(songs = songs, onOpenAlbum = onOpenAlbum)
            }

            HomeSection.ARTISTS -> {
                ArtistsSection(songs = songs, onOpenArtist = onOpenArtist)
            }

            HomeSection.PLAYLISTS -> {
                PlaylistsSection(
                    playlists = playlists,
                    onCreatePlaylist = onCreatePlaylist,
                    onOpenPlaylist = onOpenPlaylist,
                    onDeletePlaylist = onDeletePlaylist
                )
            }

            HomeSection.FAVORITES -> {
                val favSongs = songs.filter { favoriteIds.contains(it.id) }
                FavoritesSection(
                    favoriteSongs = favSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    favoriteIds = favoriteIds,
                    onSongClick = onSongClick,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowDetails = onShowDetails
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedCarousel(
    recentPlays: List<com.example.data.db.RecentPlayEntity>,
    allSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recentPlays) { item ->
                val matchedSong = allSongs.find { it.id == item.songId } ?: Song(
                    id = item.songId,
                    title = item.songTitle,
                    artist = item.songArtist,
                    album = item.songAlbum,
                    durationMs = item.durationMs,
                    contentUri = item.songUri,
                    albumArtUri = item.albumArtUri
                )

                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSongClick(matchedSong, allSongs) }
                        .testTag("recent_song_${item.songId}"),
                    colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF261240)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.albumArtUri != null) {
                                AsyncImage(
                                    model = item.albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.songTitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.songArtist,
                            style = MaterialTheme.typography.labelSmall.copy(color = SoftPurple, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllSongsSection(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowDetails: (Song) -> Unit,
    onRequestPermission: () -> Unit
) {
    if (songs.isEmpty()) {
        EmptyScanPrompt(onRequestPermission = onRequestPermission)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Quick Actions Row (Total count, Shuffle All, Play All)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${songs.size} tracks found",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SoftPurple,
                        fontWeight = FontWeight.Medium
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                val shuffled = songs.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurpleCard),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder),
                        modifier = Modifier.testTag("shuffle_all_btn")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                onSongClick(songs.first(), songs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("play_all_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        items(songs, key = { it.id }) { song ->
            val isCurrent = currentSong?.id == song.id
            val isFav = favoriteIds.contains(song.id)
            SongItem(
                song = song,
                isCurrentSong = isCurrent,
                isPlaying = isPlaying,
                isFavorite = isFav,
                onSongClick = { onSongClick(song, songs) },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onShowDetails = { onShowDetails(song) }
            )
        }
    }
}

@Composable
private fun FoldersSection(
    songs: List<Song>,
    onOpenFolder: (folderName: String, songs: List<Song>) -> Unit
) {
    val foldersMap = remember(songs) {
        songs.groupBy { it.folderName }
    }

    if (foldersMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found", color = SoftPurple)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(foldersMap.keys.toList()) { folderName ->
            val folderSongs = foldersMap[folderName] ?: emptyList()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenFolder(folderName, folderSongs) }
                    .testTag("folder_$folderName"),
                colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${folderSongs.size} audio files",
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    songs: List<Song>,
    onOpenAlbum: (albumName: String, artist: String, songs: List<Song>) -> Unit
) {
    val albumsMap = remember(songs) {
        songs.groupBy { it.displayAlbum }
    }

    if (albumsMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found", color = SoftPurple)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albumsMap.keys.toList()) { albumName ->
            val albumSongs = albumsMap[albumName] ?: emptyList()
            val artist = albumSongs.firstOrNull()?.displayArtist ?: "Unknown"
            val artUri = albumSongs.firstOrNull()?.albumArtUri

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenAlbum(albumName, artist, albumSongs) }
                    .testTag("album_$albumName"),
                colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF24103E)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artUri != null) {
                            AsyncImage(
                                model = artUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$artist • ${albumSongs.size} songs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SoftPurple,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsSection(
    songs: List<Song>,
    onOpenArtist: (artistName: String, songs: List<Song>) -> Unit
) {
    val artistsMap = remember(songs) {
        songs.groupBy { it.displayArtist }
    }

    if (artistsMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found", color = SoftPurple)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artistsMap.keys.toList()) { artistName ->
            val artistSongs = artistsMap[artistName] ?: emptyList()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenArtist(artistName, artistSongs) }
                    .testTag("artist_$artistName"),
                colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonFuchsia.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonFuchsia,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${artistSongs.size} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylist: (playlistId: Long, name: String) -> Unit,
    onDeletePlaylist: (playlistId: Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Create Playlist Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onCreatePlaylist)
                    .testTag("create_playlist_card"),
                colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(NeonPurple, NeonFuchsia))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Create New Playlist",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Curate your favorite tracks",
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple)
                        )
                    }
                }
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            val color = try {
                Color(android.graphics.Color.parseColor(playlist.iconColorHex))
            } catch (e: Exception) {
                NeonPurple
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenPlaylist(playlist.id, playlist.name) }
                    .testTag("playlist_item_${playlist.id}"),
                colors = CardDefaults.cardColors(containerColor = DarkPurpleCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "Custom Playlist",
                            style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple)
                        )
                    }

                    IconButton(
                        onClick = { onDeletePlaylist(playlist.id) },
                        modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = SoftPurple.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    favoriteSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowDetails: (Song) -> Unit
) {
    if (favoriteSongs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = ElectricPink.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No favorites yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Tap the heart icon on any song to add it here!",
                    style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${favoriteSongs.size} favorite songs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ElectricPink,
                        fontWeight = FontWeight.Bold
                    )
                )
                Button(
                    onClick = {
                        if (favoriteSongs.isNotEmpty()) {
                            onSongClick(favoriteSongs.first(), favoriteSongs)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("play_all_favorites_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        items(favoriteSongs, key = { it.id }) { song ->
            val isCurrent = currentSong?.id == song.id
            SongItem(
                song = song,
                isCurrentSong = isCurrent,
                isPlaying = isPlaying,
                isFavorite = true,
                onSongClick = { onSongClick(song, favoriteSongs) },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onShowDetails = { onShowDetails(song) }
            )
        }
    }
}

@Composable
private fun DetailSongListView(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowDetails: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Sub Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = SoftPurple),
                    maxLines = 1
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAll,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All")
            }

            Button(
                onClick = onShuffleAll,
                colors = ButtonDefaults.buttonColors(containerColor = DarkPurpleCard),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkPurpleBorder),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shuffle", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs in this collection yet", color = SoftPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    val isCurrent = currentSong?.id == song.id
                    val isFav = favoriteIds.contains(song.id)
                    SongItem(
                        song = song,
                        isCurrentSong = isCurrent,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        onSongClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onShowDetails = { onShowDetails(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScanPrompt(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkPurpleCard)
                .border(1.dp, DarkPurpleBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Scanning Music Files",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Abid Music detects all audio files (MP3, WAV, FLAC, AAC) stored in your device storage automatically.",
                style = MaterialTheme.typography.bodyMedium.copy(color = SoftPurple),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("scan_storage_permission_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Permission & Scan")
            }
        }
    }
}
