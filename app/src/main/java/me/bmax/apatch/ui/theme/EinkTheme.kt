package me.bmax.apatch.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val black = Color(0xFF000000)
private val white = Color(0xFFFFFFFF)
private val lightGray = Color(0xFFEEEEEE)
private val midGray = Color(0xFF999999)
private val darkGray = Color(0xFF444444)

val EinkLightScheme = lightColorScheme(
    primary = black,
    onPrimary = white,
    primaryContainer = lightGray,
    onPrimaryContainer = black,
    secondary = darkGray,
    onSecondary = white,
    secondaryContainer = lightGray,
    onSecondaryContainer = black,
    tertiary = darkGray,
    onTertiary = white,
    tertiaryContainer = lightGray,
    onTertiaryContainer = black,
    error = black,
    onError = white,
    errorContainer = lightGray,
    onErrorContainer = black,
    background = white,
    onBackground = black,
    surface = white,
    onSurface = black,
    surfaceVariant = lightGray,
    onSurfaceVariant = darkGray,
    outline = midGray,
    inverseOnSurface = white,
    inverseSurface = black,
    inversePrimary = white,
    surfaceTint = black,
    outlineVariant = midGray,
    scrim = black,
)

val EinkDarkScheme = darkColorScheme(
    primary = white,
    onPrimary = black,
    primaryContainer = darkGray,
    onPrimaryContainer = white,
    secondary = midGray,
    onSecondary = black,
    secondaryContainer = darkGray,
    onSecondaryContainer = white,
    tertiary = midGray,
    onTertiary = black,
    tertiaryContainer = darkGray,
    onTertiaryContainer = white,
    error = white,
    onError = black,
    errorContainer = darkGray,
    onErrorContainer = white,
    background = black,
    onBackground = white,
    surface = black,
    onSurface = white,
    surfaceVariant = darkGray,
    onSurfaceVariant = midGray,
    outline = midGray,
    inverseOnSurface = black,
    inverseSurface = white,
    inversePrimary = black,
    surfaceTint = white,
    outlineVariant = darkGray,
    scrim = white,
)
