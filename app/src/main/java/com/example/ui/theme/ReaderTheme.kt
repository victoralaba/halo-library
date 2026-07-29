package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ReaderThemeMode {
    LIGHT_PAPER,
    DARK_OBSIDIAN,
    SEPIA_VINTAGE,
    OLED_NIGHT
}

data class ReaderThemeColors(
    val backgroundColor: Color,
    val textColor: Color,
    val surfaceColor: Color,
    val accentColor: Color,
    val highlightGlowColor: Color,
    val textHighlightColor: Color
)

object ReaderThemeConfig {
    fun getColors(mode: ReaderThemeMode): ReaderThemeColors {
        return when (mode) {
            ReaderThemeMode.LIGHT_PAPER -> ReaderThemeColors(
                backgroundColor = Color(0xFFFBF9F5),
                textColor = Color(0xFF1E2022),
                surfaceColor = Color(0xFFFFFFFF),
                accentColor = Color(0xFFD97706),
                highlightGlowColor = Color(0xFFFEF3C7),
                textHighlightColor = Color(0xFF92400E)
            )
            ReaderThemeMode.DARK_OBSIDIAN -> ReaderThemeColors(
                backgroundColor = Color(0xFF12131C),
                textColor = Color(0xFFE2E4EB),
                surfaceColor = Color(0xFF1C1E2B),
                accentColor = Color(0xFFF59E0B),
                highlightGlowColor = Color(0xFF451A03),
                textHighlightColor = Color(0xFFFDE68A)
            )
            ReaderThemeMode.SEPIA_VINTAGE -> ReaderThemeColors(
                backgroundColor = Color(0xFFF4ECD8),
                textColor = Color(0xFF3E2723),
                surfaceColor = Color(0xFFEFE5CF),
                accentColor = Color(0xFF8D6E63),
                highlightGlowColor = Color(0xFFE2D4B7),
                textHighlightColor = Color(0xFF271B17)
            )
            ReaderThemeMode.OLED_NIGHT -> ReaderThemeColors(
                backgroundColor = Color(0xFF000000),
                textColor = Color(0xFFD4D4D8),
                surfaceColor = Color(0xFF121212),
                accentColor = Color(0xFF38BDF8),
                highlightGlowColor = Color(0xFF0369A1),
                textHighlightColor = Color(0xFFE0F2FE)
            )
        }
    }
}
