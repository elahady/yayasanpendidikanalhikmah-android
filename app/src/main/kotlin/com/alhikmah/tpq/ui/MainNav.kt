package com.alhikmah.tpq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alhikmah.tpq.domain.menu.BottomTabs
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.menu.TabId
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.ui.absensi.AbsensiKind
import com.alhikmah.tpq.ui.absensi.AbsensiScreen
import com.alhikmah.tpq.ui.beranda.BerandaScreen
import com.alhikmah.tpq.ui.common.EmptyBox
import com.alhikmah.tpq.ui.common.MenuListCard
import com.alhikmah.tpq.ui.common.TpqTopBar
import com.alhikmah.tpq.ui.profil.ProfilScreen
import com.alhikmah.tpq.ui.rekap.RekapScreen
import com.alhikmah.tpq.ui.santri.SantriDetailScreen
import com.alhikmah.tpq.ui.santri.SantriListScreen

private object Routes {
    const val BERANDA = "beranda"
    const val ABSENSI = "absensi"
    const val DAFTAR_SANTRI = "santri"
    const val ANAK_SAYA = "anak-saya"
    const val PROFIL = "profil"
    const val REKAP = "rekap"
    const val ABSENSI_SANTRI = "absensi-santri"
    const val ABSENSI_USTADZ = "absensi-ustadz"
    const val DETAIL = "santri/{id}"

    fun detail(id: String) = "santri/${android.net.Uri.encode(id)}"
}

private fun TabId.route(): String = when (this) {
    TabId.BERANDA -> Routes.BERANDA
    TabId.ABSENSI -> Routes.ABSENSI
    TabId.SANTRI -> Routes.DAFTAR_SANTRI
    TabId.ANAK_SAYA -> Routes.ANAK_SAYA
    TabId.PROFIL -> Routes.PROFIL
}

private fun TabId.label(): String = when (this) {
    TabId.BERANDA -> "Beranda"
    TabId.ABSENSI -> "Absensi"
    TabId.SANTRI -> "Santri"
    TabId.ANAK_SAYA -> "Anak saya"
    TabId.PROFIL -> "Profil"
}

private fun TabId.icon(): ImageVector = when (this) {
    TabId.BERANDA -> Icons.Default.Home
    TabId.ABSENSI -> Icons.Default.CheckCircle
    TabId.SANTRI -> Icons.AutoMirrored.Filled.List
    TabId.ANAK_SAYA -> Icons.Default.Face
    TabId.PROFIL -> Icons.Default.Person
}

/**
 * Aplikasi utama setelah login: navigasi bawah (tab) yang diturunkan dari `MenuResolver` lewat [BottomTabs], sehingga
 * tab/layar yang tidak boleh diakses role tidak muncul. Bilah bawah hanya tampil di layar tab; layar dalam (rekap,
 * absensi, detail santri) memakai tombol kembali.
 */
@Composable
internal fun MainNav(user: User, onLogout: () -> Unit) {
    val nav: NavHostController = rememberNavController()
    val menu = MenuResolver.menuFor(user.roles)
    val allowed = menu.map { it.id }.toSet()
    val tabs = BottomTabs.tabsFor(user.roles)
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tampilkanBilah = tabs.any { it.route() == route }
    val back: () -> Unit = { nav.popBackStack() }

    fun bukaTab(tab: TabId) {
        nav.navigate(tab.route()) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun bukaMenu(id: MenuId) {
        when (id) {
            MenuId.DAFTAR_SANTRI -> bukaTab(TabId.SANTRI)
            MenuId.ANAK_SAYA -> bukaTab(TabId.ANAK_SAYA)
            MenuId.REKAP_HARIAN -> nav.navigate(Routes.REKAP)
            MenuId.ABSENSI_USTADZ -> nav.navigate(Routes.ABSENSI_USTADZ)
            MenuId.ABSENSI_SANTRI -> nav.navigate(Routes.ABSENSI_SANTRI)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Inset ditangani layar dalam dan NavigationBar sendiri; hindari padding ganda antar-Scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tampilkanBilah) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.route() == route,
                            onClick = { bukaTab(tab) },
                            icon = { Icon(tab.icon(), contentDescription = null) },
                            label = { Text(tab.label(), maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bawah = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(nav, startDestination = Routes.BERANDA, modifier = Modifier.padding(bawah).consumeWindowInsets(bawah)) {
            composable(Routes.BERANDA) {
                BerandaScreen(user, onOpen = ::bukaMenu, onOpenSantri = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.PROFIL) { ProfilScreen(user, onLogout) }
            // Layar hanya dipasang bila role-nya berhak; kalau tidak, rute tidak ada dan navigasi tidak terjadi.
            if (TabId.ABSENSI in tabs) {
                composable(Routes.ABSENSI) { AbsensiHub(BottomTabs.absensiMenu(user.roles), onOpen = ::bukaMenu) }
            }
            if (MenuId.REKAP_HARIAN in allowed) composable(Routes.REKAP) { RekapScreen(onBack = back) }
            if (MenuId.ABSENSI_SANTRI in allowed) composable(Routes.ABSENSI_SANTRI) { AbsensiScreen(AbsensiKind.SANTRI, user, back) }
            if (MenuId.ABSENSI_USTADZ in allowed) composable(Routes.ABSENSI_USTADZ) { AbsensiScreen(AbsensiKind.USTADZ, user, back) }
            if (MenuId.DAFTAR_SANTRI in allowed) {
                composable(Routes.DAFTAR_SANTRI) {
                    SantriListScreen("Daftar Santri", searchable = true, onBack = null, onOpen = { nav.navigate(Routes.detail(it)) })
                }
            }
            if (MenuId.ANAK_SAYA in allowed) {
                composable(Routes.ANAK_SAYA) {
                    SantriListScreen("Anak Saya", searchable = false, onBack = null, onOpen = { nav.navigate(Routes.detail(it)) })
                }
            }
            if (MenuId.DAFTAR_SANTRI in allowed || MenuId.ANAK_SAYA in allowed) {
                composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    SantriDetailScreen(entry.arguments?.getString("id").orEmpty(), user, back)
                }
            }
        }
    }
}

/** Tab Absensi: pilihan Absensi Santri / Absensi Ustadz / Rekap Harian yang boleh dibuka role ini. */
@Composable
internal fun AbsensiHub(menu: List<MenuEntry>, onOpen: (MenuId) -> Unit) {
    Scaffold(topBar = { TpqTopBar("Absensi") }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (menu.isEmpty()) {
            EmptyBox("Belum ada menu absensi", "Akun Anda belum punya hak akses absensi.", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(menu, key = { it.id }) { entry -> MenuListCard(entry, onClick = { onOpen(entry.id) }) }
            }
        }
    }
}
