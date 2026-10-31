package com.alhikmah.tpq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Peran warna tambahan di luar skema Material untuk status kehadiran dan aksen grafik (token mockup `--okbg/--ok`,
 * `--nobg/--no`, `--accent`). Selalu dipasangkan dengan teks/bentuk (status tidak boleh hanya lewat warna).
 * "Hadir" memakai `colorScheme.primary` untuk kotak/batang; di sini hanya pill dan aksen.
 */
@Immutable
data class StatusColors(
    val hadirContainer: Color,
    val onHadirContainer: Color,
    val tidakHadirContainer: Color,
    val onTidakHadirContainer: Color,
    /** Terracotta untuk kotak "tidak hadir" dan batang Al-Qur'an pada grafik. */
    val aksen: Color,
    /** Teks/ikon di atas [aksen] (mis. inisial pada avatar). */
    val onAksen: Color,
)

internal val LightStatusColors = StatusColors(
    hadirContainer = Color(0xFFE2E9D6),
    onHadirContainer = Color(0xFF333F2C),
    tidakHadirContainer = Color(0xFFF7DFD0),
    onTidakHadirContainer = Color(0xFF8F3F18),
    aksen = Color(0xFFC1693D),
    onAksen = Color(0xFF1C1A15),
)

internal val DarkStatusColors = StatusColors(
    hadirContainer = Color(0xFF36442C),
    onHadirContainer = Color(0xFFB4C99E),
    tidakHadirContainer = Color(0xFF5A2D17),
    onTidakHadirContainer = Color(0xFFFFDCC8),
    aksen = Color(0xFFE08552),
    onAksen = Color(0xFF1C1A15),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
