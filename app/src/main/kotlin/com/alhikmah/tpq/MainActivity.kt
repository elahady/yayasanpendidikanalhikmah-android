package com.alhikmah.tpq

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.alhikmah.tpq.domain.session.SessionState
import com.alhikmah.tpq.ui.AppRoot
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import kotlinx.coroutines.launch

/**
 * Titik masuk. Splash sistem tetap tampil selama sesi diperiksa (GET /me), lalu AppRoot memilih
 * layar login atau beranda berdasarkan [SessionState]. Tema (Ikuti sistem / Terang / Gelap) dari [ThemeSettings]
 * diterapkan ke Compose dan ke ikon status bar/navigation bar.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AlHikmahApplication).container
        splash.setKeepOnScreenCondition { container.session.state.value is SessionState.Checking }

        // Cek sesi hanya selama status masih Checking (proses baru, atau pemeriksaan terputus karena layar diputar).
        if (container.session.state.value is SessionState.Checking) {
            lifecycleScope.launch { container.session.restore() }
        }

        setContent {
            val mode by container.themeSettings.mode.collectAsStateWithLifecycle()
            val gelap = mode.gelap(isSystemInDarkTheme())

            // enableEdgeToEdge() bawaan hanya mengikuti tema sistem; saat pilihan pengguna berbeda dari sistem,
            // warna ikon status bar/navigation bar harus ikut pilihan itu agar tetap terbaca.
            DisposableEffect(gelap) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { gelap },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { gelap },
                )
                onDispose { }
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                AlHikmahTheme(darkTheme = gelap) {
                    AppRoot()
                }
            }
        }
    }

    private companion object {
        // Sama dengan nilai bawaan enableEdgeToEdge (scrim hanya dipakai pada navigasi 3 tombol).
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
