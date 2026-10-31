package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.beranda.BerandaResolver
import com.alhikmah.tpq.domain.beranda.BerandaVariant
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.Dashboard
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.common.BrandLogo
import com.alhikmah.tpq.ui.common.EmptyBox
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.common.roleLabel
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import com.alhikmah.tpq.ui.theme.statusColors

/**
 * Beranda per peran. Pemilih varian ada di [BerandaResolver] (domain): ADMIN/TU/PENGAWAS -> admin; PIKET/USTADZ -> tugas;
 * WALI murni -> wali; WALI + role lain -> varian role lain plus bagian anak.
 */
@Composable
fun BerandaScreen(user: User, onOpen: (MenuId) -> Unit, onOpenSantri: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: BerandaViewModel = viewModel(
        key = "beranda-${user.id}",
        factory = viewModelFactory { initializer { BerandaViewModel(container.repository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // Kembali ke Beranda (mis. dari layar absensi) memuat ulang angka; resume pertama dilewati karena VM sudah memuat sendiri.
    var resumePertama by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (resumePertama) resumePertama = false else vm.segarkan()
    }

    BerandaContent(
        user = user,
        state = state,
        menu = MenuResolver.menuFor(user.roles),
        onOpen = onOpen,
        onOpenSantri = onOpenSantri,
        onRefresh = vm::segarkan,
        onRetry = vm::muat,
        onPilihAnak = vm::pilihAnak,
        onTutupGalat = vm::tutupGalatSegar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaContent(
    user: User,
    state: BerandaUiState,
    menu: List<MenuEntry>,
    onOpen: (MenuId) -> Unit,
    onOpenSantri: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onPilihAnak: (String) -> Unit,
    onTutupGalat: () -> Unit,
) {
    val allowed = remember(menu) { menu.map { it.id }.toSet() }
    val variant = BerandaResolver.variantFor(user.roles)
    val dashboard = (state.load as? Load.Success)?.data
    val tanggal = Tanggal.label(Tanggal.fromApi(dashboard?.tanggal) ?: Tanggal.today())

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(onRefresh)
                Salam(user, tanggal, tampilkanRole = variant == BerandaVariant.TUGAS)
                when (val load = state.load) {
                    Load.Loading -> MemuatInline()
                    is Load.Failure -> GalatInline(load.error, onRetry)
                    is Load.Success -> {
                        state.refreshError?.let { GalatSegar(it, onTutupGalat) }
                        IsiDashboard(user, variant, load.data, menu, allowed, state.anakTerpilih, onOpen, onOpenSantri, onPilihAnak)
                    }
                }
            }
        }
    }
}

@Composable
private fun IsiDashboard(
    user: User,
    variant: BerandaVariant,
    data: Dashboard,
    menu: List<MenuEntry>,
    allowed: Set<MenuId>,
    anakTerpilih: String?,
    onOpen: (MenuId) -> Unit,
    onOpenSantri: (String) -> Unit,
    onPilihAnak: (String) -> Unit,
) {
    val bukaDetail: ((String) -> Unit)? = if (MenuId.ANAK_SAYA in allowed) onOpenSantri else null
    val ringkasan = data.ringkasan
    when (variant) {
        BerandaVariant.ADMIN ->
            if (ringkasan != null) AdminSections(ringkasan, menu, allowed, onOpen) else RingkasanKosong()
        BerandaVariant.TUGAS ->
            if (ringkasan != null) TugasSections(user, ringkasan, menu, allowed, onOpen) else RingkasanKosong()
        BerandaVariant.WALI -> AnakSection(data.anak.orEmpty(), anakTerpilih, onPilihAnak, bukaDetail)
        BerandaVariant.TANPA_AKSES -> Box(Modifier.height(240.dp)) {
            EmptyBox("Belum ada menu", "Akun Anda belum punya hak akses. Hubungi admin yayasan.")
        }
    }
    if (BerandaResolver.tampilkanBagianAnak(variant, data.anak?.size)) {
        Eyebrow("Anak saya", Modifier.padding(top = 8.dp))
        AnakSection(data.anak.orEmpty(), anakTerpilih, onPilihAnak, bukaDetail)
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BrandLogo(40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow("Yayasan Pendidikan")
            Text("Al-Hikmah", style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Muat ulang beranda")
        }
    }
}

@Composable
private fun Salam(user: User, tanggal: String, tampilkanRole: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Eyebrow(tanggal)
        Text("Assalamu'alaikum, ${user.nama}", style = MaterialTheme.typography.headlineMedium)
        if (tampilkanRole) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                user.roles.sortedBy { it.ordinal }.forEach { role ->
                    Pill(roleLabel(role), MaterialTheme.statusColors.hadirContainer, MaterialTheme.statusColors.onHadirContainer)
                }
            }
        }
    }
}

@Composable
private fun MemuatInline() {
    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Memuat beranda...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Galat muat pertama: pesan dari server/jaringan apa adanya + tombol Coba lagi. */
@Composable
private fun GalatInline(error: ApiError, onRetry: () -> Unit) {
    SectionCard {
        SectionTitle("Gagal memuat beranda")
        Text(error.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryAction("Coba lagi", onRetry)
    }
}

/** Muat ulang gagal tetapi data lama masih ditampilkan. */
@Composable
private fun GalatSegar(pesan: String, onTutup: () -> Unit) {
    val c = MaterialTheme.statusColors
    androidx.compose.material3.Surface(Modifier.fillMaxWidth(), color = c.tidakHadirContainer, contentColor = c.onTidakHadirContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Gagal memperbarui: $pesan", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onTutup) { Text("Tutup", color = c.onTidakHadirContainer) }
        }
    }
}

@Composable
private fun RingkasanKosong() {
    SectionCard {
        SectionTitle("Ringkasan belum tersedia")
        Text("Server tidak mengirim ringkasan untuk akun ini. Tarik layar ke bawah untuk memuat ulang.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Preview: tiap varian terang dan gelap, plus keadaan loading/galat.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PreviewBeranda(user: User, state: BerandaUiState, gelap: Boolean) {
    AlHikmahTheme(darkTheme = gelap) {
        BerandaContent(user, state, MenuResolver.menuFor(user.roles), {}, {}, {}, {}, {}, {})
    }
}

private fun sukses(d: Dashboard) = BerandaUiState(load = Load.Success(d))

@Preview(showBackground = true, heightDp = 1500, name = "Admin terang")
@Composable
private fun AdminTerangPreview() = PreviewBeranda(FakeData.userAdmin, sukses(FakeData.dashboardAdmin), gelap = false)

@Preview(showBackground = true, heightDp = 1500, name = "Pengawas gelap")
@Composable
private fun PengawasGelapPreview() = PreviewBeranda(FakeData.userPengawas, sukses(FakeData.dashboardAdmin), gelap = true)

@Preview(showBackground = true, heightDp = 1500, name = "Piket+Ustadz gelap")
@Composable
private fun PiketUstadzGelapPreview() = PreviewBeranda(FakeData.userPiketUstadz, sukses(FakeData.dashboardTugas), gelap = true)

@Preview(showBackground = true, heightDp = 1300, name = "Ustadz terang")
@Composable
private fun UstadzTerangPreview() = PreviewBeranda(FakeData.userUstadz, sukses(FakeData.dashboardTugas), gelap = false)

@Preview(showBackground = true, heightDp = 1500, name = "Wali terang")
@Composable
private fun WaliTerangPreview() = PreviewBeranda(FakeData.userWali, sukses(FakeData.dashboardWali), gelap = false)

@Preview(showBackground = true, heightDp = 1500, name = "Wali gelap")
@Composable
private fun WaliGelapPreview() = PreviewBeranda(FakeData.userWali, sukses(FakeData.dashboardWali), gelap = true)

@Preview(showBackground = true, heightDp = 2200, name = "Ustadz + Wali")
@Composable
private fun UstadzWaliPreview() = PreviewBeranda(
    FakeData.userWaliUstadz,
    sukses(FakeData.dashboardTugas.copy(anak = listOf(FakeData.anakAisyah))),
    gelap = false,
)

@Preview(showBackground = true, name = "Memuat")
@Composable
private fun MemuatPreview() = PreviewBeranda(FakeData.userAdmin, BerandaUiState(), gelap = false)

@Preview(showBackground = true, name = "Galat gelap")
@Composable
private fun GalatPreview() = PreviewBeranda(
    FakeData.userAdmin,
    BerandaUiState(load = Load.Failure(ApiError.network())),
    gelap = true,
)
