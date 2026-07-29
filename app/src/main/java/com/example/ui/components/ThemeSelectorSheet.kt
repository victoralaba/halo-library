package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ReaderThemeConfig
import com.example.ui.theme.ReaderThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorSheet(
    currentThemeMode: ReaderThemeMode,
    fontSizeSp: Int,
    lineHeightMultiplier: Float,
    fontFamily: String,
    onThemeSelected: (ReaderThemeMode) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("theme_selector_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Reader Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Theme Color Swatches
            Text(
                text = "COLOR SCHEME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReaderThemeMode.values().forEach { mode ->
                    val themeColors = ReaderThemeConfig.getColors(mode)
                    val isSelected = currentThemeMode == mode
                    val modeName = when (mode) {
                        ReaderThemeMode.LIGHT_PAPER -> "Light"
                        ReaderThemeMode.DARK_OBSIDIAN -> "Dark"
                        ReaderThemeMode.SEPIA_VINTAGE -> "Sepia"
                        ReaderThemeMode.OLED_NIGHT -> "OLED"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onThemeSelected(mode) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(themeColors.backgroundColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) themeColors.accentColor else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aa",
                                color = themeColors.textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = modeName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Font Size Controls
            Text(
                text = "FONT SIZE (${fontSizeSp}sp)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onFontSizeChange(fontSizeSp - 2) },
                    enabled = fontSizeSp > 12,
                    modifier = Modifier.testTag("font_size_decrease")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size")
                }

                Text(
                    text = "A",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = fontSizeSp.toFloat(),
                    onValueChange = { onFontSizeChange(it.toInt()) },
                    valueRange = 12f..36f,
                    steps = 11,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                Text(
                    text = "A",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onFontSizeChange(fontSizeSp + 2) },
                    enabled = fontSizeSp < 36,
                    modifier = Modifier.testTag("font_size_increase")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Font Family Selector
            Text(
                text = "FONT STYLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Serif", "SansSerif", "Monospace").forEach { family ->
                    val isSelected = fontFamily.equals(family, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFontFamilyChange(family) },
                        label = {
                            Text(
                                text = when (family) {
                                    "Serif" -> "Georgia (Serif)"
                                    "SansSerif" -> "Roboto (Sans)"
                                    else -> "Mono"
                                },
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("font_family_chip_$family")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Line Spacing
            Text(
                text = "LINE SPACING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1.2f, 1.5f, 1.8f, 2.0f).forEach { multiplier ->
                    val isSelected = lineHeightMultiplier == multiplier
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLineHeightChange(multiplier) },
                        label = { Text("${multiplier}x", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("line_height_chip_$multiplier")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
