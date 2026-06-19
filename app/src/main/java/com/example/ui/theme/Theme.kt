package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkAccentGold,
    secondary = DarkAccentCitizen,
    tertiary = DarkAccentCrimson,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkBorder
  )

private val LightColorScheme = DarkColorScheme // Force dark theme by default as it is super appropriate for Mafia

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for Mafia theme suitability
  dynamicColor: Boolean = false, // Disable dynamic colors to keep premium dark theme consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
