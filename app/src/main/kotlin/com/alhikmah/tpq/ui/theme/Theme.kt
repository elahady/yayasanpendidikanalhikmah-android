package com.alhikmah.tpq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val AlHikmahShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/** Terang/gelap mengikuti pengaturan sistem (sama seperti web yang jatuh ke prefers-color-scheme). */
@Composable
fun AlHikmahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AlHikmahDarkScheme else AlHikmahLightScheme,
        typography = AlHikmahTypography,
        shapes = AlHikmahShapes,
        content = content,
    )
}
