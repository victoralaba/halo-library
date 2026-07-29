package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.audio.RepeatMode
import com.example.data.local.AudioBookmarkEntity
import com.example.data.local.AudioTrackEntity
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullAudioPlayerSheet(
    currentTrack: AudioTrackEntity?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    playlist: List<AudioTrackEntity>,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    bookmarks: List<AudioBookmarkEntity>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipForward: (Int) -> Unit,
    onSkipBackward: (Int) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (AudioBookmarkEntity) -> Unit,
    onPlayTrackFromList: (AudioTrackEntity) -> Unit
) {
    if (currentTrack == null) return

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showTracklistSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(currentPositionMs.toFloat()) }

    val effectivePosition = if (isUserSeeking) seekPositionMs.toLong() else currentPositionMs
    val progressFraction = if (durationMs > 0) (effectivePosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight(0.95f)
            .testTag("full_audio_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Collapse Icon & Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("full_player_dismiss")) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", modifier = Modifier.size(32.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "NOW PLAYING • ${currentTrack.fileType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = { showTracklistSheet = true }, modifier = Modifier.testTag("full_player_tracklist_btn")) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover Art / Visualizer Frame
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentTrack.coverUri.isNullOrBlank() && File(currentTrack.coverUri).exists()) {
                        AsyncImage(
                            model = File(currentTrack.coverUri),
                            contentDescription = "Audiobook Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Dynamic Wave Visualizer Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val centerY = height / 2f

                            if (isPlaying) {
                                for (i in 0 until 5) {
                                    val barWidth = width / 12f
                                    val amplitude = (height * 0.25f) * (sin(wavePhase + i * 1.2f) + 1.2f)
                                    val startX = (width * 0.2f) + (i * barWidth * 1.5f)

                                    drawRect(
                                        color = primaryColor.copy(alpha = 0.7f - (i * 0.1f)),
                                        topLeft = androidx.compose.ui.geometry.Offset(startX, centerY - (amplitude / 2f)),
                                        size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, amplitude)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentTrack.album,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title & Author
            Text(
                text = currentTrack.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTrack.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scrubber Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isUserSeeking) seekPositionMs else currentPositionMs.toFloat(),
                    onValueChange = {
                        isUserSeeking = true
                        seekPositionMs = it
                    },
                    onValueChangeFinished = {
                        isUserSeeking = false
                        onSeekTo(seekPositionMs.toLong())
                    },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("full_player_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(effectivePosition),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Playback Controls Row (Skip, Prev, Play/Pause, Next, Skip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat Mode Toggle
                IconButton(onClick = onToggleRepeatMode) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.NONE -> Icons.Outlined.Repeat
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Skip Backward 10s
                IconButton(onClick = { onSkipBackward(10) }, modifier = Modifier.testTag("skip_back_10")) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Previous Track
                IconButton(onClick = onPlayPrevious, modifier = Modifier.testTag("play_prev")) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                FloatingActionButton(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("full_player_play_pause_fab")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Track
                IconButton(onClick = onPlayNext, modifier = Modifier.testTag("play_next")) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Skip Forward 30s
                IconButton(onClick = { onSkipForward(30) }, modifier = Modifier.testTag("skip_forward_30")) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "Forward 30 seconds",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Shuffle Toggle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary Audio Features Chips Row (Speed, Sleep Timer, Bookmarks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Speed Selector Chip
                AssistChip(
                    onClick = { showSpeedDialog = true },
                    label = { Text("${playbackSpeed}x", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("speed_selector_chip")
                )

                // Sleep Timer Chip
                AssistChip(
                    onClick = { showSleepTimerDialog = true },
                    label = {
                        val timerLabel = if (sleepTimerRemainingSec != null) {
                            "${sleepTimerRemainingSec / 60}m ${sleepTimerRemainingSec % 60}s"
                        } else {
                            "Sleep Timer"
                        }
                        Text(timerLabel, fontWeight = FontWeight.SemiBold)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (sleepTimerRemainingSec != null) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("sleep_timer_chip")
                )

                // Audio Bookmarks Chip
                AssistChip(
                    onClick = { showBookmarksSheet = true },
                    label = { Text("Notes (${bookmarks.size})", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("bookmarks_chip")
                )
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${speed}x", fontSize = 16.sp, fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                            if (playbackSpeed == speed) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timerOptions = listOf(0 to "Off", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes")
                    timerOptions.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (mins == 0) onCancelSleepTimer() else onSetSleepTimerMinutes(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontSize = 16.sp)
                            if ((mins == 0 && sleepTimerRemainingSec == null) || (mins > 0 && sleepTimerRemainingSec != null && (sleepTimerRemainingSec / 60) <= mins)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Bookmarks Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audio Bookmarks & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddBookmarkDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Note")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No bookmarks saved for this track yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks) { bm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeekTo(bm.positionMs)
                                        showBookmarksSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formatDuration(bm.positionMs),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(text = bm.note, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete bookmark", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark Note at ${formatDuration(currentPositionMs)}") },
            text = {
                OutlinedTextField(
                    value = bookmarkNoteText,
                    onValueChange = { bookmarkNoteText = it },
                    placeholder = { Text("Enter a note or chapter landmark...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onAddBookmark(bookmarkNoteText)
                    bookmarkNoteText = ""
                    showAddBookmarkDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Queue / Playlist Tracklist Sheet
    if (showTracklistSheet) {
        ModalBottomSheet(onDismissRequest = { showTracklistSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Playlist Queue & Chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlist) { track ->
                        val isCurrent = track.id == currentTrack.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayTrackFromList(track)
                                    showTracklistSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCurrent) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
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
