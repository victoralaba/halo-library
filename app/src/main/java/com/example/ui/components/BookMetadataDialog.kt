package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookEntity
import com.example.data.parser.EpubParser
import com.example.data.parser.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookMetadataDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onReadBook: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val isPdf = book.fileType.equals("PDF", ignoreCase = true)
    val coverColor = try {
        Color(android.graphics.Color.parseColor(book.coverColorHex))
    } catch (e: Exception) {
        Color(0xFF3F51B5)
    }

    val sampleCoverRes = when (book.title) {
        "The Art of War" -> R.drawable.cover_art_of_war
        "Pride and Prejudice" -> R.drawable.cover_pride_and_prejudice
        "Lumina Reader Guide & Classics" -> R.drawable.cover_lumina_guide
        else -> null
    }

    val dynamicCoverState = produceState<Bitmap?>(initialValue = null, key1 = book.id) {
        if (book.coverPath.isNullOrBlank() && sampleCoverRes == null) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(book.filePath)
                    if (isPdf) {
                        value = PdfHelper.renderPageBitmap(context, uri, book.isAsset, book.filePath, 0, 400)
                    } else {
                        val bytes = EpubParser.extractCoverBytesFromUri(context, uri)
                        if (bytes != null && bytes.isNotEmpty()) {
                            value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }

    val coverModel: Any? = when {
        !book.coverPath.isNullOrBlank() -> book.coverPath
        sampleCoverRes != null -> sampleCoverRes
        dynamicCoverState.value != null -> dynamicCoverState.value
        else -> null
    }

    val fileSizeText = produceState(initialValue = "Calculating...", key1 = book.id) {
        withContext(Dispatchers.IO) {
            value = getFormattedFileSize(context, book)
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    val addedDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(book.addedTimestamp))
    val lastReadStr = if (book.lastReadTimestamp > 0) dateFormat.format(Date(book.lastReadTimestamp)) else "Not opened yet"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("book_metadata_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Book Metadata & Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cover Image & Basic Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(115.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(coverColor)
                    ) {
                        if (coverModel != null) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = "Cover preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "By ${book.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Format & Favorite Badges
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPdf) Color(0xFFD32F2F) else Color(0xFF00796B)
                            ) {
                                Text(
                                    text = book.fileType.uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (book.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reading Progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${book.progressPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (book.progressPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isPdf) "Current Page: ${book.lastReadPage + 1} of ${book.totalPages}"
                            else "Current Chapter: ${book.lastReadChapter + 1} of ${book.totalChapters}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Details Section
                Text(
                    text = "Document Properties",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                MetadataRow(icon = Icons.Default.Title, label = "Title", value = book.title)
                MetadataRow(icon = Icons.Default.Person, label = "Author", value = book.author)
                MetadataRow(icon = Icons.Outlined.Description, label = "File Format", value = "${book.fileType} (${if (book.isAsset) "Pre-loaded Asset" else "Local Storage File"})")
                MetadataRow(icon = Icons.Default.MenuBook, label = "Content Structure", value = "${book.totalChapters} Chapters • ${book.totalPages} Pages")
                MetadataRow(icon = Icons.Default.Storage, label = "File Size", value = fileSizeText.value)
                MetadataRow(icon = Icons.Default.Schedule, label = "Last Read", value = lastReadStr)
                MetadataRow(icon = Icons.Default.Event, label = "Date Added", value = addedDateStr)
                MetadataRow(icon = Icons.Outlined.Folder, label = "File Source", value = book.filePath)

                if (book.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Synopsis & Description",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onReadBook()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Read Book")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getFormattedFileSize(context: Context, book: BookEntity): String {
    return try {
        if (book.isAsset) {
            val assetManager = context.assets
            val fd = assetManager.openFd(book.filePath)
            formatBytes(fd.length)
        } else {
            val uri = Uri.parse(book.filePath)
            if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    formatBytes(pfd.statSize)
                } ?: "Unknown Size"
            } else {
                val file = File(uri.path ?: book.filePath)
                if (file.exists()) formatBytes(file.length()) else "Unknown Size"
            }
        }
    } catch (e: Exception) {
        "Local File"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
