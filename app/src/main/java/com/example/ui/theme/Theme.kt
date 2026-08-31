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
import com.example.model.AppThemeMode

private val GalleryDarkColorScheme = darkColorScheme(
    primary = GalleryBlueLight,
    onPrimary = Color(0xFF002E69),
    primaryContainer = GalleryPrimaryContainerDark,
    onPrimaryContainer = GalleryOnPrimaryContainerDark,
    secondary = VibrantAmberLight,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5A4400),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = GalleryBlueLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainer = DarkSurfaceContainer
)

private val GalleryAmoledColorScheme = darkColorScheme(
    primary = GalleryBlueLight,
    onPrimary = Color(0xFF002E69),
    primaryContainer = GalleryPrimaryContainerDark,
    onPrimaryContainer = GalleryOnPrimaryContainerDark,
    secondary = VibrantAmberLight,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5A4400),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = GalleryBlueLight,
    background = AmoledBackground,
    onBackground = Color(0xFFFFFFFF),
    surface = AmoledSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF2E3036),
    outlineVariant = Color(0xFF1E2024),
    surfaceContainer = AmoledSurfaceContainer
)

private val GalleryLightColorScheme = lightColorScheme(
    primary = GalleryBlue,
    onPrimary = Color.White,
    primaryContainer = GalleryPrimaryContainerLight,
    onPrimaryContainer = GalleryOnPrimaryContainerLight,
    secondary = VibrantAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Color(0xFF3B5BA9),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainer = LightSurfaceContainer
)

@Composable
fun GalleryProTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeMode == AppThemeMode.AMOLED -> GalleryAmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> GalleryDarkColorScheme
        else -> GalleryLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
