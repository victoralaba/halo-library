package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DailyReadingStat
import com.example.data.repository.ReadingStatsSummary
import com.example.ui.viewmodel.ChartMetricMode
import com.example.ui.viewmodel.ReadingStatsViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsScreen(
    viewModel: ReadingStatsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.statsSummary.collectAsState()
    val chartMode by viewModel.chartMetricMode.collectAsState()
    val scrollState = rememberScrollState()

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reading Statistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Ebook & Audio analytics summary",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("stats_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset Statistics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("reading_stats_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards Row
            SummaryMetricsOverview(stats = stats)

            // Bar Chart Container Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reading_stats_chart_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weekly Reading Trends",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Chart Metric Toggle
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.widthIn(max = 220.dp)
                        ) {
                            SegmentedButton(
                                selected = chartMode == ChartMetricMode.TIME_SPENT,
                                onClick = { viewModel.setChartMetricMode(ChartMetricMode.TIME_SPENT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("chart_mode_time")
                            ) {
                                Text("Time (Min)", fontSize = 11.sp)
                            }
                            SegmentedButton(
                                selected = chartMode == ChartMetricMode.BOOKS_COMPLETED,
                                onClick = { viewModel.setChartMetricMode(ChartMetricMode.BOOKS_COMPLETED) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("chart_mode_books")
                            ) {
                                Text("Totals", fontSize = 11.sp)
                            }
                        }
                    }

                    // Legend Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        LegendItem(
                            color = Color(0xFF1E88E5), // Ebook Blue
                            label = "📘 Ebook Reading"
                        )
                        LegendItem(
                            color = Color(0xFF8E24AA), // Audio Purple
                            label = "🎧 Audio Listening"
                        )
                    }

                    // Bar Chart Canvas Composable
                    if (chartMode == ChartMetricMode.TIME_SPENT) {
                        WeeklyBarChartCanvas(
                            weeklyStats = stats.weeklyStats,
                            selectedDayIndex = selectedDayIndex,
                            onSelectDay = { index -> selectedDayIndex = if (selectedDayIndex == index) null else index }
                        )
                    } else {
                        TotalsComparisonBarChartCanvas(
                            stats = stats
                        )
                    }

                    // Selected Day Detail Box
                    selectedDayIndex?.let { idx ->
                        if (idx in stats.weeklyStats.indices) {
                            val day = stats.weeklyStats[idx]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${day.dayName} (${day.dateKey})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (day.isToday) "Today's Activity" else "Daily breakdown",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Surface(
                                            color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "📘 ${day.ebookMinutes.toInt()} min",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF1565C0),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            color = Color(0xFF8E24AA).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "🎧 ${day.audioMinutes.toInt()} min",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF6A1B9A),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Breakdown Section Header
            Text(
                text = "DETAILED FORMAT BREAKDOWN",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Ebook Detailed Stat Card
            DetailedStatCard(
                title = "Ebook Reading Stats",
                icon = Icons.Outlined.MenuBook,
                iconTint = Color(0xFF1E88E5),
                totalTime = formatMinutesToHours(stats.totalEbookTimeMinutes),
                completedCount = stats.totalEbooksRead,
                inProgressCount = stats.totalEbooksInProgress,
                libraryCount = stats.totalEbooksInLibrary,
                accentColor = Color(0xFF1E88E5),
                testTag = "ebook_stat_card"
            )

            // Audio Detailed Stat Card
            DetailedStatCard(
                title = "Audiobook Listening Stats",
                icon = Icons.Outlined.Headphones,
                iconTint = Color(0xFF8E24AA),
                totalTime = formatMinutesToHours(stats.totalAudioTimeMinutes),
                completedCount = stats.totalAudioFinished,
                inProgressCount = stats.totalAudioInProgress,
                libraryCount = stats.totalAudioInLibrary,
                accentColor = Color(0xFF8E24AA),
                testTag = "audio_stat_card"
            )

            // Quick Add Manual Test Sessions Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_session_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Log Reading Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Tap buttons below to manually record reading/listening time for today:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addManualEbookTime(15) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_15m_ebook_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+15m Ebook", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.addManualAudioTime(15) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_15m_audio_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8E24AA)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+15m Audio", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.addManualEbookTime(30) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_30m_ebook_button"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+30m Ebook", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.addManualAudioTime(30) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_30m_audio_button"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("+30m Audio", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Reading Statistics?") },
            text = { Text("This will clear all accumulated reading and listening time history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetStats()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricsOverview(stats: ReadingStatsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Total Books Read Card
        MetricCard(
            title = "Books Read",
            mainValue = "${stats.totalEbooksRead + stats.totalAudioFinished}",
            subValue = "📘 ${stats.totalEbooksRead}  🎧 ${stats.totalAudioFinished}",
            icon = Icons.Outlined.CheckCircle,
            accentColor = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f)
        )

        // Total Time Spent Card
        MetricCard(
            title = "Total Time",
            mainValue = formatMinutesToHours(stats.totalEbookTimeMinutes + stats.totalAudioTimeMinutes),
            subValue = "📘 ${formatMinutesToHours(stats.totalEbookTimeMinutes)} | 🎧 ${formatMinutesToHours(stats.totalAudioTimeMinutes)}",
            icon = Icons.Outlined.Timer,
            accentColor = Color(0xFF1565C0),
            modifier = Modifier.weight(1.1f)
        )

        // Streak Card
        MetricCard(
            title = "Daily Streak",
            mainValue = "${stats.currentStreakDays}d",
            subValue = "Avg ${stats.averageDailyMinutes}m/day",
            icon = Icons.Default.LocalFireDepartment,
            accentColor = Color(0xFFE65100),
            modifier = Modifier.weight(0.9f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    mainValue: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = mainValue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subValue,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeeklyBarChartCanvas(
    weeklyStats: List<DailyReadingStat>,
    selectedDayIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    val ebookColor = Color(0xFF1E88E5)
    val audioColor = Color(0xFF8E24AA)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = remember(weeklyStats) {
        val peak = weeklyStats.maxOfOrNull { max(it.ebookMinutes, it.audioMinutes) } ?: 60f
        if (peak <= 0f) 60f else (peak * 1.2f).coerceAtLeast(30f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Touch handling logic handled per day bar column
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 40f
            val topPadding = 20f
            val chartHeight = height - bottomPadding - topPadding

            val count = weeklyStats.size.coerceAtLeast(1)
            val barGroupWidth = width / count

            // Draw grid lines (3 horizontal dashed lines)
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = topPadding + (chartHeight / gridLines) * i
                drawLine(
                    color = labelColor.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw Bars for each day
            weeklyStats.forEachIndexed { index, day ->
                val groupLeft = index * barGroupWidth
                val groupCenter = groupLeft + barGroupWidth / 2f
                val barWidth = (barGroupWidth * 0.28f).coerceAtMost(24f)

                val ebookHeight = (day.ebookMinutes / maxVal) * chartHeight
                val audioHeight = (day.audioMinutes / maxVal) * chartHeight

                val ebookLeft = groupCenter - barWidth - 3f
                val audioLeft = groupCenter + 3f

                val isSelected = selectedDayIndex == index

                // Draw Ebook Bar
                if (ebookHeight > 0f) {
                    drawRoundRect(
                        color = if (isSelected) ebookColor else ebookColor.copy(alpha = 0.85f),
                        topLeft = Offset(ebookLeft, height - bottomPadding - ebookHeight),
                        size = Size(barWidth, ebookHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                } else {
                    // Empty placeholder line
                    drawRoundRect(
                        color = ebookColor.copy(alpha = 0.2f),
                        topLeft = Offset(ebookLeft, height - bottomPadding - 4f),
                        size = Size(barWidth, 4f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                // Draw Audio Bar
                if (audioHeight > 0f) {
                    drawRoundRect(
                        color = if (isSelected) audioColor else audioColor.copy(alpha = 0.85f),
                        topLeft = Offset(audioLeft, height - bottomPadding - audioHeight),
                        size = Size(barWidth, audioHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                } else {
                    // Empty placeholder line
                    drawRoundRect(
                        color = audioColor.copy(alpha = 0.2f),
                        topLeft = Offset(audioLeft, height - bottomPadding - 4f),
                        size = Size(barWidth, 4f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }

        // Overlay Interactive Clickable Day Columns
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            weeklyStats.forEachIndexed { index, day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectDay(index) }
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = day.dayName,
                        fontSize = 11.sp,
                        fontWeight = if (selectedDayIndex == index || day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsComparisonBarChartCanvas(
    stats: ReadingStatsSummary
) {
    val ebookColor = Color(0xFF1E88E5)
    val audioColor = Color(0xFF8E24AA)

    val ebookTotal = stats.totalEbooksInLibrary
    val audioTotal = stats.totalAudioInLibrary
    val ebookDone = stats.totalEbooksRead
    val audioDone = stats.totalAudioFinished

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ebooks Bar Comparison
        ProgressMetricBarItem(
            label = "📘 Ebooks Progress",
            finishedCount = ebookDone,
            totalCount = ebookTotal,
            color = ebookColor
        )

        // Audio Bar Comparison
        ProgressMetricBarItem(
            label = "🎧 Audiobooks Progress",
            finishedCount = audioDone,
            totalCount = audioTotal,
            color = audioColor
        )
    }
}

@Composable
private fun ProgressMetricBarItem(
    label: String,
    finishedCount: Int,
    totalCount: Int,
    color: Color
) {
    val percentage = if (totalCount > 0) (finishedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$finishedCount of $totalCount Completed (${(percentage * 100).toInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun DetailedStatCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    totalTime: String,
    completedCount: Int,
    inProgressCount: Int,
    libraryCount: Int,
    accentColor: Color,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(iconTint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = totalTime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatSubItem(
                    label = "Total Items",
                    value = "$libraryCount"
                )
                StatSubItem(
                    label = "Finished",
                    value = "$completedCount"
                )
                StatSubItem(
                    label = "In Progress",
                    value = "$inProgressCount"
                )
            }
        }
    }
}

@Composable
private fun StatSubItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMinutesToHours(minutes: Long): String {
    if (minutes <= 0) return "0m"
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
