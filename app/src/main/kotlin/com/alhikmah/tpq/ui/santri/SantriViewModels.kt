package com.alhikmah.tpq.ui.santri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.ProgressUpdate
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.domain.util.attempt
import com.alhikmah.tpq.domain.util.toLoad
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SantriListUiState(
    val query: String = "",
    val load: Load<List<Santri>> = Load.Loading,
)

/** Daftar santri; dipakai juga untuk "Anak Saya" (server sudah memfilter anak milik wali, pencarian dimatikan). */
class SantriListViewModel(private val repo: TpqRepository, val searchable: Boolean) : ViewModel() {
    private val _state = MutableStateFlow(SantriListUiState())
    val state: StateFlow<SantriListUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load(debounce = false)
    }

    fun onQuery(query: String) {
        _state.update { it.copy(query = query) }
        load(debounce = true)
    }

    fun retry() = load(debounce = false)

    private fun load(debounce: Boolean) {
        job?.cancel()
        _state.update { it.copy(load = Load.Loading) }
        val query = _state.value.query
        job = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            val outcome = attempt { repo.santri(query.takeIf { searchable }) }
            _state.update { it.copy(load = outcome.toLoad()) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

const val RIWAYAT_HARI = 30L

data class SantriDetailUiState(
    val canEdit: Boolean,
    /** false untuk PIKET murni: server menolak GET /absensi-santri (403), jadi riwayat tidak dimuat/ditampilkan. */
    val canViewRiwayat: Boolean = true,
    val santri: Load<Santri> = Load.Loading,
    val riwayat: Load<List<AbsensiEntry>> = Load.Loading,
    /** null = belum diubah pengguna. */
    val jilidEdit: Jilid? = null,
    val catatanEdit: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
)

val SantriDetailUiState.jilidSelected: Jilid? get() = jilidEdit ?: (santri as? Load.Success)?.data?.jilidSaatIni

val SantriDetailUiState.catatanText: String get() = catatanEdit ?: (santri as? Load.Success)?.data?.catatanProgressMadin.orEmpty()

val SantriDetailUiState.hasChanges: Boolean
    get() {
        val s = (santri as? Load.Success)?.data ?: return false
        return (jilidEdit != null && jilidEdit != s.jilidSaatIni) || (catatanEdit != null && catatanEdit.trim() != s.catatanProgressMadin.orEmpty())
    }

class SantriDetailViewModel(
    private val repo: TpqRepository,
    private val santriId: String,
    canEdit: Boolean,
    canViewRiwayat: Boolean = true,
) : ViewModel() {
    private val _state = MutableStateFlow(SantriDetailUiState(canEdit, canViewRiwayat, riwayat = if (canViewRiwayat) Load.Loading else Load.Success(emptyList())))
    val state: StateFlow<SantriDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(santri = Load.Loading, riwayat = if (it.canViewRiwayat) Load.Loading else it.riwayat) }
        // Dua permintaan terpisah supaya progress tetap tampil walau riwayat gagal (dan sebaliknya).
        viewModelScope.launch {
            val outcome = attempt { repo.santriDetail(santriId) }
            _state.update { it.copy(santri = outcome.toLoad()) }
        }
        if (!_state.value.canViewRiwayat) return
        viewModelScope.launch {
            val today = Tanggal.today()
            val outcome = attempt {
                repo.absensiSantriRiwayat(santriId, Tanggal.toApi(today.minusDays(RIWAYAT_HARI)), Tanggal.toApi(today))
                    .sortedByDescending { it.tanggal }
            }
            _state.update { it.copy(riwayat = outcome.toLoad()) }
        }
    }

    fun onJilid(jilid: Jilid) = _state.update { it.copy(jilidEdit = jilid, saved = false, saveError = null) }

    fun onCatatan(text: String) = _state.update { it.copy(catatanEdit = text, saved = false, saveError = null) }

    fun save() {
        val s = _state.value
        val current = (s.santri as? Load.Success)?.data ?: return
        if (!s.canEdit || s.saving || !s.hasChanges) return
        val update = ProgressUpdate(
            jilid = s.jilidEdit?.takeIf { it != current.jilidSaatIni },
            updateCatatan = s.catatanEdit != null && s.catatanEdit.trim() != current.catatanProgressMadin.orEmpty(),
            catatan = s.catatanEdit?.trim()?.ifEmpty { null },
        )
        if (update.isEmpty) return
        _state.update { it.copy(saving = true, saveError = null, saved = false) }
        viewModelScope.launch {
            when (val outcome = attempt { repo.updateProgress(santriId, update) }) {
                is Outcome.Ok -> _state.update {
                    it.copy(santri = Load.Success(outcome.value), jilidEdit = null, catatanEdit = null, saving = false, saved = true)
                }
                is Outcome.Err -> _state.update { it.copy(saving = false, saveError = outcome.error.message) }
            }
        }
    }
}
