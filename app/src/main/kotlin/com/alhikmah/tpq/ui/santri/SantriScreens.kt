package com.alhikmah.tpq.ui.santri

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.common.EmptyBox
import com.alhikmah.tpq.ui.common.ErrorBox
import com.alhikmah.tpq.ui.common.LoadContent
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.common.TpqTopBar
import com.alhikmah.tpq.ui.theme.AlHikmahTheme

// ---------------------------------------------------------------------------------------------
// Daftar santri (juga dipakai "Anak Saya")
// ---------------------------------------------------------------------------------------------

@Composable
fun SantriListScreen(title: String, searchable: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SantriListViewModel = viewModel(
        key = "santri-list-$searchable",
        factory = viewModelFactory { initializer { SantriListViewModel(container.repository, searchable) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    SantriListContent(title, searchable, state, onBack, vm::onQuery, vm::retry, onOpen)
}

@Composable
fun SantriListContent(
    title: String,
    searchable: Boolean,
    state: SantriListUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Scaffold(topBar = { TpqTopBar(title, onBack) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (searchable) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text("Cari nama santri") },
                    singleLine = true,
                )
            }
            LoadContent(state.load, onRetry) { list ->
                if (list.isEmpty()) {
                    EmptyBox(
                        if (state.query.isBlank()) "Belum ada santri" else "Tidak ditemukan",
                        if (state.query.isBlank()) {
                            if (searchable) "Data santri belum ditambahkan oleh admin." else "Belum ada anak yang tertaut ke akun Anda. Hubungi admin yayasan."
                        } else {
                            "Tidak ada santri yang cocok dengan \"${state.query}\"."
                        },
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { santri -> SantriCard(santri, onClick = { onOpen(santri.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SantriCard(santri: Santri, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(santri.nama, style = MaterialTheme.typography.titleMedium)
                if (santri.keteranganKelas.isNotEmpty()) {
                    Text(santri.keteranganKelas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            santri.jilidSaatIni?.let {
                Pill(it.label, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detail santri: progress (bisa diubah ADMIN/TU/USTADZ) dan riwayat absensi 30 hari terakhir
// ---------------------------------------------------------------------------------------------

@Composable
fun SantriDetailScreen(santriId: String, user: User, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val canEdit = MenuResolver.canEditProgress(user.roles)
    val vm: SantriDetailViewModel = viewModel(
        key = "santri-detail-$santriId",
        factory = viewModelFactory { initializer { SantriDetailViewModel(container.repository, santriId, canEdit, MenuResolver.canViewAbsensiSantri(user.roles)) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    SantriDetailContent(state, onBack, vm::load, vm::onJilid, vm::onCatatan, vm::save)
}

@Composable
fun SantriDetailContent(
    state: SantriDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onJilid: (Jilid) -> Unit,
    onCatatan: (String) -> Unit,
    onSave: () -> Unit,
) {
    val title = (state.santri as? Load.Success)?.data?.nama ?: "Detail Santri"
    Scaffold(topBar = { TpqTopBar(title, onBack) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LoadContent(state.santri, onRetry) { santri ->
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IdentitasCard(santri)
                    ProgressCard(state, onJilid, onCatatan, onSave)
                    if (state.canViewRiwayat) RiwayatCard(state.riwayat, onRetry)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun IdentitasCard(santri: Santri) {
    SectionCard("Identitas") {
        InfoRow("Nama", santri.nama)
        InfoRow("Jenjang Madin", santri.jenjang?.label ?: "-")
        InfoRow("Kelas", santri.kelas ?: "-")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressCard(state: SantriDetailUiState, onJilid: (Jilid) -> Unit, onCatatan: (String) -> Unit, onSave: () -> Unit) {
    SectionCard("Progress belajar") {
        Text("Jilid / Al-Qur'an (TPQ)", style = MaterialTheme.typography.titleSmall)
        if (state.canEdit) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Jilid.entries.forEach { jilid ->
                    FilterChip(
                        selected = state.jilidSelected == jilid,
                        onClick = { onJilid(jilid) },
                        enabled = !state.saving,
                        label = { Text(jilid.label) },
                    )
                }
            }
        } else {
            Text(state.jilidSelected?.label ?: "Belum dicatat", style = MaterialTheme.typography.bodyLarge)
        }

        Text("Catatan progress Madin", style = MaterialTheme.typography.titleSmall)
        if (state.canEdit) {
            OutlinedTextField(
                value = state.catatanText,
                onValueChange = onCatatan,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contoh: Fiqih bab wudhu") },
                minLines = 2,
                enabled = !state.saving,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSave, enabled = state.hasChanges && !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Text("  Menyimpan...")
                    } else {
                        Text("Simpan progress")
                    }
                }
                if (state.saved && !state.hasChanges) Text("  Tersimpan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            state.saveError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        } else {
            Text(state.catatanText.ifBlank { "Belum ada catatan." }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RiwayatCard(riwayat: Load<List<AbsensiEntry>>, onRetry: () -> Unit) {
    SectionCard("Riwayat kehadiran ($RIWAYAT_HARI hari terakhir)") {
        when (riwayat) {
            Load.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Memuat riwayat...", style = MaterialTheme.typography.bodyMedium)
            }
            is Load.Failure -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(riwayat.error.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Coba lagi") }
            }
            is Load.Success -> {
                val entries = riwayat.data
                if (entries.isEmpty()) {
                    Text("Belum ada catatan kehadiran pada periode ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        "Hadir ${entries.count { it.hadir }} dari ${entries.size} hari tercatat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    entries.forEach { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val date = Tanggal.fromApi(entry.tanggal)
                                Text(date?.let(Tanggal::label) ?: entry.tanggal, style = MaterialTheme.typography.bodyMedium)
                                entry.keterangan?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            if (entry.hadir) {
                                Pill("Hadir", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            } else {
                                Pill("Tidak hadir", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Preview ----

@Preview(showBackground = true, name = "Daftar santri")
@Composable
private fun SantriListPreview() {
    AlHikmahTheme(darkTheme = false) {
        SantriListContent("Daftar Santri", true, SantriListUiState(load = Load.Success(FakeData.santri)), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Daftar santri kosong gelap")
@Composable
private fun SantriListEmptyPreview() {
    AlHikmahTheme(darkTheme = true) {
        SantriListContent("Anak Saya", false, SantriListUiState(load = Load.Success(emptyList())), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Daftar santri error")
@Composable
private fun SantriListErrorPreview() {
    AlHikmahTheme(darkTheme = false) {
        Surface { ErrorBox(com.alhikmah.tpq.domain.api.ApiError.network(), onRetry = {}) }
    }
}

private fun detailPreview(canEdit: Boolean) = SantriDetailUiState(
    canEdit = canEdit,
    santri = Load.Success(FakeData.santri[0]),
    riwayat = Load.Success(
        listOf(
            FakeData.entry("s1", "2026-11-02", true),
            FakeData.entry("s1", "2026-10-30", false, "Sakit demam"),
            FakeData.entry("s1", "2026-10-29", true),
        ),
    ),
)

@Preview(showBackground = true, heightDp = 1100, name = "Detail santri (ustadz)")
@Composable
private fun SantriDetailPreview() {
    AlHikmahTheme(darkTheme = false) { SantriDetailContent(detailPreview(true), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 1100, name = "Detail santri (wali, gelap)")
@Composable
private fun SantriDetailWaliPreview() {
    AlHikmahTheme(darkTheme = true) { SantriDetailContent(detailPreview(false), {}, {}, {}, {}, {}) }
}
