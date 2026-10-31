package com.alhikmah.tpq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.session.SessionState
import com.alhikmah.tpq.ui.common.BrandLogo
import com.alhikmah.tpq.ui.common.LoadingBox
import com.alhikmah.tpq.ui.login.LoginScreen
import kotlinx.coroutines.launch

/** Memilih layar menurut status sesi: periksa sesi, login, server tidak terjangkau, atau aplikasi utama. */
@Composable
fun AppRoot() {
    val session = LocalAppContainer.current.session
    val state by session.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            SessionState.Checking -> LoadingBox(label = "Memeriksa sesi...")
            is SessionState.LoggedOut -> LoginScreen(notice = s.notice)
            is SessionState.Unreachable -> Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                BrandLogo(96.dp)
                Text("Tidak bisa memeriksa sesi", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(s.error.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = { scope.launch { session.restore() } }) { Text("Coba lagi") }
                TextButton(onClick = session::logout) { Text("Keluar") }
            }
            is SessionState.LoggedIn -> MainNav(s.user, onLogout = session::logout)
        }
    }
}
