package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.local.AudioPlaylistEntity
import com.example.data.local.AudioTrackEntity
import com.example.ui.viewmodel.AudioPlayerViewModel
import java.io.File

enum class AudioTab {
    PLAYLISTS, TRACKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(AudioTab.PLAYLISTS) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }

    var targetPlaylistForImport by remember { mutableStateOf<AudioPlaylistEntity?>(null) }
    var trackForAddToPlaylist by remember { mutableStateOf<AudioTrackEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Multi-audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val targetId = targetPlaylistForImport?.id ?: 0L
            viewModel.importAudioFiles(uris, targetId)
            targetPlaylistForImport = null
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    targetPlaylistForImport = uiState.selectedPlaylist
                    audioPickerLauncher.launch("audio/*")
                },
                icon = { Icon(Icons.Default.AudioFile, contentDescription = null) },
                text = { Text("Import Audio") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("import_audio_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.testTag("create_playlist_btn")
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Create Playlist")
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search audiobooks, playlists, tracks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs (Playlists vs All Tracks)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == AudioTab.PLAYLISTS,
                    onClick = {
                        selectedTab = AudioTab.PLAYLISTS
                        viewModel.selectPlaylist(null)
                    },
                    text = { Text("Audiobooks & Playlists (${allPlaylists.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_playlists")
                )
                Tab(
                    selected = selectedTab == AudioTab.TRACKS,
                    onClick = {
                        selectedTab = AudioTab.TRACKS
                        viewModel.selectPlaylist(null)
                    },
                    text = { Text("All Tracks (${allTracks.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_all_tracks")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breadcrumb if playlist selected
            if (uiState.selectedPlaylist != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { viewModel.selectPlaylist(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playlist: ${uiState.selectedPlaylist?.name}",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { uiState.selectedPlaylist?.let { viewModel.playPlaylist(it) } },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == AudioTab.PLAYLISTS && uiState.selectedPlaylist == null) {
                // Playlists Grid View
                val filteredPlaylists = remember(allPlaylists, uiState.searchQuery) {
                    if (uiState.searchQuery.isBlank()) allPlaylists
                    else allPlaylists.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
                }

                if (filteredPlaylists.isEmpty()) {
                    EmptyAudioState(
                        message = if (uiState.searchQuery.isBlank()) "No audio playlists yet" else "No matching playlists",
                        subMessage = "Create custom audio playlists or import audiobook folders.",
                        onImportClick = { audioPickerLauncher.launch("audio/*") }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPlaylists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { viewModel.selectPlaylist(playlist) },
                                onPlayClick = { viewModel.playPlaylist(playlist) },
                                onDeleteClick = { viewModel.deletePlaylist(playlist) }
                            )
                        }
                    }
                }
            } else {
                // Tracks List View (All Tracks or Selected Playlist Tracks)
                val tracksToDisplay = if (uiState.selectedPlaylist != null) {
                    val selectedTracks by viewModel.selectedPlaylistTracks.collectAsState()
                    selectedTracks
                } else {
                    allTracks
                }

                val filteredTracks = remember(tracksToDisplay, uiState.searchQuery) {
                    if (uiState.searchQuery.isBlank()) tracksToDisplay
                    else tracksToDisplay.filter {
                        it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                it.artist.contains(uiState.searchQuery, ignoreCase = true) ||
                                it.album.contains(uiState.searchQuery, ignoreCase = true)
                    }
                }

                if (filteredTracks.isEmpty()) {
                    EmptyAudioState(
                        message = if (uiState.searchQuery.isBlank()) "No audio tracks imported" else "No matching tracks",
                        subMessage = "Tap 'Import Audio' to add MP3, M4A, AAC, or WAV files from your device.",
                        onImportClick = {
                            targetPlaylistForImport = uiState.selectedPlaylist
                            audioPickerLauncher.launch("audio/*")
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            AudioTrackItem(
                                track = track,
                                isCurrent = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                onClick = { viewModel.playTrack(track, filteredTracks) },
                                onAddToPlaylist = {
                                    trackForAddToPlaylist = track
                                    showAddToPlaylistDialog = true
                                },
                                onDelete = { viewModel.deleteTrack(track) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Audio Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Title") },
                        placeholder = { Text("e.g. Art of War Audiobook") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = playlistDescInput,
                        onValueChange = { playlistDescInput = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("e.g. Chapter recordings & lectures") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput, playlistDescInput)
                            playlistNameInput = ""
                            playlistDescInput = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Track to Playlist Dialog
    if (showAddToPlaylistDialog && trackForAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (allPlaylists.isEmpty()) {
                        Text("No playlists available. Create a playlist first.")
                    } else {
                        allPlaylists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        trackForAddToPlaylist?.let { tr ->
                                            viewModel.viewModelScope.launch {
                                                viewModel.repository.addTrackToPlaylist(tr, playlist.id)
                                            }
                                        }
                                        showAddToPlaylistDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(playlist.name, fontWeight = FontWeight.SemiBold)
                                    Text("${playlist.trackCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: AudioPlaylistEntity,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Play Button Overlay
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Playlist",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.trackCount} audio tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete Playlist") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioTrackItem(
    track: AudioTrackEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val resumeProgress = if (track.durationMs > 0 && track.lastPositionMs > 0) {
        (track.lastPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("track_item_${track.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Art / Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.coverUri.isNullOrBlank() && File(track.coverUri).exists()) {
                        AsyncImage(
                            model = File(track.coverUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isCurrent && isPlaying) Icons.Default.VolumeUp else Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Duration badge & format badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(track.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = track.fileType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Overflow Menu
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Track") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Resume Position indicator line if saved position exists
            if (resumeProgress > 0f) {
                LinearProgressIndicator(
                    progress = { resumeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun EmptyAudioState(
    message: String,
    subMessage: String,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AudioFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Audio File(s)")
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
