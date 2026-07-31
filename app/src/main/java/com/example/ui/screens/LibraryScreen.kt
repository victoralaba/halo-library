package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookEntity
import com.example.R
import com.example.ui.components.BookCard
import com.example.ui.components.BookMetadataDialog
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.viewmodel.BookFilter
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenDrawer: () -> Unit,
    onBookClick: (bookId: Long) -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToTtsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedBookForMetadata by remember { mutableStateOf<BookEntity?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBook(uri)
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
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_sidebar_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Sidebar Menu"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Lumina Reader",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Theme Quick Switch
                    IconButton(
                        onClick = {
                            val nextMode = when (uiState.themeMode) {
                                ReaderThemeMode.DARK_OBSIDIAN -> ReaderThemeMode.LIGHT_PAPER
                                ReaderThemeMode.LIGHT_PAPER -> ReaderThemeMode.SEPIA_VINTAGE
                                ReaderThemeMode.SEPIA_VINTAGE -> ReaderThemeMode.OLED_NIGHT
                                ReaderThemeMode.OLED_NIGHT -> ReaderThemeMode.DARK_OBSIDIAN
                            }
                            viewModel.setThemeMode(nextMode)
                        },
                        modifier = Modifier.testTag("quick_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.themeMode == ReaderThemeMode.DARK_OBSIDIAN || uiState.themeMode == ReaderThemeMode.OLED_NIGHT)
                                Icons.Outlined.LightMode else Icons.Outlined.Nightlight,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { documentPickerLauncher.launch("*/*") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Import Book") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("import_book_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Hero Image
                        Image(
                            painter = painterResource(id = R.drawable.library_hero_banner_1785256636392),
                            contentDescription = "Cozy Library Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.85f),
                                            Color.Black.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                        )

                        // Hero Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Your Personal Sanctuary",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Read EPUBs & PDFs offline with human AI voices & synchronized tracking.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search by title or author...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookFilter.values().forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        val filterName = when (filter) {
                            BookFilter.ALL -> "All (${uiState.books.size})"
                            BookFilter.EPUB -> "EPUB"
                            BookFilter.PDF -> "PDF"
                            BookFilter.FAVORITES -> "★ Favorites"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFilterChanged(filter) },
                            label = { Text(filterName, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            // Books Section
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.books.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No matching books found" else "Your library is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import a PDF or EPUB file to start reading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = uiState.books,
                    key = { it.id }
                ) { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(book) },
                        onInfoClick = { selectedBookForMetadata = book }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }
    }

    selectedBookForMetadata?.let { book ->
        BookMetadataDialog(
            book = book,
            onDismiss = { selectedBookForMetadata = null },
            onReadBook = { onBookClick(book.id) },
            onToggleFavorite = { viewModel.toggleFavorite(book) }
        )
    }
}
