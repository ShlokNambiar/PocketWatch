package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = RequestedButtonBg,
    secondary = AccentGold,
    tertiary = Purple85,
    background = DarkPageAmbientBg,
    surface = DarkCardBgGlass,
    onPrimary = ActivePillText,
    onBackground = ActivePillText,
    onSurface = ActivePillText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RequestedButtonBg,
    secondary = AccentGold,
    tertiary = AccentGreen,
    background = PageAmbientBg,
    surface = CardBgGlass,
    onPrimary = ActivePillText,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default to false to ensure our beautiful custom atmosphere is preserved
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val customSelectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
    handleColor = Primary,
    backgroundColor = Color(0x33243142) // 20% selection background
  )

  androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.foundation.text.selection.LocalTextSelectionColors provides customSelectionColors
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
