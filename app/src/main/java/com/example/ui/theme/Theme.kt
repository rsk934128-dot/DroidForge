package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT,
}

@Immutable
data class StatusPalette(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val running: Color,
  val runningContainer: Color,
  val queued: Color,
  val queuedContainer: Color,
  val failed: Color,
  val failedContainer: Color,
  val codeSurface: Color,
)

val LocalStatusPalette = staticCompositionLocalOf {
  StatusPalette(
    success = EmeraldMint,
    onSuccess = Color(0xFF002114),
    successContainer = EmeraldContainerDark,
    running = SkyCyan,
    runningContainer = CyanContainerDark,
    queued = AmberWarm,
    queuedContainer = AmberContainerDark,
    failed = CoralRed,
    failedContainer = RedContainerDark,
    codeSurface = Color(0xFF080D12),
  )
}

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldMint,
    onPrimary = Color(0xFF002114),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Color(0xFF70FFCA),
    secondary = SkyCyan,
    onSecondary = Color(0xFF002638),
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = Color(0xFFBBE7FE),
    tertiary = AmberWarm,
    onTertiary = Color(0xFF2E1E00),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = CoralRed,
    onError = Color(0xFF3D0614),
    errorContainer = RedContainerDark,
    onErrorContainer = Color(0xFFFFD9DF),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = Color(0xFF080D12),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F2B38),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF002114),
    secondary = CobaltCyan,
    onSecondary = Color.White,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = Color(0xFF002638),
    tertiary = AmberDeep,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF2E1E00),
    error = CrimsonRed,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = Color(0xFF410012),
    background = FrostBg,
    onBackground = TextPrimaryLight,
    surface = FrostSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = FrostSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = FrostSurface,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = FrostSurfaceHigh,
    surfaceContainerHigh = FrostSurfaceHighest,
    outline = FrostOutline,
    outlineVariant = Color(0xFFE2E8F0),
  )

private val DarkStatusPalette =
  StatusPalette(
    success = EmeraldMint,
    onSuccess = Color(0xFF002114),
    successContainer = EmeraldContainerDark,
    running = SkyCyan,
    runningContainer = CyanContainerDark,
    queued = AmberWarm,
    queuedContainer = AmberContainerDark,
    failed = CoralRed,
    failedContainer = RedContainerDark,
    codeSurface = Color(0xFF070C11),
  )

private val LightStatusPalette =
  StatusPalette(
    success = EmeraldDeep,
    onSuccess = Color.White,
    successContainer = EmeraldContainerLight,
    running = CobaltCyan,
    runningContainer = CyanContainerLight,
    queued = AmberDeep,
    queuedContainer = AmberContainerLight,
    failed = CrimsonRed,
    failedContainer = RedContainerLight,
    codeSurface = Color(0xFF0F172A),
  )

val DroidForgeShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun DroidForgeTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
    }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
  val statusPalette = if (isDark) DarkStatusPalette else LightStatusPalette

  CompositionLocalProvider(LocalStatusPalette provides statusPalette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = DroidForgeTypography,
      shapes = DroidForgeShapes,
      content = content,
    )
  }
}
