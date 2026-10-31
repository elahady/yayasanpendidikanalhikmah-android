package com.alhikmah.tpq.ui.absensi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.roster.Roster
import com.alhikmah.tpq.domain.roster.RosterRow
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.ui.common.DateBar
import com.alhikmah.tpq.ui.common.EmptyBox
import com.alhikmah.tpq.ui.common.LoadContent
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.common.TpqTopBar
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import java.time.LocalDate

@Composable
fun AbsensiScreen(kind: AbsensiKind, user: User, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AbsensiViewModel = viewModel(
        key = "absensi-${kind.name}",
        factory = viewModelFactory { initializer { AbsensiViewModel(container.repository, user, kind) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    AbsensiContent(
        state = state,
        onBack = onBack,
        onTanggal = vm::setTanggal,
        onRetry = vm::load,
        onQuery = vm::onQuery,
        onHadir = vm::setHadir,
        onKeterangan = vm::setKeterangan,
        onSave = vm::save,
    )
}

@Composable
fun AbsensiContent(
    state: AbsensiUiState,
    onBack: () -> Unit,
    onTanggal: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onQuery: (String) -> Unit,
    onHadir: (String, Boolean) -> Unit,
    onKeterangan: (String, String) -> Unit,
    onSave: (String) -> Unit,
) {
    Scaffold(
        topBar = { TpqTopBar(state.kind.title, onBack, subtitle = if (state.canWrite) null else "Hanya lihat") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            DateBar(state.tanggal, onTanggal, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LoadContent(state.load, onRetry) { rows ->
                RosterList(state, rows, onQuery, onHadir, onKeterangan, onSave)
            }
        }
    }
}

@Composable
private fun RosterList(
    state: AbsensiUiState,
    rows: List<RosterRow>,
    onQuery: (String) -> Unit,
    onHadir: (String, Boolean) -> Unit,
    onKeterangan: (String, String) -> Unit,
    onSave: (String) -> Unit,
) {
    val summary = Roster.summarize(rows)
    val visible = if (state.query.isBlank()) rows else rows.filter { it.person.nama.contains(state.query.trim(), ignoreCase = true) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Hadir ${summary.hadir}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    Pill("Tidak hadir ${summary.tidakHadir}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    Pill("Belum dicatat ${summary.belumDicatat}", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!state.canWrite) Note("Anda hanya dapat melihat absensi ini.")
                if (state.selfHidden) Note("Nama Anda tidak ditampilkan: ustadz tidak boleh mengisi absensi atas nama diri sendiri.")
                if (state.kind == AbsensiKind.SANTRI && rows.size > 8) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cari nama santri") },
                        singleLine = true,
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyBox(
                    if (rows.isEmpty()) "Belum ada data" else "Tidak ditemukan",
                    if (rows.isEmpty()) "Daftar ${if (state.kind == AbsensiKind.SANTRI) "santri" else "ustadz"} masih kosong." else "Tidak ada nama yang cocok dengan \"${state.query}\".",
                    Modifier.padding(vertical = 40.dp),
                )
            }
        }
        items(visible, key = { it.person.id }) { row ->
            RosterRowCard(state, row, onHadir, onKeterangan, onSave)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RosterRowCard(
    state: AbsensiUiState,
    row: RosterRow,
    onHadir: (String, Boolean) -> Unit,
    onKeterangan: (String, String) -> Unit,
    onSave: (String) -> Unit,
) {
    val id = row.person.id
    val draft = state.effective(row)
    val status = state.status[id]
    val saving = status?.saving == true
    val editable = state.isEditable(row)
    val entry = row.entry

    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.person.nama, style = MaterialTheme.typography.titleMedium)
                    row.person.keterangan?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                when {
                    entry == null -> Pill("Belum dicatat", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                    entry.hadir -> Pill("Hadir", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    else -> Pill("Tidak hadir", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = draft.hadir, onCheckedChange = { onHadir(id, it) }, enabled = !saving)
                    Text(
                        if (draft.hadir) "Hadir" else "Tidak hadir",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                OutlinedTextField(
                    value = draft.keterangan,
                    onValueChange = { onKeterangan(id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Keterangan (opsional)") },
                    singleLine = true,
                    enabled = !saving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                val dirty = state.needsSave(row)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onSave(id) }, enabled = dirty && !saving) {
                        if (saving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Text("  Menyimpan...")
                        } else {
                            Text(if (entry == null) "Simpan" else "Simpan perubahan")
                        }
                    }
                    if (entry != null && dirty && !saving) {
                        Text("  Belum disimpan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                if (entry?.keterangan != null) Text("Keterangan: ${entry.keterangan}", style = MaterialTheme.typography.bodyMedium)
                if (state.canWrite && status?.locked != true && entry != null) {
                    Note(
                        if (entry.dicatatOleh == null) {
                            "Data lama tanpa pencatat; hanya admin/TU yang bisa mengubahnya."
                        } else {
                            "Terakhir disimpan oleh ${entry.dicatatOleh}; tidak bisa Anda ubah."
                        },
                    )
                }
            }

            status?.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

// ---- Preview ----

private val previewTanggal = LocalDate.of(2026, 11, 2)

private fun previewState(canWrite: Boolean, kind: AbsensiKind = AbsensiKind.SANTRI): AbsensiUiState {
    val tgl = "2026-11-02"
    val rows = if (kind == AbsensiKind.SANTRI) {
        Roster.merge(
            FakeData.santri.map(Roster::person),
            listOf(
                FakeData.entry("s1", tgl, hadir = true),
                FakeData.entry("s2", tgl, hadir = false, keterangan = "Sakit demam", bisaDiubah = false),
            ),
        )
    } else {
        Roster.merge(FakeData.ustadz.map(Roster::person), listOf(FakeData.entry("u1", tgl, hadir = true)))
    }
    return AbsensiUiState(kind, previewTanggal, canWrite, Load.Success(rows))
}

@Preview(showBackground = true, heightDp = 900, name = "Absensi santri (tulis)")
@Composable
private fun AbsensiSantriPreview() {
    AlHikmahTheme(darkTheme = false) { AbsensiContent(previewState(true), {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}) }
}

@Preview(showBackground = true, heightDp = 900, name = "Absensi ustadz (lihat, gelap)")
@Composable
private fun AbsensiUstadzReadOnlyPreview() {
    AlHikmahTheme(darkTheme = true) { AbsensiContent(previewState(false, AbsensiKind.USTADZ), {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}) }
}

@Preview(showBackground = true, name = "Absensi memuat")
@Composable
private fun AbsensiLoadingPreview() {
    AlHikmahTheme(darkTheme = false) { AbsensiContent(AbsensiUiState(AbsensiKind.SANTRI, previewTanggal, true), {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}) }
}
