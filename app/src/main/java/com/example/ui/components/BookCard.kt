package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookEntity
import com.example.data.parser.EpubParser
import com.example.data.parser.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coverColor = try {
        Color(android.graphics.Color.parseColor(book.coverColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isPdf = book.fileType.equals("PDF", ignoreCase = true)
    val badgeColor = if (isPdf) Color(0xFFD32F2F) else Color(0xFF00796B)
    val badgeIcon = if (isPdf) Icons.Default.Description else Icons.Default.MenuBook

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
                    if (isPdf) {
                        val uri = Uri.parse(book.filePath)
                        value = PdfHelper.renderPageBitmap(context, uri, book.isAsset, book.filePath, 0, 300)
                    } else if (book.fileType.equals("EPUB", ignoreCase = true) && !book.isAsset) {
                        val uri = Uri.parse(book.filePath)
                        val bytes = EpubParser.extractCoverBytesFromUri(context, uri)
                        if (bytes != null && bytes.isNotEmpty()) {
                            value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to styled artwork
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("book_card_${book.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover Container with Floating Extension Badge
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("book_cover_box_${book.id}")
            ) {
                if (coverModel != null) {
                    // Actual Image Cover via Coil
                    AsyncImage(
                        model = coverModel,
                        contentDescription = "Cover for ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Styled Fallback Cover Artwork
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        coverColor,
                                        coverColor.copy(alpha = 0.8f),
                                        Color(0xFF1E1E2C)
                                    )
                                )
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                maxLines = 3,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // Extension Badge (Floating Badge at top-left corner overlay)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = book.fileType.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Book Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onInfoClick != null) {
                            IconButton(
                                onClick = onInfoClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("info_button_${book.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Book Details & Metadata",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("favorite_button_${book.id}")
                        ) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (book.progressPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = coverColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${book.progressPercentage.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
