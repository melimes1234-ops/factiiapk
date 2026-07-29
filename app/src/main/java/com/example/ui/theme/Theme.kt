package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- TAILWIND DESIGN TOKENS ---
data class TailwindSpacing(
    val zero: Dp = 0.dp,
    val xs: Dp = 4.dp,    // Tailwind space-1 (4px)
    val sm: Dp = 8.dp,    // Tailwind space-2 (8px)
    val md: Dp = 12.dp,   // Tailwind space-3 (12px)
    val lg: Dp = 16.dp,   // Tailwind space-4 (16px)
    val xl: Dp = 20.dp,   // Tailwind space-5 (20px)
    val xxl: Dp = 24.dp,  // Tailwind space-6 (24px)
    val xxxl: Dp = 32.dp, // Tailwind space-8 (32px)
    val hg: Dp = 48.dp,   // Tailwind space-12 (48px)
)

data class TailwindRadius(
    val none: Dp = 0.dp,
    val sm: Dp = 2.dp,
    val base: Dp = 4.dp,
    val md: Dp = 6.dp,
    val lg: Dp = 8.dp,
    val xl: Dp = 12.dp,
    val xxl: Dp = 16.dp,
    val xxxl: Dp = 24.dp,
    val full: Dp = 9999.dp,
)

// Global CompositionLocals for Tailwind Context Access
val LocalTailwindSpacing = staticCompositionLocalOf { TailwindSpacing() }
val LocalTailwindRadius = staticCompositionLocalOf { TailwindRadius() }

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityPrimaryDark,
    onPrimary = HighDensityOnPrimaryDark,
    primaryContainer = HighDensityPrimaryContainerDark,
    onPrimaryContainer = HighDensityOnPrimaryContainerDark,
    background = HighDensityBackgroundDark,
    onBackground = HighDensityOnBackgroundDark,
    surface = HighDensitySurfaceDark,
    onSurface = HighDensityOnSurfaceDark,
    surfaceVariant = HighDensitySurfaceVariantDark,
    onSurfaceVariant = HighDensityOnSurfaceVariantDark,
    outline = HighDensityOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    background = HighDensityBackground,
    onBackground = HighDensityOnBackground,
    surface = HighDensitySurface,
    onSurface = HighDensityOnSurface,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = HighDensityOnSurfaceVariant,
    outline = HighDensityOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
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

  CompositionLocalProvider(
    LocalTailwindSpacing provides TailwindSpacing(),
    LocalTailwindRadius provides TailwindRadius()
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
