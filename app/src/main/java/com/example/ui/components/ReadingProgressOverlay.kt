package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ReaderThemeColors

@Composable
fun ReadingProgressOverlay(
    progressPercent: Int,
    progressDetailText: String,
    themeColors: ReaderThemeColors,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .testTag("reading_progress_overlay"),
            shape = CircleShape,
            color = themeColors.surfaceColor.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Sleek Linear Progress Bar Indicator
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(themeColors.textColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (progressPercent.coerceIn(0, 100) / 100f).coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(themeColors.accentColor)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "$progressPercent%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColors.accentColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = themeColors.textColor.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = progressDetailText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColors.textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
