package com.alhikmah.tpq.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Identitas brand dari app/globals.css repo web (hijau olive = primary, terracotta = aksen).
 * Token web -> Material 3:
 *   --color-bg -> background/surface, --color-bg-elevated -> surfaceContainer (kartu), --color-bg-muted -> surfaceContainerHigh,
 *   --color-ink -> onSurface, --color-ink-muted -> onSurfaceVariant, --color-ink-subtle -> outline,
 *   --color-primary -> primary, --color-link (terracotta lebih gelap, aman kontras untuk teks kecil) -> secondary (terang),
 *   --color-accent (dark) -> secondary (gelap).
 * Ubah token di web lebih dulu, lalu samakan di sini.
 */

internal val AlHikmahLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF333F2C),
    onPrimary = Color(0xFFFAF6EC),
    primaryContainer = Color(0xFFE2E9D6),
    onPrimaryContainer = Color(0xFF1F2819),
    secondary = Color(0xFFA8552A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7DFD0),
    onSecondaryContainer = Color(0xFF4A2210),
    tertiary = Color(0xFF5B6B4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE7CC),
    onTertiaryContainer = Color(0xFF1B2411),
    background = Color(0xFFFAF6EC),
    onBackground = Color(0xFF231E19),
    surface = Color(0xFFFAF6EC),
    onSurface = Color(0xFF231E19),
    surfaceVariant = Color(0xFFF3EDDE),
    onSurfaceVariant = Color(0xFF5B5349),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFBF5),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF3EDDE),
    surfaceContainerHighest = Color(0xFFEBE3CF),
    inverseSurface = Color(0xFF2A3424),
    inverseOnSurface = Color(0xFFFAF6EC),
    inversePrimary = Color(0xFF8AA374),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF7A7266),
    outlineVariant = Color(0xFFD9D2C2),
)

internal val AlHikmahDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8AA374),
    onPrimary = Color(0xFF1C1A15),
    primaryContainer = Color(0xFF36442C),
    onPrimaryContainer = Color(0xFFDCE8CF),
    secondary = Color(0xFFE08552),
    onSecondary = Color(0xFF1C1A15),
    secondaryContainer = Color(0xFF5A2D17),
    onSecondaryContainer = Color(0xFFFFDCC8),
    tertiary = Color(0xFFB4C99E),
    onTertiary = Color(0xFF1F2A15),
    tertiaryContainer = Color(0xFF3A4A2D),
    onTertiaryContainer = Color(0xFFDCE8CF),
    background = Color(0xFF1C1A15),
    onBackground = Color(0xFFF2EDE1),
    surface = Color(0xFF1C1A15),
    onSurface = Color(0xFFF2EDE1),
    surfaceVariant = Color(0xFF2E2A20),
    onSurfaceVariant = Color(0xFFC9C1AF),
    surfaceContainerLowest = Color(0xFF15130F),
    surfaceContainerLow = Color(0xFF211E17),
    surfaceContainer = Color(0xFF262219),
    surfaceContainerHigh = Color(0xFF2E2A20),
    surfaceContainerHighest = Color(0xFF38332A),
    inverseSurface = Color(0xFFF2EDE1),
    inverseOnSurface = Color(0xFF231E19),
    inversePrimary = Color(0xFF333F2C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8F8775),
    outlineVariant = Color(0xFF3D382C),
)
