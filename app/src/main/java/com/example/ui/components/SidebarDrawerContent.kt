package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Screen
import com.example.ui.theme.ReaderThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarDrawerContent(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onCloseDrawer: () -> Unit,
    themeMode: ReaderThemeMode,
    onThemeSelect: (ReaderThemeMode) -> Unit,
    onOpenSuggestionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 4.dp,
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("sidebar_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Banner (Clean Solid Blue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .testTag("close_sidebar_button")
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sidebar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Lumina Reader",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Offline Book Reader & Audio Suite",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF81C784), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v1.2 • Zero Cloud Latency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Menu
            Text(
                text = "NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Mind Draft - Lightweight Notepad
            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mind Draft", fontWeight = FontWeight.SemiBold)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ".TXT Note",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                selected = currentScreen == Screen.MIND_DRAFT,
                onClick = { onNavigate(Screen.MIND_DRAFT) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_mind_draft"),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                label = { Text("Reading Statistics", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                selected = currentScreen == Screen.READING_STATS,
                onClick = { onNavigate(Screen.READING_STATS) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_reading_stats"),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                label = { Text("Saved Highlights & Quotes", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.FormatQuote, contentDescription = null) },
                selected = currentScreen == Screen.HIGHLIGHTS,
                onClick = { onNavigate(Screen.HIGHLIGHTS) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_highlights"),
                shape = RoundedCornerShape(12.dp)
            )

            NavigationDrawerItem(
                label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onNavigate(Screen.SETTINGS) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("drawer_item_settings"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Theme Picker Section
            Text(
                text = "READER THEME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReaderThemeMode.values().forEach { mode ->
                    val isSelected = themeMode == mode
                    val (modeLabel, modeColor) = when (mode) {
                        ReaderThemeMode.LIGHT_PAPER -> "Light Paper" to Color(0xFFFAF7F2)
                        ReaderThemeMode.DARK_OBSIDIAN -> "Dark Obsidian" to Color(0xFF1E1E24)
                        ReaderThemeMode.SEPIA_VINTAGE -> "Sepia Vintage" to Color(0xFFF4ECD8)
                        ReaderThemeMode.OLED_NIGHT -> "OLED Pure Night" to Color(0xFF050505)
                    }

                    Surface(
                        onClick = { onThemeSelect(mode) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_theme_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(modeColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = modeLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Feature Suggestions & Roadmap Section
            Text(
                text = "SUGGESTIONS & ROADMAP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Card(
                onClick = onOpenSuggestionDialog,
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("submit_suggestion_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Submit Feature Idea",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Tap to leave feedback or requested features",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suggestions List / Upcoming Features Ideas
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoadmapIdeaItem(
                    icon = Icons.Outlined.Analytics,
                    title = "Reading Analytics & Pace",
                    description = "Daily reading velocity & time tracker"
                )
                RoadmapIdeaItem(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Audio Equalizer & Pitch",
                    description = "Custom frequencies & vocal boost"
                )
                RoadmapIdeaItem(
                    icon = Icons.Outlined.CloudOff,
                    title = "Zero-Cloud Guarantee",
                    description = "All notes & drafts remain local to device"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoadmapIdeaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Idea",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
