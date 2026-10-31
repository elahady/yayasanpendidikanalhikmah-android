package com.alhikmah.tpq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

val AlHikmahShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/**
 * [darkTheme] ditentukan pemanggil: di app berasal dari pilihan pengguna (Ikuti sistem / Terang / Gelap, lihat
 * `ThemeSettings`); bawaan mengikuti pengaturan sistem (sama seperti web yang jatuh ke prefers-color-scheme).
 */
@Composable
fun AlHikmahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) AlHikmahDarkScheme else AlHikmahLightScheme,
            typography = AlHikmahTypography,
            shapes = AlHikmahShapes,
            content = content,
        )
    }
}
