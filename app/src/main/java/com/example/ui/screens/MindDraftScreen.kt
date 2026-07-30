package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREF_MIND_DRAFT = "mind_draft_preferences"
private const val KEY_DRAFT_CONTENT = "draft_text_content"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindDraftScreen(
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences(PREF_MIND_DRAFT, Context.MODE_PRIVATE) }

    var draftContent by remember {
        mutableStateOf(
            prefs.getString(KEY_DRAFT_CONTENT, "") ?: ""
        )
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Auto-save draft text instantly as it changes
    LaunchedEffect(draftContent) {
        prefs.edit().putString(KEY_DRAFT_CONTENT, draftContent).apply()
    }

    // Export as .txt file launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(draftContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Toast.makeText(context, "Successfully exported draft as .txt", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Live word & character count stats
    val charCount = draftContent.length
    val wordCount = remember(draftContent) {
        if (draftContent.isBlank()) 0
        else draftContent.trim().split("\\s+".toRegex()).size
    }
    val lineCount = remember(draftContent) {
        if (draftContent.isEmpty()) 0
        else draftContent.split("\n").size
    }
    val estimatedReadTimeMin = remember(wordCount) {
        (wordCount / 200).coerceAtLeast(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mind Draft",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "⚡ Zero Latency",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Lightweight offline notepad • Export as .txt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("mind_draft_hamburger")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                    }
                },
                actions = {
                    // Export TXT Button
                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportFileLauncher.launch("MindDraft_$timestamp.txt")
                        },
                        modifier = Modifier.testTag("export_txt_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export as TXT")
                    }

                    // Share Button
                    IconButton(
                        onClick = {
                            if (draftContent.isBlank()) {
                                Toast.makeText(context, "Draft is empty!", Toast.LENGTH_SHORT).show()
                            } else {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, draftContent)
                                    putExtra(Intent.EXTRA_SUBJECT, "Mind Draft Notes")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Mind Draft"))
                            }
                        },
                        modifier = Modifier.testTag("share_draft_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Notes")
                    }

                    // Copy Button
                    IconButton(
                        onClick = {
                            if (draftContent.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(draftContent))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("copy_draft_button")
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy to Clipboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Live Stats Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$charCount chars",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$lineCount lines",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "~$estimatedReadTimeMin min read",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Quick Toolbar (Templates, Clear, Timestamp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Templates button
                    OutlinedButton(
                        onClick = { showTemplatesDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("templates_button")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Templates", fontSize = 12.sp)
                    }

                    // Stamp Timestamp button
                    OutlinedButton(
                        onClick = {
                            val timeStr = SimpleDateFormat("\n\n--- [yyyy-MM-dd HH:mm] ---\n", Locale.getDefault()).format(Date())
                            draftContent += timeStr
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("timestamp_button")
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Time", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("clear_draft_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Draft",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Main Text Area
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                OutlinedTextField(
                    value = draftContent,
                    onValueChange = { draftContent = it },
                    placeholder = {
                        Text(
                            text = "Type your thoughts, reading notes, quotes, or audio timestamps here...\n\n• Zero lag: Saved locally on your device instantly.\n• Export: Tap the download icon to save as .txt file.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .testTag("mind_draft_text_input"),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Default
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }

    // Templates Dialog
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = { Text("Insert Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemplateItem(
                        title = "📖 Book Reflection",
                        description = "Title, Author, Main Takeaway, Favorite Quotes",
                        onClick = {
                            val template = """
                                📖 BOOK REFLECTION
                                Book Title: 
                                Author: 
                                Date Completed: 
                                
                                💡 MAIN TAKEAWAY:
                                - 
                                
                                📌 FAVORITE QUOTES:
                                1. ""
                                
                                📝 DETAILED NOTES:
                                
                            """.trimIndent()
                            draftContent = if (draftContent.isBlank()) template else "$draftContent\n\n$template"
                            showTemplatesDialog = false
                        }
                    )

                    TemplateItem(
                        title = "🎧 Audio Bookmark / Timestamp Log",
                        description = "Track, Timestamp, Key Point",
                        onClick = {
                            val template = """
                                🎧 AUDIOBOOK LOG
                                Track / Chapter: 
                                
                                ⏱️ TIMESTAMPS:
                                • [00:05:30] - Key insight: 
                                • [00:18:45] - Note: 
                                
                            """.trimIndent()
                            draftContent = if (draftContent.isBlank()) template else "$draftContent\n\n$template"
                            showTemplatesDialog = false
                        }
                    )

                    TemplateItem(
                        title = "⚡ Daily Mind Dump",
                        description = "Quick goals, ideas, and scratchpad",
                        onClick = {
                            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                            val template = """
                                ⚡ MIND DRAFT - $dateStr
                                
                                🎯 TOP PRIORITIES:
                                [ ] 1. 
                                [ ] 2. 
                                
                                💡 IDEAS:
                                • 
                                
                            """.trimIndent()
                            draftContent = if (draftContent.isBlank()) template else "$draftContent\n\n$template"
                            showTemplatesDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Draft?") },
            text = { Text("Are you sure you want to clear all text in your Mind Draft? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        draftContent = ""
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TemplateItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
