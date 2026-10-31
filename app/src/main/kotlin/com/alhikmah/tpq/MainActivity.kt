package com.alhikmah.tpq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.alhikmah.tpq.domain.session.SessionState
import com.alhikmah.tpq.ui.AppRoot
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import kotlinx.coroutines.launch

/**
 * Titik masuk. Splash sistem tetap tampil selama sesi diperiksa (GET /me), lalu AppRoot memilih
 * layar login atau beranda berdasarkan [SessionState].
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
            CompositionLocalProvider(LocalAppContainer provides container) {
                AlHikmahTheme {
                    AppRoot()
                }
            }
        }
    }
}
