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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.session.SessionState
import com.alhikmah.tpq.ui.absensi.AbsensiKind
import com.alhikmah.tpq.ui.absensi.AbsensiScreen
import com.alhikmah.tpq.ui.beranda.BerandaScreen
import com.alhikmah.tpq.ui.common.BrandLogo
import com.alhikmah.tpq.ui.common.LoadingBox
import com.alhikmah.tpq.ui.login.LoginScreen
import com.alhikmah.tpq.ui.rekap.RekapScreen
import com.alhikmah.tpq.ui.santri.SantriDetailScreen
import com.alhikmah.tpq.ui.santri.SantriListScreen
import kotlinx.coroutines.launch

private object Routes {
    const val BERANDA = "beranda"
    const val REKAP = "rekap"
    const val ABSENSI_SANTRI = "absensi-santri"
    const val ABSENSI_USTADZ = "absensi-ustadz"
    const val DAFTAR_SANTRI = "santri"
    const val ANAK_SAYA = "anak-saya"
    const val DETAIL = "santri/{id}"

    fun detail(id: String) = "santri/${android.net.Uri.encode(id)}"
}

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

@Composable
private fun MainNav(user: User, onLogout: () -> Unit) {
    val nav: NavHostController = rememberNavController()
    val allowed = MenuResolver.menuFor(user.roles).map { it.id }.toSet()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(nav, startDestination = Routes.BERANDA) {
        composable(Routes.BERANDA) {
            BerandaScreen(user, onLogout = onLogout, onOpen = { id ->
                nav.navigate(
                    when (id) {
                        MenuId.REKAP_HARIAN -> Routes.REKAP
                        MenuId.ABSENSI_USTADZ -> Routes.ABSENSI_USTADZ
                        MenuId.ABSENSI_SANTRI -> Routes.ABSENSI_SANTRI
                        MenuId.DAFTAR_SANTRI -> Routes.DAFTAR_SANTRI
                        MenuId.ANAK_SAYA -> Routes.ANAK_SAYA
                    },
                )
            })
        }
        // Layar hanya dipasang bila role-nya berhak; kalau tidak, rute tidak ada dan navigasi tidak terjadi.
        if (MenuId.REKAP_HARIAN in allowed) composable(Routes.REKAP) { RekapScreen(onBack = back) }
        if (MenuId.ABSENSI_SANTRI in allowed) composable(Routes.ABSENSI_SANTRI) { AbsensiScreen(AbsensiKind.SANTRI, user, back) }
        if (MenuId.ABSENSI_USTADZ in allowed) composable(Routes.ABSENSI_USTADZ) { AbsensiScreen(AbsensiKind.USTADZ, user, back) }
        if (MenuId.DAFTAR_SANTRI in allowed) {
            composable(Routes.DAFTAR_SANTRI) {
                SantriListScreen("Daftar Santri", searchable = true, onBack = back, onOpen = { nav.navigate(Routes.detail(it)) })
            }
        }
        if (MenuId.ANAK_SAYA in allowed) {
            composable(Routes.ANAK_SAYA) {
                SantriListScreen("Anak Saya", searchable = false, onBack = back, onOpen = { nav.navigate(Routes.detail(it)) })
            }
        }
        if (MenuId.DAFTAR_SANTRI in allowed || MenuId.ANAK_SAYA in allowed) {
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                SantriDetailScreen(entry.arguments?.getString("id").orEmpty(), user, back)
            }
        }
    }
}
