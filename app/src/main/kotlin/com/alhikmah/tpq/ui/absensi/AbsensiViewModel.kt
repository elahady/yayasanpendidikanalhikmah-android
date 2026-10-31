package com.alhikmah.tpq.ui.absensi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.roster.Roster
import com.alhikmah.tpq.domain.roster.RosterPerson
import com.alhikmah.tpq.domain.roster.RosterRow
import com.alhikmah.tpq.domain.util.Jadwal
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.domain.util.attempt
import com.alhikmah.tpq.domain.util.toLoad
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AbsensiKind(val title: String, val menu: MenuId) {
    SANTRI("Absensi Santri", MenuId.ABSENSI_SANTRI),
    USTADZ("Absensi Ustadz", MenuId.ABSENSI_USTADZ),
}

/** Isi yang sedang diedit pada satu baris (belum tentu sudah disimpan). */
data class RowDraft(val hadir: Boolean, val keterangan: String)

/** Status simpan satu baris. [locked] = server menolak (mis. SELF_ATTENDANCE_FORBIDDEN), baris dikunci. */
data class RowStatus(val saving: Boolean = false, val error: String? = null, val locked: Boolean = false)

data class AbsensiUiState(
    val kind: AbsensiKind,
    val tanggal: LocalDate,
    /** Akses tulis menurut role (menu WRITE). Server tetap penentu akhir. */
    val canWrite: Boolean,
    val load: Load<List<RosterRow>> = Load.Loading,
    val drafts: Map<String, RowDraft> = emptyMap(),
    val status: Map<String, RowStatus> = emptyMap(),
    val query: String = "",
    /** Terisi bila satu ustadz disembunyikan karena tertaut ke akun ini. */
    val selfHidden: Boolean = false,
) {
    /**
     * Tanggal terpilih Sabtu/Minggu: TPQ libur, absensi tidak bisa dicatat (server menolak 422). Kontrol input dan
     * Simpan dikunci; data lama pada tanggal itu tetap terlihat.
     */
    val libur: Boolean get() = Jadwal.libur(tanggal)
}

/** Nilai yang tampil di baris: draft bila ada, selain itu isi entri tersimpan, selain itu bawaan (hadir). */
fun AbsensiUiState.effective(row: RosterRow): RowDraft =
    drafts[row.person.id] ?: RowDraft(row.entry?.hadir ?: true, row.entry?.keterangan.orEmpty())

/** Baris dengan entri hanya perlu Simpan bila berubah; baris yang belum dicatat selalu bisa disimpan. */
fun AbsensiUiState.needsSave(row: RosterRow): Boolean {
    val entry = row.entry ?: return true
    val draft = drafts[row.person.id] ?: return false
    return draft != RowDraft(entry.hadir, entry.keterangan.orEmpty())
}

fun AbsensiUiState.isEditable(row: RosterRow): Boolean =
    canWrite && !libur && row.bisaDiubah && status[row.person.id]?.locked != true

class AbsensiViewModel(
    private val repo: TpqRepository,
    private val user: User,
    private val kind: AbsensiKind,
    /** Tanggal awal layar (bawaan: hari ini, Asia/Jakarta); parameter agar tes tidak bergantung pada hari tes dijalankan. */
    hariIni: LocalDate = Tanggal.today(),
) : ViewModel() {

    private val hiddenId: String? = if (kind == AbsensiKind.USTADZ) Roster.hiddenUstadzId(user) else null

    private val _state = MutableStateFlow(
        AbsensiUiState(
            kind = kind,
            tanggal = hariIni,
            canWrite = MenuResolver.menuFor(user.roles).firstOrNull { it.id == kind.menu }?.access == MenuAccess.WRITE,
            selfHidden = hiddenId != null,
        ),
    )
    val state: StateFlow<AbsensiUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun setTanggal(date: LocalDate) {
        if (date == _state.value.tanggal) return
        _state.update { it.copy(tanggal = date) }
        load()
    }

    /** Dari tanggal libur pindah ke hari belajar terakhir (Sabtu/Minggu -> Jumat); tanggal hari belajar tidak berubah. */
    fun keHariBelajarTerakhir() = setTanggal(Jadwal.hariBelajarTerakhir(_state.value.tanggal))

    fun onQuery(query: String) = _state.update { it.copy(query = query) }

    fun load() {
        loadJob?.cancel()
        val date = _state.value.tanggal
        _state.update { it.copy(load = Load.Loading, drafts = emptyMap(), status = emptyMap()) }
        loadJob = viewModelScope.launch {
            val outcome = attempt {
                coroutineScope {
                    val people = async { people() }
                    val entries = async { entries(Tanggal.toApi(date)) }
                    Roster.merge(Roster.excluding(people.await(), hiddenId), entries.await())
                }
            }
            _state.update { it.copy(load = outcome.toLoad()) }
        }
    }

    fun setHadir(id: String, hadir: Boolean) = editDraft(id) { it.copy(hadir = hadir) }

    fun setKeterangan(id: String, text: String) = editDraft(id) { it.copy(keterangan = text) }

    private fun editDraft(id: String, change: (RowDraft) -> RowDraft) {
        _state.update { s ->
            val row = (s.load as? Load.Success)?.data?.firstOrNull { it.person.id == id } ?: return@update s
            s.copy(
                drafts = s.drafts + (id to change(s.effective(row))),
                status = s.status + (id to (s.status[id]?.copy(error = null) ?: RowStatus())),
            )
        }
    }

    fun save(id: String) {
        val snapshot = _state.value
        val row = (snapshot.load as? Load.Success)?.data?.firstOrNull { it.person.id == id } ?: return
        if (!snapshot.isEditable(row) || snapshot.status[id]?.saving == true) return
        val draft = snapshot.effective(row)
        val date = snapshot.tanggal
        _state.update { it.copy(status = it.status + (id to RowStatus(saving = true))) }
        viewModelScope.launch {
            val tanggal = Tanggal.toApi(date)
            val keterangan = draft.keterangan.trim().ifEmpty { null }
            val outcome = attempt {
                when (kind) {
                    AbsensiKind.SANTRI -> repo.simpanAbsensiSantri(id, tanggal, draft.hadir, keterangan)
                    AbsensiKind.USTADZ -> repo.simpanAbsensiUstadz(id, tanggal, draft.hadir, keterangan)
                }
            }
            _state.update { s ->
                if (s.tanggal != date) return@update s // pengguna sudah pindah tanggal
                when (outcome) {
                    is Outcome.Ok -> s.copy(
                        load = withEntry(s.load, id, outcome.value),
                        drafts = s.drafts - id,
                        status = s.status - id,
                    )
                    is Outcome.Err -> s.copy(
                        status = s.status + (id to RowStatus(error = outcome.error.message, locked = outcome.error.isSelfAttendance)),
                    )
                }
            }
        }
    }

    private fun withEntry(load: Load<List<RosterRow>>, id: String, entry: AbsensiEntry): Load<List<RosterRow>> =
        if (load is Load.Success) Load.Success(load.data.map { if (it.person.id == id) it.copy(entry = entry) else it }) else load

    private suspend fun people(): List<RosterPerson> = when (kind) {
        AbsensiKind.SANTRI -> repo.santri(null).map(Roster::person)
        AbsensiKind.USTADZ -> repo.ustadz().map(Roster::person)
    }

    private suspend fun entries(tanggal: String): List<AbsensiEntry> = when (kind) {
        AbsensiKind.SANTRI -> repo.absensiSantriByTanggal(tanggal)
        AbsensiKind.USTADZ -> repo.absensiUstadzByTanggal(tanggal)
    }
}
