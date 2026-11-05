package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = Indigo400,
  onPrimary = Indigo900,
  primaryContainer = Indigo800,
  onPrimaryContainer = Indigo100,
  secondary = Cyan500,
  onSecondary = Color.Black,
  secondaryContainer = Slate800,
  onSecondaryContainer = Cyan500,
  tertiary = Emerald500,
  onTertiary = Color.Black,
  background = Color(0xFF0B0F19),
  surface = Color(0xFF111827),
  onBackground = Color(0xFFF9FAFB),
  onSurface = Color(0xFFF9FAFB),
  surfaceVariant = Color(0xFF1F2937),
  onSurfaceVariant = Color(0xFF9CA3AF),
  outline = Color(0xFF374151),
  outlineVariant = Color(0xFF1F2937)
)

private val LightColorScheme = lightColorScheme(
  primary = Indigo600,
  onPrimary = Color.White,
  primaryContainer = Indigo50,
  onPrimaryContainer = Indigo700,
  secondary = Blue600,
  onSecondary = Color.White,
  secondaryContainer = Blue50,
  onSecondaryContainer = Blue600,
  tertiary = Emerald600,
  onTertiary = Color.White,
  background = Slate50,
  surface = Color.White,
  onBackground = Slate900,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate600,
  outline = Slate200,
  outlineVariant = Slate100
)

@Composable
fun AttendXTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
